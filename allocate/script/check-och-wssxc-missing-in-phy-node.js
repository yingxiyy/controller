// Usage:
//   mongo --host localhost:27010 -u dci -p '***' --authenticationDatabase admin sotn check-och-wssxc-missing-in-phy-node.js
//
// Default is read-only check:
//   Find WSS XCs referenced by OCH link route snapshots, then verify the same XC
//   exists in the corresponding config-phy-node document.
//
// Repair mode:
//   Set UPDATE_DB = true to copy only the missing XC object from config-och-link
//   into the matching config-phy-node cross-connections array.

if (typeof PRINT_OK === "undefined") {
    var PRINT_OK = false;
}
if (typeof MAX_PRINT === "undefined") {
    var MAX_PRINT = 0; // 0 means no limit
}
if (typeof UPDATE_DB === "undefined") {
    var UPDATE_DB = false;
}

var ochCol = db.getCollection("config-och-link");
var phyCol = db.getCollection("config-phy-node");

function isObj(v) {
    return v !== null && typeof v === "object";
}

function scalar(v) {
    if (v === null || v === undefined) {
        return null;
    }
    if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") {
        return "" + v;
    }
    if (isObj(v) && v._value !== undefined) {
        return scalar(v._value);
    }
    return null;
}

function keyValue(obj, names) {
    if (!isObj(obj)) {
        return null;
    }
    for (var i = 0; i < names.length; i++) {
        if (obj[names[i]] !== undefined) {
            return obj[names[i]];
        }
    }
    var keys = Object.keys(obj);
    for (var k = 0; k < keys.length; k++) {
        for (var n = 0; n < names.length; n++) {
            if (keys[k].endsWith(":" + names[n])) {
                return obj[keys[k]];
            }
        }
    }
    return null;
}

function firstScalarByKey(root, names) {
    var found = null;
    function walk(v) {
        if (found !== null || !isObj(v)) {
            return;
        }
        var kv = keyValue(v, names);
        var sv = scalar(kv);
        if (sv !== null) {
            found = sv;
            return;
        }
        if (Array.isArray(v)) {
            for (var i = 0; i < v.length; i++) {
                walk(v[i]);
                if (found !== null) {
                    return;
                }
            }
            return;
        }
        var keys = Object.keys(v);
        for (var k = 0; k < keys.length; k++) {
            walk(v[keys[k]]);
            if (found !== null) {
                return;
            }
        }
    }
    walk(root);
    return found;
}

function hasWssChannel(obj) {
    return keyValue(obj, ["wss-channel", "wssChannel"]) !== null;
}

function collectWssXcs(root) {
    var list = [];
    var seen = {};
    function walk(v) {
        if (!isObj(v)) {
            return;
        }
        var id = scalar(keyValue(v, ["cross-connection-id", "crossConnectionId"]));
        if (id !== null && hasWssChannel(v) && !seen[id]) {
            seen[id] = true;
            list.push({
                xcId: id,
                sourceXc: clone(v),
                description: scalar(keyValue(v, ["description"])),
                lowerFrequency: scalar(keyValue(keyValue(v, ["wss-channel", "wssChannel"]), ["lower-frequency", "lowerFrequency"])),
                upperFrequency: scalar(keyValue(keyValue(v, ["wss-channel", "wssChannel"]), ["upper-frequency", "upperFrequency"]))
            });
        }
        if (Array.isArray(v)) {
            for (var i = 0; i < v.length; i++) {
                walk(v[i]);
            }
            return;
        }
        var keys = Object.keys(v);
        for (var k = 0; k < keys.length; k++) {
            walk(v[keys[k]]);
        }
    }
    walk(root);
    return list;
}

function clone(v) {
    return JSON.parse(JSON.stringify(v));
}

function collectAllXcIds(root) {
    var ids = {};
    function walk(v) {
        if (!isObj(v)) {
            return;
        }
        var id = scalar(keyValue(v, ["cross-connection-id", "crossConnectionId"]));
        if (id !== null) {
            ids[id] = true;
        }
        if (Array.isArray(v)) {
            for (var i = 0; i < v.length; i++) {
                walk(v[i]);
            }
            return;
        }
        var keys = Object.keys(v);
        for (var k = 0; k < keys.length; k++) {
            walk(v[keys[k]]);
        }
    }
    walk(root);
    return ids;
}

function nodeIdFromXcId(xcId) {
    var m = /(?:^|-)XC-(Site-[^#]+#Ne-[^#]+)/.exec(xcId);
    if (m !== null) {
        return m[1];
    }
    m = /(?:^|-)ASEXC-(Site-[^#]+#Ne-[^#]+)/.exec(xcId);
    if (m !== null) {
        return m[1];
    }
    m = /(Site-[^#]+#Ne-[^#]+)/.exec(xcId);
    return m === null ? null : m[1];
}

function findXcArrayPath(root) {
    var found = null;
    function walk(v, path) {
        if (found !== null || !isObj(v)) {
            return;
        }
        if (Array.isArray(v)) {
            for (var i = 0; i < v.length; i++) {
                walk(v[i], path.concat(i));
                if (found !== null) {
                    return;
                }
            }
            return;
        }
        var keys = Object.keys(v);
        for (var k = 0; k < keys.length; k++) {
            var key = keys[k];
            if ((key === "cross-connections" || key.endsWith(":cross-connections"))
                    && Array.isArray(v[key])) {
                found = path.concat(key).join(".");
                return;
            }
            walk(v[key], path.concat(key));
            if (found !== null) {
                return;
            }
        }
    }
    walk(root, []);
    return found;
}

var phyCache = {};
function getPhyInfo(neId) {
    if (phyCache[neId] !== undefined) {
        return phyCache[neId];
    }
    var doc = phyCol.findOne({neId: neId});
    if (doc === null) {
        doc = phyCol.findOne({"data.node.node-id": neId});
    }
    if (doc === null) {
        doc = phyCol.findOne({"data.node.node-id._value": neId});
    }
    var info = {
        exists: doc !== null,
        id: doc === null ? null : doc._id,
        xcArrayPath: doc === null ? null : findXcArrayPath(doc),
        xcIds: doc === null ? {} : collectAllXcIds(doc)
    };
    phyCache[neId] = info;
    return info;
}

function repairMissingXc(neId, xcId, sourceXc) {
    var phy = getPhyInfo(neId);
    if (!phy.exists) {
        return {updated: false, reason: "missing_config_phy_node"};
    }
    if (phy.xcIds[xcId]) {
        return {updated: false, reason: "already_exists"};
    }
    if (phy.xcArrayPath === null) {
        return {updated: false, reason: "missing_cross_connections_array"};
    }

    var update = {$push: {}};
    update.$push[phy.xcArrayPath] = clone(sourceXc);
    var result = phyCol.updateOne({_id: phy.id}, update);
    if (result.matchedCount === 1 && result.modifiedCount === 1) {
        phy.xcIds[xcId] = true;
        return {updated: true, reason: "copied_xc_to_config_phy_node", path: phy.xcArrayPath};
    }
    return {updated: false, reason: "update_failed", path: phy.xcArrayPath, result: result};
}

var checkedOch = 0;
var checkedWssXc = 0;
var missing = [];
var repaired = [];
var repairSkipped = [];

ochCol.find({}).forEach(function(ochDoc) {
    var implementState = firstScalarByKey(ochDoc, ["implement-state", "implementState"]);

    var ochLinkId = scalar(ochDoc.ochLinkId) || firstScalarByKey(ochDoc, ["link-id", "linkId"]);
    var friendlyName = firstScalarByKey(ochDoc, ["friendly-name", "friendlyName"]);
    var wssXcs = collectWssXcs(ochDoc);
    if (wssXcs.length === 0) {
        return;
    }

    checkedOch++;
    for (var i = 0; i < wssXcs.length; i++) {
        var xc = wssXcs[i];
        var neId = nodeIdFromXcId(xc.xcId);
        checkedWssXc++;
        if (neId === null) {
            missing.push({
                reason: "cannot_parse_ne_id",
                ochLinkId: ochLinkId,
                friendlyName: friendlyName,
                implementState: implementState,
                xcId: xc.xcId,
                description: xc.description,
                lowerFrequency: xc.lowerFrequency,
                upperFrequency: xc.upperFrequency
            });
            continue;
        }

        var phy = getPhyInfo(neId);
        if (!phy.exists || !phy.xcIds[xc.xcId]) {
            var missingRecord = {
                reason: phy.exists ? "missing_xc_in_config_phy_node" : "missing_config_phy_node",
                ochLinkId: ochLinkId,
                friendlyName: friendlyName,
                implementState: implementState,
                neId: neId,
                xcId: xc.xcId,
                description: xc.description,
                lowerFrequency: xc.lowerFrequency,
                upperFrequency: xc.upperFrequency
            };
            missing.push(missingRecord);

            if (UPDATE_DB) {
                var repairResult = repairMissingXc(neId, xc.xcId, xc.sourceXc);
                if (repairResult.updated) {
                    repaired.push(Object.assign({repairPath: repairResult.path}, missingRecord));
                } else {
                    repairSkipped.push(Object.assign({
                        repairReason: repairResult.reason,
                        repairPath: repairResult.path
                    }, missingRecord));
                }
            }
        } else if (PRINT_OK) {
            printjson({
                result: "ok",
                ochLinkId: ochLinkId,
                neId: neId,
                xcId: xc.xcId
            });
        }
    }
});

print("checkedOchWithWssXc=" + checkedOch
        + ", checkedWssXc=" + checkedWssXc
        + ", missing=" + missing.length
        + ", updateDb=" + UPDATE_DB
        + ", repaired=" + repaired.length
        + ", repairSkipped=" + repairSkipped.length);

for (var i = 0; i < missing.length; i++) {
    if (MAX_PRINT > 0 && i >= MAX_PRINT) {
        print("skip printing remaining " + (missing.length - i) + " missing records");
        break;
    }
    printjson(missing[i]);
}

if (UPDATE_DB) {
    for (var r = 0; r < repaired.length; r++) {
        printjson(Object.assign({repair: "updated"}, repaired[r]));
    }
    for (var s = 0; s < repairSkipped.length; s++) {
        printjson(Object.assign({repair: "skipped"}, repairSkipped[s]));
    }
}

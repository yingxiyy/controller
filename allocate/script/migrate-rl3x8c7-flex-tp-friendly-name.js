/*
 * Migrates RL3X8C7_FLEX termination-point friendly names to the physical-port
 * naming rule introduced in 2606-release.
 *
 * Preview:
 *   mongo <connection options> <database> migrate-rl3x8c7-flex-tp-friendly-name.js
 *
 * Apply:
 *   mongo <connection options> <database> \
 *     --eval "var APPLY_CHANGES=true" migrate-rl3x8c7-flex-tp-friendly-name.js
 *
 * The script is idempotent. It only changes old names ending in Cn(port) or
 * Ln(port), and verifies that "port" matches the physical port in tp-id.
 */

if (typeof APPLY_CHANGES === "undefined") {
    APPLY_CHANGES = false;
}

var COLLECTION_NAME = "config-phy-node";
var CARD_TYPE = "RL3X8C7_FLEX";
var PHYSICAL_KEY = "otn-phy-topology:physical";
var collection = db.getCollection(COLLECTION_NAME);
var query = {};
query["data.node." + PHYSICAL_KEY + ".equipments.equip-type-configed"] = CARD_TYPE;

var stats = {
    documentsMatched: 0,
    documentsChanged: 0,
    cardsMatched: 0,
    terminationPointsExamined: 0,
    terminationPointsChanged: 0,
    invalidOldNames: 0,
    concurrentChanges: 0
};

function getPhysicalPort(tpId) {
    var match = typeof tpId === "string"
        ? tpId.match(/#PORT-\d+-\d+-(\d+)$/)
        : null;
    return match ? match[1] : null;
}

function getMigratedFriendlyName(tp) {
    var physical = tp[PHYSICAL_KEY];
    if (!physical || typeof physical["friendly-name"] !== "string") {
        return null;
    }

    var oldName = physical["friendly-name"];
    var oldNameMatch = oldName.match(/^(.*-)([CL])\d+\((\d+)\)$/);
    if (!oldNameMatch) {
        return null;
    }

    var physicalPort = getPhysicalPort(tp["tp-id"]);
    if (physicalPort === null || physicalPort !== oldNameMatch[3]) {
        stats.invalidOldNames++;
        print("[SKIP] tp-id and friendly-name physical ports differ: tp-id="
            + tp["tp-id"] + ", friendly-name=" + oldName);
        return null;
    }

    return oldNameMatch[1] + oldNameMatch[2] + physicalPort;
}

collection.find(query).forEach(function (document) {
    stats.documentsMatched++;

    var nodes = document.data && document.data.node ? document.data.node : [];
    var setOperations = {};
    var oldValueGuards = {};

    nodes.forEach(function (node, nodeIndex) {
        var nodePhysical = node[PHYSICAL_KEY] || {};
        var equipmentIds = {};

        (nodePhysical.equipments || []).forEach(function (equipment) {
            if (equipment["equip-type-configed"] === CARD_TYPE) {
                equipmentIds[equipment["equipment-id"]] = true;
                stats.cardsMatched++;
            }
        });

        (node["termination-point"] || []).forEach(function (tp, tpIndex) {
            var tpPhysical = tp[PHYSICAL_KEY] || {};
            if (!equipmentIds[tpPhysical["equipment-ref"]]) {
                return;
            }

            stats.terminationPointsExamined++;
            var newName = getMigratedFriendlyName(tp);
            if (newName === null || newName === tpPhysical["friendly-name"]) {
                return;
            }

            var path = "data.node." + nodeIndex + ".termination-point." + tpIndex
                + "." + PHYSICAL_KEY + ".friendly-name";
            setOperations[path] = newName;
            oldValueGuards[path] = tpPhysical["friendly-name"];
            stats.terminationPointsChanged++;

            print("[CHANGE] " + tp["tp-id"] + ": "
                + tpPhysical["friendly-name"] + " -> " + newName);
        });
    });

    if (Object.keys(setOperations).length === 0) {
        return;
    }

    stats.documentsChanged++;
    if (!APPLY_CHANGES) {
        return;
    }

    // Guard every old value so a concurrent update prevents the whole document update.
    var guardedQuery = {_id: document._id};
    Object.keys(oldValueGuards).forEach(function (path) {
        guardedQuery[path] = oldValueGuards[path];
    });

    var result = collection.updateOne(guardedQuery, {$set: setOperations});
    var matchedCount = result.matchedCount !== undefined
        ? result.matchedCount
        : result.nMatched;
    if (matchedCount !== 1) {
        stats.concurrentChanges++;
        print("[SKIP] document changed concurrently: neId=" + document.neId);
    }
});

printjson({
    mode: APPLY_CHANGES ? "APPLY" : "DRY_RUN",
    collection: COLLECTION_NAME,
    cardType: CARD_TYPE,
    statistics: stats
});

if (!APPLY_CHANGES) {
    print("Dry-run only. Re-run with APPLY_CHANGES=true after reviewing the changes.");
}

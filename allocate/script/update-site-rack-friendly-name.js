cat > updateRackFriendlyName.js << 'EOF'
/*
 * 修改指定 SiteNode 中一个 rack 的 friendly-name。
 *
 * 使用前修改 SITE_NODE_ID、OLD_FRIENDLY_NAME、NEW_FRIENDLY_NAME。
 *
 * 预览：
 *   mongo <连接参数> <数据库> update-site-rack-friendly-name.js
 *
 * 执行：
 *   mongo <连接参数> <数据库> \
 *     --eval "var APPLY_CHANGES=true" update-site-rack-friendly-name.js
 */

if (typeof APPLY_CHANGES === "undefined") {
    APPLY_CHANGES = false;
}

if (typeof SITE_NODE_ID === "undefined") {
    SITE_NODE_ID = "Site-2052327206834802688";
}
if (typeof OLD_FRIENDLY_NAME === "undefined") {
    OLD_FRIENDLY_NAME = "包河大道-六安徐集-华北-华东-C平面-路由一-(3--2)";
}
if (typeof NEW_FRIENDLY_NAME === "undefined") {
    NEW_FRIENDLY_NAME = "包河大道-六安徐集-华北-华东-D平面-路由一-(3--2)";
}

var COLLECTION_NAME = "config-site-node";
var SITE_KEY = "site-topology:site";
var collection = db.getCollection(COLLECTION_NAME);

function fail(message) {
    throw new Error(message);
}

if (SITE_NODE_ID.indexOf("REPLACE_WITH_") === 0
        || OLD_FRIENDLY_NAME.indexOf("REPLACE_WITH_") === 0
        || NEW_FRIENDLY_NAME.indexOf("REPLACE_WITH_") === 0) {
    fail("请先修改脚本顶部的 SITE_NODE_ID、OLD_FRIENDLY_NAME 和 NEW_FRIENDLY_NAME");
}

if (OLD_FRIENDLY_NAME === NEW_FRIENDLY_NAME) {
    fail("原 friendly-name 和新 friendly-name 不能相同");
}

var siteNodeDocument = collection.findOne({siteId: SITE_NODE_ID});
if (siteNodeDocument === null) {
    fail("未找到 SiteNode: " + SITE_NODE_ID);
}

var matches = [];
var nodes = siteNodeDocument.data && siteNodeDocument.data.node
    ? siteNodeDocument.data.node
    : [];

nodes.forEach(function (node, nodeIndex) {
    var site = node[SITE_KEY] || {};
    var racks = site["supporting-rack"] || [];

    racks.forEach(function (rack, rackIndex) {
        if (rack["friendly-name"] === OLD_FRIENDLY_NAME) {
            matches.push({
                nodeIndex: nodeIndex,
                rackIndex: rackIndex,
                rackId: rack["rack-id"]
            });
        }
    });
});

if (matches.length !== 1) {
    fail("SiteNode " + SITE_NODE_ID + " 中 friendly-name='" + OLD_FRIENDLY_NAME
        + "' 的 rack 数量必须为1，实际为 " + matches.length);
}

var match = matches[0];
var friendlyNamePath = "data.node." + match.nodeIndex + "." + SITE_KEY
    + ".supporting-rack." + match.rackIndex + ".friendly-name";

printjson({
    mode: APPLY_CHANGES ? "APPLY" : "DRY_RUN",
    collection: COLLECTION_NAME,
    siteNodeId: SITE_NODE_ID,
    rackId: match.rackId,
    oldFriendlyName: OLD_FRIENDLY_NAME,
    newFriendlyName: NEW_FRIENDLY_NAME,
    updatePath: friendlyNamePath
});

if (!APPLY_CHANGES) {
    print("Dry-run only. 确认输出后，使用 APPLY_CHANGES=true 再次执行。");
} else {
    // 使用原值作为更新条件，防止预览后其他操作已经修改了同一个 rack。
    var guardedQuery = {_id: siteNodeDocument._id, siteId: SITE_NODE_ID};
    guardedQuery[friendlyNamePath] = OLD_FRIENDLY_NAME;

    var setOperation = {};
    setOperation[friendlyNamePath] = NEW_FRIENDLY_NAME;

    var result = collection.updateOne(guardedQuery, {$set: setOperation});
    var matchedCount = result.matchedCount !== undefined ? result.matchedCount : result.nMatched;
    var modifiedCount = result.modifiedCount !== undefined ? result.modifiedCount : result.nModified;

    if (matchedCount !== 1 || modifiedCount !== 1) {
        fail("更新失败，数据可能已被并发修改。matched=" + matchedCount
            + ", modified=" + modifiedCount);
    }

    var verifiedDocument = collection.findOne({_id: siteNodeDocument._id});
    var verifiedValue = verifiedDocument.data.node[match.nodeIndex][SITE_KEY]
        ["supporting-rack"][match.rackIndex]["friendly-name"];
    if (verifiedValue !== NEW_FRIENDLY_NAME) {
        fail("更新后校验失败，当前 friendly-name=" + verifiedValue);
    }

    print("更新成功: " + OLD_FRIENDLY_NAME + " -> " + NEW_FRIENDLY_NAME);
}
EOF
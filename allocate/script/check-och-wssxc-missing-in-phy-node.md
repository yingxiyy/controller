# 检查 OCH 路由中的 WSS XC 是否缺失于 config-phy-node

这个脚本默认只读数据库，不会修改 MongoDB。它用于检查 `config-och-link`
路由快照中引用的 WSS XC，是否也存在于对应网元的 `config-phy-node` 文档中。

如果显式开启 `UPDATE_DB=true`，脚本会把缺失的 WSS XC 从 `config-och-link`
中的 OCH 路由快照复制到对应网元的 `config-phy-node`。修复时只复制缺失的
XC 对象本身，不修改其他字段。

## 文件

`check-och-wssxc-missing-in-phy-node.js`

## 使用方式

在脚本所在目录执行：

```bash
mongo --host localhost:27010 -u dci -p '***' --authenticationDatabase admin sotn check-och-wssxc-missing-in-phy-node.js
```

如果是在其他实验室环境执行，需要把 host、用户名、密码、认证库和 Mongo
数据库名替换为该环境运行时的实际配置。

如果需要执行修复，显式传入 `UPDATE_DB=true`：

```bash
mongosh --host localhost:27010 -u dci -p '***' --authenticationDatabase admin sotn --eval 'UPDATE_DB=true; load("check-och-wssxc-missing-in-phy-node.js")'
```

建议先只读执行一次，确认输出符合预期后，再开启 `UPDATE_DB=true`。

## 检查内容

1. 扫描 `config-och-link`。
2. 递归查找所有带 `wss-channel` 的 cross-connection 对象。
3. 从 WSS XC ID 中解析 NE ID。
4. 到 `config-phy-node` 中查找对应网元文档。
5. 如果网元文档不存在，或者该网元中缺少对应 XC，则打印缺失记录。

脚本兼容常见的 YANG 包装结构，例如 `data.link`、`data.node`、
带 namespace 前缀的字段，以及 `_value` 形式的标量包装。

开启 `UPDATE_DB=true` 后，脚本会：

1. 对每条缺失的 WSS XC，取 `config-och-link` 中保存的原始 XC 对象。
2. 定位对应 `config-phy-node` 文档中的 `cross-connections` 数组。
3. 使用 `$push` 只插入这个缺失 XC。
4. 不创建网元文档，不创建新的资源结构，不修改已有 XC 或其他资源。

## 输出

汇总行：

```text
checkedOchWithWssXc=<n>, checkedWssXc=<n>, missing=<n>
```

每条缺失记录会以 JSON 形式打印，主要字段包括：

- `reason`：`missing_xc_in_config_phy_node`、`missing_config_phy_node` 或 `cannot_parse_ne_id`
- `ochLinkId`
- `friendlyName`
- `implementState`
- `neId`
- `xcId`
- `description`
- `lowerFrequency`
- `upperFrequency`

开启修复模式后，汇总行还会包含：

```text
updateDb=<true|false>, repaired=<n>, repairSkipped=<n>
```

并打印修复结果：

- `repair: "updated"`：已经把缺失 XC 插入 `config-phy-node`。
- `repair: "skipped"`：没有修复，原因在 `repairReason` 中。

## 选项

脚本顶部有以下参数：

```js
var PRINT_OK = false;
var MAX_PRINT = 0;
var UPDATE_DB = false;
```

- `PRINT_OK = true`：除了缺失记录，也打印匹配成功的 XC 记录。
- `MAX_PRINT > 0`：限制最多打印多少条缺失记录。
- `UPDATE_DB = true`：把缺失的 WSS XC 复制到对应 `config-phy-node`。

这些参数也可以通过 `mongosh --eval` 传入，例如：

```bash
mongosh --host localhost:27010 -u dci -p '***' --authenticationDatabase admin sotn --eval 'UPDATE_DB=true; load("check-och-wssxc-missing-in-phy-node.js")'
```

## 备注

默认模式只用于数据诊断。修复模式只补齐缺失的 WSS XC 数据，不能替代根因分析。

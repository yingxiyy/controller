RL3X8C7_FLEX 命名规则的变迁，
原来     现在
C1(2) → C2
C3(4) → C4
L2(5) → L5
L4(10) → L10

脚本行为：
- 扫描全部 RL3X8C7_FLEX 板卡。
- 只修改对应 TP 的 friendly-name。
- 按物理端口转换，例如 C1(2) → C2、L1(5) → L5。
- 校验括号内端口与 tp-id 端口一致。
- 默认 dry-run，显式设置 APPLY_CHANGES=true 才写入。
- 使用旧值作为更新条件，避免覆盖并发变化。
- 可重复执行。

mongo <连接参数> sotn --eval "var APPLY_CHANGES=true" `  ./migrate-rl3x8c7-flex-tp-friendly-name.js
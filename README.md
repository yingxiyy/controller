### sdn controller

作为分布式网络管理系统，其主要的模块如下：

1. adapterMgr netconf 适配器管理模块
2. allocate 资源计算模块
3. gateway 用于给前端提供restful 接口模块
4. ne-designer 网元资源计算模块，用于辅佐业务的下发计算
5. implement 业务下发模块
6. neMgr 网元管理模块
7. notifier 处理通知模块
8. pmc-monitor 进程监听模块
9. sftpserver sftp server 管理模块
10. status 状态变化管理模块
11. task 任务模块
12. telemetryMgr telemetry管理模块

公共组件用于支持以上模块的组件 tools，其主要的结构如下：

1. batchlock 同步锁
2. kafka kafka 组件
4. route 路由计算组件
5. webapp web基本配置组件
6. websocket webscoket 组件
7. rpc-client rpc调用组件
8. db-mysql mysql数据库组件

端口设置：

gateway 18181

pmc-monitor 18000

adapterMgr 18001

neMgr 18002

notifier 18003

status 18004

telemetryMgr 18005

allocate 18006

implement 18007

telemetryServer 18020

pmCollector 18021

pmQuery 18022

task 18030

sftpserver 18031

liftcycle 18032

### 设备类型定义

TD -> Transponder device 电设备

OD -> optical device 光设备

### 模型定义 yang version

yangVersion only existed when the NE synced

for 1.0 model, the yangVersoin will be 1.33/1.35/1.36/1.37

for 2.0 model, the yangVersion will be dx_2.0
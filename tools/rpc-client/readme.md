### rpc client

rpc client是封装了所有的http rpc调用的组件，根据目前各模块之间的关系，现已经集成的rpc。 使用方法在pom文件中引入该模块，然后在需要的地方用spring的IOC注入即可使用

#### AdapterManager Rpc

对于adapter的管理的rpc

rpc 命令：

1. create-adapter
2. delete-adapter

#### Adapter Rpc

向adapter下发的操作的rpc

rpc 命令：

1. get-managed-nes
2. connect-ne
3. get-ne-data
4. compare-ne

#### Alarm Rpc

alarm的rpc

rpc 命令：

1. generate-alarm
2. clear-alarm
3. get-current-alarms

#### CollectorManager RPC

telemetry server collector管理的rpc

rpc 命令：

1. create-telemetry-server

2. delete-telemetry-server

#### Ne manager RPC

ne 操作rpc

rpc命令：

1. manage-ne
2. registe-ne
3. config-ne
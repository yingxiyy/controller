#### alarm-notifier send message manager

##### 概述

alarm 接收并发送至运维人员的组件，运维人员选择自己订阅的alarm的主题，能通过邮件等第三方消息平台实时接收到，网元设备的实时告警信息，并针对告警信息并进行相关的处理

##### 模块简述

1. an-cache

缓存模块，主要缓存相关网络设备的基本信息

2. an-common

公共模块，提供各个模块之间所需要的基础组件信息

3. an-rest

对外提供接口，提供运维人员管理的接口

4. an-subscribe

提供支持，alarm主题订阅，取消的功能

5. an-mail

消息发送模块，当前支持email发送

6. an-translator

alarm消息中告警信息翻译模块
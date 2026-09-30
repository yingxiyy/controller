### alarm 订阅接口定义

本文档用于描述alarm 订阅功能的http api的描述


#### 基本response结构定义

response base model：

    message: 返回的消息 返回接口当前的成功和错误信息描述

    code： 状态码 200成功

    content： 主要内容结构


#### 获取订阅alarm主题

 定义关于alarm主题的展示和订阅alarm 主题功能的http 接口

1. 获取所有alarm topic


    接口： /alarm/topic

    method: get

    response:

    返回list列表

        severity: 告警级别

        alarmGroup: 告警分组

        alarmTypeId: 告警归类

        alarmText: 告警信息

        subscribe： 是否订阅 boolean true/false

        id: topic id (不显示)

        boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX

2. 根据板卡告警类型name获取对应的所有topic信息

   
   接口： /alarm/topic/group/name/{name}/topics

   method: get

   request: name 板卡类型名（OA OP OTU ILA CMUX）

   respnose:

        返回list列表

        severity: 告警级别

        alarmGroup: 告警分组

        alarmTypeId: 告警归类

        alarmText: 告警信息

        subscribe： 是否订阅 boolean true/false

        id: topic id (不显示)

        boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX


3. 根据板卡告警类型id获取对应的所有topic信息

   接口： /alarm/topic/group/id/{id}/topics

   method: get

   request: id 板卡告警信息类型id  1.OA 2.OP 3.OTU 4.ILA 5.CMUX

   response:

        返回list列表

        severity: 告警级别

        alarmGroup: 告警分组

        alarmTypeId: 告警归类

        alarmText: 告警信息

        subscribe： 是否订阅 boolean true/false

        id: topic id (不显示)

        boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX


4. 获取所有已经订阅的alarm topic信息

   接口： /alarm/topic/subscribed-topics

   method: get

   response:

   返回所有已经订阅的topic消息 

        返回list列表

        severity: 告警级别

        alarmGroup: 告警分组

        alarmTypeId: 告警归类

        alarmText: 告警信息

        subscribe： 是否订阅 boolean true/false

        id: topic id (不显示)

        boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX

5. 根据板卡信息分类返回所有topic

   接口： /alarm/topic/byGroup

   method: get

   response: 

        alarm-topic-group:

            group-name: 板卡类型

            alarm-topics: 告警主题

               返回list列表

                severity: 告警级别

                alarmGroup: 告警分组

                alarmTypeId: 告警归类

                alarmText: 告警信息

                subscribe： 是否订阅 boolean true/false

                id: topic id (不显示)

                boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX

6. 订阅alarm topic主题

   接口： /alarm/topic

   method: put

   request: 
    
        alarm-topics: list列表

            severity: 告警级别

                alarmGroup: 告警分组

                alarmTypeId: 告警归类

                alarmText: 告警信息

                subscribe： 是否订阅 boolean true/false

                id: topic id (不显示)

                boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX
            
   response:
      
   成功返回 response 
         
         code 200 
           
          essage success

          content：返回修改状态的topic的内容信息和所在的组

               alarm-topic-group:

                group-name: 板卡类型

                alarm-topics: 告警主题

                返回list列表

                    severity: 告警级别

                    alarmGroup: 告警分组

                    alarmTypeId: 告警归类

                    alarmText: 告警信息

                    subscribe： 是否订阅 boolean true/false

                    id: topic id (不显示)

                    boardAlarmType: 板卡告警类别 1.OA 2.OP 3.OTU 4.ILA 5.CMUX

#### 邮箱配置
定义关于邮件收信人的http 接口

1. 获取所有邮件收信人

    接口： /alarm/mail/getRecipients

    method: get

    response example:
    
 ```json
 {
      "code": 200,
      "message": "success",
      "content": [
          "stu@demo.com"
      ]
  } 
```
 

2. 增加，删除，修改所有邮件收信人

    接口： /alarm/mail/updateRecipients

    method: post

    response example:
 ```json
{
    "code": 200,
    "message": "success",
    "content": null
}
```

3. 修改邮件服务器配置

      接口： /alarm/mail/updateConfigure
  
      method: post
  
      input example:
 ```json
{
    "smtpHost": "n3",
    "smtpPort": 25,
    "enableSSL": true,
    "user": "stu@demo.com",
    "password": "123456"
}
```
  output example:    
 ```json
  {
      "code": 200,
      "message": "success",
      "content": null
  }
  ``` 
4. 获取邮件服务器配置
   接口： /alarm/mail/getConfigure
   
       method: get
   
   
   output example:    
 ```json
{
    "code": 200,
    "message": "success",
    "content": {
        "smtpHost": "n3",
        "smtpPort": 25,
        "enableSSL": true,
        "user": "stu@demo.com",
        "password": "123456"
    }
}
 ``` 
或者
```json
{
    "code": 200,
    "message": "success",
    "content": null
}
```
5. 发送测试邮件
   接口： /alarm/mail/check
   
       method: get
   
   
   output example:  
 ```json
{
    "code": 200,
    "message": "success",
    "content": "测试邮件发送成功，请用户:stu@demo.com检查是否收到。"
}
```
或者
```json
{
    "code": 500,
    "message": "failed",
    "content": "测试邮件发送失败，请检查日志"
}
```
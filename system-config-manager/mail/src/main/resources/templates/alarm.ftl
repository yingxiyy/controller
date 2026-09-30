<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="description" content="email code">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>新告警</title>
</head>
<body>
<div style="background-color:#ECECEC; padding: 35px;">
<table cellpadding="0" align="center"
style="width: 800px;height: 100%; margin: 0px auto; text-align: left; position: relative; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 5px; border-bottom-left-radius: 5px; font-size: 14px; font-family:微软雅黑, 黑体; line-height: 1.5; box-shadow: rgb(153, 153, 153) 0px 0px 5px; border-collapse: collapse; background-position: initial initial; background-repeat: initial initial;background:#fff;">
<tbody>
    <tr>
        <th valign="middle"
            style="height: 25px; line-height: 25px; padding: 15px 35px; border-bottom-width: 1px; border-bottom-style: solid; border-bottom-color: #205aa6; background-color: #205aa6; border-top-left-radius: 5px; border-top-right-radius: 5px; border-bottom-right-radius: 0px; border-bottom-left-radius: 0px;">
            <font face="微软雅黑" size="5" style="color: rgb(255, 255, 255); ">设备告警监控</font>
        </th>
    </tr>
    <tr>
        <td style="word-break:break-all">
            <div style="padding:25px 35px 40px; background-color:#fff;opacity:0.8;">

                <h2 style="margin: 5px 0px; ">
                    <font color="#333333" style="line-height: 20px; ">
                        <font style="line-height: 22px; " size="4">
                            设备告警${isClear?then('清除','产生')}：</font>
                    </font>
                </h2>
                <!-- 中文 -->
                <p>设备告警ID：${id!""}</p>
                <P>告警级别： ${severity!""}</P>
                <P>告警类型: ${typeId!""}</P>
                <P>告警原因: ${text!""}</P>
                <P>告警分组: ${group!""}</P>
                <P>告警告警源:  ${toopKey!""}</P>
                <P>设备IP: ${ip!""}</P>
                <P>设备${isClear?then('清除','产生')}时间: ${timeCreated!""}</P>
                <P>是否影响业务: ${serviceAffect?string('是','否')}</P>
                <P>故障模块: ${component!""!""}</P>

                <div style="width:100%;margin:0 auto;">
                    <div style="padding:10px 10px 0;border-top:1px solid #ccc;color:#747474;margin-bottom:20px;line-height:1.3em;font-size:12px;">
                        <p>dciworld 网管</p>
                        <br>
                        <p>此为系统邮件，请勿回复<br>
                            Please do not reply to this system email
                        </p>
                        <!--<p>©***</p>-->
                    </div>
                </div>
            </div>
        </td>
    </tr>
</tbody>
</table>
</div>



</body>
</html>

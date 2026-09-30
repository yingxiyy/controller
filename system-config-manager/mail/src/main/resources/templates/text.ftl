告警ID: ${id!""}
级别: ${severity!""}
类型: ${typeId!""}
原因: ${text!""}
分组: ${group!""}
告警源:  ${toopKey!""}
设备IP: ${ip!""}
设备产生时间: ${timeCreated!""}
资源:  ${component!""}
是否影响业务: ${serviceAffect?string('是','否')}
故障模块: ${component!""!""}
告警状态: ${isClear?then('产生','清除')}


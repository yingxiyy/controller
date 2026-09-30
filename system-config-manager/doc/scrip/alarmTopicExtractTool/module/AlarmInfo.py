#!/usr/bin/python
# alarm resource info 

class AlarmInfo:
    
    def __init__(self,alarm_group,severity,typeId,message,origin,description):
    
        self.alarm_group=alarm_group
        self.severity=severity
        self.type_id=typeId
        self.message=message
        self.orgin = origin
        self.description=description
    def displayAlarmInfo(self):
        
        print ("alarm info--resource:",self.resource,
               " component:",self.component,
               " alarm_group:",self.alarm_group,
               " severity:",self.severity,
               " type-id:",self.typeId,
               " message:",self.message,
               " description:",self.description)
    def __str__(self):
        return "alarm info-- alarm_group:"+self.alarm_group+" severity:"+self.severity+" type-id:"+self.typeId+" message:"+self.message
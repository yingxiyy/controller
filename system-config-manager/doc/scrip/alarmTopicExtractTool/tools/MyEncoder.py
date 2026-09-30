from email.policy import default
import json

from module.AlarmInfoTemplate import AlarmGroupTemplate
from module.AlarmTextTemplate import AlarmTextTemplate
from module.MessageTemplate import MessageTemplate


class MyEncoder(json.JSONEncoder):
    def default(self,obj):
        if isinstance(obj,set):
            return list(obj)
        if isinstance(obj,bytes):
            return str(obj,encoding='utf-8')
        if isinstance(obj,AlarmGroupTemplate):
            return {"name":obj.name,"template":tuple(obj.templates)}
        if isinstance(obj,AlarmTextTemplate):
            return obj.templateGroup
        if isinstance(obj,MessageTemplate):
            return {"message":obj.message,"origin":obj.originTemplate,"zh_template":obj.template}
        return json.JSONEncoder.default(self,obj)

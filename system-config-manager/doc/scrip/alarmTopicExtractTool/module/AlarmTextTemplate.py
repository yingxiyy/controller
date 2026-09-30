from typing import List

from module.AlarmInfoTemplate import AlarmGroupTemplate


class AlarmTextTemplate:
    
     templateGroup: List[AlarmGroupTemplate]
     
     def __init__(self,templateGroup) :
         self.templateGroup = templateGroup
    
#!/usr/bin/python
# alarm resource info 

from typing import List, Set
from module.MessageTemplate import MessageTemplate


class AlarmGroupTemplate:
    
    name: str
    
    templates: Set[MessageTemplate]
    
    
    def __init__(self,name,templates):
        self.name = name
        self.templates = templates
    
    


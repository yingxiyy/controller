#!/usr/bin/python
# alarm resource info 

from ast import Str


class MessageTemplate:
    
    message: str
    
    originTemplate:str
    
    template: str
    
    
    def __init__(self,message,originTemplate,template) :
        self.message=message
        self.originTemplate=originTemplate
        self.template=template
#!/usr/bin/python
# method for handle the open the excel to mysql database
from email import charset
from email.policy import default
from gettext import npgettext
import json
import logging
from typing import List
from dao.AlarmInfoDao import AlarmInfoDao
from dao.AlarmInfoGroupDao import AlarmInfoGroupDao
from module.AlarmInfoTemplate import AlarmGroupTemplate
from module.AlarmTextTemplate import AlarmTextTemplate
from module.MessageTemplate import MessageTemplate
import tools.ExcelHelper as ExcelHelper
from tools.Convertor import Convertor
from tools.MyEncoder import MyEncoder
from tools.MysqlHelper import MysqlHelper

class Excel2DbService:
    
    def __init__(self,path):
        self.book=ExcelHelper.ExcelHelper(path)
        self.alarmInfoDao = AlarmInfoDao.getInstance()
        self.alarmInfoGroupDao = AlarmInfoGroupDao.getInstance()
    
    def loadExcelData2Dict(self,name,index):
        alarmInfos=[]
        self.book.set_current_sheet(index)
        oaRowValues=self.book.row_values(3)
        for rowValues in oaRowValues:
            alarmInfo = Convertor.transfer2AlarmInfo(rowValues)
            alarmInfos.append(alarmInfo)
        alarmDic={name:alarmInfos}
        return alarmDic
    
    def loadExcelData2AlamrInfoDict(self,index):
        alarmInfos=[]
        self.book.set_current_sheet(index)
        oaRowValues=self.book.row_values(3)
        for rowValues in oaRowValues:
            alarmInfo = Convertor.transfer2AlarmInfo(rowValues)
            alarmInfos.append(alarmInfo)
        
        return alarmInfos
    
    def saveAlarmGroup2DB(self,alarmGroup):
        logging.info("start to save the alarm group info 2 db")
        for key in alarmGroup.keys():
            self.__saveAlarmGroup2DB(key,alarmGroup[key])
            
    def __saveAlarmGroup2DB(self,group,alarmInfos):
        logging.info("start to load group {} alarm info to db",group)
        rows=self.alarmInfoGroupDao.queryAlarmGroupTypeByName(group)
        for alarmInfo in alarmInfos:
            self.alarmInfoDao.insertAlarmInfo(alarmInfo,rows["id"])
    
    def write2json(self,alarmInfos):
        alarmGroupInfoMap = {}
        logging.info("start to write the akarm group info 2 json file")
        for alarmInfo in alarmInfos:
            group= alarmInfo.alarm_group
            messageTemplate = MessageTemplate(alarmInfo.message,alarmInfo.orgin,alarmInfo.description)
            if group in alarmGroupInfoMap.keys():
                templates=alarmGroupInfoMap.get(group)
                templates.add(messageTemplate)
            else:
                templates=set()
                templates.add(messageTemplate)
                alarmGroupInfoMap.update({group:templates})
        alarmInfos =list()
        for key in alarmGroupInfoMap :
            alarmgroup = AlarmGroupTemplate(key,alarmGroupInfoMap.get(key))
            alarmInfos.append(alarmgroup)
        templateGroup = AlarmTextTemplate(alarmInfos)
    
       
        with open("C:\\workbench\\doc\\alarmTextTemplate_zh.json","a+",encoding='utf-8') as f:
             json.dump(templateGroup,cls=MyEncoder,ensure_ascii=False,indent=4,fp=f)
    
    
      
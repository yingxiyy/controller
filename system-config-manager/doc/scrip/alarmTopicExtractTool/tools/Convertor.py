#!/usr/bin/python
# tools convertor

from module.AlarmInfo import AlarmInfo

class Convertor:
    @staticmethod
    def transfer2AlarmInfo(rowValue):
        alarmText =Convertor.getAlarmText(rowValue[8])
        alarmInfo=AlarmInfo(rowValue[4],rowValue[6],rowValue[7],alarmText,rowValue[8],rowValue[9])
        return alarmInfo
    @staticmethod
    def getAlarmText(text) :
        strlist=text.split(";")
        
        return strlist[0]
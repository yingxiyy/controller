#!/usr/bin/python
# alarm info dao

from tools.MysqlHelper import MysqlHelper


class AlarmInfoGroupDao:
    
    alarmInfoGroupDao = None
    
    def queryAlarmGroupTypeByName(self,name):
        sql = "select id,name,ne_group from board_alarm_group where name= %s";
        rows =MysqlHelper.getInstance().selectone(sql,name)
        return rows
    
    @classmethod
    def getInstance(self):
        if AlarmInfoGroupDao.alarmInfoGroupDao is None:
            AlarmInfoGroupDao.alarmInfoGroupDao = AlarmInfoGroupDao()
        return AlarmInfoGroupDao.alarmInfoGroupDao
# !/usr/bin/python
#
# alarm info group dao
#

from tools.MysqlHelper import MysqlHelper


class AlarmInfoDao:

  alarmInfoDao=None    

    
  def insertAlarmInfo(self,alarmInfo,typeId):
        print("start to insert alarm info ")
        sql = "insert into board_alarm_info (alarm_group,severity,type_id,message,board_alarm_type) values ('%s','%s','%s','%s',%d)"% \
        (
                alarmInfo.alarm_group,
                alarmInfo.severity,
                alarmInfo.type_id,
                alarmInfo.message,
                typeId)        
        MysqlHelper.getInstance().insert(sql)
        print("success to insert alarm info")
    
  
  @classmethod
  def getInstance(self):
    
     if AlarmInfoDao.alarmInfoDao is None:
         AlarmInfoDao.alarmInfoDao=AlarmInfoDao()
     return AlarmInfoDao.alarmInfoDao
        
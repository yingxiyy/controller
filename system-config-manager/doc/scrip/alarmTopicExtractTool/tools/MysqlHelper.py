#!/usr/bin/python
# mysql helper for the
import pymysql
import logging

from utils.DBConnectionPool import getDBConnection

class MysqlHelper:
    mysql=None
    def __init__(self):
        logging.info("start init mysql connection")
        self.db=getDBConnection()
  
    
    
    def selectall(self,sql='',param=()):
        try:
            cursor,conn=self.execute(sql,param)
            res=cursor.fetchall()
            self.close(cursor,conn)
            return res
        except Exception as e:
            print ('select all except  ',e.args)
            self.close(cursor,conn)
            return None
    
    def selectone(self,sql='',param=()):
        try:
            cursor,conn=self.execute(sql,param)
            res=cursor.fetchone()
            desc=cursor.description
            row={}
            for i in range(0,len(res)):
                row[desc[i][0]]=res[i]
            self.close(cursor,conn)
            return row
        except Exception as e:
            print('select one except  ',e.args)
            self.close(cursor,conn)
            return None
        
    def insert(self,sql='',param=()):
       try:
           cursor,conn=self.execute(sql,param)
           print('------------------')
           _id=cursor.lastrowid
           print('_id:',_id)
           conn.commit()
           self.close(cursor,conn)
           if _id ==0:
               return True
           return _id
       except Exception as e:
           print('inset except ',e.args)
           conn.rollback()
           self.close(cursor,conn)
           return 0 
             
    def execute(self,sql='',param=(),autoclose=False):
        cursor,conn = self.db.getConn()
        try:
            if param:
                cursor.execute(sql,param)
            else:
                cursor.execute(sql)
            conn.commit()
            if autoclose:
                self.close(cursor,conn)
        except Exception as e:
            pass
        return cursor,conn
        
    def close(self,cursor,conn) :
        cursor.close()
        conn.close()
        print("db connection pool release")
    
    @classmethod
    def getInstance(self):
        if MysqlHelper.mysql==None:
            MysqlHelper.mysql=MysqlHelper()
        return MysqlHelper.mysql
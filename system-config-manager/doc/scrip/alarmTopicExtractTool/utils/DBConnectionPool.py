#!/usr/bin/python
# db connection pool

import pymysql
from dbutils.pooled_db import PooledDB
import config.DB_config as Config

class DBConnectionPool(object):
    __pool=None
    
    def __enter__(self):
        self.conn = self.__getConn()
        self.cursor=self.conn.cursor()
        return self
    def __getConn(self):
        if self.__pool is None:
            self.__pool = PooledDB(
            creator=pymysql,
            mincached=Config.DB_MIN_CACHED,
            maxcached=Config.DB_MAX_CACHED,
            maxshared=Config.DB_MAX_SHARED,
            blocking=Config.DB_BLOCKING,
            host=Config.DB_HOST,
            port=Config.DB_PORT,
            user=Config.DB_USER,
            password=Config.DB_PASSWORD,
            db=Config.DB_DATABASE,
            charset=Config.DB_CHARSET
        )
        return self.__pool.connection()    
        
    def __exit__(self,type,value,trace):
        self.cursor.close()
        self.conn.close()
    
    def getConn(self):
        conn=self.__getConn();
        cursor=conn.cursor()
        return cursor,conn
def getDBConnection():
    return DBConnectionPool()
        
    
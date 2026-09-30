#!/usr/bin/python
#
# load excel data to database like mysql mongodb sqlite
#

from itertools import chain
import sys
from service.Excel2DbService import Excel2DbService

from collections import ChainMap
import logging



def  printList(headers):
    for header in headers:
        print(header)
    


def main1(argv) :
    path= "C:\\workbench\\doc\\alarm1.xlsx"
    logging.info("open excel path",path)
    excelService = Excel2DbService(path)
    ilaalarmGroup = excelService.loadExcelData2Dict("ILA",0)
    oaalarmGroup = excelService.loadExcelData2Dict("OA",1)
    opalarmGroup = excelService.loadExcelData2Dict("OP",2)
    cumxalarmgroup= excelService.loadExcelData2Dict("CMUX",4)
    otualarmGroup = excelService.loadExcelData2Dict("OTU",5)
    alarmGroupDict = dict(ChainMap(ilaalarmGroup,oaalarmGroup,opalarmGroup,cumxalarmgroup,otualarmGroup))
    excelService.write2json(alarmGroupDict)
    # excelService.saveAlarmGroup2DB(alarmGroupDict);

def main(argv) :
    path= "C:\\workbench\\doc\\alarm1.xlsx"
    logging.info("open excel path",path)
    excelService = Excel2DbService(path)
    
    ilaalarmGroup = excelService.loadExcelData2AlamrInfoDict(0)
    oaalarmGroup = excelService.loadExcelData2AlamrInfoDict(1)
    opalarmGroup = excelService.loadExcelData2AlamrInfoDict(2)
    cumxalarmgroup= excelService.loadExcelData2AlamrInfoDict(4)
    otualarmGroup = excelService.loadExcelData2AlamrInfoDict(5)
    alarmGroupList = list(chain(ilaalarmGroup,oaalarmGroup,opalarmGroup,cumxalarmgroup,otualarmGroup))
    excelService.write2json(alarmGroupList)
    # excelService.saveAlarmGroup2DB(alarmGroupDict);


if __name__=="__main__":
    main(sys.argv[1:])
#!/usr/bin/python
# excel helper for open excel 

import xlrd

class ExcelHelper:
    def __init__(self,filename,encoding=None):
        if not encoding is None:
            xlrd.Book.encoding= 'gbk'
        self.book = xlrd.open_workbook(filename,encoding_override=encoding)
        self.current_sheet = self.book.sheet_by_index(0)
    
    def set_current_sheet(self,index=None,name=None):
        if index is None and name is None:
            raise ValueError("must give a value")
        elif name is None:
            self.current_sheet = self.book.sheet_by_index(index)
        elif index is None:
            self.current_sheet = self.book.sheet_by_name(name)
    
    def cell_value(self,row,col):
        return self.current_sheet.cell_value(row,col)
    
    def row_values(self,index):
        data_rows=[]
        for row in range(index,self.row_num):
            data_rows.append(self.current_sheet.row_values(row))
        return data_rows
    
    def col_values(self,index):
        data_cols=[]
        for col in range(index,self.col_num):
            data_cols.append(self.current_sheet.col_values(col))
        return data_cols
    
    @property
    def col_num(self):
        return self.current_sheet.ncols
    
    @property
    def row_num(self):
        return self.current_sheet.nrows
    
    @property
    def headers(self):
        if self.row_num==0:
            return []
        return self.current_sheet.row_values(1)
    
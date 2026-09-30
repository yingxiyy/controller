#### task info api doc

#### 分页获取taskInfo

```
     接口url：/taskinfo?page=0&limit=20&sort={sort}&order={order}&user={usename}&keywords={keywords}&resourceType={resourceType}

     http method: get
     request: 
         {
           page: 1 当前页数， 可选  默认 0
           limit：一页显示的个数，可选 默认 20
           sort: desc/asc 默认 desc
           order： 排序的项目名，默认id排序，
                   resourceName, 资源名称
                   resourceType, 资源类型
                   endTime,      结束时间
                   actionTime,   开始时间
                   actionType    资源类型
           user: username 用户名 默认为空
           keywords: keywords filter 模糊查询关键字 默认为空
           resourceType: resourceType filter 资源名 例如 复用段(siteLink) 关键字括号内的英文 默认为空
         }

     response:
        {
            "code": 200,
            "message": "success",
            "content": {
                "taskInfoList": [], //主要内容在这里表达
                "currentPage": 1,
                "totalElements": 0,
                "totalPages": 0
            }
        }
```

# 获取特定task的detail信息

```

     接口url：/taskinfo/detail/{id}      //获取taskID=100的detail信息
     http method: get

```

# 获取特定task的detail信息

```
     接口url：/taskinfo/delete/{id}       //获取taskID=100的信息
     http method: delete
     
     request body:
       
      path param: id 请求的id
      
      
```

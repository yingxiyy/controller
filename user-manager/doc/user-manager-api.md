### user manager api

#### 概述

本接口文档主要用于用户管理相关api接口的定义和描述

#### 基本response结构定义

成功
response base model：

    message: 返回的消息 返回接口当前的成功和错误信息描述

    code： 状态码 200成功

    content： 主要内容结构

失败
response base model:

    "error_code": 错误编码,

    "message": 错误描述,

    "status": 错误状态

#### user manager api 主要分为user role permission 三个部分

##### USER API

1.1 user 的获取

    接口： /users?page={page}&limit={limit}&keywords={keywords}&order={order}&sort={sort}

    method: get
    
    request:

    page: 页数，默认为0  可选

    limit：一页的数据个数，默认为20 可选

    keywords：模糊查询搜索关键字，支持搜索用户的nam和email 可选

    order：以某一项进行排序，默认以create-time排序 可选

    sort：desc or asc 默认desc 可选


    response:

    content： 结构


     "current-page": 1, 当前页数
        "total": 1, 当前所有数据个数
        "total-page": 1, 所有页数
        "users": [
            {
                "create-time": "2022-04-22 13:15:47", 创建用户时间
                "id": 1,                              用户id
                "role-code": "ADMIN",                 用户角色编码
                "role-name": "管理员",                 用户角色名称
                "roleId": 2,                          用户角色id
                "username": "manager_haha"            用户名
            }
        ]

1.2 user 的添加

    接口： /users

    method： post

    request：
     
        {
            "username":"manager_haha", 用户名
            "password":"managerHaha",  用户密码
            "role" :{
                "role-id":1           用户角色id
            }
        } 

    response：

     返回成功：
        {
            "code": 200,
            "message": "success"
        }

     返回失败：
       {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

1.3 user 的删除

     接口： /users/{id}

     method: delete

     request:
        
        pathVariable: id 用户id 不能为空

     response:
       
      返回成功：
        {
            "code": 200,
            "message": "success"
        }

     返回失败：
       {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

1.5 user 详情获取

     接口： /users/{id}

     method: get

     request:
        
        pathVariable: id 用户id 不能为空

     response:
       
      返回成功：
        {
            "code": 200,
            "message": "success"
            "content": {
                "create-time": "2022-04-22 13:15:47", 创建时间
                "id": 1,                              用户id
                "modify-time": "2022-04-22 13:53:03", 修改时间
                "role-code": "ADMIN",                 角色编码
                "role-name": "管理员",                 角色名
                "email": "123@123.com"                
                "roleId": 2,
                "username": "manager_haha"
            }
        }

     返回失败：
       {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

1.4 user的修改 修改角色

      接口： /users/password

      method: put

      request: 

        {
        "user-id":"1",             用户id
        "password":"test_hello"    用户密码
        }

      response:

        返回成功：
        {
            "code": 200,
            "message": "success"
        }

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

1.5 user 的角色修改

       接口：/users/role

       method: put

       request:

       {
          "user-id":"1", 用户id
          "role-id":2,   用户角色id
          "role-code":"ADMIN" 用户角色code
       }

       response:

        返回成功：
        {
            "code": 200,
            "message": "success"
        }

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

##### role api

2.1 获取所有的role

接口 /roles

method: get

response:

    "content": [
        {
            "role-code": "MANAGER",  角色code
            "role-id": 1,            角色id
            "role-name": "系统管理员" 角色名称
        },
        {
            "role-code": "ADMIN",
            "role-id": 2,
            "role-name": "管理员"
        },
        {
            "role-code": "OPERATOR",
            "role-id": 3,
            "role-name": "操作员"
        },
        {
            "role-code": "VIEWER",
            "role-id": 4,
            "role-name": "观察者"
        }
    ],

2.2 新增 role

接口：/roles

method: post

request body:

{
"role-name":"观察者",
"role-code":"VIEWER",
"description":"观察者角色，只能看不能操作"
}

response:

返回成功：
{
"code": 200,
"message": "success"
}

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

2.3 删除角色role

接口：/role/{id}

method: delete

request variable: id 角色id

response:

返回成功：
{
"code": 200,
"message": "success"
}

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

##### permission api

3.1 获取所有的permission：

接口：/permission

method: get

response:

返回成功：

返回成功：
{
"code": 200,
"message": "success",
"content":[
{
"component": "implementor", 微服务模块名称
"description": "异步更新复用段信息，包括状态", 接口说明
"id": 56, 权限id
"name": "更新复用段", 权限名称
"path": "/restconf/operations/**", 模糊匹配路径
"type": "POST", http method 例如 POST GET PUT PATCH DELETE 可选，如果填写了具体的url，那么type必填必须是http method
"url": "/restconf/operations/site-topology:update-link" 具体权限路径   可选
}
]
}

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

3.2 获取对应role所拥有的所有权限

接口：/permission/role/{id}

method: get

request path variable: id role 的id

response：

  ``` 
  {
     "code": 200,
        "message": "success",
        "content": "permissions": [
            {
                "component": "user-manager",
                "description": "包括角色的获取添加修改删除",
                "id": 61,
                "name": "角色管理",
                "path": "/roles/**"
                "url":"****",
                "type":"POST",
            }
        ],
        "role-code": "MANAGER",
        "role-id": 1,
        "role-name": "系统管理员"
    }
    
   ```

    返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }
    
    3.3 给对应的role分配权限

    接口：/permission/role

    method: post

    request body:

    {
        "role-id":1,            角色id
        "permissions":[
            1,2,3,4             权限id列表
        ]
    }

    response body:

     返回成功：
    
        {
            "code": 200,
            "message": "success"
        }

        返回失败：

        {
            "error_code": "41001",
            "message": "the username: manager_haha is already existed ",
            "status": "BAD_REQUEST"
        }

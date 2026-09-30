### 机房管理接口定义

接口机房管理功能主要是：

1. 批量导入

2. 手工单条导入

3. 单条删除

4. 批量删除

5. 禁用与启用当前机房

### 主要结构定义

#### 基本response结构定义

response base model：

    message: 返回的消息 返回接口当前的成功和错误信息描述

    code： 状态码 200成功

    content： 主要内容结构

包括错误的返回

#### 机房信息结构：

```
  {
            "city": "深圳",              市
            "country": "中国",           国家
            "district": "光明",          区/县
            "province": "广东",          省
            "region": "华南",            区域
            "room": "深圳移动光明DC",     机房名
            "room_abbreviation": "光明",  机房简称
            "room_code": "SZ-GM",         机房编码
            "site": "深圳-光明",           园区
            "site_latitude": 22.74666,    园区经度坐标
            "site_longitude": 113.951984, 园区维度坐标
            "active": true                是否启用 默认启用 
        }

```

#### 接口描述

1. 获取所有站点信息接口

    ```

      接口url：/idc/all

      http method : get

      content-type: application/json

      request: none

      response:
          
          类型： list

          body：
            
            {
              "city": "深圳",              市
              "country": "中国",           国家
              "district": "光明",          区/县
              "province": "广东",          省
              "region": "华南",            区域
              "room": "深圳移动光明DC",     机房名
              "room_abbreviation": "光明",  机房简称
              "room_code": "SZ-GM",         机房编码
              "site": "深圳-光明",           园区
              "site_latitude": 22.74666,    园区经度坐标
              "site_longitude": 113.951984, 园区维度坐标
              "active": true                是否启用
              }      
    ```

2. 下载模板文件接口

     ```
       接口url：/idc/download_template 

       http method: get

       request: none 


       response:
           
           content-type: application/octet-stream

           Content-Disposition: attachment; filename=idc_template.xlsx

     ```

    3. 获取所有的机房信息分页

        ```
         接口url：/idc?page=0&limit=20
 
         http method: get
 
         request: 
          
             {
               page: 1 当前页数， 可选  默认 0
 
               limit：一页显示的个数，可选 默认 20
             }
 
 
         response:
          
             {
                  "current-page": 1, 当前页 第几页
                  "idc": [
                      {
                          "active": true,
                          "city": "昆明",
                          "country": "中国",
                          "district": "经开",
                          "province": "云南",
                          "region": "西南",
                          "room": "昆明OC-经开",
                          "room_abbreviation": "经开",
                          "room_code": "KM-JK",
                          "site": "昆明电信",
                          "site_latitude": 24.976873,
                          "site_longitude": 102.828807
                      },
                      {
                          "active": true,
                          "city": "深圳",
                          "country": "中国",
                          "district": "光明",
                          "province": "广东",
                          "region": "华南",
                          "room": "深圳移动光明DC",
                          "room_abbreviation": "光明",
                          "room_code": "SZ-GM",
                          "site": "深圳-光明",
                          "site_latitude": 22.74666,
                          "site_longitude": 113.951984
                      }
                  ],
                  "total-elements": 2, 共有多少内容
                  "total-pages": 1     共有多少页
              }
 
       ```

        5. 批量添加导入数据文件（excel）

       ```
            接口url：/idc/import
 
            http method: post
 
            content-type: multipart/form-data
 
            request: 
                 
                form-data: 
 
                file: 要上传的文件
 
             response:
                 
                 {
                     message: 返回的消息 返回接口当前的成功和错误信息描述
 
                     error_code：错误状态码，出错时将出现
 
                     status： 状态
 
 
 
                 }  
 
        ```

        6. 删除机房

        ```
          接口url： /idc/{id}
 
          http method: post
 
          request:
 
             id: 机房id
 
           response:
 
                {
                     message: 返回的消息 返回接口当前的成功和错误信息描述
 
                     error_code：错误状态码，出错时将出现
 
                     status： 状态
                }  
 
 
        ```

        9. 手工添加一条机房信息

       ```
          接口url： /idc
 
          http method: put
 
          request:
 
          request body ：
 
                    {
                          "city": "昆明",
                          "country": "中国",
                          "district": "经开",
                          "province": "云南",
                          "region": "西南",
                          "room": "昆明OC-经开",
                          "room_abbreviation": "经开",
                          "room_code": "KM-JK",
                          "site": "昆明电信",
                          "site_latitude": 24.976873,
                          "site_longitude": 102.828807
                      }
 
           response:
 
                {
                     message: 返回的消息 返回接口当前的成功和错误信息描述
 
                     error_code：错误状态码，出错时将出现
 
                     status： 状态
                }  
 
 
        ```

        10. 编辑修改一条机房信息,当前支持修改是否停用当前机房

        ```
          接口url： /idc/edit
 
          http method: post
 
          request:
 
          request body ：
 
 
                {
                    id: 机房id
                      
                    enable：是否停用 boolean
                }
 
           response:
 
                {
                     message: 返回的消息 返回接口当前的成功和错误信息描述
 
                     error_code：错误状态码，出错时将出现
 
                     status： 状态
                }  
 
 
        ```

        11. 根据区域获取所有城市的rpc接口

        ```
          接口url：/idc/get-city-list
 
          http method: post
 
          request: 
         
            request body: 
 
               {
                "input":{
                    "region-name":"西南" 区域名
                    "region-id" : 1 区域id
                  }
               }
 
          response:
        
             {
                 "output": {
                  "city": [
                {
                    "cityId": 2,
                    "cityName": "成都"
                },
                {
                    "cityId": 3,
                    "cityName": "昆明"
                },
                {
                    "cityId": 4,
                    "cityName": "雅安"
                },
                {
                    "cityId": 5,
                    "cityName": "贵阳"
                }
                ]
               }
            }
 
         
        ```

        12. 根据城市获取所有未停用站点的接口

        ```
            接口url：/idc/get-idc-list
 
            http method: post
 
            request: 
             
                request body: 
 
                {
                    "input":{
                          "city-name":"昆明", 城市名
                          "city-id": 3 城市id
                    }
                }
 
            response:
 
               {
        "output": {
            "idc-list": [
                {
                "idc-code": "KM-LQ",
                "idc-display-name": "昆明-龙泉(KM-LQ)",
                "idc-id": 1,
                "idc-name": "昆明OC-龙泉"
                }
            ]
        }

}

    ```

接口机房管理功能主要是：

1. 批量导入

2. 手工单条导入

3. 单条删除

4. 批量删除

5. 禁用与启用当前机房

### 主要结构定义

#### 基本response结构定义

response base model：

    message: 返回的消息 返回接口当前的成功和错误信息描述

    code： 状态码 200成功

    content： 主要内容结构

包括错误的返回

#### 机房信息结构：

```

{
"city": "深圳", 市
"country": "中国", 国家
"district": "光明", 区/县
"province": "广东", 省
"region": "华南", 区域
"room": "深圳移动光明DC", 机房名
"room_abbreviation": "光明", 机房简称
"room_code": "SZ-GM", 机房编码
"site": "深圳-光明", 园区
"site_latitude": 22.74666, 园区经度坐标
"site_longitude": 113.951984, 园区维度坐标
"active": true 是否启用 默认启用 }

```

#### 接口描述

1. 获取所有站点信息接口

    ```

      接口url：/idc/all

      http method : get

      content-type: application/json

      request: none

      response:
          
          类型： list

          body：
            
            {
              "city": "深圳",              市
              "country": "中国",           国家
              "district": "光明",          区/县
              "province": "广东",          省
              "region": "华南",            区域
              "room": "深圳移动光明DC",     机房名
              "room_abbreviation": "光明",  机房简称
              "room_code": "SZ-GM",         机房编码
              "site": "深圳-光明",           园区
              "site_latitude": 22.74666,    园区经度坐标
              "site_longitude": 113.951984, 园区维度坐标
              "active": true                是否启用
              }      
    ```

2. 下载模板文件接口

     ```
       接口url：/idc/download_template 

       http method: get

       request: none 


       response:
           
           content-type: application/octet-stream

           Content-Disposition: attachment; filename=idc_template.xlsx

     ```

3. 获取所有的机房信息分页

    ```
     接口url：/idc?page=0&limit=20

     http method: get

     request: 
         
         {
           page: 1 当前页数， 可选  默认 0

           limit：一页显示的个数，可选 默认 20
         }


     response:
         
         {
              "current-page": 1, 当前页 第几页
              "idc": [
                  {
                      "active": true,
                      "city": "昆明",
                      "country": "中国",
                      "district": "经开",
                      "province": "云南",
                      "region": "西南",
                      "room": "昆明OC-经开",
                      "room_abbreviation": "经开",
                      "room_code": "KM-JK",
                      "site": "昆明电信",
                      "site_latitude": 24.976873,
                      "site_longitude": 102.828807
                  },
                  {
                      "active": true,
                      "city": "深圳",
                      "country": "中国",
                      "district": "光明",
                      "province": "广东",
                      "region": "华南",
                      "room": "深圳移动光明DC",
                      "room_abbreviation": "光明",
                      "room_code": "SZ-GM",
                      "site": "深圳-光明",
                      "site_latitude": 22.74666,
                      "site_longitude": 113.951984
                  }
              ],
              "total-elements": 2, 共有多少内容
              "total-pages": 1     共有多少页
          }

   ```

5. 批量添加导入数据文件（excel）

   ```
        接口url：/idc/import

        http method: post

        content-type: multipart/form-data

       
         response:
                
             {
                 message: 返回的消息 返回接口当前的成功和错误信息描述

                 error_code：错误状态码，出错时将出现

                 status： 状态



             }  

    ```

    6. 删除机房

    ```
      接口url： /idc/{id}

      http method: delete

      request:

         id: 机房id

       response:

            {
                 message: 返回的消息 返回接口当前的成功和错误信息描述

                 error_code：错误状态码，出错时将出现

                 status： 状态
            }  


    ```

    9. 手工添加一条机房信息

   ```

      接口url： /idc

      http method: put

      request:

      request body ：

                {
                      "city": "昆明",
                      "country": "中国",
                      "district": "经开",
                      "province": "云南",
                      "region": "西南",
                      "room": "昆明OC-经开",
                      "room_abbreviation": "经开",
                      "room_code": "KM-JK",
                      "site": "昆明电信",
                      "site_latitude": 24.976873,
                      "site_longitude": 102.828807
                  }

       response:

            {
                 message: 返回的消息 返回接口当前的成功和错误信息描述

                 error_code：错误状态码，出错时将出现

                 status： 状态
            }  


    ```

    10. 编辑修改一条机房信息,当前支持修改是否停用当前机房

        ```
          接口url： /idc/edit
 
          http method: post
 
          request:
 
          request body ：
 
 
                {
                    id: 机房id
                      
                    enable：是否停用 boolean
                }
 
           response:
 
                {
                     message: 返回的消息 返回接口当前的成功和错误信息描述
 
                     error_code：错误状态码，出错时将出现
 
                     status： 状态
                }  
 
 
        ```
        11. 根据区域获取所有区域的

        ```
          接口url：/idc/get-region-list
 
          http method: get
 
          request: 
         
 
          response:
        
            {
                "code": 200,
                "content": [
                     {
                "region-id": 1, 区域id
                "region-name": "华南" 区域名
                },
            {
                "region-id": 2, 区域id
                "region-name": "西南" 区域名
            }
                ],
                "message": "success"
            }
 
         
        ```

        12. 根据区域获取所有城市的rpc接口

        ```
          接口url：/idc/get-city-list
 
          http method: post
 
          request: 
         
            request body: 
 
               {
                "input":{
                    "region-name":"西南" 区域名
                  }
               }
 
          response:
        
             {
                 "output": {
                   "city-name": [
                       "昆明"    list
                  ]
               }
            }
 
         
        ```

        13. 根据城市获取所有未停用站点的接口

        ```
            接口url：/idc/get-idc-list
 
            http method: post
 
            request: 
             
                request body: 
 
                {
                    "input":{
                        "city-name":"昆明" 区域名
                    }
                }
 
            response:
 
               {
                "output": {
                    "idc-list": [
                        {
                            "idc-display-name": "昆明-经开(KM-JK)",
                            "idc-name": "昆明OC-经开"
                            “idc-id":1 idc的id
                        }
                    ]
                }
            }
 
             
        ```

#### 注意

删除机房时，机房必须是停用的状态才能操作
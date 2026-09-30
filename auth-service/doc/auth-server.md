### auth-server api接口说明

#### 概述

本接口描述主要描述认证服务和如何使用token

#### 认证登录接口

1. 本认证服务采用oauth2.0 进行登录认证，采用oauth2.0中的密码模式进行登录验证

接口：
/oauth2/token?username={username}&password={password}&client_id={client_id}&client_secret={client_secret}&scope={scope}&grant_type={grant_type}

http method: POST

request param:

username 用户名

password 用户密码

client_id 注册oauth2.0 认证服务器的客户端id，由于只支持密码模式登录，当前的client_id 是 password-dci

client_secret 注册到oauth2.0 认证服务器的客户端密钥，必填默认值是dciWorld

scope： 认证域，oauth2.0服务器允许客户端所有操作请求的作用域增加额外的访问范围

grant_type: oatuh2.0服务所支持的认证模式，分为：

授权码（authorization-code）
隐藏式（implicit）
密码式（password）：
客户端凭证（client credentials）

当前仅支持 密码模式（password）

response：

{
"access_token": "
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbmlzdHJhdG9yIiwiYXVkIjoicGFzc3dvcmQtZGNpIiwibmJmIjoxNjUwODk0MTg5LCJzY29wZSI6WyJhbGwiXSwiZXhwIjoxNjUwOTgwNTg5LCJpYXQiOjE2NTA4OTQxODksImp0aSI6IjM4ZThjOGE0LTM4MmEtNDdhNi04N2YwLTEyNDE4ZmE4ZTg0YSIsImF1dGhvcml0aWVzIjpbIk1BTkFHRVIiXX0.WflwXmhyZx9KI1WlsPP9vMPsR4_Nz74vZERvpGeU3AFxz1NyvFGzOVqnkjJj6AgL_OGaz5p1R-964QJHYQ64s-DhXGDiEDGHIE4YoGp_bXi7BTtXhiIXq9CMzJVTL9ycNwY8NJd0gFyCDMubzt2ea1w0fKvkjXQUq_RPLNAtBgI4387vmOK7YAIDEbHwgYhteKbpFYj9hdT0pTr4JakSVCgW8_IxTmcEPOlOuxuoKuzVZQ9QeWHV3yNDDdyd_9GAg2hIdFleIgMNwiFi3J68RtcSHJTkak-6Gdor-RhWRXU6oWP9SKRdaG7vkAuKLQZqMJk440YqDf79VRzBSRgRqw",
令牌token
"refresh_token": "
j2qNIWwTzlfcv0eIIZR7COVP0dmgb5CC_TOYjR20Eg_ExUIth-0J99CE5_LyVSPYfrkoGHTNiDf0_m1ma8bsuoQRB450QCdASF0MohpUzgcG2QX7vgfiY1Nb4lCSL7Uh",
刷新令牌token
"sub": "administrator", 用户名 administrator
"aud": [
"password-dci"      客户端id
],
"user_id": 0, 用户id
"scope": "all", 作用域
"roles": [
"MANAGER"    角色code
],
"token_type": "Bearer", token类型
"expires_in": 86399, 过期时间
"jti": "38e8c8a4-382a-47a6-87f0-12418fa8e84a" jwt token的id
}

### 注意

在登录鉴权成功的后续的请求中在 http header中添加 header

key Authorization

value token_type access_token
# dianping
简化版大众点评
项目简介:本项目是一个类似于大众点评等打卡类APP的点评类项目。实现了短信登陆、探店点评、优惠券秒杀、每日签到、好友关注、粉丝关注博主后，主动推送博主的相关博客等多个模块。
用户可以浏览首页的推荐内容，搜索附近的商家，查看商家的详情和评价以及发表探店博客，抢购商家发布的限时秒杀商品。

## 克隆完整项目
git clone https://github.com/Old-Second/hmdp.git
## 前端环境部署
nginx-1.18.0   启动nginx.exe    
## 后端环境部署
在application.yaml文件中，Mysql、Redis、RabbitMQ相关的配置需要自行更改



RabbitMQ版本为3.9

| 技术 | 说明 |
| --- | --- |
| SpringBoot | 容器+MVC框架 |
| Redis | 分布式缓存 |
| RabbitMQ | 消息中间件 |
| MySQL | 关系型数据库 |
| Lombok | 简化对象封装工具 |
| SMTP | SMTP协议邮箱文件传输 |




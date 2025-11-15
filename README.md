
# part 1: 需求与功能梳理
> 主要功能分为两个部分：对于消息的管理、对用户的管理

## 对消息的管理
用户与用户之间发送消息的形式：文字、表情、图片、语音、视频。另外用户是否在线。如果不在线，如何让用户能够正常接收到消息。另外还有消息的已读和未读。

除了用户一对一聊天，还有群聊的形式。

需求点：发送文字、发送图片、发送文件、发送语音、发送视频、消息缓存、消息未读、消息已读、历史消息、单人聊天、多人聊天、。。。。

## 对用户的管理
需求点：添加好友、好友列表、删除好友、创建群聊、查看群成员、拉人进群、踢人出群、解散群聊、编辑群公告、修改备注、修改群名称......

# part 2: 链路交互
## 1. 用户请求交互
1. 用户在端上发送请求到服务器，先经过网关或负载均衡器将请求发往业务网关；
2. 如果是HTTP请求，业务网关会路由到后端平台，后端平台需要发送消息也会调用IM即时通讯系统；如果是TCP请求或WebSocket请求，业务网关会路由到IM即时通讯服务
3. 后端平台与IM即时通讯系统接收到请求后，会进行业务处理，后调用基础服务接口进一步处理业务；
4. 基础服务执行业务逻辑之后，将数据写入数据存储服务，例如Redis、MySQL等

## 2. 发送消息交互
1. 用户A调用后端平台向用户B发送消息
2. 后端平台将消息缓存起来，并存储到数据库
3. 后端平台从Redis中获取到B的即时通讯系统ID，向对应用户B的即时通讯系统的Topic发送消息
4. 即时通讯系统监听消息，根据用户B的ID以及终端信息获取到对应链接，向用户B推送消息


# part 3: 准备环境
## 1. 安装系统环境
包含docker、docker-compose。使用docker-compose一件安装即时通讯系统依赖的基础服务和中间件：MySQL、Redis、RocketMQ、ElasticSearch、Logstash、Kibana、Nacos、Sentinel等
1. 安装docker、设置开机自启动、配置镜像加速，安装docker-compose
2. 使用docker-compose一件安装基础服务
3. 测试安装的服务：MySQL、Redis、RocketMQ、ElasticSearch、Logstash、Kibana、Nacos、Sentinel等

切换软件源
```bash
sudo vim /etc/apt/sources.list
```
```bash
# 默认注释了源码镜像以提高 apt update 速度，如有需要可自行取消注释
deb https://mirrors.aliyun.com/ubuntu/ jammy main restricted universe multiverse
# deb-src https://mirrors.aliyun.com/ubuntu/ jammy main restricted universe multiverse

deb https://mirrors.aliyun.com/ubuntu/ jammy-updates main restricted universe multiverse
# deb-src https://mirrors.aliyun.com/ubuntu/ jammy-updates main restricted universe multiverse

deb https://mirrors.aliyun.com/ubuntu/ jammy-backports main restricted universe multiverse
# deb-src https://mirrors.aliyun.com/ubuntu/ jammy-backports main restricted universe multiverse

deb https://mirrors.aliyun.com/ubuntu/ jammy-security main restricted universe multiverse
# deb-src https://mirrors.aliyun.com/ubuntu/ jammy-security main restricted universe multiverse

```bash
sudo apt update
```
安装docker
```bash
#  更新软件包索引并安装依赖
sudo apt update
sudo apt install apt-transport-https ca-certificates curl gnupg lsb-release

添加 Docker 的官方 GPG 密钥
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
# 添加 Docker 软件仓库
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
#  更新软件包索引并安装 Docker 
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

启动：
```bash
# 启动docker
systemctl start docker
# 设置开机自启
systemctl enable docker
# 查看版本
docker version

# 逐个pull镜像
# 逐个拉取需要的镜像
docker pull mysql:5.7
docker pull redislabs/redismod:latest
docker pull rediscommander/redis-commander:latest
docker pull apacherocketmq/rocketmq:4.5.0
docker pull elasticsearch:7.14.2
docker pull logstash:7.14.2
docker pull kibana:7.14.2
docker pull bladex/sentinel-dashboard
docker pull nacos/nacos-server:2.0.3
```

搭建maven私服
```bash
docker run -d -p 8081:8081 --name nexus -v /usr/local/nexus-data:/nexus-data --restart=always sonatype/nexus3
```

搭建minio分布式文件系统
1. 使用docker-compose安装四个minio服务器
2. 编写nginx配置，使用nginx进行负载均衡访问
3. 配置minio，创建bucket


## 2. 项目工程搭建
1. 创建根项目
2. 创建子模块：domain、infrastructure、application、interfaces、stater
   a. domain：领域层，即时通讯后端服务中相对不变的部分抽象出来的领域对象
   b. infrastructure：基础设施层，包含数据库、缓存、消息队列、分布式锁、分布式事务等
   c. application：应用层，处理容易变化的业务场景，对相关事件、调度和其他聚合操作进行相关处理
   d. interfaces：接口层、展示层，DDD设计的最上层，对外提供API接口，接受客户端请求，解析参数，返回结果数据，对异常进行处理
   e. stater：启动类，项目的启动工程
3. 依赖关系：starter -> interfaces -> application -> infrastructure -> domain







# 通用模型设计
> 涵盖各系统和服务之间的交互模型，`用户终端`与`大后台平台`和`即时通用服务`之间的交互模型 
 
## 登录信息模型
> 包含用户登录的Token信息，
> 

## Session信息模型
> 主要包含用户id和终端类型
> 

## 3. 用户信息模型
> 发送消息的用户，包含用户id和终端类型

## 4. 心跳数据模型
> 客户端与即时通讯系统之间的心跳机制

## 5. 私聊数据模型
> 用户在系统中发送的单聊消息。包含消息发送者信息、消息接受者id、消息接受者终端列表、是否将消息发送给自己的 其他终端、消息具体内容

## 6. 群聊数据模型
> 用户在系统中发送的群聊消息。包含发送者信息、接受者id列表、接受者终端类型、消息内容
## 7. 通用发送数据模型
> 用于即时通讯服务想客户端响应登录结果、发送心跳数据、单聊消息、群聊消息等
> 主要包含发送消息指令类型、具体的数据
## 8. 通用接收数据模型
> 大后端平台与即时通讯服务之间的数据交互。
> 包含消息指令类型、消息发送者信息、消息接受者列表、是否需要回调发送结果、具体的数据
## 9. 响应结果数据模型
> 用于发送相应的结果数据
> 包含消息发送者、消息接受者、指令类型、具体数据
## 10. RocketMQ数据模型
> 用于想RocketMQ发送数据和接收RocketMQ中的数据
> 包含消息的Topic
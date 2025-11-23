
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
docker pull sonatype/nexus3
mkdir /usr/local/nexus-data && chmod -R 200 /usr/local/nexus-data
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
## 系统通用模型设计
1. 登录信息模型
> 包含用户登录的Token信息，登录系统时会根据这个token进行校验
2. Session信息模型
> 用户登录系统之后保存用户信息，主要包含用户id和终端类型
3. 用户信息模型
> 发送消息的用户，包含用户id和终端类型
4. 心跳数据模型
> 客户端与即时通讯系统之间的心跳机制
5. 私聊数据模型
> 用户在系统中发送的单聊消息。包含消息发送者信息、消息接受者id、消息接受者终端列表、是否将消息发送给自己的其他终端、是否回推发送的结果数据、消息具体内容
6. 群聊数据模型
> 用户在系统中发送的群聊消息。包含发送者信息、接受者id列表、接受者终端类型、是否将消息发送给自己的其他终端、是否回推发送的结果数据、消息内容
7. 通用发送数据模型
> 用于即时通讯服务向客户端响应登录结果、发送心跳数据、单聊消息、群聊消息等
> 主要包含发送消息指令类型、具体的数据
8. 通用接收数据模型
> 大后端平台与即时通讯服务之间的数据交互。
> 包含消息指令类型、消息发送者信息、消息接受者列表、是否需要回调发送结果、具体的数据
9. 响应结果数据模型
> 用于发送相应的结果数据
> 包含消息发送者、消息接受者、指令类型、具体数据
10. RocketMQ数据模型
> 用于想RocketMQ发送数据和接收RocketMQ中的数据
> 包含消息的Topic
## 数据枚举设计
1. 发送消息的类型
> 包含：登录、心跳、强制下线、私聊消息和群发消息
2. 监听消息的类型
> 包含：全部消息、私聊消息和群聊消息
3. 发送消息的状态
> 包含：发送成功、对方当前不在线、未找到对方的Channel、未知异常
4. 发送和接收消息的终端类型
> 包含Web端、APP端
## 通用缓存设计
> 分布式缓存需要支持的场景：存储与用户终端建立连接的IM即时通讯服务ID，用户的在线状态、用户已读的单聊以及群聊消息的最大ID，视频通话信息等等内容
> 
> 让缓存具有良好扩展性的原则：面向接口编程，具体业务逻辑里依赖的是接口，而非实现类。在接口不变的前提下，可以随时切换具体的实现类，也可以随时新增接口的实现类。业务中可以根据配置加载接口的某个具体实现类

混合型缓存读取顺序：优先读取本地缓存数据，如果本地缓存未开启或者已经失效，读取再去分布式缓存。

混合型缓存的优点：使用本地缓存不会发生远程IO操作，性能更高。大部分请求会命中本地单机缓存
### 本地缓存刷新机制
> 本地缓存数据时间不宜过长，将容量设置为15，过期时间设置为5秒。并且会设计基于版本号机制来实现缓存的失效策略
1. **主动刷新**：请求接口传入的版本号大于本地缓存中的版本号，说明本地缓存已经失效，需要从分布式缓存中重新获取数据进行刷新
2. **被动刷新**：本地缓存自动过期，被动从缓存中移除。需要从分布式缓存中重新获取数据进行刷新
### 分布式缓存刷新机制
> 分布式缓存基于Redis实现，也需要缓存过期时间。不能让缓存数据永久性驻留到Redis。分布式缓存过期时间相较于本地缓存要长一些
1. **主动刷新**：业务数据变更驱动分布式缓存数据。业务数据发生变更时，主动刷新分布式缓存中的数据
2. **被动刷新**：基于Redis提供的LRU、TTL等缓存过期策略。
**注意：**刷新分布式缓存的时候，并不会同时刷新本地缓存的数据，本地缓存采用自身的缓存刷新机制。
### 缓存设计原则
1. 热点数据一律进缓存
2. 一律采用本地缓存+分布式缓存的方案
3. 优先读取本地缓存，本地缓存为主，分布式缓存为辅
4. 所有缓存都要设置过期时间，本地缓存过期时间控制在秒级
5. 本地缓存需要同时设置容量驱逐和时间驱逐两种方式
6. 缓存KEY具有业务可读性，杜绝不同场景出现相同的KEY
7. 缓存列表数据时，仅缓存第一页，缓存数量不超过20
8. 杜绝并发更新缓存，防止”缓存击穿“。（10000个请求去更新缓存，就带给了数据库非常大的压力。如果使用分布式锁就不会出现这个问题）
9. 空数据进缓存，防止缓存穿透
10. 读数据时，先读缓存，再读数据库
11. 写数据时，先写数据库，再写缓存
### 具体实现
#### 本地缓存
一个本地缓存接口，有以下抽象方法：
1. 向缓存中添加数据
2. 根据key从缓存中获取数据
3. 移除缓存中的数据
#### 本地缓存接口基于Guava实现
1. 定义一个工厂类，实现不同参数创建Guava实例的多个方法
2. 一个使用Guava的本地缓存类，实现接口的方法。并且使用@ConditionalOnProperty注解，根据配置决定是否加载该类

#### 分布式缓存
一个分布式缓存接口，有以下抽象方法
1. 根据key设置缓存方法：不设置过期时间、设置过期时间
2. 设置缓存过期时间方法
3. 设置缓存时设置逻辑过期时间方法
4. 获取缓存方法：根据key获取字符串、根据key获取指定类型的缓存、根据key列表批量获取缓存
5. 根据正则表达式获取所有key的方法
6. 删除指定的key方法
#### 分布式缓存基于Redis实现
1. 创建一个RedisCache类，实现接口的方法。并且使用@ConditionalOnProperty注解，根据配置决定是否加载该类
## 通用分布式锁设计
基于面向接口设计原则，便于后续切换不同的分布式锁实现
1. 一个分布式锁工厂接口，声明提供获取分布式锁的方法，通过调用该方法可以获取一个分布式锁实例
2. 分布式锁接口。定义分布式锁常规操作方法，包含加锁、释放锁、判断是否添加分布式锁方法
3. RedissionLockFactory是分布式锁工厂的具体实现类，通过分布式锁接口的匿名内部类实现分布式锁的常用方法
## MQ消息发送通用设计
1. 通用发送消息接口，声明两个发送消息的方法：send方法发送普通消息、sendMessageInTransaction方法发送事务消息
2. 消息发送实现类，基于RocketMQ实现接口中的方法，另外添加一个getMessage方法用于构造向RocketMQ中真正发送的消息数据
## 分布式ID生成器的设计
### 需要考虑到的问题
1. 全局唯一性，整个分布式系统中，ID应该是唯一的
2. 有序性，ID应该具有一定的有序性，便于排序和查询
3. 低延迟，ID生成速度不应该成为系统的瓶颈，或者拖慢系统的运行
4. 可扩展性，应该支持水平扩展，能够容纳更多节点和请求
### 雪花算法的实现
> 雪花算法是经典的分布式ID生成算法，使用一个64位的整数作为ID，根据时间戳、机器ID（可以根据IP地址或者MAC地址生成）和序列号（同一毫秒内并发生成多个ID时使用，保证ID的唯一性和有序性）生成唯一的ID
> 
> 如果同一毫秒有多个线程生成ID，会通过竞争机制来获取序列号，保证唯一性

雪花算法的组成：1bit符号位、41bit时间戳、10bit工作机器ID、12bit序列号

# 即时通讯系统后端设计
## 即时通讯通用代码设计
> 即时通讯服务中主要食用Netty进行消息的收发。同时支持TCP和WebSocket来年各种功能长链接方式

### 通用接口设计
IMNettyServer接口声明三个抽象方法：isReady（服务是否准备就绪）、start（启动服务）、shutdown（停止服务）
分别使用TCP和WebSocket两种方式实现IMNettyServer接口，**具体怎么使用Netty还得学习一下**

另外，用户终端与即时通讯后端服务建立连接之后，后端服务会将用户ID和用户终端类型作为Key，用户终端与后端服务建立的连接对洗那个座位Value，将其存储到本地缓存中。具体逻辑在UserChannelContextCache类中实现，在类中设计了一个Map<Long,Map<Integer, ChannelHandlerContext>>
类型的私有成员变量来存储数据。外层Map的Key是用户ID，内层Map的Key是用户终端类型，Value是ChannelHandlerContext对象。UserChannelContextCache提供存储用户连接的方法、移除用户连接的方法和获取用户连接的方法。
### 同时启动SpringBoot已经加载的Netty服务实现类
需要用到SpringBoot中的一个扩展点CommandLineRunner接口，CommandLineRunner接口的run方法会在SpringBoot启动时执行。可以使用run()方法启动Netty服务实现类。

## 自定义编解码器
netty的编解码器分为一次编解码器（MessageToByteEncoder/ByteToMessageEncoder）、二次编解码器（MessageToMessageEncoder/MessageToMessageDecoder）。

继承对应的编解码器父类，重写encoder/decoder方法即可完成自定义编解码器的编写。
## ChannelPipeline的设计与使用
> ChannelPipeline是Netty中用于处理网络事件和数据的管道。它是一个双向链表结构，存储了ChannelHandler对象，每个ChannelHandler对象负责处理特定类型的网络事件和数据。
> ChannelPipeline中的ChannelHandler对象按照添加顺序依次执行，每个ChannelHandler对象都可以处理ChannelPipeline中的数据，并且可以决定是否将数据传递给下一个ChannelHandler对象或者直接发送响应结果给客户端。
### 事件传播机制
> ChannelPipeline分为ChannelInboundHandler和ChannelOutboundHandler两个部分。ChannelInboundHandler负责处理入站事件和数据的处理（需要经过Decoder处理），ChannelOutboundHandler负责处理出站事件和数据发送（需要经过Encoder处理）。

当客户端想服务端发送请求的时候，会触发ChannelInboundHandler的channelRead方法，处理完成之后会调用writeAndFlush()方法向客户端写数据，此时会触发ChannelOutboundHandler的write方法，将数据发送给客户端。
在Netty中Inbound事件是由Head向Tail传播，Outbound事件是由Tail向Head传播。
### 异常传播机制
出现异常时Netty会调用exceptionCaught()方法，将异常从Head节点传播到Tail节点。如果没有对异常处理，异常会传播到Tail节点，由Tail节点处理异常。也可以在Netty中继承ChannelDuplexHandler类来自定义全局异常处理器来统一处理异常。
### ChannelHandler具体实现
继承SimpleChannelInboundHandler类，实现以下方法：
1. channelRead0方法：处理入站事件和数据
2. exceptionCaught方法：处理异常
3. handlerAdded()方法是用户终端与即时通讯后端服务建立链接后Netty回调的方法，可以执行一些保存操作。但此时在这个方法里没有做任何操作
4. handlerRemoved()方法是在用户终端与即时通讯后端服务断开链接后Netty回调的方法，可以执行一些移除操作。这里从通道属性中获取到用户ID和终端类型，通过用户ID和终端类型将其从本地缓存和分布式缓存中删除。另外通过判断缓存中存在该用户的通道上下文、缓存中的通道ID与当前断开链接的通道ID一致来防止异地登陆误删链接
5. 在userEventTriggered()方法中检测超时时间，读超时事件中关闭对应的Channel链接
## 即时通讯服务后端服务登陆处理器的设计与实现
> 收到登陆消息后，对登陆逻辑进行处理。主要是对JWT Token进行校验，获取用户终端与即时通讯服务后端建立的链接、处理异地登陆逻辑、设置用户与终端属性、初始化心跳次数、缓存与用户建立的即时通讯后端服务ID
### 具体实现
1. MessageProcessor接口是消息处理器接口，任何对消息的处理都会实现该接口。提供了三个方法：process(ChannelHandlerContext ctx, T Data)方法处理消息、process(T data)、T transForm(Object obj)
2. LoginProcessor类实现了MessageProcessor接口，主要处理登陆消息逻辑，实现process方法。校验token，校验不通过则关闭当前链接。校验通过则通过session获得用户ID和用户终端，处理异地登录逻辑。缓存用户终端与即时通讯后端服务建立的链接，设置用户和终端属性。初始化心跳次数，缓存与用户终端建立链接的即时通讯后端服务ID，并响应客户端登录的信息
3. ProcessorFactory类在getProcessor()方法中，从IOC容器中获取到LoginProcessor类的对象并返回。预留获取心跳消息处理器、单聊消息处理器、群聊消息处理器的实现逻辑
4. 修改IMChannelHandler类的channelRead0()方法，通过添加ProcessorFactory类获取消息处理器，并通过消息处理器处理逻辑
## 心跳处理器的设计与实现
> 发送心跳消息的流程与发送登录消息流程大体相似。即时通讯后端服务接收到消息后，调用消息处理器工厂的方法从IOC容器中获取对应的消息处理器，处理相应的消息
1. 新增实现MessageProcessor接口的HeartBeatProcessor类，用于处理心跳消息。使用@Value注入心跳次数，process()方法中首先响应客户端的心跳消息。从用户终端与即时通讯后端服务建立的Channel链接中获取当前心跳次数，对心跳次数+1重新放入Channel链接中。对当前心跳次数求模，结果为0则延长与用户终端建立链接的即时通讯系统后端服务ID在分布式缓存中的有效时长
2. 在ProcessFactory类中添加从IOC容器获取心跳消息处理器的方法

im-messaging-server调用流程
1. 使用TCP或WebSocket实现NettyServer，NettyServer中需要注册编码器、解码器、通道处理器
2. Encoder编码，Decoder解码，ChannelHandler处理逻辑。Handler通过ProcessFactory获取具体的Processor
3. ProcessFactory（获得不同类型的processor，例如LoginProcessor、PrivateMessageProcessor等， 都实现了接口MessageProcessor）
4. MessageProcessor是各种Processor的接口，Processor中有两个方法供Handler调用，分别是process()和transform()。process()中实现Processor的具体逻辑，例如登陆验证、缓存交互等。transform()方法用于将原始数据转换为process的入参。

数据存储
- UserChannelContextCache中使用一个CurrentHashMap存储用户与数据通道的信息，结构是Map<Long,Map<Integer, ChannelHandlerContext>>，一个用户ID可以得到一个Map<Integer, ChannelHandlerContext>。一个用户ID和一个终端ID可以得到一个数据通道。
- 登陆的时候会分布式缓存用户与建立链接的后端服务id，key是im:user:server_id:user_id:terminal_id，value是后端服务id
- 心跳的时候随着10次心跳，更新一下分布式缓存中用户与建立链接的后端服务id的过期时间

## 单聊处理器的设计与实现
> 用户A向用户B发送消息，先将存储消息。然后将消息发送给B，如果B不在线，则等待B上线后发送消息。B上线后会调用后端平台的接口拉取所有未读消息，并通过用户B在线流程向用户B推送消息
1. 创建PrivateMessageProcessor类实现MessageProcessor接口。process()方法中获取到消息的发送者ID和接收者ID，如果能通过接收者的ID和终端类型获取到链接信息，则封装消息进行发送（向消息中间件发送发送成功状态）。否则向消息中间件发送未找到的消息
2. 修改ProcessorFactory类
3. 创建BaseConsumer类，是消费消息中间件消息数据的基础消费者类
4. 创建PrivateMessageConsumer作为消息中间件中单聊的消费类。实现RocketMQPushConsumerLifecycleListener接口，实现preStart()方法，动态添加监听Topic，实现即时通讯后端服务集群中每个实例，都能动态监听与自身服务ID相关的Topic数据。

## 群聊处理器的设计与实现
> 群聊中发送消息的时候，通过群组ID找到群内所有在线的用户，将消息即时发给在线的用户。未在线的用户按照单聊未在线的用户进行处理。
1. GroupMessageProcessor类实现MessageProcessor接口，用来处理用户发送的群聊消息。process()方法中获取消息发送着和接收者列表，遍历接收者列表，获取到消息接收者终端与后端服务建立的链接，链接存在则推送消息。链接不存在则向消息中间件发送未找到的消息。
2. 修改ProcessorFactory类的getProcessor()方法中新增从IOC容器中获取群聊消息处理器的逻辑
3. GroupMessageConsumer类extends BaseMessageConsumer implements RocketMQListener<String>, RocketMQPushConsumerLifecycleListener。实现onMessage方法，接收到消息之后将其解析为CommonReceiveData。从ProcessorFactory中获取群聊消息处理器，调用process方法处理消息
GroupMessageConsumer同样实现了RocketMQPushConsumerLifecycleListener接口，实现preStart()方法，动态添加监听Topic，实现即时通讯后端服务集群中每个实例，都能动态监听与自身服务ID相关的Topic数据。
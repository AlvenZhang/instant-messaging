即时通讯系统后端设计
![./images/img.png](结构图)
> 负责即时通讯系统的业务逻辑功能。包括用户管理、好友管理、群组管理等等

# 后端服务通用模型
## 1、通用模型
领域对象模型：User、Friend（好友关系类）、Group（群组关系模型类）、GroupMember（群成员模型类）、PrivateMessage（单聊消息模型类）、GroupMessage（群聊消息模型类）、FileType（文件类型）、MessageType（消息类型）、MessageStatus（消息状态）、HttpStatus（HTTP状态码）

## 2、通用逻辑
> 后端平台分为：用户微服务、好友关系微服务、群组微服务、消息微服务。各个微服务之间独立存在。
> 
> 通用逻辑涉及Session机制、全局异常捕获、过滤器&拦截器链的实现

各个微服务之间都需要用户的Session机制、全局异常捕获机制和通用数据响应机制。这些可以放在一个公共模块中。
### Session机制
实体类：UserSession
- 属性：userName、nickName
上下文类：SessionContext
- 方法：getSession，从上下文中获取当前请求的session。使用RequestContextHolder类实现
### 全局异常捕获
异常类：IMException，继承RuntimeException，定义了异常码和异常信息
- 属性：code、message
全局异常捕获类：IMExceptionHandler，捕获IMException、Exception两种异常
### 响应数据模型
响应实体类：ResponseMessage
- 属性：code、message、data（泛型）
响应数据工厂类：ResponseMessageFactory，定义了一些静态方法，用于创建响应数据实体类

### 过滤器的设计与实现
#### 缓存参数过滤器
> 由于HTTP请求体只能读取一次，所以需要将请求参数缓存起来。

参数缓存类：CacheHttpServletRequestWrapper，继承HttpServletRequestWrapper
- 属性：byte[]类型的requestBody、request
参数过滤器类：CacheFilter，实现Filter接口。将封装好的参数缓存类通过doFilter方法传递给下一个过滤器。
#### 通用拦截器链
拦截器链服务接口：RuleChainService
- 方法：execute，抽象方法用于实现具体的拦截器逻辑。getOrder，返回当前拦截器服务的优先级
拦截器链基础抽象类：BaseRuleChainService
- 方法：getIp，获取当前请求的IP地址。getUserSession，获取当前请求的session
- 方法：getUserSession，获取当前请求的session。需要验证token，并从token中获取用户信息
拦截器抽象类：BaseInterceptor，实现Handlerinterceptor接口
- 属性：自动注入List<RuleChainService>类型的ruleChainServices。已定义的拦截器服务都会被注入
- 方法：getRuleChainServices，排序返回所有的拦截器服务
通用拦截器实现类：IMInterceptor，继承BaseInterceptor
- 方法：preHandle，拦截请求，依次执行已经排序的拦截器链服务
拦截器配置类：MvcConfig，实现WebMvcConfigurer接口
- 方法：addInterceptors，添加拦截器imInterceptor
#### XSS漏洞校验规则设计实现
> XSS漏洞是指攻击者通过在网页中注入恶意脚本代码，当用户浏览该网页时，恶意脚本代码会被执行，从而窃取用户信息或进行其他恶意行为。

可以在拦截器链上加一个拦截器服务节点实现XSS漏洞校验规则。只要有校验规则不通过，则不再继续向下执行。
定义拦截器类之后会自动被注入拦截器链中

XSS拦截器类：XssRuleChainService，继承BaseRuleChainService类
- 方法：execute，使用XssUtils.checkXss方法校验请求参数中是否包含XSS漏洞
#### 滑动窗口IP校验
> 基于用户终端IP、访问资源进行滑动窗口限流。通过拦截器链的方式实现。

滑动窗口接口：SlidingWindowLimitService
- 方法：passThough，能否通过滑动窗口的验证
滑动窗口实现类：RedisSlidingWindowLimitService，实现SlidingWindowLimitService接口
- 方法：passThough，使用Redis的zset实现滑动窗口限流
IP限制拦截器服务类：IPRuleChainService，继承BaseRuleChainService类
- 方法：execute，调用SlidingWindowLimitService的passThough方法进行IP限制
访问资源限制拦截器类：PathRuleChainService，继承BaseRuleChainService类
- 方法：execute，调用SlidingWindowLimitService的passThough方法，根据请求路径进行访问资源限制

tip：IP限制时Redis的key是请求的IP，资源限制的key是请求的路径、userId、terminalId
#### 账号安全校验（是否登陆）
账号校验拦截器服务类：AuthRuleChainService，继承BaseRuleChainService类
- 方法：execute，校验当前请求的session中是否包含用户信息。（用户信息通过BaseRuleChainService的getUserSession方法获得）

# 用户服务
## 用户注册与登陆授权
用户数据仓库接口：UserRepository，继承MyBatisPlus的BaseMapper。依靠MyBatisPlus实现功能，没有定义具体的接口。
用户领域层业务接口：UserDomainService，继承MyBatisPlus的IService
- 方法：getUserByUserName，查询用户信息。
- 方法：saveOrUpdateUser，保存或更新用户信息
用户领域层业务实现类：UserDomainServiceImpl，继承MyBatisPlus的ServiceImpl，实现UserDomainService接口，实现UserDomainService接口的两个方法
用户应用层接口：UserService
- 方法：register，注册用户
- 方法：login，登陆用户
用户应用层实现类：UserServiceImpl，实现UserService接口
- 方法：register，检查分布式缓存中是否存在用户信息，如果存在表示用户已存在。不存在则创建用户信息，使用saveOrUpdateUser方法保存用户信息
- 方法：login，从分布式缓存中获取用户信息，为空当前用户不存在。判断用户密码是否正确。将用户信息转换为Session信息，然后转换为token，并返回LoginVO
登陆注册表现层类：LoginController
- 方法：register，接收前端注册请求，调用UserService的register方法注册用户
- 方法：login，接收前端登陆请求，调用UserService的login方法登陆用户
MvcConfig配置类中配置BCrypt加密方法
## JWT Token刷新机制
UserService接口，新增refreshToken方法
UserServiceImpl实现类，添加refreshToken方法。生成accessToken和refreshToken，封装到LoginVO中返回
LoginController中，添加refreshToken方法。接收前端请求，调用UserService的refreshToken方法刷新token

## 领域事件的发送与接收
> 完成用户信息修改之后，需要同步到分布式缓存中。采用修改数据库之后，发布事件，服务订阅事件更新缓存的方式
> 
> 领域层修改完数据库之后，发布一个领域事件。应用层监听到事件之后异步处理分布式缓存中的数据

UserDomainServiceImpl类中，修改saveOrUpdateUser方法。修改完数据之后发布一个领域事件
用户事件模型：IMUserEvent类，有一个属性username

# 好友服务


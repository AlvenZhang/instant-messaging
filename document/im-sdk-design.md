业务系统需要使用即时通讯系统的时候，可以引入SDK。通过SDK实现发送单聊、群聊消息，在线状态，消息监听等能力

# 发送消息的设计与实现
## 发送单聊消息

## 发送群聊消息

# 在线状态
修改IMSender接口：添加三个方法，getOnlineTerminal、isOnline、getOnlineTerminal
修改DefaultIMSender类
- 方法：getOnlineTerminal，根据用户ID列表获取在线用户与其终端的对应关系。创建Map<分布式缓存key, IMUserInfo>，根据刚才创建的Map从分布式缓存中批量获取数据，封装返回一个Map<userId, List<IMTerminalType>>
- 方法：isOnline，判断某个用户是否在线。构建分布式缓存key，从分布式缓存中查找数据，找得到说明存在返回true，否则返回false
- 方法：getOnlineTerminal，根据用户ID列表筛选在线用户ID列表。直接调用getOnlineTerminal，将得到Map的keySet封装到List中并返回
修改IMClient接口：添加三个方法，getOnlineTerminal、isOnline、getOnlineTerminal
修改DefaultIMClient类：注入IMSender。调用IMSender刚添加的三个方法，实现IMClient的三个方法

# 消息监听与广播机制

# 接收消息发送结果
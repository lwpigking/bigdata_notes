#### 系统架构

HBase 系统遵循 Master/Salve 架构，由三种不同类型的组件组成：

**Zookeeper**

1. 保证任何时候，集群中只有一个 Master；

2. 存贮所有 Region 的寻址入口；

3. 实时监控 Region Server 的状态，将 Region Server 的上线和下线信息实时通知给 Master；

4. 存储 HBase 的 Schema，包括有哪些 Table，每个 Table 有哪些 Column Family 等信息。

**Master**

1. 为 Region Server 分配 Region ；

2. 负责 Region Server 的负载均衡 ；

3. 发现失效的 Region Server 并重新分配其上的 Region； 

4. GFS 上的垃圾文件回收；

5. 处理 Schema 的更新请求。

**Region Server**

1. Region Server 负责维护 Master 分配给它的 Region ，并处理发送到 Region 上的 IO 请求；

2. Region Server 负责切分在运行过程中变得过大的 Region。

#### 组件间的协作

 HBase 使用 ZooKeeper 作为分布式协调服务来维护集群中的服务器状态。 Zookeeper 负责维护可用服务列表，并提供服务故障通知等服务：

+ 每个 Region Server 都会在 ZooKeeper 上创建一个临时节点，Master 通过 Zookeeper 的 Watcher 机制对节点进行监控，从而可以发现新加入的 Region Server 或故障退出的 Region Server；

+ 所有 Masters 会竞争性地在 Zookeeper 上创建同一个临时节点，由于 Zookeeper 只能有一个同名节点，所以必然只有一个 Master 能够创建成功，此时该 Master 就是主 Master，主 Master 会定期向 Zookeeper 发送心跳。备用 Masters 则通过 Watcher 机制对主 HMaster 所在节点进行监听；

+ 如果主 Master 未能定时发送心跳，则其持有的 Zookeeper 会话会过期，相应的临时节点也会被删除，这会触发定义在该节点上的 Watcher 事件，使得备用的 Master Servers 得到通知。所有备用的 Master Servers 在接到通知后，会再次去竞争性地创建临时节点，完成主 Master 的选举。


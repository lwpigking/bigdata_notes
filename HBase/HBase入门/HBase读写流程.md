#### 写入数据的流程

1. Client 向 Region Server 提交写请求；

2. Region Server 找到目标 Region；

3. Region 检查数据是否与 Schema 一致；

4. 如果客户端没有指定版本，则获取当前系统时间作为数据版本；

5. 将更新写入 WAL Log；

6. 将更新写入 Memstore；

7. 判断 Memstore 存储是否已满，如果存储已满则需要 flush 为 Store Hfile 文件。



#### 读取数据的流程

以下是客户端首次读写 HBase 上数据的流程：

1. 客户端从 Zookeeper 获取 `META` 表所在的 Region Server；

2. 客户端访问 `META` 表所在的 Region Server，从 `META` 表中查询到访问行键所在的 Region Server，之后客户端将缓存这些信息以及 `META` 表的位置；

3. 客户端从行键所在的 Region Server 上获取数据。

如果再次读取，客户端将从缓存中获取行键所在的 Region Server。这样客户端就不需要再次查询 `META` 表，除非 Region 移动导致缓存失效，这样的话，则将会重新查询并更新缓存。

注：`META` 表是 HBase 中一张特殊的表，它保存了所有 Region 的位置信息，META 表自己的位置信息则存储在 ZooKeeper 上。


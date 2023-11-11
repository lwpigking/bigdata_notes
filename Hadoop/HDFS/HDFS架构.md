## HDFS架构

![HDFS架构](..\..\img\HDFS架构.png)

HDFS是一个主/从（Mater/Slave）体系结构，由三部分组成： **NameNode** 和 **DataNode** 以及 **SecondaryNamenode**：

- NameNode 负责管理整个**文件系统的元数据**，以及每一个路径（文件）所对应的数据块信息。
- DataNode 负责管理用户的**文件数据块**，每一个数据块都可以在多个 DataNode 上存储多个副本，默认为3个。
- Secondary NameNode 用来监控 HDFS 状态的辅助后台程序，每隔一段时间获取 HDFS 元数据的快照。最主要作用是**辅助 NameNode 管理元数据信息**。
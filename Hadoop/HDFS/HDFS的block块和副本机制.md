## HDFS的block块和副本机制

HDFS 将所有的文件全部抽象成为 block 块来进行存储，不管文件大小，全部一视同仁都是以 block 块的统一大小和形式进行存储，方便我们的分布式文件系统对文件的管理。

所有的文件都是以 block 块的方式存放在 hdfs 文件系统当中，在 Hadoop 1 版本当中，文件的 block 块默认大小是 64M，Hadoop 2 版本当中，文件的 block 块大小默认是128M，block块的大小可以通过 hdfs-site.xml 当中的配置文件进行指定。

```xml
<property>
    <name>dfs.block.size</name>
    <value>块大小 以字节为单位</value> //只写数值就可以
</property>
```

### 抽象为block块的好处

- 1. 一个文件有可能大于集群中任意一个磁盘 10T*3/128 = xxx块 2T，2T，2T 文件方式存—–>多个block块，这些block块属于一个文件
- 1. 使用块抽象而不是文件可以简化存储子系统
- 1. 块非常适合用于数据备份进而提供数据容错能力和可用性

### 块缓存

**通常 DataNode 从磁盘中读取块，但对于访问频繁的文件，其对应的块可能被显示的缓存在 DataNode 的内存中，以堆外块缓存的形式存在**。默认情况下，一个块仅缓存在一个DataNode的内存中，当然可以针对每个文件配置DataNode的数量。**作业调度器通过在缓存块的DataNode上运行任务，可以利用块缓存的优势提高读操作的性能**。

例如： 连接（join）操作中使用的一个小的查询表就是块缓存的一个很好的候选。 用户或应用通过在缓存池中增加一个cache directive来告诉namenode需要缓存哪些文件及存多久。缓存池（cache pool）是一个拥有管理缓存权限和资源使用的管理性分组。

例如:

一个文件 130M，会被切分成2个block块，保存在两个block块里面，实际占用磁盘130M空间，而不是占用256M的磁盘空间

### hdfs的文件权限验证

hdfs的文件权限机制与linux系统的文件权限机制类似

r:read w:write x:execute
权限x对于文件表示忽略，对于文件夹表示是否有权限访问其内容

如果linux系统用户zhangsan使用hadoop命令创建一个文件，那么这个文件在HDFS当中的owner就是zhangsan

HDFS文件权限的目的，防止好人做错事，而不是阻止坏人做坏事。HDFS相信你告诉我你是谁，你就是谁

### hdfs的副本因子

为了保证block块的安全性，也就是数据的安全性，在hadoop2当中，文件默认保存三个副本，我们可以更改副本数以提高数据的安全性

在hdfs-site.xml当中修改以下配置属性，即可更改文件的副本数

```xml
<property>
     <name>dfs.replication</name>
     <value>3</value>
</property>
```
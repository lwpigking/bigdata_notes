## HDFS高级命令

### HDFS文件限额配置

在多人共用HDFS的环境下，配置设置非常重要。特别是在 Hadoop 处理大量资料的环境，如果没有配额管理，很容易把所有的空间用完造成别人无法存取。**HDFS 的配额设定是针对目录而不是针对账号，可以让每个账号仅操作某一个目录，然后对目录设置配置**。

HDFS 文件的限额配置允许我们以文件个数，或者文件大小来限制我们在某个目录下上传的文件数量或者文件内容总量，以便达到我们类似百度网盘网盘等限制每个用户允许上传的最大的文件的量。

```shell
 hdfs dfs -count -q -h /user/root/dir1  #查看配额信息
```

![HDFS限额](..\..\img\HDFS限额.png)

#### 数量限额

```shell
hdfs dfs  -mkdir -p /user/root/dir    #创建hdfs文件夹
hdfs dfsadmin -setQuota 2  dir      # 给该文件夹下面设置最多上传两个文件，发现只能上传一个文件
```

------

```shell
hdfs dfsadmin -clrQuota /user/root/dir  # 清除文件数量限制
```

#### 空间大小限额

在设置空间配额时，设置的空间至少是 block_size * 3 大小

```shell
hdfs dfsadmin -setSpaceQuota 4k /user/root/dir   # 限制空间大小4KB
hdfs dfs -put  /root/a.txt  /user/root/dir 
```

1
2

生成任意大小文件的命令:

```shell
dd if=/dev/zero of=1.txt  bs=1M count=2     #生成2M的文件
```

1

清除空间配额限制

```shell
hdfs dfsadmin -clrSpaceQuota /user/root/dir
```

1

### HDFS 的安全模式

**安全模式是hadoop的一种保护机制，用于保证集群中的数据块的安全性**。当集群启动的时候，会首先进入安全模式。当系统处于安全模式时会检查数据块的完整性。

假设我们设置的副本数（即参数dfs.replication）是3，那么在datanode上就应该有3个副本存在，假设只存在2个副本，那么比例就是2/3=0.666。hdfs默认的副本率0.999。我们的副本率0.666明显小于0.999，因此系统会自动的复制副本到其他dataNode，使得副本率不小于0.999。如果系统中有5个副本，超过我们设定的3个副本，那么系统也会删除多于的2个副本。

**在安全模式状态下，文件系统只接受读数据请求，而不接受删除、修改等变更请求**。在，当整个系统达到安全标准时，HDFS自动离开安全模式。30s

安全模式操作命令

```shell
    hdfs  dfsadmin  -safemode  get #查看安全模式状态
    hdfs  dfsadmin  -safemode  enter #进入安全模式
    hdfs  dfsadmin  -safemode  leave #离开安全模式
```
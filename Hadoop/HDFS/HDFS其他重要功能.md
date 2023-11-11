## HDFS其他重要功能

### 多个集群之间的数据拷贝

在我们实际工作当中，极有可能会遇到将测试集群的数据拷贝到生产环境集群，或者将生产环境集群的数据拷贝到测试集群，那么就需要我们在多个集群之间进行数据的远程拷贝，hadoop自带也有命令可以帮我们实现这个功能

`1.本地文件拷贝scp`

```shell
cd /export/softwares/

scp -r jdk-8u141-linux-x64.tar.gz root@node02:/export/
```

`2.集群之间的数据拷贝distcp`

```shell
cd /export/servers/hadoop-2.6.0-cdh5.14.0/

bin/hadoop distcp hdfs://node01:8020/jdk-8u141-linux-x64.tar.gz  hdfs://cluster2:8020/
```

### hadoop归档文件archive

每个文件均按块存储，每个块的元数据存储在namenode的内存中，因此hadoop存储小文件会非常低效。因为大量的小文件会耗尽namenode中的大部分内存。但注意，存储小文件所需要的磁盘容量和存储这些文件原始内容所需要的磁盘空间相比也不会增多。例如，一个1MB的文件以大小为128MB的块存储，使用的是1MB的磁盘空间，而不是128MB。

Hadoop存档文件或HAR文件，是一个更高效的文件存档工具，它将文件存入HDFS块，在减少namenode内存使用的同时，允许对文件进行透明的访问。具体说来，Hadoop存档文件可以用作MapReduce的输入。

创建归档文件

- `第一步：创建归档文件`

  注意：归档文件一定要保证yarn集群启动

```text
cd /export/servers/hadoop-2.6.0-cdh5.14.0

bin/hadoop archive -archiveName myhar.har -p /user/root /user
```

- `第二步：查看归档文件内容`

```text
hdfs dfs -lsr /user/myhar.har

hdfs dfs -lsr har:///user/myhar.har
```

- `第三步：解压归档文件`

```text
hdfs dfs -mkdir -p /user/har
hdfs dfs -cp har:///user/myhar.har/* /user/har/
```

### hdfs快照snapShot管理

快照顾名思义，就是相当于对我们的hdfs文件系统做一个备份，我们可以通过快照对我们指定的文件夹设置备份，但是添加快照之后，并不会立即复制所有文件，而是指向同一个文件。当写入发生时，才会产生新文件

`快照使用基本语法`

```text
1、 开启指定目录的快照功能
	hdfs dfsadmin  -allowSnapshot  路径 
2、禁用指定目录的快照功能（默认就是禁用状态）
	hdfs dfsadmin  -disallowSnapshot  路径
3、给某个路径创建快照snapshot
	hdfs dfs -createSnapshot  路径
4、指定快照名称进行创建快照snapshot
	hdfs dfs  -createSanpshot 路径 名称    
5、给快照重新命名
	hdfs dfs  -renameSnapshot  路径 旧名称  新名称
6、列出当前用户所有可快照目录
	hdfs lsSnapshottableDir  
7、比较两个快照的目录不同之处
	hdfs snapshotDiff  路径1  路径2
8、删除快照snapshot
	hdfs dfs -deleteSnapshot <path> <snapshotName> 
```

`快照操作实际案例`

```text
1、开启与禁用指定目录的快照

    [root@node01 hadoop-2.6.0-cdh5.14.0]# hdfs dfsadmin -allowSnapshot /user

    Allowing snaphot on /user succeeded

    [root@node01 hadoop-2.6.0-cdh5.14.0]# hdfs dfsadmin -disallowSnapshot /user

    Disallowing snaphot on /user succeeded

2、对指定目录创建快照

	注意：创建快照之前，先要允许该目录创建快照

    [root@node01 hadoop-2.6.0-cdh5.14.0]# hdfs dfsadmin -allowSnapshot /user

    Allowing snaphot on /user succeeded

    [root@node01 hadoop-2.6.0-cdh5.14.0]# hdfs dfs -createSnapshot /user    

    Created snapshot /user/.snapshot/s20190317-210906.549

	通过web浏览器访问快照

	http://node01:50070/explorer.html#/user/.snapshot/s20190317-210906.549

3、指定名称创建快照

    [root@node01 hadoop-2.6.0-cdh5.14.0]# hdfs dfs -createSnapshot /user mysnap1

    Created snapshot /user/.snapshot/mysnap1

4、重命名快照

	hdfs dfs -renameSnapshot /user mysnap1 mysnap2
 
5、列出当前用户所有可以快照的目录

	hdfs lsSnapshottableDir

6、比较两个快照不同之处

    hdfs dfs -createSnapshot /user snap1

    hdfs dfs -createSnapshot /user snap2

    hdfs snapshotDiff  snap1 snap2

7、删除快照

	hdfs dfs -deleteSnapshot /user snap1
```

### hdfs回收站

任何一个文件系统，基本上都会有垃圾桶机制，也就是删除的文件，不会直接彻底清掉，我们一把都是将文件放置到垃圾桶当中去，过一段时间之后，自动清空垃圾桶当中的文件，这样对于文件的安全删除比较有保证，避免我们一些误操作，导致误删除文件或者数据

`回收站配置两个参数`

```
默认值fs.trash.interval=0，0表示禁用回收站，可以设置删除文件的存活时间。

默认值fs.trash.checkpoint.interval=0，检查回收站的间隔时间。
```

要求fs.trash.checkpoint.interval<=fs.trash.interval。

`启用回收站`

修改所有服务器的core-site.xml配置文件

```xml
<!--  开启hdfs的垃圾桶机制，删除掉的数据可以从垃圾桶中回收，单位分钟 -->
        <property>
                <name>fs.trash.interval</name>
                <value>10080</value>
        </property>
```

`查看回收站`

回收站在集群的 /user/root/.Trash/ 这个路径下

`通过javaAPI删除的数据，不会进入回收站，需要调用moveToTrash()才会进入回收站`

```java
//使用回收站的方式: 删除数据
    @Test
    public void  deleteFile() throws Exception{
        //1. 获取FileSystem对象
        Configuration configuration = new Configuration();
        FileSystem fileSystem = FileSystem.get(new URI("hdfs://node01:8020"), configuration, "root");
        //2. 执行删除操作
        // fileSystem.delete();  这种操作会直接将数据删除, 不会进入垃圾桶
        Trash trash = new Trash(fileSystem,configuration);
        boolean flag = trash.isEnabled(); // 是否已经开启了垃圾桶机制
        System.out.println(flag);

        trash.moveToTrash(new Path("/quota"));

        //3. 释放资源
        fileSystem.close();

    }
```

`恢复回收站数据`

hdfs dfs -mv trashFileDir hdfsdir

trashFileDir ：回收站的文件路径

hdfsdir ：将文件移动到hdfs的哪个路径下

`清空回收站`

hdfs dfs -expunge
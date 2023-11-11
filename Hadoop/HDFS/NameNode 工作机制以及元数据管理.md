## NameNode 工作机制以及元数据管理

![NameNode工作机制](..\..\img\NameNode工作机制.png)

### namenode 与 datanode 启动

- **namenode工作机制**

1. 第一次启动namenode格式化后，创建fsimage和edits文件。如果不是第一次启动，直接加载编辑日志和镜像文件到内存。
2. 客户端对元数据进行增删改的请求。
3. namenode记录操作日志，更新滚动日志。
4. namenode在内存中对数据进行增删改查。

- **secondary namenode**

1. secondary namenode询问 namenode 是否需要 checkpoint。直接带回 namenode 是否检查结果。
2. secondary namenode 请求执行 checkpoint。
3. namenode 滚动正在写的edits日志。
4. 将滚动前的编辑日志和镜像文件拷贝到 secondary namenode。
5. secondary namenode 加载编辑日志和镜像文件到内存，并合并。
6. 生成新的镜像文件 fsimage.chkpoint。
7. 拷贝 fsimage.chkpoint 到 namenode。
8. namenode将 fsimage.chkpoint 重新命名成fsimage。

### FSImage与edits详解

所有的元数据信息都保存在了FsImage与Eidts文件当中，这两个文件就记录了所有的数据的元数据信息，元数据信息的保存目录配置在了 **hdfs-site.xml** 当中

```xml
		<!--fsimage文件存储的路径-->
		<property>
                <name>dfs.namenode.name.dir</name>
                <value>file:///opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/namenodeDatas</value>
        </property>
        <!-- edits文件存储的路径 -->
		<property>
                <name>dfs.namenode.edits.dir</name>
                <value>file:///opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/dfs/nn/edits</value>
  		</property>
```

客户端对hdfs进行写文件时会首先被记录在edits文件中。

edits修改时元数据也会更新。

每次hdfs更新时edits先更新后客户端才会看到最新信息。

fsimage：是namenode中关于元数据的镜像，一般称为检查点。

一般开始时对namenode的操作都放在edits中，为什么不放在fsimage中呢？

因为fsimage是namenode的完整的镜像，内容很大，如果每次都加载到内存的话生成树状拓扑结构，这是非常耗内存和CPU。

fsimage内容包含了namenode管理下的所有datanode中文件及文件block及block所在的datanode的元数据信息。随着edits内容增大，就需要在一定时间点和fsimage合并。

### FSimage文件当中的文件信息查看

- 使用命令 hdfs oiv

```text
cd  /opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/namenodeDatas/current
hdfs oiv -i fsimage_0000000000000000112 -p XML -o hello.xml
```

### edits当中的文件信息查看

- 查看命令 hdfs oev

```text
cd  /opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/dfs/nn/edits
hdfs oev -i  edits_0000000000000000112-0000000000000000113 -o myedit.xml -p XML
```

### secondarynameNode如何辅助管理FSImage与Edits文件

1. secnonaryNN通知NameNode切换editlog。
2. secondaryNN从NameNode中获得FSImage和editlog(通过http方式)。
3. secondaryNN将FSImage载入内存，然后开始合并editlog，合并之后成为新的fsimage。
4. secondaryNN将新的fsimage发回给NameNode。
5. NameNode用新的fsimage替换旧的fsimage。

![NameNode工作机制2](..\..\img\NameNode工作机制2.png)

完成合并的是 secondarynamenode，会请求namenode停止使用edits，暂时将新写操作放入一个新的文件中（edits.new)。

secondarynamenode从namenode中**通过http get获得edits**，因为要和fsimage合并，所以也是通过http get 的方式把fsimage加载到内存，然后逐一执行具体对文件系统的操作，与fsimage合并，生成新的fsimage，然后把fsimage发送给namenode，**通过http post的方式**。

namenode从secondarynamenode获得了fsimage后会把原有的fsimage替换为新的fsimage，把edits.new变成edits。同时会更新fsimage。

hadoop进入安全模式时需要管理员使用dfsadmin的save namespace来创建新的检查点。

secondarynamenode在合并edits和fsimage时需要消耗的内存和namenode差不多，所以一般把namenode和secondarynamenode放在不同的机器上。

fsimage与edits的合并时机取决于两个参数，第一个参数是默认1小时fsimage与edits合并一次。

- 第一个参数：时间达到一个小时fsimage与edits就会进行合并

```text
dfs.namenode.checkpoint.period     3600
```

- 第二个参数：hdfs操作达到1000000次也会进行合并

```text
dfs.namenode.checkpoint.txns       1000000
```

- 第三个参数：每隔多长时间检查一次hdfs的操作次数

```text
dfs.namenode.checkpoint.check.period   60
```

### namenode元数据信息多目录配置

为了保证元数据的安全性，我们一般都是先确定好我们的磁盘挂载目录，将元数据的磁盘做RAID1

namenode的本地目录可以配置成多个，且每个目录存放内容相同，增加了可靠性。

- 具体配置方案:

  **hdfs-site.xml**

```text
	<property>
         <name>dfs.namenode.name.dir</name>
         <value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/namenodeDatas</value>
    </property>
```

### namenode故障恢复

在我们的secondaryNamenode对namenode当中的fsimage和edits进行合并的时候，每次都会先将namenode的fsimage与edits文件拷贝一份过来，所以fsimage与edits文件在secondarNamendoe当中也会保存有一份，如果namenode的fsimage与edits文件损坏，那么我们可以将secondaryNamenode当中的fsimage与edits拷贝过去给namenode继续使用，只不过有可能会丢失一部分数据。这里涉及到几个配置选项

- namenode保存fsimage的配置路径

```text
<!--  namenode元数据存储路径，实际工作当中一般使用SSD固态硬盘，并使用多个固态硬盘隔开，冗余元数据 -->
	<property>
		<name>dfs.namenode.name.dir</name>
		<value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/namenodeDatas</value>
	</property>
```

- namenode保存edits文件的配置路径

```text
<property>
		<name>dfs.namenode.edits.dir</name>
		<value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/dfs/nn/edits</value>
</property>
```

- secondaryNamenode保存fsimage文件的配置路径

```text
<property>
		<name>dfs.namenode.checkpoint.dir</name>
		<value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/dfs/snn/name</value>
</property>
```

- secondaryNamenode保存edits文件的配置路径

```text
<property>
		<name>dfs.namenode.checkpoint.edits.dir</name>
		<value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/dfs/nn/snn/edits</value>
</property>
```

**接下来我们来模拟namenode的故障恢复功能**

1.杀死namenode进程: 使用jps查看namenode的进程号 , kill -9 直接杀死。

2.删除namenode的fsimage文件和edits文件。

> 根据上述配置, 找到namenode放置fsimage和edits路径. 直接全部rm -rf 删除。

3.拷贝secondaryNamenode的fsimage与edits文件到namenode的fsimage与edits文件夹下面去。

> 根据上述配置, 找到secondaryNamenode的fsimage和edits路径, 将内容 使用cp -r 全部复制到namenode对应的目录下即可。

4.重新启动namenode, 观察数据是否存在。
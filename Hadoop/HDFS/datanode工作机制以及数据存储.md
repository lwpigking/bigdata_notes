## datanode工作机制以及数据存储

- **datanode工作机制**

1. 一个数据块在datanode上以文件形式存储在磁盘上，包括两个文件，一个是数据本身，一个是元数据包括数据块的长度，块数据的校验和，以及时间戳。
2. DataNode启动后向namenode注册，通过后，周期性（1小时）的向namenode上报所有的块信息。(dfs.blockreport.intervalMsec)。
3. 心跳是每3秒一次，心跳返回结果带有namenode给该datanode的命令如复制块数据到另一台机器，或删除某个数据块。如果超过10分钟没有收到某个datanode的心跳，则认为该节点不可用。
4. 集群运行中可以安全加入和退出一些机器。

- **数据完整性**

1. 当DataNode读取block的时候，它会计算checksum。
2. 如果计算后的checksum，与block创建时值不一样，说明block已经损坏。
3. client读取其他DataNode上的block。
4. datanode在其文件创建后周期验证checksum。

- **掉线时限参数设置**

datanode进程死亡或者网络故障造成datanode无法与namenode通信，namenode不会立即把该节点判定为死亡，要经过一段时间，这段时间暂称作超时时长。HDFS默认的超时时长为10分钟+30秒。如果定义超时时间为timeout，则超时时长的计算公式为：

**timeout = 2 \* dfs.namenode.heartbeat.recheck-interval + 10 \* dfs.heartbeat.interval。**

而默认的dfs.namenode.heartbeat.recheck-interval 大小为5分钟，dfs.heartbeat.interval默认为3秒。

需要注意的是hdfs-site.xml **配置文件中的heartbeat.recheck.interval的单位为毫秒**，**dfs.heartbeat.interval的单位为秒**。

```xml
<property>
    <name>dfs.namenode.heartbeat.recheck-interval</name>
    <value>300000</value>
</property>
<property>
    <name>dfs.heartbeat.interval </name>
    <value>3</value>
</property>
```

- **DataNode的目录结构**

  和namenode不同的是，datanode的存储目录是初始阶段自动创建的，不需要额外格式化。

在/opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/datanodeDatas/current这个目录下查看版本号

```shell
    cat VERSION 
    
    #Thu Mar 14 07:58:46 CST 2019
    storageID=DS-47bcc6d5-c9b7-4c88-9cc8-6154b8a2bf39
    clusterID=CID-dac2e9fa-65d2-4963-a7b5-bb4d0280d3f4
    cTime=0
    datanodeUuid=c44514a0-9ed6-4642-b3a8-5af79f03d7a4
    storageType=DATA_NODE
    layoutVersion=-56
```

具体解释:

storageID：存储id号。

clusterID集群id，全局唯一。

cTime属性标记了datanode存储系统的创建时间，对于刚刚格式化的存储系统，这个属性为0；但是在文件系统升级之后，该值会更新到新的时间戳。

datanodeUuid：datanode的唯一识别码。

storageType：存储类型。

layoutVersion是一个负整数。通常只有HDFS增加新特性时才会更新这个版本号。

- **datanode多目录配置**

datanode也可以配置成多个目录，每个目录存储的数据不一样。即：数据不是副本。具体配置如下： - 只需要在value中使用逗号分隔出多个存储目录即可

```shell
  cd /opt/hadoop-2.6.0-cdh5.14.0/etc/hadoop
  <!--  定义dataNode数据存储的节点位置，实际工作中，一般先确定磁盘的挂载目录，然后多个目录用，进行分割  -->
          <property>
                  <name>dfs.datanode.data.dir</name>
                  <value>file:///opt/hadoop-2.6.0-cdh5.14.0/hadoopDatas/datanodeDatas</value>
          </property>
```

### 服役新数据节点

需求说明:

随着公司业务的增长，数据量越来越大，原有的数据节点的容量已经不能满足存储数据的需求，需要在原有集群基础上动态添加新的数据节点。

#### 环境准备

复制一台新的虚拟机出来

> 将我们纯净的虚拟机复制一台出来，作为我们新的节点

修改mac地址以及IP地址

```shell
修改mac地址命令
	vim /etc/udev/rules.d/70-persistent-net.rules
修改ip地址命令
	vim /etc/sysconfig/network-scripts/ifcfg-eth0
```

关闭防火墙，关闭selinux

```shell
关闭防火墙
	service iptables stop
关闭selinux
	vim /etc/selinux/config
```

更改主机名

```shell
更改主机名命令，将node04主机名更改为node04.hadoop.com
vim /etc/sysconfig/network
```

四台机器更改主机名与IP地址映射

```shell
四台机器都要添加hosts文件
vim /etc/hosts

192.168.52.100 node01.hadoop.com  node01
192.168.52.110 node02.hadoop.com  node02
192.168.52.120 node03.hadoop.com  node03
192.168.52.130 node04.hadoop.com  node04
```

node04服务器关机重启

```shell
node04执行以下命令关机重启
	reboot -h now
```

node04安装jdk

```shell
node04统一两个路径
	mkdir -p /export/softwares/
	mkdir -p /export/servers/
```

**然后解压jdk安装包，配置环境变量**

解压hadoop安装包

```shell
在node04服务器上面解压hadoop安装包到/export/servers , node01执行以下命令将hadoop安装包拷贝到node04服务器
	cd /export/softwares/
	scp hadoop-2.6.0-cdh5.14.0-自己编译后的版本.tar.gz node04:$PWD

node04解压安装包
	tar -zxf hadoop-2.6.0-cdh5.14.0-自己编译后的版本.tar.gz -C /export/servers/
```

将node01关于hadoop的配置文件全部拷贝到node04

```shell
node01执行以下命令，将hadoop的配置文件全部拷贝到node04服务器上面
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop/
	scp ./* node04:$PWD
```

#### 服役新节点具体步骤

创建dfs.hosts文件

```shell
在node01也就是namenode所在的机器的/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop目录下创建dfs.hosts文件

[root@node01 hadoop]# cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
[root@node01 hadoop]# touch dfs.hosts
[root@node01 hadoop]# vim dfs.hosts

添加如下主机名称（包含新服役的节点）
node01
node02
node03
node04
```

node01编辑hdfs-site.xml添加以下配置

> 在namenode的hdfs-site.xml配置文件中增加dfs.hosts属性

```shell
node01执行以下命令 :

cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
vim hdfs-site.xml

# 添加一下内容
	<property>
         <name>dfs.hosts</name>
         <value>/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop/dfs.hosts</value>
    </property>
    <!--动态上下线配置: 如果配置文件中有, 就不需要配置-->
    <property>
		<name>dfs.hosts</name>
		<value>/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop/accept_host</value>
	</property>
	
	<property>
		<name>dfs.hosts.exclude</name>
		<value>/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop/deny_host</value>
	</property>
```

刷新namenode

- node01执行以下命令刷新namenode

```shell
[root@node01 hadoop]# hdfs dfsadmin -refreshNodes
Refresh nodes successful
```

更新resourceManager节点

- node01执行以下命令刷新resourceManager

```shell
[root@node01 hadoop]# yarn rmadmin -refreshNodes
19/03/16 11:19:47 INFO client.RMProxy: Connecting to ResourceManager at node01/192.168.52.100:8033
```

namenode的slaves文件增加新服务节点主机名称

> node01编辑slaves文件，并添加新增节点的主机，更改完后，slaves文件不需要分发到其他机器上面去

```shell
node01执行以下命令编辑slaves文件 :
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
	vim slaves
	
添加一下内容: 	
node01
node02
node03
node04
```

单独启动新增节点

```shell
node04服务器执行以下命令，启动datanode和nodemanager : 
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/
	sbin/hadoop-daemon.sh start datanode
	sbin/yarn-daemon.sh start nodemanager
```

使用负载均衡命令，让数据均匀负载所有机器

```shell
node01执行以下命令 : 
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/
	sbin/start-balancer.sh
```

### 退役旧数据

创建dfs.hosts.exclude配置文件

在namenod所在服务器的/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop目录下创建dfs.hosts.exclude文件，并添加需要退役的主机名称

```shell
node01执行以下命令 : 
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
	touch dfs.hosts.exclude
	vim dfs.hosts.exclude
添加以下内容:
	node04.hadoop.com

特别注意：该文件当中一定要写真正的主机名或者ip地址都行，不能写node04
```

编辑namenode所在机器的hdfs-site.xml

> 编辑namenode所在的机器的hdfs-site.xml配置文件，添加以下配置

```shell
cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
vim hdfs-site.xml

#添加一下内容:
	<property>
         <name>dfs.hosts.exclude</name>
         <value>/export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop/dfs.hosts.exclude</value>
   </property>
```

刷新namenode，刷新resourceManager

```shell
在namenode所在的机器执行以下命令，刷新namenode，刷新resourceManager : 

hdfs dfsadmin -refreshNodes
yarn rmadmin -refreshNodes
```

节点退役完成，停止该节点进程

等待退役节点状态为decommissioned（所有块已经复制完成），停止该节点及节点资源管理器。注意：如果副本数是3，服役的节点小于等于3，是不能退役成功的，需要修改副本数后才能退役。

```shell
node04执行以下命令，停止该节点进程 : 
	cd /export/servers/hadoop-2.6.0-cdh5.14.0
	sbin/hadoop-daemon.sh stop datanode
	sbin/yarn-daemon.sh stop nodemanager
```

从include文件中删除退役节点

```shell
namenode所在节点也就是node01执行以下命令删除退役节点 :
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
	vim dfs.hosts
	
删除后的内容: 删除了node04
node01
node02
node03
```

node01执行一下命令刷新namenode，刷新resourceManager

```shell
hdfs dfsadmin -refreshNodes
yarn rmadmin -refreshNodes
```

从namenode的slave文件中删除退役节点

```shell
namenode所在机器也就是node01执行以下命令从slaves文件中删除退役节点 : 
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
	vim slaves
删除后的内容: 删除了 node04 
node01
node02
node03
```

如果数据负载不均衡，执行以下命令进行均衡负载

```shell
node01执行以下命令进行均衡负载
	cd /export/servers/hadoop-2.6.0-cdh5.14.0/
	sbin/start-balancer.sh
```
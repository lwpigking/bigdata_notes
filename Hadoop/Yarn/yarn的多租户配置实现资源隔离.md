## yarn的多租户配置实现资源隔离

资源隔离目前有2种，静态隔离和动态隔离。

**静态隔离**:

所谓静态隔离是以服务隔离，是通过cgroups（LINUX control groups) 功能来支持的。比如HADOOP服务包含HDFS, HBASE, YARN等等，那么我们固定的设置比例，HDFS:20%, HBASE:40%, YARN：40%， 系统会帮我们根据整个集群的CPU，内存，IO数量来分割资源，先提一下，IO是无法分割的，所以只能说当遇到IO问题时根据比例由谁先拿到资源，CPU和内存是预先分配好的。

上面这种按照比例固定分割就是静态分割了，仔细想想，这种做法弊端太多，假设我按照一定的比例预先分割好了，但是如果我晚上主要跑mapreduce, 白天主要是HBASE工作，这种情况怎么办？ 静态分割无法很好的支持，缺陷太大，这种模型可能不太合适。

**动态隔离**:

动态隔离只要是针对 YARN以及impala, 所谓动态只是相对静态来说，其实也不是动态。 先说YARN， 在HADOOP整个环境，主要服务有哪些？ mapreduce（这里再提一下，mapreduce是应用，YARN是框架，搞清楚这个概念），HBASE, HIVE，SPARK，HDFS，IMPALA， 实际上主要的大概这些，很多人估计会表示不赞同，oozie, ES, storm , kylin，flink等等这些和YARN离的太远了，不依赖YARN的资源服务，而且这些服务都是单独部署就OK，关联性不大。 所以主要和YARN有关也就是HIVE, SPARK，Mapreduce。这几个服务也正式目前用的最多的（HBASE用的也很多，但是和YARN没啥关系）。

根据上面的描述，大家应该能理解为什么所谓的动态隔离主要是针对YARN。好了，既然YARN占的比重这么多，那么如果能很好的对YARN进行资源隔离，那也是不错的。如果我有3个部分都需要使用HADOOP，那么我希望能根据不同部门设置资源的优先级别，实际上也是根据比例来设置，建立3个queue name, 开发部们30%，数据分析部分50%，运营部门20%。

设置了比例之后，再提交JOB的时候设置mapreduce.queue.name，那么JOB就会进入指定的队列里面。 非常可惜的是，如果你指定了一个不存在的队列，JOB仍然可以执行，这个是目前无解的，默认提交JOB到YARN的时候，规则是root.users.username ， 队列不存在，会自动以这种格式生成队列名称。 队列设置好之后，再通过ACL来控制谁能提交或者KIll job。

> 从上面2种资源隔离来看，没有哪一种做的很好，如果非要选一种，建议选取后者，隔离YARN资源， 第一种固定分割服务的方式实在支持不了现在的业务

需求：现在一个集群当中，可能有多个用户都需要使用，例如开发人员需要提交任务，测试人员需要提交任务，以及其他部门工作同事也需要提交任务到集群上面去，对于我们多个用户同时提交任务，我们可以通过配置yarn的多用户资源隔离来进行实现

1. node01编辑yarn-site.xml

```xml
<!--  指定我们的任务调度使用fairScheduler的调度方式  -->
<property>
	<name>yarn.resourcemanager.scheduler.class</name>
	<value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair.FairScheduler</value>
</property>

<!--  指定我们的任务调度的配置文件路径  -->
<property>
	<name>yarn.scheduler.fair.allocation.file</name>
	<value>/etc/hadoop/fair-scheduler.xml</value>
</property>

<!-- 是否启用资源抢占，如果启用，那么当该队列资源使用
yarn.scheduler.fair.preemption.cluster-utilization-threshold 这么多比例的时候，就从其他空闲队列抢占资源
  -->
<property>
	<name>yarn.scheduler.fair.preemption</name>
	<value>true</value>
</property>
<property>
	<name>yarn.scheduler.fair.preemption.cluster-utilization-threshold</name>
	<value>0.8f</value>
</property>


<!-- 默认提交到default队列  -->
<property>
	<name>yarn.scheduler.fair.user-as-default-queue</name>
	<value>true</value>
	<description>default is True</description>
</property>

<!-- 如果提交一个任务没有到任何的队列，是否允许创建一个新的队列，设置false不允许  -->
<property>
	<name>yarn.scheduler.fair.allow-undeclared-pools</name>
	<value>false</value>
	<description>default is True</description>
</property>
```



1. node01添加fair-scheduler.xml配置文件

```xml
<?xml version="1.0"?>
<allocations>
<!-- users max running apps  -->
<userMaxAppsDefault>30</userMaxAppsDefault>
<!-- 定义我们的队列  -->
<queue name="root">
	<minResources>512mb,4vcores</minResources>
	<maxResources>102400mb,100vcores</maxResources>
	<maxRunningApps>100</maxRunningApps>
	<weight>1.0</weight>
	<schedulingMode>fair</schedulingMode>
	<aclSubmitApps> </aclSubmitApps>
	<aclAdministerApps> </aclAdministerApps>

	<queue name="default">
		<minResources>512mb,4vcores</minResources>
		<maxResources>30720mb,30vcores</maxResources>
		<maxRunningApps>100</maxRunningApps>
		<schedulingMode>fair</schedulingMode>
		<weight>1.0</weight>
		<!--  所有的任务如果不指定任务队列，都提交到default队列里面来 -->
		<aclSubmitApps>*</aclSubmitApps>
	</queue>

<!-- 

weight
资源池权重

aclSubmitApps
允许提交任务的用户名和组；
格式为： 用户名 用户组

当有多个用户时候，格式为：用户名1,用户名2 用户名1所属组,用户名2所属组

aclAdministerApps
允许管理任务的用户名和组；

格式同上。
 -->
	<queue name="hadoop">
		<minResources>512mb,4vcores</minResources>
		<maxResources>20480mb,20vcores</maxResources>
		<maxRunningApps>100</maxRunningApps>
		<schedulingMode>fair</schedulingMode>
		<weight>2.0</weight>
		<aclSubmitApps>hadoop hadoop</aclSubmitApps>
		<aclAdministerApps>hadoop hadoop</aclAdministerApps>
	</queue>

	<queue name="develop">
		<minResources>512mb,4vcores</minResources>
		<maxResources>20480mb,20vcores</maxResources>
		<maxRunningApps>100</maxRunningApps>
		<schedulingMode>fair</schedulingMode>
		<weight>1</weight>
		<aclSubmitApps>develop develop</aclSubmitApps>
		<aclAdministerApps>develop develop</aclAdministerApps>
	</queue>

	<queue name="test1">
		<minResources>512mb,4vcores</minResources>
		<maxResources>20480mb,20vcores</maxResources>
		<maxRunningApps>100</maxRunningApps>
		<schedulingMode>fair</schedulingMode>
		<weight>1.5</weight>
		<aclSubmitApps>test1,hadoop,develop test1</aclSubmitApps>
		<aclAdministerApps>test1 group_businessC,supergroup</aclAdministerApps>
	</queue>
</queue>
</allocations>
```



1. 将修改后的配置文件拷贝到其他机器上

```shell
cd /export/servers/hadoop-2.6.0-cdh5.14.0/etc/hadoop
[root@node01 hadoop]# scp yarn-site.xml  fair-scheduler.xml node02:$PWD
[root@node01 hadoop]# scp yarn-site.xml  fair-scheduler.xml node03:$PWD
```

1. 重启yarn集群

```shell
[root@node01 hadoop]# cd /export/servers/hadoop-2.6.0-cdh5.14.0/
[root@node01 hadoop-2.6.0-cdh5.14.0]# sbin/stop-yarn.sh
[root@node01 hadoop-2.6.0-cdh5.14.0]# sbin/start-yarn.sh
```

1. 创建普通用户hadoop

```text
useradd hadoop
passwd hadoop
```



1. 修改文件夹权限

node01执行以下命令，修改hdfs上面tmp文件夹的权限，不然普通用户执行任务的时候会抛出权限不足的异常

```text
groupadd supergroup
usermod -a -G supergroup hadoop
su - root -s /bin/bash -c "hdfs dfsadmin -refreshUserToGroupsMappings"
```

1. 使用hadoop用户提交mr任务

node01执行以下命令，切换到普通用户hadoop，然后使用hadoop来提交mr的任务

```shell
[root@node01 hadoop-2.6.0-cdh5.14.0]# su hadoop
[hadoop@node01 hadoop-2.6.0-cdh5.14.0]$ yarn jar /export/servers/hadoop-2.6.0-cdh5.14.0/share/hadoop/mapreduce/hadoop-mapreduce-examples-2.6.0-cdh5.14.0.jar pi 10 20
```
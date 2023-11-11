## block块手动拼接成为完整数据

所有的数据都是以一个个的block块存储的，只要我们能够将文件的所有block块全部找出来，拼接到一起，又会成为一个完整的文件，接下来我们就来通过命令将文件进行拼接:

`1.上传一个大于128M的文件到hdfs上面去`

我们选择一个大于128M的文件上传到hdfs上面去，只有一个大于128M的文件才会有多个block块。

这里我们选择将我们的jdk安装包上传到hdfs上面去。

node01执行以下命令上传jdk安装包

```shell
cd /export/softwares/
hdfs dfs -put jdk-8u141-linux-x64.tar.gz  /
```

`2.web浏览器界面查看jdk的两个block块id`

这里我们看到两个block块id分别为

1073742699和1073742700

那么我们就可以通过blockid将我们两个block块进行手动拼接了。

`3.根据我们的配置文件找到block块所在的路径`

```shell
根据我们hdfs-site.xml的配置，找到datanode所在的路径
<!--  定义dataNode数据存储的节点位置，实际工作中，一般先确定磁盘的挂载目录，然后多个目录用，进行分割  -->
        <property>
                <name>dfs.datanode.data.dir</name>
                <value>file:///export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/datanodeDatas</value>
        </property>
        
        
进入到以下路径 : 此基础路径为 上述配置中value的路径
cd /export/servers/hadoop-2.6.0-cdh5.14.0/hadoopDatas/datanodeDatas/current/BP-557466926-192.168.52.100-1549868683602/current/finalized/subdir0/subdir3
```

`4.执行block块的拼接`

```shell
将不同的各个block块按照顺序进行拼接起来，成为一个完整的文件
cat blk_1073742699 >> jdk8u141.tar.gz
cat blk_1073742700 >> jdk8u141.tar.gz
移动我们的jdk到/export路径，然后进行解压
mv  jdk8u141.tar.gz /export/
cd /export/
tar -zxf jdk8u141.tar.gz
正常解压，没有问题，说明我们的程序按照block块存储没有问题
```
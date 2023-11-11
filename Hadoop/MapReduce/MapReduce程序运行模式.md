## MapReduce程序运行模式

- 本地运行模式

1. mapreduce程序是被提交给LocalJobRunner在本地以单进程的形式运行
2. 而处理的数据及输出结果可以在本地文件系统，也可以在hdfs上
3. 怎样实现本地运行？写一个程序，不要带集群的配置文件本质是程序的conf中是否有`mapreduce.framework.name=local`以及`yarn.resourcemanager.hostname=local`参数
4. 本地模式非常便于进行业务逻辑的debug，只要在idea中打断点即可

【本地模式运行代码设置】

```text
configuration.set("mapreduce.framework.name","local");
configuration.set("yarn.resourcemanager.hostname","local");
-----------以上两个是不需要修改的,如果要在本地目录测试, 可有修改hdfs的路径-----------------
TextInputFormat.addInputPath(job,new Path("file:///D:\\wordcount\\input"));
TextOutputFormat.setOutputPath(job,new Path("file:///D:\\wordcount\\output"));
```

- 集群运行模式

1. 将mapreduce程序提交给yarn集群，分发到很多的节点上并发执行

2. 处理的数据和输出结果应该位于hdfs文件系统

3. 提交集群的实现步骤：

   将程序打成JAR包，然后在集群的任意一个节点上用hadoop命令启动 yarn jar hadoop_hdfs_operate-1.0-SNAPSHOT.jar cn.itcast.hdfs.demo1.JobMain
## yarn的applicationMaster介绍

ApplicationMaster实际上是特定计算框架的一个实例，每种计算框架都有自己独特的ApplicationMaster，负责与ResourceManager协商资源，并和NodeManager协同来执行和监控Container。MapReduce只是可以运行在YARN上一种计算框架。

### applicationMaster的职能

Application启动后，将负责以下任务：

1. 初始化向ResourceManager报告自己的活跃信息的进程 (注册)
2. 计算应用程序的的资源需求。
3. 将需求转换为YARN调度器可以理解的ResourceRequest。
4. 与调度器协商申请资源
5. 与NodeManager协同合作使用分配的Container。
6. 跟踪正在运行的Container状态，监控它的运行。
7. 对Container或者节点失败的情况进行处理，在必要的情况下重新申请资源。

### 报告活跃

1. 注册

ApplicationMaster执行的第一个操作就是向ResourceManager注册，注册时AM告诉RM它的IPC的地址和网页的URL。

IPC地址是面向客户端的服务地址；网页URL是AM的一个Web服务的地址，客户端可以通过Http获取应用程序的状态和信息。

注册后，RM返回AM可以使用的信息，包括：YARN接受的资源的大小范围、应用程序的ACL信息。

1. 心跳

   注册成功后，AM需要周期性地发送心跳到RM确认他还活着。参数yarn.am.liveness-monitor.expiry配置AM心跳最大周期，如果RM发现超过这个时间还没有收到AM的心跳，那么就判断AM已经死掉。

### 资源需求

AM所需要的资源分为静态资源和动态资源。

1. 静态资源

在任务提交时就能确定，并且在AM运行时不再变化的资源是静态资源，比如MapReduce程序中的Map的数量。

1. 动态资源

AM在运行时确定要请求数量的资源是动态资源。

### 调度任务

当AM的资源请求数量达到一定数量或者到了心跳时，AM才会发送心跳到RM，请求资源，心跳是以ResourceRequest形式发送的，包括的信息有：resourceAsks、ContainerID、containersToBeReleased。

RM响应的信息包括：新分配的Container列表、已经完成了的Container状态、集群可用的资源上限。

### 启动container

1. AM从RM那里得到了Container后就可以启动Container了。
2. AM首先构造ContainerLaunchContext对象，包括分配资源的大小、安全令牌、启动Container执行的命令、进程环境、必要的文件等
3. AM与NM通讯，发送StartContainerRequest请求，逐一或者批量启动Container。
4. NM通过StartContainerResponse回应请求，包括：成功启动的Container列表、失败的Container信信息等。
5. 整个过程中，AM没有跟RM进行通信。
6. AM也可以发送StopContainerRequest请求来停止Container。

### 完成的container

当Container执行结束时，由RM通知AM Container的状态，AM解释Container状态并决定如何继续操作。所以YARN平台只是负责为计算框架提供Container信息。

### AM的失败和恢复

当AM失效后，YARN只负责重新启动一个AM，任务恢复到失效前的状态是由AM自己完成的。AM为了能实现恢复任务的目标，可以采用以下方案：将任务的状态持久化到外部存储中。比如：MapReduce框架的ApplicationMaster会将已完成的任务持久化，失效后的恢复时可以将已完成的任务恢复，重新运行未完成的任务。

### applicationMaster启动过程

![applicationmaster启动过程](..\..\img\applicationmaster启动过程.png)
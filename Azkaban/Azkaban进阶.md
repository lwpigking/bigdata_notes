# Azkaban进阶

## JavaProcess作业类型案例

JavaProcess类型可以运行一个自定义主类方法，type类型为javaprocess，可用的配置为：

Xms：最小堆  

Xmx：最大堆  

classpath：类路径  

java.class：要运行的 Java 对象，其中必须包含Main方法  

main.args：main 方法的参数  

案例：

1）新建一个azkaban的maven工程 

2）创建包名：com.atguigu  

3）创建AzTest类

```java
package com.atguigu; 

public class AzTest { 
	public static void main(String[] args) { 
		System.out.println("This is for testing!"); 
	} 
}
```

4）打包成jar包azkaban-1.0-SNAPSHOT.jar  

5）新建testJava.flow，内容如下

```yaml
nodes: 
  - name: test_java 
    type: javaprocess 
    config: 
      Xms: 96M 
      Xmx: 200M 
      java.class: com.atguigu.AzTest 
```

6）将Jar包、flow文件和project文件打包成javatest.zip   

7）创建项目 -> 上传javatest.zip -> 执行作业 -> 观察结果

![jp1](img\jp1.jpg)

![jp2](img\jp2.png)

![jp3](img\jp3.png)

![jp4](img\jp4.png)





## 条件工作流案例

条件工作流功能允许用户`自定义执行条件`来决定是否运行某些Job。条件可以由当前Job 的父Job输出的运行时参数构成，也可以使用预定义宏。在这些条件下，用户可以在确定Job 执行逻辑时获得更大的灵活性，例如，只要父Job之一成功，就可以运行当前Job。

### 运行时参数案例

1）基本原理  

（1）父Job将参数写入JOB_OUTPUT_PROP_FILE环境变量所指向的文件  

（2）子Job使用${jobName:param}来获取父Job输出的参数并定义执行条件  

2）支持的条件运算符：  

（1）== 等于  

（2）!= 不等于  

（3）> 大于  

（4）>= 大于等于  

（5）< 小于  

（6）<= 小于等于  

（7）&& 与  

（8）|| 或  

（9）! 非

3）案例：  

需求：JobA 执行一个shell脚本。JobB 执行一个shell脚本，但JobB不需要每天都执行，而只需要每个周一执行。

（1）新建JobA.sh

```sh
#!/bin/bash 
echo "do JobA" 
wk=`date +%w` 
echo "{\"wk\":$wk}" > $JOB_OUTPUT_PROP_FILE 
```

（2）新建JobB.sh

```sh
#!/bin/bash 
echo "do JobB" 
```

（3）新建condition.flow

```yaml
nodes: 
  - name: JobA 
    type: command 
    config: 
      command: sh JobA.sh
  - name: JobB 
    type: command 
    dependsOn: - JobA 
    config: 
      command: sh JobB.sh 
      condition: ${JobA:wk} == 1
```

（4）将JobA.sh、JobB.sh、condition.flow 和 azkaban.project 打包成 condition.zip

（5）创建condition 项目 -> 上传condition.zip 文件 -> 执行作业 -> 观察结果  

（6）按照我们设定的条件，JobB会根据当日日期决定是否执行。



### 预定义宏案例

Azkaban中预置了几个特殊的判断条件，称为`预定义宏`。预定义宏会根据所有父Job的完成情况进行判断，再决定是否执行。可用的预定义宏如下：  

（1）all_success: 表示父 Job 全部成功才执行(默认)  

（2）all_done：表示父Job全部完成才执行   

（3）all_failed：表示父 Job 全部失败才执行  

（4）one_success：表示父Job至少一个成功才执行  

（5）one_failed：表示父Job 至少一个失败才执行

1）案例 

需求：JobA 执行一个shell脚本，JobB 执行一个shell脚本，JobC执行一个shell脚本，要求JobA、JobB中有一个成功即可执行  

（1）新建JobA.sh

```yaml
#!/bin/bash 
echo "do JobA"
```

（2）新建JobC.sh

```yaml
#!/bin/bash 
echo "do JobC" 
```

（3）新建macro.flow

```yaml
nodes: 
  - name: JobA 
    type: command 
    config: 
      command: sh JobA.sh 
  - name: JobB 
    type: command 
    config: 
      command: sh JobB.sh 
  - name: JobC 
    type: command 
    dependsOn: - JobA 
               - JobB 
    config: 
      command: sh JobC.sh 
      condition: one_success
```

（4）JobA.sh、JobC.sh、macro.flow、azkaban.project 文件，打包成 macro.zip。注意：没有JobB.sh。  

（5）创建macro项目 -> 上传macro.zip文件 -> 执行作业 -> 观察结果





## 定时执行案例

需求：JobA每间隔1分钟执行一次；  

具体步骤：  

1）Azkaban 可以定时执行工作流。在执行工作流时候，选择左下角Schedule。

![定时1](img\定时1.png)

2）右上角注意时区是上海，然后在左面填写具体执行事件，填写的方法和crontab配置定时 任务规则一致。 

![定时2](img\定时2.jpg)

![定时3](img\定时3.jpg)

3）观察结果

![定时4](img\定时4.jpg)

![定时5](img\定时5.jpg)

4）删除定时调度  

点击remove Schedule即可删除当前任务的调度规则。

![定时6](img\定时6.jpg)



## 多Executor模式注意事项

Azkaban多Executor模式是指，在集群中多个节点部署Executor。在这种模式下，Azkaban web Server会根据策略，选取其中一个Executor去执行任务。为确保所选的Executor能够准确的执行任务，我们须在以下两种方案任选其一，推荐使用方案二。  

方案一：指定特定的Executor（hadoop102）去执行任务。  

1）在MySQL中azkaban数据库executors表中，查询hadoop102上的Executor的id。

```mysql
use azkaban;
select * from executors;
```

2）在执行工作流程时加入useExecutor属性，如下

![1692090927591](img\1692090927591.jpg)

方案二：在Executor所在所有节点部署任务所需脚本和应用。
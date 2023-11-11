# WorkFlow案例

## HelloWorld案例

1）在windows环境，新建azkaban.project文件，编辑内容如下  

```
azkaban-flow-version: 2.0  
```

注意：该文件作用，是采用新的Flow-API方式解析flow文件。

2）新建basic.flow文件，内容如下

```yaml
nodes: 
  - name: jobA 
    type: command 
    config: 
      command: echo "Hello World"
```

3）将`azkaban.project`、`basic.flow` 文件压缩到一个`zip`文件，文件名称`必须是英文`。

4）在WebServer 新建项目：http://master:8081/index

![HelloWorld1](img\HelloWorld1.jpg)

5）给项目名称命名和添加项目描述

![HelloWorld2](img\HelloWorld2.jpg)

6）first.zip文件上传

![HelloWorld3](img\HelloWorld3.jpg)

7）选择上传的文件

![HelloWorld4](img\HelloWorld4.jpg)

8）执行任务流

![HelloWorld5](img\HelloWorld5.jpg)

![HelloWorld6](img\HelloWorld6.jpg)

![HelloWorld7](img\HelloWorld7.jpg)

9）在日志中，查看运行结果

![HelloWorld8](img\HelloWorld8.jpg)

![HelloWorld9](img\HelloWorld9.jpg)





## 作业依赖案例

需求：`JobA和JobB执行完了，才能执行JobC` 

具体步骤：  

1）修改basic.flow为如下内容 

```yaml
nodes: 
  - name: jobC 
    type: command 
    # jobC 依赖 JobA和JobB 
    dependsOn: 
      - jobA 
      - jobB 
    config: 
      command: echo "I’m JobC" 
 
  - name: jobA 
    type: command 
    config: 
      command: echo "I’m JobA" 
 
  - name: jobB 
    type: command 
    config: 
      command: echo "I’m JobB" 
```

2）将修改后的basic.flow和azkaban.project压缩成second.zip文件

3）重复HelloWorld案例后续步骤。

![作业依赖1](img\作业依赖1.jpg)

![作业依赖2](img\作业依赖2.jpg)

![作业依赖3](img\作业依赖3.jpg)

![作业依赖4](img\作业依赖4.jpg)

![作业依赖5](img\作业依赖5.png)

![作业依赖6](img\作业依赖6.png)





## 自动失败重试案例

需求：如果执行任务失败，需要重试3次，重试的时间间隔10000ms

具体步骤：  

1）编译配置流

```yaml
nodes: 
  - name: JobA 
    type: command 
    config: 
      command: sh /not_exists.sh 
      retries: 3 
      retry.backoff: 10000 
```

2）将修改后的basic.flow和azkaban.project压缩成four.zip文件

3）重复HelloWorld案例后续步骤。 

4）执行并观察到一次失败+三次重试

![自动失败1](img\自动失败1.jpg)

5）也可以点击上图中的Log，在任务日志中看到，总共执行了4次。

![自动失败2](img\自动失败2.jpg)





## 手动失败重试案例

需求：JobA -> JobB（依赖于A） -> JobC -> JobD -> JobE -> JobF。生产环境下任何Job都有可能挂掉，可以`根据需求执行想要执行的Job`。

具体步骤：  

1）编译配置流

```yaml
nodes: 
  - name: JobA 
    type: command 
    config: 
      command: echo "This is JobA." 
 
  - name: JobB 
    type: command 
    dependsOn: 
      - JobA 
    config: 
      command: echo "This is JobB." 
 
  - name: JobC 
    type: command 
    dependsOn: 
      - JobB 
    config: 
      command: echo "This is JobC." 
 
  - name: JobD 
    type: command 
    dependsOn: 
      - JobC 
    config: 
      command: echo "This is JobD." 
 
  - name: JobE 
    type: command 
    dependsOn: 
      - JobD 
    config: 
      command: echo "This is JobE." 
 
  - name: JobF 
    type: command 
    dependsOn: 
      - JobE 
    config: 
      command: echo "This is JobF."
```

2）将修改后的basic.flow和azkaban.project压缩成five.zip文件

3）重复HelloWorld案例后续步骤。

![手动失败1](img\手动失败1.jpg)

![手动失败2](img\手动失败2.png)

![手动失败3](img\手动失败3.png)

Enable和Disable下面都分别有如下参数：  

Parents：该作业的上一个任务  

Ancestors：该作业前的所有任务  

Children：该作业后的一个任务  

Descendents：该作业后的所有任务  

Enable All：所有的任务

4）可以根据需求选择性执行对应的任务。
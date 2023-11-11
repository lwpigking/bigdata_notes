## MapReduce 编程规范

> MapReduce 的开发一共有八个步骤, 其中 Map 阶段分为 2 个步骤，Shuffle 阶段 4 个步骤，Reduce 阶段分为 2 个步骤

1.`Map 阶段 2 个步骤：`

- 1.1 设置 InputFormat 类, 将数据切分为 Key-Value**(K1和V1)** 对, 输入到第二步
- 1.2 自定义 Map 逻辑, 将第一步的结果转换成另外的 Key-Value（**K2和V2**） 对, 输出结果

2.`Shuffle 阶段 4 个步骤：`

- 2.1 对输出的 Key-Value 对进行**分区**
- 2.2 对不同分区的数据按照相同的 Key **排序**
- 2.3 (可选) 对分组过的数据初步**规约**, 降低数据的网络拷贝
- 2.4 对数据进行**分组**, 相同 Key 的 Value 放入一个集合中

3.`Reduce 阶段 2 个步骤：`

- 3.1 对多个 Map 任务的结果进行排序以及合并, 编写 Reduce 函数实现自己的逻辑, 对输入的 Key-Value 进行处理, 转为新的 Key-Value（**K3和V3**）输出
- 3.2 设置 OutputFormat 处理并保存 Reduce 输出的 Key-Value 数据

![MapReduce编程规范](..\..\img\MapReduce编程规范.png)
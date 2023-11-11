## 窗口函数

个人对over()的窗口理解：这个永远是`一行对应一个窗口`，至于这个窗口的范围是什么就要看over()函数里面对窗口范围的约束是什么了（partition by order by between ... and）通过`partition by关键字来对窗口分组`，特殊注意：通过order by 来对order by字段排序后的行进行开窗，只不过注意的是第一行数据的窗口大小是1，第二行数据的窗口范围是前2行，第n行的窗口范围是前n行，以此类推。如果里面没有条件，则每一行对应整张表。

特殊的窗口函数如rank(),rownumber(),dense()等，即使后面over()里面没有条件，默认的开窗类似order by效果，即第一行窗口大小为1，第二行窗口大小为2，以此类推，但是数据只不过没有什么统计意义，所以一般还是会在over()里加入partiton by和order by（分组，排序）等，为其赋予意义，如排名等。

over(partition by ) 和 普通的group by的区别，为什么不同group by，因为有group by，只能select group by 后面的字段，和一些聚合函数 sum(),avg(),max(),min()等，而用了over(partition by)，还能select 别的非partition by 字段 或者能直接“select *”，而且对于join 等有更好的支持。



### 窗口函数语法

avg()、sum()、max()、min()是分析函数，而over()才是窗口函数，下面我们来看看over()窗口函数的语法结构、及常与over()一起使用的分析函数

- over()窗口函数的语法结构
- 常与over()一起使用的分析函数
- 窗口函数总结

#### over()窗口函数的语法结构

```
分析函数 over(partition by 列名 order by 列名 rows between 开始位置 and 结束位置)
```

over()函数中包括三个函数：包括分区`partition by 列名`、排序`order by 列名`、指定窗口范围`rows between 开始位置 and 结束位置`。我们在使用over()窗口函数时，over()函数中的这三个函数可组合使用也可以不使用。

over()函数中如果不使用这三个函数，窗口大小是针对查询产生的所有数据，如果指定了分区，窗口大小是针对每个分区的数据。

#### over() 默认此时每一行的窗口都是所有的行

```
select *,count(1) over() from business;
```

#### over(order by orderdate)

orderdate=1的窗口只有一行，orderdate=2的窗口包括orderdate=2017-01-01,orderdate=2017-01-02

```
select *,count(1) over(order by orderdate) from business;
```

#### over(partition by name)每一行根据 name来区分窗口

```
select *,sum(cost) over(partition by name) from business;
```

#### over(partition by name order by id) 每一行根据name来区分窗口,再根据order by取具体的范围

```
select *,sum(cost) over(partition by name order by orderdate) from business;
```

### over()函数中的三个函数讲解


A、partition by
`partition by`可理解为group by 分组。`over(partition by 列名)`搭配分析函数时，分析函数按照每一组每一组的数据进行计算的。

B、rows between 开始位置 and 结束位置
是指定窗口范围，比如第一行到当前行。而这个范围是随着数据变化的。`over(rows between 开始位置 and 结束位置)`搭配分析函数时，分析函数按照这个范围进行计算的。

窗口范围说明：
我们常使用的窗口范围是`ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW（表示从起点到当前行）`，常用该窗口来计算累加。

```
PRECEDING：往前
```

```
FOLLOWING：往后
CURRENT ROW：当前行
UNBOUNDED：起点（一般结合PRECEDING，FOLLOWING使用）
UNBOUNDED PRECEDING 表示该窗口最前面的行（起点）
UNBOUNDED FOLLOWING：表示该窗口最后面的行（终点）
```

```
比如说：
ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW（表示从起点到当前行）
ROWS BETWEEN 2 PRECEDING AND 1 FOLLOWING（表示往前2行到往后1行）
ROWS BETWEEN 2 PRECEDING AND 1 CURRENT ROW（表示往前2行到当前行）
ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING（表示当前行到终点）
```



### 常与over()一起使用的分析函数

#### 聚合类

```
avg()、sum()、max()、min()
```

#### 排名类

```
row_number()按照值排序时产生一个自增编号，不会重复（如：1、2、3、4、5、6）
rank() 按照值排序时产生一个自增编号，值相等时会重复，会产生空位（如：1、2、3、3、3、6）
dense_rank() 按照值排序时产生一个自增编号，值相等时会重复，不会产生空位（如：1、2、3、3、3、4）
```

#### 其他类

```
lag(列名,往前的行数,[行数为null时的默认值，不指定为null])，可以计算用户上次购买时间，或者用户下次购买时间。或者上次登录时间和下次登录时间
lead(列名,往后的行数,[行数为null时的默认值，不指定为null])
ntile(n) 把有序分区中的行分发到指定数据的组中，各个组有编号，编号从1开始，对于每一行，ntile返回此行所属的组的编号
```



### 练习题

```
测试数据

20191020,11111,85
20191020,22222,83
20191020,33333,86
20191021,11111,87
20191021,22222,65
20191021,33333,98
20191022,11111,67
20191022,22222,34
20191022,33333,88
20191023,11111,99
20191023,22222,33
```

```
create table test_window
(logday string,    #logday时间
userid string,
score int)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ',';
 
#加载数据
load data local inpath '/home/xiaowangzi/hive_test_data/test_window.txt' into table test_window;
```

#### 使用 over() 函数进行数据统计, 统计每个用户及表中数据的总数

```
select *, count(userid) over() as total  from  test_window;
```

这里使用 over() 与 select count(*) 有相同的作用，好处就是，在需要计算总数时不用再进行一次关联。

#### 求用户明细并统计每天的用户总数

可以使用 partition by 按日期列对数据进行分区处理，如：over(partition by logday)

```
select  *,count() over(partition by logday) as day_total from  test_window;
```

求每天的用户数可以使用`select logday, count(userid) from recommend.test_window group by logday`，但是当想要得到 userid 信息时，这种用法的优势就很明显。

#### 计算从第一天到现在的所有 score 大于80分的用户总数

此时简单的分区不能满足需求，需要将 order by 和 窗口定义结合使用。

```
select *,count() over(order by logday rows between unbounded preceding and current row)as total from test_window where score > 80;
```

#### 计算每个用户到当前日期分数大于80的天数

```
select *, count() over(partition by userid order by logday rows between unbounded preceding and current row) as total from test_window where score > 80 order by logday, userid;
```

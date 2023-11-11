## WordCount示例编写

需求：在一堆给定的文本文件中统计输出每一个单词出现的总次数

node01服务器执行以下命令，准备数，数据格式准备如下：

```text
cd /export/servers
vim wordcount.txt

#添加以下内容:
hello hello
world world
hadoop hadoop
hello world
hello flume
hadoop hive
hive kafka
flume storm
hive oozie
```

```
将数据文件上传到hdfs上面去
hdfs dfs -mkdir /wordcount/
hdfs dfs -put wordcount.txt /wordcount/
```

- 定义一个mapper类

```java
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

// mapper程序:  需要继承 mapper类, 需要传入 四个类型:
/*  在hadoop中, 对java的类型都进行包装, 以提高传输的效率  writable
    keyin :  k1   Long     ---- LongWritable
    valin :  v1   String   ------ Text
    keyout : k2   String   ------- Text
    valout : v2   Long     -------LongWritable

 */

public class MapTask extends Mapper<LongWritable,Text,Text,LongWritable> {

    /**
     *
     * @param key  : k1
     * @param value   v1
     * @param context  上下文对象   承上启下功能
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        //1. 获取 v1 中数据
        String val = value.toString();

        //2. 切割数据
        String[] words = val.split(" ");

        Text text = new Text();
        LongWritable longWritable = new LongWritable(1);
        //3. 遍历循环, 发给 reduce
        for (String word : words) {
            text.set(word);
            context.write(text,longWritable);
        }
    }
}
```

- 定义一个reducer类

```java
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * KEYIN  :  k2    -----Text
 * VALUEIN :  v2   ------LongWritable
 * KEYOUT  : k3    ------  Text
 * VALUEOUT : v3   ------ LongWritable
 */
public class ReducerTask extends Reducer<Text, LongWritable, Text, LongWritable> {


    @Override
    protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {

        //1. 遍历 values 获取每一个值
        long  v3 = 0;
        for (LongWritable longWritable : values) {

            v3 += longWritable.get();  //1
        }

        //2. 输出
        context.write(key,new LongWritable(v3));

    }
}
```

- 定义一个主类，用来描述job并提交job

```java
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.nativeio.NativeIO;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

// 任务的执行入口: 将八步组合在一起
public class JobMain extends Configured implements Tool {
    // 在run方法中编写组装八步
    @Override
    public int run(String[] args) throws Exception {

        Job job = Job.getInstance(super.getConf(), "JobMain");
        //如果提交到集群操作. 需要添加一步 : 指定入口类
        job.setJarByClass(JobMain.class);


        //1. 封装第一步:  读取数据
        job.setInputFormatClass(TextInputFormat.class);
        TextInputFormat.addInputPath(job,new Path("hdfs://node01:8020/wordcount.txt"));

        //2. 封装第二步:  自定义 map程序
        job.setMapperClass(MapTask.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        //3. 第三步 第四步 第五步 第六步 省略

        //4. 第七步:  自定义reduce程序
        job.setReducerClass(ReducerTask.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        //5) 第八步  : 输出路径是一个目录, 而且这个目录必须不存在的
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(job,new Path("hdfs://node01:8020/output"));

        //6) 提交任务:
        boolean flag = job.waitForCompletion(true); // 成功  true  不成功 false

        return flag ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        JobMain jobMain = new JobMain();
        int i = ToolRunner.run(configuration, jobMain, args); //返回值 退出码

        System.exit(i); // 退出程序  0 表示正常  其他值表示有异常 1
    }
}
```



提醒：代码开发完成之后，就可以打成jar包放到服务器上面去运行了，实际工作当中，都是将代码打成jar包，开发main方法作为程序的入口，然后放到集群上面去运行
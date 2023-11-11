

## 1 String原理

### 概览

String 被声明为 final，因此它不可被继承。(Integer 等包装类也不能被继承）

在 Java 8 中，String 内部使用 char 数组存储数据。

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];
}
```

在 Java 9 之后，String 类的实现改用 byte 数组存储字符串，同时使用 `coder` 来标识使用了哪种编码。

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final byte[] value;

    /** The identifier of the encoding used to encode the bytes in {@code value}. */
    private final byte coder;
}
```

value 数组被声明为 final，这意味着 value 数组初始化之后就不能再引用其它数组。并且 String 内部没有改变 value 数组的方法，因此可以保证 String 不可变。

### 不可变的好处

**1. 可以缓存 hash 值**  

因为 String 的 hash 值经常被使用，例如 String 用做 HashMap 的 key。不可变的特性可以使得 hash 值也不可变，因此只需要进行一次计算。

**2. String Pool 的需要**  

如果一个 String 对象已经被创建过了，那么就会从 String Pool 中取得引用。只有 String 是不可变的，才可能使用 String Pool。

<div align="center"> <img src="https://cs-notes-1256109796.cos.ap-guangzhou.myqcloud.com/image-20191210004132894.png"/> </div><br>

**3. 安全性**  

String 经常作为参数，String 不可变性可以保证参数不可变。例如在作为网络连接参数的情况下如果 String 是可变的，那么在网络连接过程中，String 被改变，改变 String 的那一方以为现在连接的是其它主机，而实际情况却不一定是。

**4. 线程安全**  

String 不可变性天生具备线程安全，可以在多个线程中安全地使用。

[Program Creek : Why String is immutable in Java?](https://www.programcreek.com/2013/04/why-string-is-immutable-in-java/)

### String, StringBuffer and StringBuilder	

**1. 可变性**  

- String 不可变
- StringBuffer 和 StringBuilder 可变

**2. 线程安全**  

- String 不可变，因此是线程安全的
- StringBuilder 不是线程安全的
- StringBuffer 是线程安全的，内部使用 synchronized 进行同步

[StackOverflow : String, StringBuffer, and StringBuilder](https://stackoverflow.com/questions/2971315/string-stringbuffer-and-stringbuilder)

### String Pool

字符串常量池（String Pool）保存着所有字符串字面量（literal strings），这些字面量在编译时期就确定。不仅如此，还可以使用 String 的 intern() 方法在运行过程将字符串添加到 String Pool 中。

当一个字符串调用 intern() 方法时，如果 String Pool 中已经存在一个字符串和该字符串值相等（使用 equals() 方法进行确定），那么就会返回 String Pool 中字符串的引用；否则，就会在 String Pool 中添加一个新的字符串，并返回这个新字符串的引用。

下面示例中，s1 和 s2 采用 new String() 的方式新建了两个不同字符串，而 s3 和 s4 是通过 s1.intern() 和 s2.intern() 方法取得同一个字符串引用。intern() 首先把 "aaa" 放到 String Pool 中，然后返回这个字符串引用，因此 s3 和 s4 引用的是同一个字符串。

```java
String s1 = new String("aaa");
String s2 = new String("aaa");
System.out.println(s1 == s2);           // false
String s3 = s1.intern();
String s4 = s2.intern();
System.out.println(s3 == s4);           // true
```

如果是采用 "bbb" 这种字面量的形式创建字符串，会自动地将字符串放入 String Pool 中。

```java
String s5 = "bbb";
String s6 = "bbb";
System.out.println(s5 == s6);  // true
```

在 Java 7 之前，String Pool 被放在运行时常量池中，它属于永久代。而在 Java 7，String Pool 被移到堆中。这是因为永久代的空间有限，在大量使用字符串的场景下会导致 OutOfMemoryError 错误。

- [StackOverflow : What is String interning?](https://stackoverflow.com/questions/10578984/what-is-string-interning)
- [深入解析 String#intern](https://tech.meituan.com/in_depth_understanding_string_intern.html)

### new String("abc")

使用这种方式一共会创建两个字符串对象（前提是 String Pool 中还没有 "abc" 字符串对象）。

- "abc" 属于字符串字面量，因此编译时期会在 String Pool 中创建一个字符串对象，指向这个 "abc" 字符串字面量；
- 而使用 new 的方式会在堆中创建一个字符串对象。

创建一个测试类，其 main 方法中使用这种方式来创建字符串对象。

```java
public class NewStringTest {
    public static void main(String[] args) {
        String s = new String("abc");
    }
}
```

使用 javap -verbose 进行反编译，得到以下内容：

```java
// ...
Constant pool:
// ...
   #2 = Class              #18            // java/lang/String
   #3 = String             #19            // abc
// ...
  #18 = Utf8               java/lang/String
  #19 = Utf8               abc
// ...

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=2, args_size=1
         0: new           #2                  // class java/lang/String
         3: dup
         4: ldc           #3                  // String abc
         6: invokespecial #4                  // Method java/lang/String."<init>":(Ljava/lang/String;)V
         9: astore_1
// ...
```

在 Constant Pool 中，#19 存储这字符串字面量 "abc"，#3 是 String Pool 的字符串对象，它指向 #19 这个字符串字面量。在 main 方法中，0: 行使用 new #2 在堆中创建一个字符串对象，并且使用 ldc #3 将 String Pool 中的字符串对象作为 String 构造函数的参数。

以下是 String 构造函数的源码，可以看到，在将一个字符串对象作为另一个字符串对象的构造函数参数时，并不会完全复制 value 数组内容，而是都会指向同一个 value 数组。

```java
public String(String original) {
    this.value = original.value;
    this.hash = original.hash;
}
```


```java
String sa = new String("hello");
String sb = new String("hello");
System.out.println(sa==sb);
//False

String sc = "hello";
String sd = "hello";
System.out.println(sc==sd);
//True
```


## 2 字符串使用

### 创建方法

* 直接等于字符串返回的是字面常量的引用。intern创建的字面常量的引用。所以s1==s2
* 使用new创建字符串相当于在堆上创建了字符串。其引用不相等。所以s4!=s5
```java
String str = "Runoob";
String str2=new String("Runoob");
String s1 = "Runoob";              // String 直接创建
String s2 = "Runoob";              // String 直
// s1==s2 true
String s3 = s1;                    // 相同引用

String s4 = new String("Runoob");   // String 对象创建
String s5 = new String("Runoob");   // String 对象创建
//s4==s5 false
```
![](image/2022-07-12-10-56-21.png)



常见的构造方法

* String s = “xxx”	最常用
* String(String original)	String(“xxx”)
* String(char数组)	
* String(char数组,起始下标,长度)	
* String(byte数组)	
* String(byte数组,起始下标,长度)	
* String(StringBuffer buffer)	
* String(StringBuilder builder)
### 字符串拼接


* 字符串链接concat和+号的功能类似。
```java
"我的名字是 ".concat("Runoob");
"Hello," + " runoob" + "!"
```

### 格式化方法
* 创建格式化字符串printf和format两个方法

```java
System.out.printf("浮点型变量的值为 " +
                  "%f, 整型变量的值为 " +
                  " %d, 字符串变量的值为 " +
                  "is %s", floatVar, intVar, stringVar);
```

### 带正则表达式的方法
* boolean matches(String regex)告知此字符串是否匹配给定的正则表达式。
* String replaceAll(String regex, String replacement)使用给定的 replacement 替换此字符串所有匹配给定的正则表达式的子字符串。
* String replaceFirst(String regex, String replacement)&nbsp;使用给定的 replacement 替换此字符串匹配给定的正则表达式的第一个子字符串。
* String[] split(String regex)根据给定正则表达式的匹配拆分此字符串。
* String[] split(String regex, int limit)根据匹配给定的正则表达式来拆分此字符串。


### 其他方法
* char charAt(int index)返回指定索引处的 char 值。
* int compareTo(Object o)把这个字符串和另一个对象比较。
* int compareTo(String anotherString)按字典顺序比较两个字符串。
* int compareToIgnoreCase(String str)按字典顺序比较两个字符串，不考虑大小写。
* String concat(String str)将指定字符串连接到此字符串的结尾。
* boolean contentEquals(StringBuffer sb)当且仅当字符串与指定的StringBuffer有相同顺序的字符时候返回真。
* static String copyValueOf(char[] data)返回指定数组中表示该字符序列的 String。
* static String copyValueOf(char[] data, int offset, int count)返回指定数组中表示该字符序列的 String。
* boolean endsWith(String suffix)测试此字符串是否以指定的后缀结束。
* boolean equals(Object anObject)将此字符串与指定的对象比较。
* boolean equalsIgnoreCase(String anotherString)将此 String 与另一个 String 比较，不考虑大小写。
* byte[] getBytes()&nbsp;使用平台的默认字符集将此 String 编码为 byte 序列，并将结果存储到一个新的 byte 数组中。
* byte[] getBytes(String charsetName)使用指定的字符集将此 String 编码为 byte 序列，并将结果存储到一个新的 byte 数组中。
* void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)将字符从此字符串复制到目标字符数组。
* int hashCode()返回此字符串的哈希码。
* int indexOf(int ch)返回指定字符在此字符串中第一次出现处的索引。
* int indexOf(int ch, int fromIndex)返回在此字符串中第一次出现指定字符处的索引，从指定的索引开始搜索。
* int indexOf(String str)&nbsp;返回指定子字符串在此字符串中第一次出现处的索引。
* int indexOf(String str, int fromIndex)返回指定子字符串在此字符串中第一次出现处的索引，从指定的索引开始。
* String intern()&nbsp;返回字符串对象的规范化表示形式。
* int lastIndexOf(int ch)&nbsp;返回指定字符在此字符串中最后一次出现处的索引。
* int lastIndexOf(int ch, int fromIndex)返回指定字符在此字符串中最后一次出现处的索引，从指定的索引处开始进行反向搜索。
* int lastIndexOf(String str)返回指定子字符串在此字符串中最右边出现处的索引。
* int lastIndexOf(String str, int fromIndex)&nbsp;返回指定子字符串在此字符串中最后一次出现处的索引，从指定的索引开始反向搜索。
* int length()返回此字符串的长度。

* boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len)测试两个字符串区域是否相等。
* boolean regionMatches(int toffset, String other, int ooffset, int len)测试两个字符串区域是否相等。
* String replace(char oldChar, char newChar)返回一个新的字符串，它是通过用 newChar 替换此字符串中出现的所有 oldChar 得到的。

* boolean startsWith(String prefix)测试此字符串是否以指定的前缀开始。
* boolean startsWith(String prefix, int toffset)测试此字符串从指定索引开始的子字符串是否以指定前缀开始。
* CharSequence subSequence(int beginIndex, int endIndex)&nbsp;返回一个新的字符序列，它是此序列的一个子序列。
* String substring(int beginIndex)返回一个新的字符串，它是此字符串的一个子字符串。
* String substring(int beginIndex, int endIndex)返回一个新字符串，它是此字符串的一个子字符串。
* char[] toCharArray()将此字符串转换为一个新的字符数组。
* String toLowerCase()使用默认语言环境的规则将此 String 中的所有字符都转换为小写。
* String toLowerCase(Locale locale)&nbsp;使用给定 Locale 的规则将此 String 中的所有字符都转换为小写。
* String toString()&nbsp;返回此对象本身（它已经是一个字符串！）。
* String toUpperCase()使用默认语言环境的规则将此 String 中的所有字符都转换为大写。
* String toUpperCase(Locale locale)使用给定 Locale 的规则将此 String 中的所有字符都转换为大写。
* String trim()返回字符串的副本，忽略前导空白和尾部空白。
* static String valueOf(primitive data type x)返回给定data type类型x参数的字符串表示形式。
* contains(CharSequence chars)判断是否包含指定的字符系列。
* isEmpty()判断字符串是否为空。



## 3 StringBuffer&StringBuilder

![](image/2022-07-12-11-07-56.png)

### 简介

StringBuffer 和 StringBuilder 类的对象能够被多次的修改，并且不产生新的未使用对象。
* 使用 StringBuffer 类时，每次都会对 StringBuffer 对象本身进行操作，而不是生成新的对象，所以如果需要对字符串进行修改推荐使用 StringBuffer。

* StringBuilder 的方法不是线程安全的（不能同步访问）。StringBuffer是线程安全的。

* 由于 StringBuilder 相较于 StringBuffer 有速度优势，所以多数情况下建议使用 StringBuilder 类。


StringBuffer的内部实现方式和String不同.
* StringBuffer在进行字符串处理时，不生成新的对象，在内存使用上要优于String类。所以在实际使用时，如果经常需要对一个字符串进行修改，例如插入、删除等操作，使用StringBuffer要更加适合一些。
* String:在String类中没有用来改变已有字符串中的某个字符的方法，由于不能改变一个java字符串中的某个单独字符，所以在JDK文档中称String类的对象是不可改变的。然而，不可改变的字符串具有一个很大的优点:编译器可以把字符串设为共享的。

### 使用

```java
public class RunoobTest{
    public static void main(String args[]){
        StringBuilder sb = new StringBuilder(10);
        sb.append("Runoob..");
        System.out.println(sb);  
        sb.append("!");
        System.out.println(sb); 
        sb.insert(8, "Java");
        System.out.println(sb); 
        sb.delete(5,8);
        System.out.println(sb);  
    }
}
```

### StringBuffer常用方法

常用方法
* public StringBuffer append(String s)
将指定的字符串追加到此字符序列。
* public StringBuffer reverse()
 将此字符序列用其反转形式取代。
* public delete(int start, int end)
移除此序列的子字符串中的字符。
* public insert(int offset, int i)
将 int 参数的字符串表示形式插入此序列中。
* insert(int offset, String str)
将 str 参数的字符串插入此序列中。
* replace(int start, int end, String str)
使用给定 String 中的字符替换此序列的子字符串中的字符。

其他方法

* char charAt(int index)				返回此序列中指定索引处的 char 值。
* void ensureCapacity(int minimumCapacity)				确保容量至少等于指定的最小值。
* void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)				将字符从此序列复制到目标字符数组 dst。
* int indexOf(String str)				返回第一次出现的指定子字符串在该字符串中的索引。
* int indexOf(String str, int fromIndex)				从指定的索引处开始，返回第一次出现的指定子字符串在该字符串中的索引。
* int lastIndexOf(String str)				返回最右边出现的指定子字符串在此字符串中的索引。
* int lastIndexOf(String str, int fromIndex)返回 String 对象中子字符串最后出现的位置。
* int length()				&nbsp;返回长度（字符数）。
* void setCharAt(int index, char ch)				将给定索引处的字符设置为 ch。
* void setLength(int newLength)				设置字符序列的长度。
* CharSequence subSequence(int start, int end)				返回一个新的字符序列，该字符序列是此序列的子序列。
* String substring(int start)				返回一个新的 String，它包含此字符序列当前所包含的字符子序列。
* String substring(int start, int end)				返回一个新的 String，它包含此序列当前所包含的字符子序列。
* String toString()				返回此序列中数据的字符串表示形式。



## 4 字符串格式化

利用了Formatter类。printf、printfln等方法原理一直。

字符串的格式化相当于将字符串按照指定的格式进行toString(),一般有两种形式：

```java
//使用指定的格式字符串和参数返回一个格式化字符串。
 public static String format(String format, Object... args) {
        return new Formatter().format(format, args).toString();
    }

//使用指定的语言环境、格式字符串和参数返回一个格式化字符串。
public static String format(Locale l, String format, Object... args) {
        return new Formatter(l).format(format, args).toString();
    }
```
* 实例

```
String fs;
fs = String.format("浮点型变量的值为 " +
                   "%f, 整型变量的值为 " +
                   " %d, 字符串变量的值为 " +
                   " %s", floatVar, intVar, stringVar);
```

### 数据转化符

| 数据类型 | 说明         | 转化形式       |
|------|------------|------------|
| %s   | 字符串类型      | “string”   |
| %c   | 字符类型       | ‘A’        |
| %b   | 布尔类型       | true/false |
| %o   | 整数类型（八进制）  | 111        |
| %d   | 整数类型（十进制）  | 17         |
| %x   | 整数类型（十六进制） | 11         |
| %f   | 浮点类型（基本）   | 66.66      |
| %e   | 指数类型       | 1.11e+5    |
| %a   | 浮点类型（十六进制） | FF.22      |
| %h   | 散列码        | 11         |
| %%   | 百分比类型      | 17%        |
| %n   | 换行符        |
| %tx  | 日期与时间类型    |

```java
public class Format01 {
	public static void main(String[] args) {
		System.out.println(String.format("字符串：%s", "String"));
		System.out.println(String.format("字符：%c", 'M'));
		System.out.println(String.format("布尔类型：%b", 'M'>'A'));
		System.out.println(String.format("八进制整数类型：%o", 17));
		System.out.println(String.format("十进制整数类型：%d", 17));
		System.out.println(String.format("十六进制整数类型：%x", 17));
		System.out.println(String.format("基本浮点类型：%f", 99.1));
		System.out.println(String.format("指数类型：%e", 100.111111111));
		System.out.println(String.format("十六进制浮点类型：%a", 17.111111));
		System.out.println(String.format("散列码：%h", 17));
		System.out.println(String.format("百分比类型：17%%"));
		System.out.print(String.format("换行符：%n", 17));	
	}

}
```

### 格式化控制符

| 标志 | 说明 | 示例 | 输出 |
|---|---|---|---|
| + | 为正数添加符号 | (“正数：%+f”,11.11)) | 正数：+11.110000 |
| - | 左对齐 | (“左对齐：%-5d”,11) | 左对齐：11 |
| 0 | 整数前面补0 | (“数字前面补0：%04d”,11) | 数字前面补0：0011 |
| , | 对数字分组 | (“按,对数字分组：%,d”,111111111) | 按,对数字分组：111,111,111 |
| 空格 | 数字前面补空格 | (“空格：% 4d”,11) | 空格： 11 |
| （ | 包含负数 | (“使用括号包含负数：%(f”,-11.11) | 使用括号包含负数：(11.110000) |
| # | 浮点数包含小数，八进制包含0，十六进制包含0x |
| &lt; | 格式化前一个转换符描述的参数 | (“格式化前描述的参数：%f转化后%❤️.3f”,111.1111111) | 格式化前描述的参数：111.111111转化后111.111 |
| $ | 被格式化的参数索引 | (“被格式化的参数索引：%1


```java
public class formatString {
	public static void main(String[] args) {
		System.out.println(String.format("正数：%+f",11.11));
		System.out.println(String.format("右对齐：%+10d",11));
		System.out.println(String.format("左对齐：%-5d",11));
		System.out.println(String.format("数字前面补0：%044d",11));
		System.out.println(String.format("空格：% 4d",11));
		System.out.println(String.format("按,对数字分组：%,d",111111111));
		System.out.println(String.format("使用括号包含负数：%(f",-11.11));
		System.out.println(String.format("浮点数包含小数点：%#f",11.1));
		System.out.println(String.format("八进制包含0：%#o",11));
		System.out.println(String.format("十六进制包含0x：%#x",11));
		System.out.println(String.format("格式化前描述的参数：%f转化后%<3.3f",111.1111111));
		System.out.println(String.format("被格式化的参数索引：%1$d,%2$s",11,"111.1111111"));
		
	}

}
```

### 日期格式化符

| 转换符 | 说明 | 示例 |
|---|---|---|
| c | 全部时间日期 | 星期四 十二月 17 13:11:35 CST 2020 |
| F | 年-月-日格式 | 2020-12-17 |
| D | 月/日/年格式 | 12/17/20 |
| r | HH:MM:SS PM格式（12时制） | 01:11:35 下午 |
| T | HH:MM:SS格式（24时制） | 13:11:35 |
| R | HH:MM格式（24时制） | 13:11 |


```java
public class formatDate {
	public static void main(String[] args) {
		Date date = new Date();
		System.out.println(String.format("全部时间日期：%tc",date));
		System.out.println(String.format("年-月-日格式：%tF",date));
		System.out.println(String.format("月/日/年格式：%tD",date));
		System.out.println(String.format("HH:MM:SS PM格式（12时制）：%tr",date));
		System.out.println(String.format("HH:MM:SS格式（24时制）：%tT",date));
		System.out.println(String.format("HH:MM格式（24时制）：%tR",date));
	}
}
```


### 时间格式化符

| 转换符 | 说明 | 示例 |
|---|---|---|
| H | 2位数字24时制的小时（不足2位前面补0） | 13 |
| l | 2位数字12时制的小时 | 1 |
| k | 2位数字24时制的小时 | 13 |
| M | 2位数字的分钟 | 33 |
| L | 3位数字的毫秒 | 745 |
| S | 2位数字的秒 | 33 |
| N | 9位数字的毫秒数 | 745000000 |
| p | Locale.US,"小写字母的上午或下午标记(英) | 下午 |
| Z | 时区缩写字符串 | CST |
| z | 相对于GMT的RFC822时区的偏移量 | +0800 |
| s | 1970-1-1 00:00:00 到现在所经过的秒数 | 1608183213 |
| Q | 1970-1-1 00:00:00 到现在所经过的毫秒数 | 1608183213745 |

```java
public class formatTime {
	public static void main(String[] args) {
		Date date = new Date();
		System.out.println(String.format("2位数字24时制的小时（不足2位前面补0）：%tH", date));
		System.out.println(String.format("2位数字12时制的小时：%tl", date));
		System.out.println(String.format("2位数字24时制的小时：%tk", date));
		System.out.println(String.format("2位数字的分钟：%tM", date));
		System.out.println(String.format("3位数字的毫秒：%tL", date));
		System.out.println(String.format("2位数字的秒：%tS", date));
		System.out.println(String.format("9位数字的毫秒数：%tN", date));
		System.out.println(String.format("时区缩写字符串：%tZ", date));
		System.out.println(String.format("相对于GMT的RFC822时区的偏移量：%tz", date));
		System.out.println(String.format("Locale.US,\"小写字母的上午或下午标记(英)：%tp", date));
		System.out.println(String.format("1970-1-1 00:00:00 到现在所经过的秒数：%ts", date));
		System.out.println(String.format("1970-1-1 00:00:00 到现在所经过的毫秒数：%tQ", date));

	}

}
```
### 类型转换

* 其他类型转字符串

```java
1.String s=""+i;
2.String s=Integer.toString(i);
3.String s=String.valueOf(i);
```

* 字符串转其他类型

```java
1.int i=Integer.parsenInt(s);
2.int i=Integer.valueOf(s).intValue();
```

# Mysql索引
> 参考文献
> * [MySQL索引类型](cnblogs.com/luyucheng/p/6289714.html)


## 1 索引类型——应用

### 索引定义
* 普通索引
* 唯一索引
* 主键索引
* 组合索引
* 全文索引

```
CREATE TABLE table_name[col_name data type]
[unique|fulltext][index|key][index_name](col_name[length])[asc|desc]
```

1. unique|fulltext为可选参数，分别表示唯一索引、全文索引
2. index和key为同义词，两者作用相同，用来指定创建索引
3. col_name为需要创建索引的字段列，该列必须从数据表中该定义的多个列中选择
4. index_name指定索引的名称，为可选参数，如果不指定，默认col_name为索引值
5. length为可选参数，表示索引的长度，只有字符串类型的字段才能指定索引长度
6. asc或desc指定升序或降序的索引值存储

### 普通索引

1.普通索引
是最基本的索引，它没有任何限制。它有以下几种创建方式：

（1）直接创建索引

```sql
CREATE INDEX index_name ON table(column(length))
```
（2）修改表结构的方式添加索引
```sql
ALTER TABLE table_name ADD INDEX index_name ON (column(length))

```
（3）创建表的时候同时创建索引
```sql
CREATE TABLE `table` (
    `id` int(11) NOT NULL AUTO_INCREMENT ,
    `title` char(255) CHARACTER NOT NULL ,
    `content` text CHARACTER NULL ,
    `time` int(10) NULL DEFAULT NULL ,
    PRIMARY KEY (`id`),
    INDEX index_name (title(length))
)

```
（4）删除索引

```sql
DROP INDEX index_name ON table
```

### 唯一索引

与前面的普通索引类似，不同的就是：索引列的值必须唯一，但允许有空值。如果是组合索引，则列值的组合必须唯一。它有以下几种创建方式：
（1）创建唯一索引
```sql
CREATE UNIQUE INDEX indexName ON table(column(length))
```
（2）修改表结构
```sql
ALTER TABLE table_name ADD UNIQUE indexName ON (column(length))
```
（3）创建表的时候直接指定
```sql
CREATE TABLE `table` (
    `id` int(11) NOT NULL AUTO_INCREMENT ,
    `title` char(255) CHARACTER NOT NULL ,
    `content` text CHARACTER NULL ,
    `time` int(10) NULL DEFAULT NULL ,
    UNIQUE indexName (title(length))
);
```

### 主键索引
是一种特殊的唯一索引，一个表只能有一个主键，不允许有空值。一般是在建表的时候同时创建主键索引：
```sql
CREATE TABLE `table` (
    `id` int(11) NOT NULL AUTO_INCREMENT ,
    `title` char(255) NOT NULL ,
    PRIMARY KEY (`id`)
);
```

主键设计的原则：
1. 一定要显式定义主键
2. 采用与业务无关的单独列
3. 采用自增列
4. 数据类型采用int，并尽可能小，能用tinyint就不用int，能用int就不用bigint
5. 将主键放在表的第一列


这样设计的原因：
1. 在innodb引擎中只能有一个聚集索引，我们知道，聚集索引的叶子节点上直接存有行数据，所以聚集索引列尽量不要更改，而innodb表在有主键时会自动将主键设为聚集索引，如果不显式定义主键，会选第一个没有null值的唯一索引作为聚集索引，唯一索引涉及到的列内容难免被修改引发存储碎片且可能不是递增关系，存取效率低，所以最好显式定义主键且采用与业务无关的列以避免修改；如果这个条件也不符合，就会自动添加一个不可见不可引用的6byte大小的rowid作为聚集索引


2. 需采用自增列来使数据顺序插入，新增数据顺序插入到当前索引的后面，符合叶子节点的分裂顺序，性能较高；若不用自增列，数据的插入近似于随机，插入时需要插入到现在索引页的某个中间位置，需要移动数据，造成大量的数据碎片，索引结构松散，性能很差

3. 在主键插入时，会判断是否有重复值，所以尽量采用较小的数据类型，以减小比对长度提高性能，且可以减小存储需求，磁盘占用小，进而减少磁盘IO和内存占用；而且主键存储占用小，普通索引的占用也相应较小，减少占用，减少IO，且存储索引的页中能包含较多的数据，减少页的分裂，提高效率

4. 将主键放在表的第一列好像是习惯，原因我也不知道，试了下是可以放在其他列的......

### 组合索引


指多个字段上创建的索引，只有在查询条件中使用了创建索引时的第一个字段，索引才会被使用。使用组合索引时遵循最左前缀集合
```
ALTER TABLE `table` ADD INDEX name_city_age (name,city,age); 
```

### 全文索引
* FULLTEXT即为全文索引，目前只有MyISAM引擎支持。其可以在CREATE TABLE ，ALTER TABLE ，CREATE INDEX 使用，不过目前只有 CHAR、VARCHAR ，TEXT 列上可以创建全文索引。
* 主要用来查找文本中的关键字，而不是直接与索引中的值相比较。fulltext索引跟其它索引大不相同，它更像是一个搜索引擎，而不是简单的where语句的参数匹配。
* fulltext索引配合match against操作使用，而不是一般的where语句加like。它可以在create table，alter table ，create index使用，不过目前只有char、varchar，text 列上可以创建全文索引。值得一提的是，在数据量较大时候，现将数据放入一个没有全局索引的表中，然后再用CREATE index创建fulltext索引，要比先为一张表建立fulltext然后再将数据写入的速度快很多。


（1）创建表的适合添加全文索引
```sql
CREATE TABLE `table` (
    `id` int(11) NOT NULL AUTO_INCREMENT ,
    `title` char(255) CHARACTER NOT NULL ,
    `content` text CHARACTER NULL ,
    `time` int(10) NULL DEFAULT NULL ,
    PRIMARY KEY (`id`),
    FULLTEXT (content)
);
```
（2）修改表结构添加全文索引

```
ALTER TABLE article ADD FULLTEXT index_content(content)
```
（3）直接创建索引
```
CREATE FULLTEXT INDEX index_content ON article(content)
```

### 覆盖索引
索引包含所有需要查询的字段的值。

* 覆盖索引，select的数据列只用从索引中就能够取得，不必读取数据行，换句话说查询列要被所建的索引覆盖。
* 这个时候，可以不用访问指定的数据。也就不需要回表查询具体的数据，通过索引即可得到想要的数据。



具有以下优点：

- 索引通常远小于数据行的大小，只读取索引能大大减少数据访问量。
- 一些存储引擎（例如 MyISAM）在内存中只缓存索引，而数据依赖于操作系统来缓存。因此，只访问索引可以不使用系统调用（通常比较费时）。
- 对于 InnoDB 引擎，若辅助索引能够覆盖查询，则无需访问主索引。


### 索引优点

- 大大减少了服务器需要扫描的数据行数。

- 帮助服务器避免进行排序和分组，以及避免创建临时表（B+Tree 索引是有序的，可以用于 ORDER BY 和 GROUP BY 操作。临时表主要是在排序和分组过程中创建，不需要排序和分组，也就不需要创建临时表）。

- 将随机 I/O 变为顺序 I/O（B+Tree 索引是有序的，会将相邻的数据都存储在一起）。
### 索引缺点

1. 虽然索引大大提高了查询速度，同时却会降低更新表的速度，如对表进行insert、update和delete。因为更新表时，不仅要保存数据，还要保存一下索引文件。
2. 建立索引会占用磁盘空间的索引文件。一般情况这个问题不太严重，但如果你在一个大表上创建了多种组合索引，索引文件的会增长很快。

索引只是提高效率的一个因素，如果有大数据量的表，就需要花时间研究建立最优秀的索引，或优化查询语句。


### 注意事项

1. 索引不会包含有null值的列。只要列中包含有null值都将不会被包含在索引中，复合索引中只要有一列含有null值，那么这一列对于此复合索引就是无效的。所以我们在数据库设计时不要让字段的默认值为null。
2. 使用短索引。对串列进行索引，如果可能应该指定一个前缀长度。例如，如果有一个char(255)的列，如果在前10个或20个字符内，多数值是惟一的，那么就不要对整个列进行索引。短索引不仅可以提高查询速度而且可以节省磁盘空间和I/O操作。
3. 索引列排序。查询只使用一个索引，因此如果where子句中已经使用了索引的话，那么order by中的列是不会使用索引的。因此数据库默认排序可以符合要求的情况下不要使用排序操作；尽量不要包含多个列的排序，如果需要最好给这些列创建复合索引。
4. like语句操作。一般情况下不推荐使用like操作，如果非使用不可，如何使用也是一个问题。like “%aaa%” 不会使用索引而like “aaa%”可以使用索引。
5. 不要在列上进行运算。这将导致索引失效而进行全表扫描，例如
```sql
SELECT * FROM table_name WHERE YEAR(column_name)<2017;
```
6. 不使用not in和<>操作
## 2 索引类型——聚集

* **聚集索引**（聚簇索引）：数据行的物理顺序与列值（一般是主键的那一列）的逻辑顺序相同，一个表中只能拥有一个聚集索引。以 InnoDB 作为存储引擎的表，表中的数据都会有一个主键，即使你不创建主键，系统也会帮你创建一个隐式的主键。这是因为 InnoDB 是把数据存放在 B+ 树中的，而 B+ 树的键值就是主键，在 B+ 树的叶子节点中，存储了表中所有的数据。这种以主键作为 B+ 树索引的键值而构建的 B+ 树索引，我们称之为聚集索引。
* **非聚集索引**（非聚簇索引）：该索引中索引的逻辑顺序与磁盘上行的物理存储顺序不同，一个表中可以拥有多个非聚集索引。以主键以外的列值作为键值构建的 B+ 树索引，我们称之为非聚集索引。
* **回表**：非聚集索引与聚集索引的区别在于非聚集索引的叶子节点不存储表中的数据，而是存储该列对应的主键，想要查找数据我们还需要根据主键再去聚集索引中进行查找，这个再根据聚集索引查找数据的过程，我们称为**回表**。


使用以下语句进行查询，不需要进行二次查询，直接就可以从非聚集索引的节点里面就可以获取到查询列的数据。

```
select id, username from t1 where username = '小明'
select username from t1 where username = '小明'
```
但是使用以下语句进行查询，就需要二次的查询去获取原数据行的score：
```
select username, score from t1 where username = '小明'
```
## 3 索引类型——算法

### 顺序索引
* 顺序文件上的索引：按指定属性升序或降序存储的关系。在属性上建立一个顺序索引文件，索引文件有属性值和响应的元组指针组成。

### B+树索引
* B+树上的索引：B+树的叶节点为属性值和相应的元组指针。具有动态平衡的特点。

### hash索引
* 散列索引(哈希索引)：建立若干个桶，将索引属性按照其散列函数值映射到相应的桶中，同种存放索引属性值和响应的元组指针。散列索引具有查找速度快的特点。

* HASH索引可以一次定位，不需要像树形索引那样逐层查找,因此具有极高的效率。但是，这种高效是有条件的，即只在“=”和“in”条件下高效，对于范围查询、排序及组合索引仍然效率不高。这是MySQL里默认和最常用的索引类型。

### 位图索引
* 位图索引：用为向量记录索引属性中可能出现的值，每个为向量对应一个可能的值。

### 前缀索引
* tire索引/前缀索引：用来索引字符串。


> 特点：索引能够加快数据库查询，需要占用一定的存储空间。基本表更新后，索引也需要更新。用户不必显示地选择索引，关系数据库管理系统在执行查询时，会自动选择合适的索引作为存储路径。


> MySQL中常用的索引结构有：B+树索引和哈希索引两种。目前建表用的B+树索引就是BTREE索引。
> 在MySQL中，MyISAM和InnoDB两种存储引擎都不支持哈希索引。只有HEAP/MEMORY引擎才能显示支持哈希索引。


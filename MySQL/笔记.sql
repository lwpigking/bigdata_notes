# 数据库操作

# 创建一个名称为lwpigking_db01的数据库
CREATE DATABASE lwpigking_db01;

# 创建一个使用utf8字符集的lwpigking_db02数据库
CREATE DATABASE lwpigking_db02 CHARACTER 
SET utf8;

# 创建一个使用utf8字符集，并带校对规则的lwpingking_db03数据库
CREATE DATABASE lwpigking_db03 CHARACTER 
SET utf8 COLLATE utf8_bin;
# 校对规则：utf_bin区分大小写，默认utf8_general_ci不区分大小写

# 查看当前数据库服务器中的所有数据库
SHOW DATABASES;

# 查看创建的lwpigking_db01数据库的定义信息
SHOW CREATE DATABASE lwpigking_db01;
# 在创建数据库或者表时吗，为了规避关键字，可以使用``反引号来解决

# 删除lwpigking_db01数据库
DROP DATABASE lwpigking_db01;

# 备份数据库
# 该命令需要在Dos下执行
mysqldump -u root -p -B lwpigking_db02 lwpigking_db03 > d:\\bak.sql
# 恢复(导入)数据库(在mysql命令行下执行)
source d:\\bak.sql
# 备份数据表
mysqldump -u root -p 数据库 表1  > 文件名.sql


# 创建表
CREATE TABLE `user` ( id INT, `name` VARCHAR ( 255 ), `password` VARCHAR ( 255 ), `birthday` DATE ) CHARACTER 
SET utf8 COLLATE utf8_bin ENGINE INNODB;


# DECIMAL(P，D)表示列可以存储D位小数的P位数

# CHAR是字符，VARCHAR是字节
# VARCHAR(4)是4个字符，不是4个字节，其他都是字节
# CHAR的查询速度大于VARCHAR
# 如果数据是定长，则使用CHAR，如果是变长，就是用VARCHAR

# DATE记录年月日，DATETIME记录年月日时分秒，TIMESTAMP记录年月日时分秒，可以自动更新
# TIMESTAMP NOT NULL DEFAULT CURRENT_TIME ON UPDATE CURRENT_TIMESTAMP


CREATE TABLE `emp` (
	id INT,
	`name` VARCHAR ( 32 ),
	sex CHAR ( 1 ),
	birthday DATE,
	entry_date DATETIME,
	job VARCHAR ( 32 ),
	salary DOUBLE,
	resume TEXT 
) CHARSET utf8 COLLATE utf8_bin ENGINE INNODB;


# 修改表
# ALTER TABLE tablename ADD column datatype
# ALTER TABLE tablename MODIFY column datatype
# ALTER TABLE tablename DROP column
# ALTER TABLE tablename CHANGE col newcol datatype
# ALTER TABLE tablename RENAME to .. 
# ALTER TABLE tablename CHARACTER SET ..

-- 员工表emp增加一个image列，varchar类型，加在resume后面
ALTER TABLE `emp` ADD image VARCHAR ( 32 ) AFTER resume;
DESC `emp`;
-- 修改job列，使其长度为60
ALTER TABLE `emp` MODIFY job VARCHAR ( 60 );
-- 删除sex列
ALTER TABLE `emp` DROP sex;
-- 表名改为employee
ALTER TABLE emp RENAME TO employee;
-- 修改表的字符集为utf8
ALTER TABLE `employee` CHARACTER SET utf8;
-- 列明name修改为user_name
ALTER TABLE employee CHANGE `name` user_name VARCHAR ( 32 );


# INSERT语句
# INSERT INTO table_name (column) VALUES (....)
CREATE TABLE goods ( id INT, goods_name VARCHAR ( 32 ), price DOUBLE );

INSERT INTO goods ( id, goods_name, price )
VALUES
	(
		10,
	'华为手机',
	2000);
	
INSERT INTO goods ( id, goods_name, price )
VALUES
	(
		20,
	'苹果手机',
	3000);

SELECT * FROM goods;

# UPDATE语句
# UPDATE tablename SET col_name=expr1 WHERE condition
UPDATE goods SET price=4000 WHERE goods_name='华为手机';
UPDATE goods SET price=price+2000 WHERE goods_name='苹果手机';

# DELETE语句
# DELETE FROM tablename WHERE condition
DELETE FROM goods WHERE goods_name='华为手机'; 

# SELECT语句
CREATE TABLE student(
	id INT NOT NULL DEFAULT 1,
	`name` VARCHAR(20) NOT NULL DEFAULT '',
	chinese FLOAT NOT NULL DEFAULT 0.0,
	english FLOAT NOT NULL DEFAULT 0.0,
	math FLOAT NOT NULL DEFAULT 0.0
);

INSERT INTO student VALUES(1, '韩顺平', 89, 78, 90);
INSERT INTO student VALUES(2, '张飞', 67, 98, 56);
INSERT INTO student VALUES(3, '宋江', 87, 78, 77);
INSERT INTO student VALUES(4, '关羽', 88, 98, 90);
INSERT INTO student VALUES(5, '赵云', 82, 84, 67);
INSERT INTO student VALUES(6, '欧阳锋', 55, 85, 45);
INSERT INTO student VALUES(7, '黄蓉', 75, 65, 30);

SELECT * FROM student;

SELECT `name`, (chinese + english + math) as sum FROM student;

SELECT `name` AS '姓名', (chinese + english + math + 10) AS '总分' FROM student;

SELECT * FROM student WHERE `name` = '赵云';

SELECT * FROM student WHERE english > 90; 

SELECT
	`name` AS '姓名',
	( chinese + english + math ) AS '总分' 
FROM
	student 
WHERE
	( chinese + english + math ) > 200;
	
SELECT
	* 
FROM
	student 
WHERE
	( chinese + english + math ) > 200 
	AND math < chinese 
	AND `name` LIKE '赵%';

SELECT
	* 
FROM
	student 
ORDER BY
	math;

SELECT
	`name`,
	( chinese + english + math ) AS total_score 
FROM
	student 
ORDER BY
	total_score DESC;

INSERT INTO student VALUES(8, '韩信', 45, 65, 99);

SELECT
	* 
FROM
	student 
WHERE
	`name` LIKE '韩%';
ORDER BY
	math DESC;
	
SELECT COUNT(*) FROM student;
SELECT COUNT(*) FROM student WHERE math > 90;
SELECT COUNT(*) FROM student WHERE (math + english + chinese) > 250;
-- count(*) 返回满足条件的记录的行数
-- conut(column) 统计满足条件的某列有多少个，会排除null

SELECT SUM(math) FROM student;
SELECT AVG(math + english + chinese) FROM student;

SELECT
	MAX( math + english + chinese ) AS max_score,
	MIN( math + english + chinese ) AS min_score 
FROM
	student;


# 字符串函数
-- CHARACTER：返回字符集
-- CONCAT：连接字符串
-- INSTR(str,substr)：返回substr在str中出现的位置
-- UCASE(str)：转大写
-- LCASE(str)：转小写
-- LEFT(str,len)：从str的左边取len个字符
-- RIGHT(str,len)：从str的右边取len个字符
-- LENGTH(str)：str长度，按字节返回。
SELECT LENGTH('你好');
-- REPLACE(str,from_str,to_str)：替换
-- STRCMP(expr1,expr2)：比较字符串大小
-- SUBSTRING：取字符
-- LTRIM(str) RTRIM(str) TRIM()：去空格

# 数学函数
# ABS(X) 取X的绝对值
# CEILING(X) 向上取整，得到比x大的最小整数
# CONV(N,from_base,to_base) 进制转换
# FLOOR(X) 向下取整，得到比x小的最大整数
# FORMAT(X,D) 保留小数位数
# HEX(N_or_S) 转十六进制
# LEAST(value1,value2,...) 最小值
# MOD(N,M) 求余
# RAND() 范围为[0,1]

# 时间函数
# CURRENT_DATE 
# CURRENT_TIME
# CURRENT_TIMESTAMP
# DATE(expr)
# DATE_ADD(date,INTERVAL expr unit)
# DATE_SUB(date,INTERVAL expr unit)
# DATEDIFF(expr1,expr2)
# TIMEDIFF(expr1,expr2)
# NOW()
# YEAR(date)  MONTH(date)  FROM_UNIXTIME()

# 加密和系统函数
# USER() 查询用户@IP地址
SELECT USER();
# DATABASE() 数据库名称
SELECT DATABASE();
# MD5(str) 为字符串算出一个MD5 32的字符串，（用户密码）加密
SELECT MD5('123456');

# 流程控制函数
# IF(expr1,expr2,expr3) 如果expr1为True，则返回expr2，否则返回expr3
# IFNULL(expr1,expr2) 如果expr1不为空值，则返回expr1，否则返回expr2
# SELECT CASE WHEN expr1 THEN expr2 WHEN expr3 THEN expr4 ELSE expr5 END
# 如果expr1为True，则返回expr2，如果expr3为true，则返回expr4，否则返回expr5

# 查询加强
# 在mysql中，日期类型可以直接比较
# LIKE模糊查询 %：表示从0到多个任意字符，_：表示单个任意字符
# 分页查询 SELECT....LIMIT start, rows：表示从start+1行开始取，取出rows行


# 外连接
# 左外连接：左侧的表完全显示 
# 右外连接：右侧的表完全显示
# SELECT ... FROM 表1 LEFT JOIN 表2 ON 条件


# 复合主键
CREATE TABLE t18(
	id INT,
	`name` VARCHAR(32),
	email VARCHAR(32),
	PRIMARY KEY (id, `name`)  -- id和name为复合主键，即id和name这两个列不能相等
) ;

INSERT INTO t18 VALUES(1, 'tom', 'tom@sohu.com');
INSERT INTO t18 VALUES(1, 'jack', 'jack@sohu.com');
INSERT INTO t18 VALUES(1, 'tom', 'tom@sohu.com');  -- 不能插入


# 外键
# FOREIGN KEY (本表字段名) REFERENCES 主表名(UNIQUE或者primary KEY的列)
CREATE TABLE my_class (
	id INT PRIMARY KEY,
	`name` VARCHAR(32) NOT NULL DEFAULT ''
);

CREATE TABLE my_stu(
	id INT PRIMARY KEY,
	`name` VARCHAR(32) NOT NULL DEFAULT '',
	class_id INT,
	FOREIGN KEY (class_id) REFERENCES my_class(id)
);


# CHECK
# 强制要求在某一定范围内


# 索引
# 在emp表的empno字段创建索引，索引名称为empno_index
CREATE INDEX empno_index ON emp(empno);
# 删除索引
DROP INDEX empno_index ON emp;
# 删除主键索引
ALTER TABLE emp DROP PRIMARY KEY;

# 查询索引
SHOW INDEX FROM emp;
SHOW KEYS FROM emp;


# 事务
CREATE TABLE t27 (
	id INT,
	`name` VARCHAR(32)
);
# 开启事务
START TRANSACTION;
# 创建检查点a
SAVEPOINT a;
# 执行dml语句
INSERT INTO t27 VALUES (100, 'tom');
SELECT * FROM t27;
# 创建检查点b
SAVEPOINT b;
# 执行dml
INSERT INTO t27 VALUES (200, 'jack');
# 回退到b
ROLLBACK TO b;
# 回退到a
ROLLBACK TO a;
# 直接回退到事务开始的状态
ROLLBACK
# 提交事务
COMMIT

# 事务隔离级别
# 查看隔离级别
SELECT @@transaction_isolation;

# 脏读（dirty read）：当一个事务读取另一个事务尚未提交的修改时，产生脏读

# 不可重复读(nonrepeatable read)：同一查询在同一事务中多次进行，由于其他提交事务所做的
# 修改或删除，每次返回不同的结果集

# 幻读(phantom read)：同一查询在同一事务中多次进行，由于其他提交事务所做的插入操作，
# 每次返回不同的结果集


# 引擎
SHOW ENGINES;


# 视图
CREATE VIEW 视图名 AS SELECT ...
ALTER VIEW 视图名 AS SELECT ...
SHOW CREATE VIEW 视图名
DROP VIEW 视图名


# 用户管理
# 创建用户
CREATE USER 'lwpigking'@'localhost' IDENTIFIED BY '123456';
# 查询用户
SELECT * FROM mysql.user;
# 删除用户
DROP USER 'lwpigking'@'localhost';
# 修改密码
ALTER USER 'root'@'localhost' IDENTIFIED BY '123456';

# 权限管理
GRANT 权限列表 ON 库.对象名 TO '用户名'@'登录ip'
# 权限列表
GRANT SELECT ON...
GRANT SELECT, DELETE, CREATE ON....
GRANT ALL ON...
# 回收权限
REVOKE 权限列表 ON 库.对象名 FROM '用户名'@'登录ip'
# 刷新权限
FLUSH PRIVILEGES

CREATE TABLE admin (
	NAME VARCHAR(32) NOT NULL UNIQUE,
	pwd VARCHAR(32) NOT NULL DEFAULT ''
)
CHARACTER SET utf8;

INSERT INTO admin VALUES ('tom', '123');

SELECT * FROM admin WHERE NAME = '1' OR' AND pwd = 'OR '1' = '1'
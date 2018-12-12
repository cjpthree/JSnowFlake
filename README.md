```
改良的twitter的snowflake算法。双核cpu每秒大约可产生400万id值
ref: https://blog.csdn.net/u013970991/article/details/69388427
/---32 bit 秒单位时间戳---22 bit序列号---10 bit机器标识---/
* 32位秒单位时间戳，能用136年
* 22位序列号，最大值四百多万。从随机数开始，不断自增，序列号不会根据时间归零，序列号到了2^22归零继续自增。不惧怕时钟回拨，毕竟时钟回拨的时间窗口内序列号不会重复
* 10位机器标识，可以通过构造函数传入，没有传入就根据mac地址生成

使用方法：
在项目pom.xml中添加依赖
<dependency>
    <groupId>com.jarxi</groupId>
    <artifactId>jsnowflake</artifactId>
    <version>1.0.0</version>
</dependency>
```

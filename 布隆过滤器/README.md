# 布隆过滤器

布隆过滤器（英语：Bloom Filter）是1970年由布隆提出的。它实际上是一个很长的二进制向量和一系列随机映射函数。布隆过滤器可以用于检索一个元素是否在一个集合中。它的优点是空间效率和查询时间都远远超过一般的算法，缺点是有一定的误识别率和删除困难。

## 原理
布隆过滤器是由一个固定大小的二进制位图和一系列散列函数组成。

初始状态下，二进制位图中所有位都为0

![布隆过滤器初始状态](/image/布隆过滤器初始状态.png)

添加元素，将传入的元素进行多次(图中为3此)不同的散列函数计算，并将计算结果对应的位置为1

![布隆过滤器添加数据.png](/image/布隆过滤器添加数据.png)

查询的时候，同样将传入的元素进行多次不同的散列函数计算。如果其中某一位为0，则代表该元素 **绝对不存在**。但如果都为1，则表示该元素 **可能存在**。

> 例如objA的计算结果为1, 5, objB的计算结果为2, 4, objC的计算结果为1, 4
> 这种情况布隆过滤器会认为objC存在


## 特性
1. 判断为 **不存在** 的元素 **一定不存在**，判断为 **存在** 的元素 **不一定存在**
2. 可以中途添加元素，但不可以中途删除元素。

## 优点
1. 速度快 -- 因为是用散列函数，计算很快，时间复杂度为o(1)
2. 占用空间少 -- 不用存储所有数据，仅需要存储散列计算的结果对应的位置
3. 安全 -- 因布隆过滤器不存储元素本身，所以安全性较高


## 缺点
1. 误识别率(不存在的元素被当成存在)
    > 当存入的数值越多，布隆过滤器中的bit位为1的就越多，后续输入的数值对应的bit位有可能已经都被置为1，导致判断错误。  
      同时，极端情况下，整个bitmap有可能都被置为1，这种情况下布隆过滤器就失效了
   
2. 无法删除
    > 原因同上，由于无法知道每个bit位的是来自于哪个元素的，如果贸然重置为0，可能会导致其他数据判断错误。

3. 无法扩容，必须初期就预计好数据数量
    > 如果扩容但不修改散列函数，这样多出来的容量只会浪费。  
      如果要修改散列函数，但因为无法知道原先的元素值，所以无法重新进行散列计算

## 运用场景
1. 判断元素是否存在，且不需要知道具体数据。
2. 海量数据去重，重复数据不在进行计算。
3. 过滤垃圾请求，数据库不存在的数据直接返回。

## 示例
### Google的Guava示例
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>
```
```java
/**
 * 创建布隆过滤器
 * Funnels.integerFunnel() -- integer类型
 * 预计数据条数
 * 预计误判率
 */
BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), 100000, 0.01);
// 添加数据
for (int i = 0; i < 100000; i++) {
    bloomFilter.put(i);
}
// 判断误判数量
int count = 0;
for (int i = 900000; i < 1000000; i++) {
    if (bloomFilter.mightContain(i)) {
        count++;
    }
}
System.out.println("10000条的误判数：" + count);
System.out.println("10000条的误判率：" + count / 10000.0);
```
```log
10000条的误判数：115
10000条的误判率：0.0115
```
从上可以看到，当布隆过滤器数据量合适的时候，误判率是基本符合的。
但若在添加元素，误判率则会极大增加
```java
// 此时继续增加元素，再次计算误判率
for (int i = 100000; i < 200000; i++) {
    bloomFilter.put(i);
}
count = 0;
for (int i = 900000; i < 1000000; i++) {
   if (bloomFilter.mightContain(i)) {
      count++;
   }
}
System.out.println("20000条的误判数：" + count);
System.out.println("20000条的误判率：" + count / 10000.0);
```
```log
20000条的误判数：1601
20000条的误判率：0.1601
```

### Redis的布隆过滤器
Redis可以通过自带bitMap实现布隆过滤器，但具体的逻辑需要自己编写。

#### 直接安装插件
Redis官方也提供了[布隆过滤器插件](https://github.com/RedisBloom/RedisBloom)，需要下载并手动编译，启动时加载到现有的Redis中。

```bash
# 编译插件前，需要系统有gcc, make, python3, wget
apt install gcc make python3 wget

# 克隆代码(一定要加上, recursive, 循环克隆子项目，不然编译可能会出错)
git clone --recursive https://github.com/RedisBloom/RedisBloom.git
cd RedisBloom
# 编译
make setup
make

# 编译完后，重启Redis的命令带上编译好的so插件即可
redis-server --loadmodule ./redisbloom.so
```


~~**注意** 布隆插件的2.2.15版本疑似有BUG，会编译报文件不存在的错误，2.2.14版本就没有问题。~~
```bash
/mnt/d/Docker/Redis/RedisBloom-2.2.15/src/rm_tdigest.c: In function ‘TDigestSketch_Create’:
/mnt/d/Docker/Redis/RedisBloom-2.2.15/src/rm_tdigest.c:72:5: error: unknown type name ‘td_histogram_t’
   72 |     td_histogram_t *tdigest = td_new(compression);
      |     ^~~~~~~~~~~~~~
```




#### Docker中安装带布隆过滤器的Redis镜像
在Docker中，官方有一个带有RedisBloom的插件版本的Redis，可以直接拉取镜像使用。
```bash
# docker搜索redislabs可以搜索到很多有趣的插件，其中redislabs/rebloom就是我们需要的带布隆过滤器版本的redis
docker search redislabs

NAME                                DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
redislabs/redisearch                Redis With the RedisSearch module pre-loaded…   52                       
redislabs/redisinsight              RedisInsight - The GUI for Redis                47                       
redislabs/rejson                    RedisJSON - Enhanced JSON data type processi…   44                       
redislabs/redis                     Clustered in-memory database engine compatib…   33                       
redislabs/redismod                  An automated build of redismod - latest Redi…   23                   [OK]
redislabs/redisgraph                A graph database module for Redis               19                   [OK]
redislabs/rebloom                   A probablistic datatypes module for Redis       19                   [OK]

# 拉取镜像
docker pull redislabs/rebloom 

# 启动
docker run -d -p 6379:6379 --name redis-redisbloom redislabs/rebloom

# 进入容器
docker exec -it redis-redisbloom bash

# 打开redis命令行
redis-cli
```

#### Redis布隆过滤器基本指令
* 创建布隆过滤器
   ```bash
   # bf.reserve
   # filter1是过滤器的名字
   # 0.01是错误率，值越小，数组越大
   # 元素个数，超出个数后，准确率会明显下降
   127.0.0.1:6379> bf.reserve filter1 0.01 10000
   OK
   # 再次创建，会提示错误
   127.0.0.1:6379> bf.reserve filter1 0.01 10000
   (error) ERR item exists
   ```
  
* 添加元素
   ```bash
   # bf.add
   # filter1是过滤器的名字
   # objA是添加的元素
   127.0.0.1:6379> bf.add filter1 objA
   (integer) 1
   ```

* 判断元素是否在布隆过滤器
   ```bash
   # bf.exists
   # filter1是过滤器的名字
   # objA是查询的元素
   127.0.0.1:6379> bf.exists filter1 objA
   (integer) 1
   127.0.0.1:6379> bf.exists filter1 objB
   (integer) 0
   ```

* 添加多个元素
   ```bash
   # bf.madd
   # filter1是过滤器的名字
   # objB objC objD是添加的元素
   127.0.0.1:6379> bf.madd filter1 objB objC objD
   1) (integer) 1
   2) (integer) 1
   3) (integer) 1
   ``` 

* 判断多个元素是否在布隆过滤器
   ```bash
   # bf.mexists
   # filter1是过滤器的名字
   # objB objC objF是查询的元素
   127.0.0.1:6379> bf.mexists filter1 objB objC objF
   1) (integer) 1
   2) (integer) 1
   3) (integer) 0
   ```

* 删除布隆过滤器
  ```bash
  # 直接删除key即可
  127.0.0.1:6379> del filter1
  (integer) 1
  ```


#### Redisson
Redisson已经封装好了相关的操作逻辑，且使用原版的Redis即可，不对Redis进行任何修改，直接使用即可。
```java
Config config = new Config();
config.useSingleServer().setAddress("redis://myServer:3306");
RedissonClient redisson = Redisson.create(config);
RBloomFilter<String> bloomFilter = redisson.getBloomFilter("bloomFilter");
// 初始化布隆过滤器，预计统计元素数量为10000，期望误差率为0.01
bloomFilter.tryInit(10000L, 0.01);
// 添加数据
for (int i = 0; i < 10000; i++) {
    bloomFilter.add(String.valueOf(i));
}
// 判断是否存在
int count = 0;
for (int i = 90000; i < 100000; i++) {
    if (bloomFilter.contains(String.valueOf(i))) {
      count++;
    }
}
```


## 参考资料
1. [Redis布隆过滤器官方文档](https://redis.io/docs/stack/bloom/)
2. [维基百科](https://zh.wikipedia.org/wiki/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8)
3. [布隆过滤器，这一篇给你讲的明明白白](https://developer.aliyun.com/article/773205)
4. [Guava包中的BloomFilter](https://blog.csdn.net/zc19921215/article/details/91047708)
5. [布隆过滤器](https://www.cnblogs.com/Howlet/p/12688707.html)
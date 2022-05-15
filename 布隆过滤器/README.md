# 布隆过滤器

布隆过滤器（英语：Bloom Filter）是1970年由布隆提出的。它实际上是一个很长的二进制向量和一系列随机映射函数。布隆过滤器可以用于检索一个元素是否在一个集合中。它的优点是空间效率和查询时间都远远超过一般的算法，缺点是有一定的误识别率和删除困难。

## 原理
TODO 

## 步骤

## 特性
1. 判断为 **不存在** 的元素 **一定不存在**，判断为 **存在** 的元素 **不一定存在**
2. 可以中途添加元素，但不可以中途删除元素。

## 场景
1. 判断元素是否存在，且不需要知道具体数据。
2. 海量数据去重。
3. 过滤垃圾请求。


## 优点
1. 速度快 -- 因为是用散列函数，计算很快，时间复杂度为o(1)
2. 占用空间少 -- 不用存储所有数据，仅需要存储散列计算的结果对应的位置
3. 安全 -- 因布隆过滤器不存储元素本身，所以安全性较高


## 缺点
1. 误识别率(不存在的元素被当成存在)

    当存入的数值越多，布隆过滤器中的bit位为1的就越多，后续输入的数值对应的bit位有可能已经都被置为1，导致判断错误。
    同时，极端情况下，整个bitmap有可能都被置为1，这种情况下布隆过滤器就失效了
    
    例如objA的计算结果为1, 5, objB的计算结果为2, 4, objC的计算结果为1, 4, 这种情况就会出现误判


2. 无法删除

    原因同上，由于无法知道每个bit位的是来自于哪个元素的，如果贸然重置为0，可能会导致其他数据判断错误。
    
    如上述例子，如果将位置4重置为0，则objB和objC都无法再次判断。


3. 无法扩容，必须初期就预计好数据数量


## 示例
### Google的Guava示例
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

### Redisson示例
Redisson已经封装好了相关的操作逻辑，使用的是Redis自带的bitmap(本质是一个字符串)，因此不需要安装Redis的布隆过滤器插件也可以实现Redis中的布隆过滤器。
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
        // 判断误判数量
        int count = 0;
        for (int i = 90000; i < 100000; i++) {
            if (bloomFilter.contains(String.valueOf(i))) {
              count++;
            }
        }
```


## 参考资料
1. [维基百科](https://zh.wikipedia.org/wiki/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8)
2. [布隆过滤器，这一篇给你讲的明明白白](https://developer.aliyun.com/article/773205)
3. [Guava包中的BloomFilter](https://blog.csdn.net/zc19921215/article/details/91047708)
4. [布隆过滤器](https://www.cnblogs.com/Howlet/p/12688707.html)
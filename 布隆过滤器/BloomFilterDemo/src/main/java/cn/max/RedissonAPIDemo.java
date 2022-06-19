package cn.max;


import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * 采用Redisson的RBloomFilter API操作Redis的Bitmap，不需要安装布隆过滤器插件
 *
 * @author MaxStar
 * @date 2022/5/18
 */
public class RedissonAPIDemo {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://myServer:3306");
        RedissonClient redissonClient = Redisson.create(config);
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("filter2");
        // 初始化布隆过滤器，预计统计元素数量为10000，期望误差率为0.01
        bloomFilter.tryInit(10000L, 0.01);
        // 添加数据
        bloomFilter.add("TOM");
        bloomFilter.add("JACK");
        // 判断数据
        // true
        System.out.println("TOM是否存在: " + bloomFilter.contains("TOM"));
        // ture
        System.out.println("JACK是否存在: " + bloomFilter.contains("JACK"));
        // false
        System.out.println("Black是否存在: " + bloomFilter.contains("Black"));
        System.out.println("预期插入数量: " +  bloomFilter.getExpectedInsertions());
        System.out.println("容错率: " + bloomFilter.getFalseProbability());
        System.out.println("hash函数的个数: " + bloomFilter.getHashIterations());
        System.out.println("插入对象的个数: " + bloomFilter.count());

        redissonClient.getAtomicLong("key").addAndGet(1L);

    }
}

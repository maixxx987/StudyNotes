package cn.max;


import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonDemo {
    public static void main(String[] args) {
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
        System.out.println("10000条的误判数：" + count);
        System.out.println("10000条的误判率：" + count / 10000.0);


        // 此时继续增加元素，再次计算误判率
        for (int i = 10000; i < 20000; i++) {
            bloomFilter.add(String.valueOf(i));
        }
        count = 0;
        for (int i = 90000; i < 100000; i++) {
            if (bloomFilter.contains(String.valueOf(i))) {
                count++;
            }
        }
        System.out.println("20000条的误判数：" + count);
        System.out.println("20000条的误判率：" + count / 10000.0);
    }
}

package cn.max;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class GuavaDemo {
    public static void main(String[] args) {
        /**
         * 创建布隆过滤器
         * Funnels.integerFunnel() -- integer类型
         * 预计数据条数
         * 预计误判率
         */
        BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), 10000, 0.01);
        // 添加数据
        for (int i = 0; i < 10000; i++) {
            bloomFilter.put(i);
        }
        // 判断误判数量
        int count = 0;
        for (int i = 90000; i < 100000; i++) {
            if (bloomFilter.mightContain(i)) {
                count++;
            }
        }
        System.out.println("10000条的误判数：" + count);
        System.out.println("10000条的误判率：" + count / 10000.0);


        // 此时继续增加元素，再次计算误判率
        for (int i = 10000; i < 20000; i++) {
            bloomFilter.put(i);
        }
        count = 0;
        for (int i = 90000; i < 100000; i++) {
            if (bloomFilter.mightContain(i)) {
                count++;
            }
        }
        System.out.println("20000条的误判数：" + count);
        System.out.println("20000条的误判率：" + count / 10000.0);
    }
}

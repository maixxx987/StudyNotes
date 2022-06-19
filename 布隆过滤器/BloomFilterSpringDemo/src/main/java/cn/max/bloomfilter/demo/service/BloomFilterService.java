package cn.max.bloomfilter.demo.service;

import cn.max.bloomfilter.demo.annotation.FilterProperty;
import cn.max.bloomfilter.demo.common.Constants;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@Slf4j
@Service
@AllArgsConstructor
public class BloomFilterService {

    private RedissonClient redissonClient;

    /**
     * 反射检查每个需要检查的属性是否重复
     * 注：一个属性可能存在多个布隆过滤器，所以需要将查询内容传入每个过滤器过滤
     * <p>
     * 主要步骤
     * 1.获取布隆过滤器的名称
     * 2.获取所有的布隆过滤器
     * 3.逐个布隆过滤器判断元素是否存在
     *
     * @param object 数据
     */
    public Boolean isExist(Object object) throws IllegalAccessException {
        // 1.获取布隆过滤器的名称，此处以类的前缀 + 属性的后缀 + _ + 布隆过滤器序号 为key (如 video:name_1)
        Class<?> clazz = object.getClass();
        // 此处以类名作为具体key的前缀
        String keyPrefix = clazz.getSimpleName();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String value = (String) field.get(object);
            if (Strings.isBlank(value)) {
                continue;
            }
            // 有注解的代表需要通过布隆过滤器过滤
            FilterProperty annotation = field.getAnnotation(FilterProperty.class);
            if (Objects.nonNull(annotation)) {
                // 如果有注解，则代表需要查询布隆过滤器，判断该属性是否存在
                String keySuffix = field.getName();
                String keyPattern = keyPrefix + ":" + keySuffix + "_*";
                // 2.获取所有的布隆过滤器，此处因为要判断多个布隆过滤器，用一个临时标识标记
                boolean tempResult = false;
                for (String key : redissonClient.getKeys().getKeysByPattern(keyPattern)) {
                    RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
                    if (bloomFilter.isExists()) {
                        // 3.逐个布隆过滤器判断元素是否存在
                        if (bloomFilter.contains(value)) {
                            // 判断类型为判断元素全都存在，只要任意一个布隆过滤器存在改元素则则将标记改为true，结束当前元素判断
                            tempResult = true;
                            break;
                        }
                    }
                }
                if (!tempResult) {
                    // 某个元素不存在，直接返回
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 将元素添加进布隆过滤器(此方法在插入数据库后调用)
     * 主要步骤
     * 1. 获取布隆过滤器
     * 2. 检查布隆过滤器的容量是否满足
     * 3. 若不满足，则新建一个布隆过滤器，序号在上一个布隆过滤器的基础上 + 1
     * 4. 添加元素进布隆过滤器
     *
     * @param object 元素
     */
    public void insertBloomFilter(Object object) throws IllegalAccessException {
        // 1.获取布隆过滤器的名称，此处以类的前缀 + 属性的后缀 + _ + 布隆过滤器序号 为key (如 video:name_1)
        Class<?> clazz = object.getClass();
        // 此处以类名作为具体key的前缀
        String keyPrefix = clazz.getSimpleName();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String value = (String) field.get(object);
            if (Strings.isBlank(value)) {
                continue;
            }
            // 有注解的代表需要通过布隆过滤器过滤
            FilterProperty annotation = field.getAnnotation(FilterProperty.class);
            if (Objects.nonNull(annotation)) {
                // 如果有注解，则代表需要查询布隆过滤器，判断该属性是否存在
                String keySuffix = field.getName();
                // 2.判断是否蒸菜重构布隆过滤器正在，若正在重构则直接将重构的布隆过滤器作为目标布隆过滤器，将元素添加进去
                String newKey = keyPrefix + ":" + keySuffix + ":new";
                RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(newKey);
                if (!bloomFilter.isExists()) {
                    String keyPattern = keyPrefix + ":" + keySuffix + "_*";
                    // 3.布隆过滤器没有重构，获取所有布隆过滤器的key，找出最后一个作为目标布隆过滤器，将元素添加进去
                    Iterable<String> keysIterable = redissonClient.getKeys().getKeysByPattern(keyPattern);
                    List<String> keys = Lists.newArrayList(keysIterable.iterator());
                    String key = keys.get(keys.size() - 1);
                    bloomFilter = redissonClient.getBloomFilter(key);
                    // 4.判断布隆过滤器的容量是否已经达到极限，若达到则需要新建一个布隆过滤器(此处不直接重新创建是重建过程比新建要慢，重建可以放在凌晨执行)
                    long expectedInsertions = bloomFilter.getExpectedInsertions();
                    if (expectedInsertions <= bloomFilter.count()) {
                        // 5.添加分布式锁，防止布隆过滤器被覆盖创建
                        RLock lock = null;
                        try {
                            lock = redissonClient.getLock(keyPrefix + ":" + keySuffix + ":" + "lock");
                            // 6.将新建的布隆过滤器的序号为上一个布隆过滤器的序号 + 1
                            int num = Integer.parseInt(key.substring(key.indexOf("_") + 1));
                            key = key.replace("_" + num, "_" + (++num));
                            bloomFilter = redissonClient.getBloomFilter(key);
                            // 创建新的布隆过滤器
                            bloomFilter.tryInit(expectedInsertions, 0.01);
                        } catch (Exception e) {
                            log.error("新建布隆过滤器失败, 异常信息:{}", e.getMessage(), e);
                        } finally {
                            // 7.解锁
                            if (null != lock && lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        }
                    }
                }
                // 8.添加元素进布隆过滤器
                bloomFilter.add(field.get(object));
            }
        }
    }

    /**
     * 记录布隆过滤器中删除，修改次数，可以用来做是否要重建布隆过滤器的判断依据
     * 比如修改次数超过10次，则布隆过滤器要重新创建
     */
    public void updateModifyCounts(String prefix, List<String> suffixList) {
        for (String suffix : suffixList) {
            // 如果有注解，则代表需要查询布隆过滤器，判断该属性是否存在
            String key = prefix + ":" + suffix + ":count";
            redissonClient.getAtomicLong(key).addAndGet(1L);
        }
    }

    /**
     * 定时任务，每日新建/重建布隆过滤器
     * <p>
     * 主要步骤:
     * 1.查询数据
     * 2.建立新的布隆过滤器
     * 3.替换老的布隆过滤器
     * 4.删除多余的布隆过滤器
     * <p>
     * 此处可以追加一个步骤，比如该布隆过滤器内的元素删除或修改的个数达到一定阈值时才重建，这样可以减少重建次数
     */
    public void buildBloomFilter() throws ClassNotFoundException {
        // 1.从数据库中获取需要构建布隆过滤器的类，此处直接赋值
        String className = "cn.max.bloomfilter.demo.entity.Video";
        Class<?> clazz = Class.forName(className);
        // 2.为类中的每个属性构建布隆过滤器
        String keyPrefix = clazz.getSimpleName();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 3.判断该属性是否要构建布隆过滤器(有注解则需要)
            FilterProperty annotation = field.getAnnotation(FilterProperty.class);
            if (Objects.nonNull(annotation)) {
                String keySuffix = field.getName();
                // 4.获取需要存储的数据，此处模拟数据库操作
                List<String> dataList;
                if ("name".equals(keySuffix)) {
                    dataList = Constants.NAME_LIST;
                } else {
                    dataList = Constants.TYPE_LIST;
                }
                // 5.构建布隆过滤器名称，类名 + 属性名 + new，代表是新的布隆过滤器
                String key = keyPrefix + ":" + keySuffix + ":new";
                RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(key);
                bloomFilter = redissonClient.getBloomFilter(key);
                // 6.创建新的布隆过滤器，大小根据实际情况调整，此处为了测试方便，直接 + 1
                bloomFilter.tryInit(dataList.size() + 1, 0.01);
                // 7.为布隆过滤器添加数据
                for (String data : dataList) {
                    bloomFilter.add(data);
                }
                // 8.替换原先的布隆过滤器
                bloomFilter.rename(keyPrefix + ":" + keySuffix + "_1");
                // 9.删除其他的布隆过滤器
                String keyPattern = keyPrefix + ":" + keySuffix + "_*";
                Iterable<String> keysIterable = redissonClient.getKeys().getKeysByPattern(keyPattern);
                for (String deleteKey : keysIterable) {
                    // 第一个不删除，因为是刚刚新建的
                    if (Integer.parseInt(deleteKey.substring(deleteKey.indexOf("_") + 1)) == 1) {
                        continue;
                    }
                    // 删除
                    RBloomFilter<Object> deletedFilter = redissonClient.getBloomFilter(deleteKey);
                    if (bloomFilter.isExists()) {
                        deletedFilter.delete();
                    }
                }
            }
        }
    }
}

package cn.max.bloomfilter.demo;

import cn.max.bloomfilter.demo.entity.Video;
import cn.max.bloomfilter.demo.exception.NotFoundException;
import cn.max.bloomfilter.demo.service.BloomFilterService;
import cn.max.bloomfilter.demo.service.VideoService;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@SpringBootTest
class RedissonTests {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VideoService videoService;

    @Autowired
    private BloomFilterService bloomFilterService;

//    @Test
//    public void testRedissonConnect() {
//        redissonClient.getBucket("hello").set("world");
//        String test = (String) redissonClient.getBucket("hello").get();
//        System.out.println(test);
//    }

    /**
     * 每次测试前先清空所有Redis缓存并重建布隆过滤器
     */
    @BeforeEach
    public void buildFilter() throws ClassNotFoundException {
        RKeys keys = redissonClient.getKeys();
        keys.flushall();
        bloomFilterService.buildBloomFilter();
    }

    /**
     * name初始存在元素 name1, name2, name3
     * type初始存在元素 type1, type2, type3
     */
    @Test
    public void testFind() throws ClassNotFoundException {
        // 测试过滤不通过
        // 类型不存在
        Assertions.assertThrows(NotFoundException.class, () -> testAopException(videoService::findOne, new Video("name1", "type4", null)));
        // 名字不存在
        Assertions.assertThrows(NotFoundException.class, () -> testAopException(videoService::findOne, new Video("name4", "type1", null)));
        // 两个都不存在
        Assertions.assertThrows(NotFoundException.class, () -> testAopException(videoService::findOne, new Video("name4", "type4", null)));

        // 测试查询通过
        Assertions.assertEquals(new Video("name1", "type1", null), videoService.findOne(new Video("name1", "type1", null)));

        // 测试保存通过，且保存后未扩容
        Assertions.assertEquals(new Video("newName4", "newType4", null), videoService.save(new Video("newName4", "newType4", null)));
        List<String> keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "name" + "_*").iterator());
        Assertions.assertEquals(1, keys.size());
        keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "type" + "_*").iterator());
        Assertions.assertEquals(1, keys.size());

        // 再次查询，元素已存在
        Assertions.assertEquals(new Video("newName4", "newType4", null), videoService.findOne(new Video("newName4", "newType4", null)));

        // 再次保存，此时布隆过滤器扩容
        Assertions.assertEquals(new Video("newName5", "newType5", null), videoService.save(new Video("newName5", "newType5", null)));
        keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "name" + "_*").iterator());
        Assertions.assertEquals(2, keys.size());
        keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "type" + "_*").iterator());
        Assertions.assertEquals(2, keys.size());

        // 再次查询，元素已存在
        Assertions.assertEquals(new Video("newName5", "newType5", null), videoService.findOne(new Video("newName5", "newType5", null)));

        // 重建布隆过滤器，元素存在，且扩容的布隆过滤器移除
        bloomFilterService.buildBloomFilter();
        Assertions.assertEquals(new Video("newName4", "newType4", null), videoService.findOne(new Video("newName4", "newType4", null)));
        Assertions.assertEquals(new Video("newName5", "newType5", null), videoService.findOne(new Video("newName5", "newType5", null)));
        keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "name" + "_*").iterator());
        Assertions.assertEquals(1, keys.size());
        keys = Lists.newArrayList(redissonClient.getKeys().getKeysByPattern("Video" + ":" + "type" + "_*").iterator());
        Assertions.assertEquals(1, keys.size());
    }

    /**
     * 测试AOP抛出的异常
     * AOP捕获的自定义异常会变成java.lang.reflect.UndeclaredThrowableException，通过getRealThrowable()获取真正的异常
     */
    public void testAopException(Consumer<Video> consumer, Video video) throws Throwable {
        try {
            consumer.accept(video);
        } catch (UndeclaredThrowableException e) {
            throw e.getUndeclaredThrowable();
        }
    }
}

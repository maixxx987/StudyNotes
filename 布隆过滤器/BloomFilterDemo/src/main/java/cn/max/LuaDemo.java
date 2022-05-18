package cn.max;

import com.google.common.collect.Lists;
import org.redisson.Redisson;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * 采用Lua操作Redis的布隆过滤器，需要Redis安装布隆过滤器插件
 *
 * @author MaxStar
 * @date 2022/5/18
 */
public class LuaDemo {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://myServer:3306");
        RedissonClient redissonClient = Redisson.create(config);
        // Lua
        // 布隆过滤器命名为 filter1 (如不存在则会默认创建), 并写入参数TOM
        redissonClient.getScript().eval(RScript.Mode.READ_WRITE, "return redis.call('bf.add', KEYS[1], KEYS[2])", RScript.ReturnType.VALUE,
                Lists.newArrayList("filter1", "TOM"));
        // 判断TOM是否存在
        Boolean exists1 = redissonClient.getScript().eval(RScript.Mode.READ_WRITE, "return redis.call('bf.exists', KEYS[1], KEYS[2])", RScript.ReturnType.BOOLEAN,
                Lists.newArrayList("filter1", "TOM"));
        // true
        System.out.println(exists1);
        // 判断JACK是否存在
        Boolean exists2 = redissonClient.getScript().eval(RScript.Mode.READ_WRITE, "return redis.call('bf.exists', KEYS[1], KEYS[2])", RScript.ReturnType.BOOLEAN,
                Lists.newArrayList("filter1", "JACK"));
        // false
        System.out.println(exists2);
    }
}

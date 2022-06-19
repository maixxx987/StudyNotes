package cn.max.bloomfilter.demo.aop;


import cn.max.bloomfilter.demo.exception.NotFoundException;
import cn.max.bloomfilter.demo.service.BloomFilterService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class BloomFilterAop {

    private BloomFilterService bloomFilterService;


    @Pointcut("execution(* cn.max.bloomfilter.demo.service.VideoService.findOne(..))")
    public void findPointCut() {
    }

    @Pointcut("execution(* cn.max.bloomfilter.demo.service.VideoService.save(..))")
    public void savePointCut() {
    }

    /**
     * 查询，若布隆过滤器中不存在元素，直接返回
     */
    @Before("findPointCut()")
    public void beforeFind(JoinPoint joinPoint) throws Throwable {
        log.info("[BloomFilterAop#beforeFind]进入beforeFind");
        // 布隆过滤器中的元素不存在，直接返回
        if (!bloomFilterService.isExist(joinPoint.getArgs()[0])) {
            log.error("[BloomFilterAop#beforeFind]元素不存在");
            throw new NotFoundException("元素不存在");
        }
        log.info("[BloomFilterAop#beforeFind]退出beforeFind");
    }

    /**
     * 执行新增或更新的环绕通知
     * 1.插入前先检查数据是否重复
     * 2.插入后将数据添加到布隆过滤器
     */
    @AfterReturning(value = "savePointCut()", returning = "returnObj")
    public void afterSave(JoinPoint joinPoint, Object returnObj) throws Throwable {
        // 将新值插入布隆过滤器
        log.info("[BloomFilterAop#afterSave]开始添加数据 -> {}", returnObj);
        bloomFilterService.insertBloomFilter(returnObj);
        log.info("[BloomFilterAop#afterSave]结束添加数据 -> {}", returnObj);
    }
}

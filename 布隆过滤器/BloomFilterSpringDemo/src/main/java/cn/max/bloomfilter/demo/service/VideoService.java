package cn.max.bloomfilter.demo.service;

import cn.max.bloomfilter.demo.common.Constants;
import cn.max.bloomfilter.demo.entity.Video;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@Service
public class VideoService {

    /**
     * 模拟查询
     *
     * @param video 查询条件
     */
    public Video findOne(Video video) {
        return video;
    }

    /**
     * 模拟保存
     *
     * @param video 新的video对象
     */
    public Video save(Video video) {
        // 模拟入库
        if (Strings.isNotBlank(video.getName())) {
            Constants.NAME_LIST.add(video.getName());
        }
        // 模拟入库
        if (Strings.isNotBlank(video.getType())) {
            Constants.TYPE_LIST.add(video.getType());
        }
        return video;
    }

}

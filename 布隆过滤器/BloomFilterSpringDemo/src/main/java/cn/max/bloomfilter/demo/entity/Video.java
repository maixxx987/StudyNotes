package cn.max.bloomfilter.demo.entity;

import cn.max.bloomfilter.demo.annotation.FilterProperty;
import com.google.common.base.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    /**
     * 视频名称
     */
    @FilterProperty
    private String name;
    /**
     * 视频类型
     */
    @FilterProperty
    private String type;
    /**
     * 创作者
     */
    private String creator;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Video video = (Video) o;
        return Objects.equal(name, video.name) && Objects.equal(type, video.type) && Objects.equal(creator, video.creator);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type, creator);
    }
}

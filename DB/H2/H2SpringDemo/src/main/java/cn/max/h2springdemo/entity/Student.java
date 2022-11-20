package cn.max.h2springdemo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;

/**
 * @author MaxStar
 * @date 2022/11/19
 */
@Data
public class Student implements Serializable {
    @Id
    private Long id;
    private String name;
    private Integer gender;
    private Integer age;
    @JsonFormat(pattern = "yyy-MM-dd")
    private Date birthday;
    /**
     * 创建时间，采用时间戳形式
     */
    private Long createTime;
}

package cn.max.esdemo.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * @author MaxStar
 * @date 2022/10/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user")
public class Employee implements Serializable {
    private String id;
    private String empName;
    private Integer age;
    private Date birthday;
}

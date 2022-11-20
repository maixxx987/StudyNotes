package cn.max.esdemo.controller;

import cn.max.esdemo.model.entity.Employee;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author MaxStar
 * @date 2022/10/29
 */
@RestController
public class EmployeeController {

    @Autowired
    private RestHighLevelClient client;
}

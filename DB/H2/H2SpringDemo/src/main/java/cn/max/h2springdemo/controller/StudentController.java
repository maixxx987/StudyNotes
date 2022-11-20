package cn.max.h2springdemo.controller;

import cn.max.h2springdemo.dao.StudentDao;
import cn.max.h2springdemo.entity.Student;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author MaxStar
 * @date 2022/11/19
 */
@RestController
@AllArgsConstructor
@RequestMapping("/student")
public class StudentController {

    private final StudentDao studentDao;

    @GetMapping("/findById/{id}")
    public Student findById(@PathVariable Long id) {
        return studentDao.findById(id);
    }

    @GetMapping("/findAll")
    public List<Student> findAll() {
        return studentDao.findAll();
    }

    @PostMapping("/save")
    public void save(@RequestBody Student student) {
        studentDao.save(student);
    }

    @DeleteMapping("/deletedById/{id}")
    public void deleteById(@PathVariable Long id) {
        studentDao.deleteById(id);
    }
}

package cn.max.h2springdemo.dao;

import cn.max.h2springdemo.entity.Student;
import lombok.AllArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author MaxStar
 * @date 2022/11/20
 */

@Repository
@AllArgsConstructor
public class StudentDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Student findById(Long id) {
        Student student = null;
        try {
            student = jdbcTemplate.queryForObject("SELECT * FROM student WHERE id = :id ", Map.of("id", id), BeanPropertyRowMapper.newInstance(Student.class));
        } catch (EmptyResultDataAccessException e) {
            System.err.println(String.format("[StudentDao#findById]data not found, id = %d", id));
        }
        return student;
    }

    public List<Student> findAll() {
        return jdbcTemplate.query("SELECT * FROM student", BeanPropertyRowMapper.newInstance(Student.class));
    }

    public void save(Student student) {
        jdbcTemplate.update("INSERT INTO `student`(`name`, `gender`, `age`, `birthday`, `create_time`) " +
                        "VALUES (:name, :gender, :age, :birthday, :createTime)",
                Map.of("name", student.getName(),
                        "gender", student.getGender(),
                        "age", student.getAge(),
                        "birthday", student.getBirthday(),
                        "createTime", System.currentTimeMillis()));
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM student WHERE id = :id", Map.of("id", id));
    }
}

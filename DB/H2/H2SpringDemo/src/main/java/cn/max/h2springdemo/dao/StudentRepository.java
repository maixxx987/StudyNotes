//package cn.max.h2springdemo.dao;
//
//import cn.max.h2springdemo.entity.Student;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.repository.CrudRepository;
//import org.springframework.stereotype.Repository;
//
///**
// * @author MaxStar
// * @date 2022/11/19
// */
//@Repository
//public interface StudentRepository extends CrudRepository<Student, Long> {
//
//    /**
//     * 排序
//     */
//    Iterable<Student> findAll(Sort sort);
//
//    /**
//     * 分页
//     */
//    Page<Student> findAll(Pageable pageable);
//}

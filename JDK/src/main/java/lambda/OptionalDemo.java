package lambda;

import lombok.Data;

import java.util.Objects;
import java.util.Optional;

public class OptionalDemo {

    public static void main(String[] args) {
        Student student = new Student();
        // 没有教师
        old(student);           // 无输出
        jdk8Optional(student);  // 无输出
        jdk9Optional(student);  // teacher is null

        // 有教师
        Teacher teacher = new Teacher();
        student.setTeacher(teacher);
        old(student);           // Teacher(age=null)
        jdk8Optional(student);  // Teacher(age=null)
        jdk9Optional(student);  // Teacher(age=null)

        // 有教师年龄
        teacher.setAge(35);
        old(student);           // Teacher(age=35)
        jdk8Optional(student);  // Teacher(age=35)
        jdk9Optional(student);  // Teacher(age=35)
    }

    /**
     * 传统方法
     *
     * @param student param
     */
    static void old(Student student) {
        if (Objects.nonNull(student)) {
            Teacher teacher = student.getTeacher();
            if (Objects.nonNull(teacher)) {
                System.out.println(teacher);
            }
        }
    }

    /**
     * JDK8的Optional
     *
     * @param student param
     */
    static void jdk8Optional(Student student) {
        Optional.ofNullable(student)
                .map(Student::getTeacher)
                .ifPresent(System.out::println);
    }

    /**
     * JDK9的Optional
     *
     * @param student param
     */
    static void jdk9Optional(Student student) {
        Optional.ofNullable(student)
                .map(Student::getTeacher)
                .ifPresentOrElse(System.out::println, () -> System.out.println("teacher is null"));
    }
}


@Data
class Teacher {
    private Integer age;
}

@Data
class Student {
    private Teacher teacher;
}

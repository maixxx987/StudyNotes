# Java语法
> 平时上班基本是使用JDK7(是的，你没看错，还有Java7......)和JDK8，但现在JDK最新的LTS(Long-term Support 长期支持版本)已经到JDK17了。
> 
> 由于无法在工作中使用到最新的JDK，只能在私下时间体验下新版本的一些变化。因此本篇文章主要是用于学习记录一些JDK8及JDK8以后的的语法。



## switch

> 在JDK12中，增强了switch表达式，有以下几个特点
> 1. 支持lambda
> 2. 可以直接赋值
> 3. 不需要break
>
> 以上内容在JDK14正式加入



### quick start

先来看一个老版本的例子

```java
/**
 * 老版本 switch
 *
 * @param i param
 */
static void oldSwitch(Integer i) {
    switch (i) {
        case 1:
        case 2:
        case 3:
            System.out.println("less than three");
            break;
        case 4:
            System.out.println("four");
            break;
        default:
            System.out.println("bigger than four");
            break;
    }
}
```



相比老版本，新版本的switch有以下特点

- 不需要break，默认带break
- 可以使用lambda表达式

```java
/**
 * 新版本switch
 *
 * @param i param
 */
static void newSwitch(Integer i) {
    switch (i) {
        case 1, 2, 3 -> System.out.println("less than three");
        case 4       -> System.out.println("four");
        default      -> System.out.println("bigger than four");
    }
}
```



### 赋值

新版本的switch也可以直接赋值

```java
/**
 * 使用switch赋值
 * <p>
 * 在复杂函数体中可以使用yield返回
 * 注意，这里需要一个default操作，否则会报编译错误。因为可能存在未遍历的值。
 *
 * @param i param
 * @return result
 */
static String assign(Integer i) {
    return switch (i) {
        case 1, 2, 3 -> "A";
        case 4, 5, 6 -> "B";
        case 50 -> {
            System.out.println(i);
            yield "C";
        }
        default -> throw new IllegalStateException("illegal param");
    };
}
```



### 模式匹配

在JDK17中优化了模式匹配运算符

```java
/**
 * instanceof 优化
 *
 * @param obj param
 */
static void instanceofFun(Object obj) {
    // 老版本
    if (obj instanceof String) {
        String str = (String) obj;
        System.out.println(str);
    }

    // 新版本
    if (obj instanceof String str) {
        System.out.println(str);
    }
}
```



模式匹配搭配switch使用

- 允许匹配null

```java
/**
 * 模式匹配
 *
 * @param o param
 * @return String
 */
enum Color {RED, GREEN, BLUE}

/**
 * 模式匹配，除了基础类外，还可以匹配null，自定义类
 * 父类必须写在子类下面(CharSequence是String的父类，若写在String上面则会报错)
 *
 * @param o param
 * @return String
 */
static String formatterPatternSwitch(Object o) {
    return switch (o) {
        case null            -> "null";
        case Color c         -> String.format("color %s", c.name());
        case Integer i       -> String.format("int %d", i);
        case Long l          -> String.format("long %d", l);
        case Double d        -> String.format("double %f", d);
        case String s        -> String.format("String %s", s);
        // charSequence是string的父类，因此必须写在String下面
        case CharSequence cs -> String.format("CharSequence %s", cs);
        case int[] array     -> Arrays.toString(array);
        default              -> o.toString();
    };
}
```

对某种类型判断的时候可以细化

```java
/**
 * 对类型判断的时候可以细化
 */
static void boolCondition(Object o) {
    switch (o) {
        case null                       -> System.out.println("null");
        case Integer i && (i > 100)     -> System.out.println("big number");
        case Integer i && (i > 50)      -> System.out.println("mid number");
        case String s && (!s.isBlank()) -> System.out.printf("not blank string -> %s%n", s);
        default -> throw new IllegalStateException("Unexpected value: " + o.toString());
    }
}
```


### 混合条件

default也可以和其他条件一起使用

```java
/**
 * default可以和其他条件一起使用
 * 但此时不能有入参(因为default没入参)
 */
static void multiCondition(Object o) {
    switch (o) {
        case null, default -> System.out.println("null");
        case Integer i     -> String.format("int %d", i);
        case Long l        -> String.format("long %d", l);
    }
}
```

### 参考资料

- [JDK 17 switch模式匹配 - 掘金 (juejin.cn)](https://juejin.cn/post/7031160512553435173)



## Lambda & Function



### Optional

> JDK8中加入了Optional，专门用于解决NPE问题。
> 
> JDK9中则对Optional进行了增强，新增了 ifPresentOrElse()、or() 和 stream() 等方法。



#### 解决NPE

假设有一个Student类，里面有属性Teacher，我们需要获取Teacher内的age

```java
// 这里采用了lombok
@Data
class Teacher {
    private Integer age;
}

@Data
class Student {
    private Teacher teacher;
}
```

传统解决NPE方法如下
```java
/**
 * 传统方法，解决NPE
 *
 * @param student param
 */
static void old(Student student) {
    if (Objects.nonNull(student)) {
        Teacher teacher = student.getTeacher();
        if (Objects.nonNull(teacher)) {
            System.out.println("teacher age = " + teacher.getAge());
        }
    }
}
```

采用Optional获取
```java
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
```

可以看到，Optional可以很方便的解决NPE问题。



但是，在JDK8时，无法解决Else问题，比如如果上面如果获取不到，我需要输出null，这时候就需要JDK9的ifPresentOrElse()方法

> ifPresentOrElse() 方法接受两个参数 Consumer 和 Runnable ，如果 Optional 不为空调用 Consumer 参数，为空则调用 Runnable 参数。

```java
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
```



测试结果

```java
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
```


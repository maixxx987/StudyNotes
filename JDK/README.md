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

先来看一个老版本的例子
```java
    /**
 * old version switch
 *
 * @param i param
 */
private static void oldSwitch(Integer i) {
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

用JDK12的语法来写将会是一下这样
```java
/**
 * 新版本switch
 *
 * @param i param
 */
private static void newSwitch(Integer i) {
    switch (i) {
        case 1, 2, 3 -> System.out.println("less than three");
        case 4       -> System.out.println("four");
        default      -> System.out.println("bigger than four");
    }
}
```

两者对比可以看到，节省了break，也使得代码更精简。


同时，新版本的switch也可以直接赋值
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
private static String assign(Integer i) {
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

同时，在JDK17中优化了模式匹配运算符
```java
    /**
 * instanceof 优化
 *
 * @param obj param
 */
private static void instanceofFun(Object obj) {
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
```java
/**
 * 模式匹配
 *
 * @param o param
 * @return String
 */
private static String formatterPatternSwitch(Object o) {
    return switch (o) {
        case Integer i -> String.format("int %d", i);
        case Long l    -> String.format("long %d", l);
        case Double d  -> String.format("double %f", d);
        case String s  -> String.format("String %s", s);
        default        -> o.toString();
    };
}
```

## Lambda & Function

### Optional
> JDK8中加入了Option，专门用于判断非空处理，但只能处理if，不能处理else
> 
> JDK9中加入了ifPresentOrElse，用于处理else

import java.util.Arrays;

/**
 * @author MaxStar
 */
public class SwitchDemo {

    public static void main(String[] args) {
        System.out.println(formatterPatternSwitch(Color.RED));
    }

    /**
     * old version switch
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
                // 代码块中可以用yield返回
                yield "C";
            }
            default -> throw new IllegalStateException("illegal param");
        };
    }

    /**
     * instanceof 优化
     *
     * @param o param
     */
    static void instanceofFun(Object o) {
        // 老版本
        if (o instanceof String) {
            String s = (String) o;
            System.out.println(s);
        }

        // 新版本
        if (o instanceof String s) {
            System.out.println(s);
        }
    }

    /**
     * 枚举
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
}

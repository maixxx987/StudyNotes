/**
 * @author MaxStar
 */
public class SwitchDemo {

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

    /**
     * 新版本switch
     *
     * @param i param
     */
    private static void newSwitch(Integer i) {
        switch (i) {
            case 1, 2, 3 -> System.out.println("less than three");
            case 4 -> System.out.println("four");
            default -> System.out.println("bigger than four");
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
}

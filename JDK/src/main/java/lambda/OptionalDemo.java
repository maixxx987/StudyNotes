package lambda;

import java.util.Optional;

public class OptionalDemo {
    public static void main(String[] args) {

        String str1 = "hello world";
        String str2 = null;

        Optional.of(str1).ifPresent(System.out::println);
        Optional.ofNullable(str2).ifPresentOrElse(System.out::println, () -> {
            System.out.println("null str");
        });

        Optional.ofNullable(str1).map(str -> str.substring(0,5)).ifPresent(System.out::println);
    }
}

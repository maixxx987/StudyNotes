package cn.max.bloomfilter.demo.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author MaxStar
 * @date 2022/6/18
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {
    private String code;
    private String msg;
    private T data;

    /**
     * 成功返回值
     */
    public static <T> Result<T> success() {
        return new Result<>("success", "", null);
    }

    /**
     * 成功返回值
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("success", "", data);
    }

    /**
     * 失败
     */
    public static <T> Result<T> failed(String errorMsg) {
        return new Result<>("error", errorMsg, null);
    }
}

-- 初始化student表
CREATE TABLE IF NOT EXISTS `student`
(
    `id`          BIGINT GENERATED BY DEFAULT AS IDENTITY,
    `name`        varchar(255) NULL COMMENT '名字',
    `gender`      TINYINT      NULL COMMENT '性别',
    `age`         INT          NULL COMMENT '年龄',
    `birthday`    TIMESTAMP    NULL COMMENT '生日',
    `create_time` BIGINT       NULL COMMENT '创建时间戳',
    PRIMARY KEY (`id`)
);
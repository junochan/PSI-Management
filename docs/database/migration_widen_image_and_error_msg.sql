-- 扩展商品图片与操作日志错误信息字段（支持 Base64 数据 URL 与较长异常堆栈摘要）
-- 在已有库上执行一次即可。

ALTER TABLE product
    MODIFY COLUMN image MEDIUMTEXT NULL COMMENT '商品图片URL或Base64数据URL';

ALTER TABLE sys_operation_log
    MODIFY COLUMN error_msg TEXT NULL COMMENT '错误信息';

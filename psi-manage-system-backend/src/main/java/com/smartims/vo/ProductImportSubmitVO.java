package com.smartims.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 提交异步导入后的响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportSubmitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String message;
}

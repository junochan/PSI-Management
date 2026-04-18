package com.smartims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品以图搜图：在筛选条件内对商品主图做多模态向量相似度排序。
 */
@Data
public class ProductImageSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小值为1")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 5000, message = "每页数量最大值为5000")
    private Long size = 10L;

    private String keyword;

    /** 分类名称，与列表筛选一致 */
    private String categoryName;

    /** 在售 / 停售 */
    private String status;

    @NotBlank(message = "请上传查询图片")
    private String imageBase64;

    /** 0~1，不传则用服务端默认阈值 */
    private Double similarityThreshold;
}

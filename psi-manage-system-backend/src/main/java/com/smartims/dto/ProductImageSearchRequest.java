package com.smartims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品以图搜图：在筛选条件内对商品主图做多模态向量相似度排序。
 * 查询图片由接口 {@code multipart} 字段 {@code image} 单独上传，本对象仅承载表单中的筛选与分页参数。
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

    /** 0~1，不传则用服务端默认阈值 */
    private Double similarityThreshold;
}

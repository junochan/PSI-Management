package com.smartims.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存以图搜图：在筛选条件内对商品主图做多模态向量相似度排序。
 */
@Data
public class InventoryImageSearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小值为1")
    private Long page = 1L;

    @Min(value = 1, message = "每页数量最小值为1")
    @Max(value = 100, message = "每页数量最大值为100")
    private Long size = 10L;

    private String keyword;

    private Long productId;

    private Long warehouseId;

    /** stagnant：呆滞；normal：正常 */
    private String stagnantStatus;

    /** yyyy-MM-dd */
    private String lastOutboundStart;
    private String lastOutboundEnd;
    private String lastInboundStart;
    private String lastInboundEnd;

    @NotBlank(message = "请上传查询图片")
    private String imageBase64;

    /** 0~1，不传则用服务端默认阈值 */
    private Double similarityThreshold;
}

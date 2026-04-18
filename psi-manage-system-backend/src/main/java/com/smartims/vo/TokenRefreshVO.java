package com.smartims.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Token 刷新响应
 */
@Data
@AllArgsConstructor
public class TokenRefreshVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 新签发的 JWT
     */
    private String token;
}

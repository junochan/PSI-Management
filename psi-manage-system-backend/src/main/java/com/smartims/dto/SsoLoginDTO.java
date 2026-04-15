package com.smartims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 中转页 SSO 凭证（与配置的共享密钥比对，换取指定用户 JWT）
 */
@Data
public class SsoLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "凭证不能为空")
    private String key;

}

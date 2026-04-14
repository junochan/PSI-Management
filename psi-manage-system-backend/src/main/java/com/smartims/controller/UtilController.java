package com.smartims.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 临时工具控制器（用于生成密码哈希）
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@RestController
@RequestMapping("/v1/util")
@RequiredArgsConstructor
public class UtilController {

    private final PasswordEncoder passwordEncoder;

    /**
     * 生成BCrypt密码哈希（临时用于测试）
     */
    @GetMapping("/encode")
    public String encodePassword(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        return "Password: " + password + "\nBCrypt Hash: " + hash;
    }

}
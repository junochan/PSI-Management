package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.dto.SystemConfigDTO;
import com.smartims.entity.SysConfig;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.SysConfigMapper;
import com.smartims.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String KEY_COMPANY_NAME = "companyName";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_STOCK_WARNING = "stockWarning";
    private static final String KEY_STALE_DAYS = "staleDays";

    private static final String DEFAULT_COMPANY_NAME = "深圳华创科技有限公司";
    private static final String DEFAULT_PHONE = "0755-88888888";
    private static final String DEFAULT_ADDRESS = "深圳市南山区科技园";
    private static final String DEFAULT_STOCK_WARNING = "开启";
    private static final Integer DEFAULT_STALE_DAYS = 90;

    private final SysConfigMapper sysConfigMapper;

    @Override
    public SystemConfigDTO getSystemConfig() {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getDeleted, 0);
        List<SysConfig> configs = sysConfigMapper.selectList(queryWrapper);
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue, (left, right) -> right));

        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setCompanyName(getOrDefault(configMap, KEY_COMPANY_NAME, DEFAULT_COMPANY_NAME));
        dto.setPhone(getOrDefault(configMap, KEY_PHONE, DEFAULT_PHONE));
        dto.setAddress(getOrDefault(configMap, KEY_ADDRESS, DEFAULT_ADDRESS));
        dto.setStockWarning(getOrDefault(configMap, KEY_STOCK_WARNING, DEFAULT_STOCK_WARNING));

        String staleDaysValue = getOrDefault(configMap, KEY_STALE_DAYS, String.valueOf(DEFAULT_STALE_DAYS));
        dto.setStaleDays(parseStaleDays(staleDaysValue));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSystemConfig(SystemConfigDTO dto) {
        if (dto == null) {
            throw new BusinessException("系统配置不能为空");
        }
        validate(dto);

        upsertConfig(KEY_COMPANY_NAME, dto.getCompanyName().trim());
        upsertConfig(KEY_PHONE, dto.getPhone().trim());
        upsertConfig(KEY_ADDRESS, dto.getAddress().trim());
        upsertConfig(KEY_STOCK_WARNING, dto.getStockWarning().trim());
        upsertConfig(KEY_STALE_DAYS, String.valueOf(dto.getStaleDays()));
    }

    private String getOrDefault(Map<String, String> map, String key, String defaultValue) {
        String value = map.get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Integer parseStaleDays(String value) {
        try {
            int days = Integer.parseInt(value);
            return days >= 1 ? days : DEFAULT_STALE_DAYS;
        } catch (NumberFormatException ignored) {
            return DEFAULT_STALE_DAYS;
        }
    }

    private void validate(SystemConfigDTO dto) {
        validateText(dto.getCompanyName(), "公司名称不能为空", v -> dto.setCompanyName(v.trim()));
        validateText(dto.getPhone(), "联系电话不能为空", v -> dto.setPhone(v.trim()));
        validateText(dto.getAddress(), "公司地址不能为空", v -> dto.setAddress(v.trim()));
        validateText(dto.getStockWarning(), "安全库存预警不能为空", v -> dto.setStockWarning(v.trim()));

        if (!"开启".equals(dto.getStockWarning()) && !"关闭".equals(dto.getStockWarning())) {
            throw new BusinessException("安全库存预警仅支持“开启”或“关闭”");
        }
        if (dto.getStaleDays() == null || dto.getStaleDays() < 1) {
            throw new BusinessException("呆滞商品天数必须大于等于1");
        }
    }

    private void validateText(String value, String errorMessage, Consumer<String> setter) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(errorMessage);
        }
        setter.accept(value);
    }

    private void upsertConfig(String key, String value) {
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigKey, key);
        SysConfig existing = sysConfigMapper.selectOne(queryWrapper);

        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDeleted(0);
            sysConfigMapper.insert(config);
            return;
        }
        existing.setConfigValue(value);
        existing.setDeleted(0);
        sysConfigMapper.updateById(existing);
    }
}

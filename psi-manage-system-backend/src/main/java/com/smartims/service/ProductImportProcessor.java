package com.smartims.service;

import com.smartims.dto.ProductDTO;
import com.smartims.exception.BusinessException;
import com.smartims.vo.ProductImportTaskStatus;
import com.smartims.vo.ProductImportTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 异步解析 Excel 并逐条调用 {@link ProductService#addProduct(ProductDTO)}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportProcessor {

    private static final int MAX_ERROR_LINES = 80;

    /** 与前端下载模板表头一致 */
    private static final String[] HEADERS = {
            "商品编码", "商品名称", "分类名称", "品牌", "规格", "成本价", "销售价", "状态",
            "图片URL", "商品描述", "安全库存", "初始库存", "入库仓库ID"
    };

    private final ProductService productService;
    private final ProductImportTaskRegistry taskRegistry;

    @Async("productImportExecutor")
    public void runImport(String jobId, byte[] fileBytes) {
        ProductImportTaskVO task = taskRegistry.get(jobId);
        if (task == null) {
            log.warn("导入任务不存在 jobId={}", jobId);
            return;
        }
        task.setStatus(ProductImportTaskStatus.RUNNING);
        task.setMessage("正在处理");
        taskRegistry.update(jobId, task);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheet("商品导入");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                failTask(jobId, "Excel 中没有可用的工作表");
                return;
            }

            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                failTask(jobId, "Excel 首行表头缺失");
                return;
            }

            Map<String, Integer> colIndex = new HashMap<>();
            for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) {
                    continue;
                }
                String name = formatter.formatCellValue(cell).trim();
                if (StringUtils.hasText(name)) {
                    colIndex.put(name, c);
                }
            }
            for (String h : HEADERS) {
                if (!colIndex.containsKey(h)) {
                    failTask(jobId, "缺少必需的表头列：「" + h + "」，请使用系统提供的导入模板");
                    return;
                }
            }

            int lastRow = sheet.getLastRowNum();
            List<String> errors = new ArrayList<>();
            int ok = 0;
            int fail = 0;

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, colIndex, formatter)) {
                    continue;
                }
                int excelRow = r + 1;
                try {
                    ProductDTO dto = buildDto(row, colIndex, formatter);
                    productService.addProduct(dto);
                    ok++;
                } catch (BusinessException e) {
                    fail++;
                    addError(errors, "第 " + excelRow + " 行：" + e.getMessage());
                } catch (Exception e) {
                    fail++;
                    log.warn("导入第 {} 行失败", excelRow, e);
                    addError(errors, "第 " + excelRow + " 行：" + (e.getMessage() != null ? e.getMessage() : "写入失败"));
                }
            }

            ProductImportTaskVO done = taskRegistry.get(jobId);
            if (done == null) {
                return;
            }
            done.setStatus(ProductImportTaskStatus.COMPLETED);
            done.setTotalRows(ok + fail);
            done.setSuccessCount(ok);
            done.setFailCount(fail);
            done.setErrors(errors);
            done.setFinishedAt(LocalDateTime.now());
            if (ok == 0 && fail == 0) {
                done.setMessage("未解析到有效数据行（请确认从第 2 行起填写，且表头未被改动）");
            } else if (fail == 0) {
                done.setMessage("导入完成，成功 " + ok + " 条");
            } else if (ok == 0) {
                done.setMessage("导入结束，全部失败 " + fail + " 条");
            } else {
                done.setMessage("导入结束，成功 " + ok + " 条，失败 " + fail + " 条");
            }
            taskRegistry.update(jobId, done);
        } catch (Exception e) {
            log.error("导入任务解析失败 jobId={}", jobId, e);
            failTask(jobId, "无法读取 Excel：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    private void failTask(String jobId, String message) {
        ProductImportTaskVO task = taskRegistry.get(jobId);
        if (task == null) {
            return;
        }
        task.setStatus(ProductImportTaskStatus.FAILED);
        task.setMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        taskRegistry.update(jobId, task);
    }

    private void addError(List<String> errors, String line) {
        if (errors.size() < MAX_ERROR_LINES) {
            errors.add(line);
        }
    }

    private boolean isBlankRow(Row row, Map<String, Integer> colIndex, DataFormatter formatter) {
        for (String h : HEADERS) {
            Integer idx = colIndex.get(h);
            if (idx == null) {
                continue;
            }
            Cell cell = row.getCell(idx);
            String v = cell == null ? "" : formatter.formatCellValue(cell).trim();
            if (StringUtils.hasText(v)) {
                return false;
            }
        }
        return true;
    }

    private ProductDTO buildDto(Row row, Map<String, Integer> colIndex, DataFormatter formatter) {
        String name = cellStr(row, colIndex, "商品名称", formatter);
        String catName = cellStr(row, colIndex, "分类名称", formatter);
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("商品名称不能为空");
        }
        if (!StringUtils.hasText(catName)) {
            throw new BusinessException("分类名称不能为空");
        }

        BigDecimal cost = cellDecimal(row, colIndex, "成本价", formatter);
        BigDecimal sale = cellDecimal(row, colIndex, "销售价", formatter);
        if (cost == null) {
            throw new BusinessException("成本价无效或为空");
        }
        if (sale == null) {
            throw new BusinessException("销售价无效或为空");
        }

        String statusRaw = cellStr(row, colIndex, "状态", formatter);
        String status = "停售".equals(statusRaw) ? "停售" : "在售";

        ProductDTO dto = new ProductDTO();
        dto.setName(name.trim());
        dto.setCategoryName(catName.trim());
        dto.setCostPrice(cost);
        dto.setSalePrice(sale);
        dto.setStatus(status);

        String code = cellStr(row, colIndex, "商品编码", formatter);
        if (StringUtils.hasText(code)) {
            dto.setCode(code.trim());
        }
        String brand = cellStr(row, colIndex, "品牌", formatter);
        if (StringUtils.hasText(brand)) {
            dto.setBrand(brand.trim());
        }
        String spec = cellStr(row, colIndex, "规格", formatter);
        if (StringUtils.hasText(spec)) {
            dto.setSpec(spec.trim());
        }
        String desc = cellStr(row, colIndex, "商品描述", formatter);
        if (StringUtils.hasText(desc)) {
            dto.setDescription(desc.trim());
        }
        String img = cellStr(row, colIndex, "图片URL", formatter);
        if (StringUtils.hasText(img)) {
            dto.setImage(img.trim());
        }

        Integer safe = cellInt(row, colIndex, "安全库存", formatter);
        if (safe != null) {
            dto.setSafeStock(safe);
        }
        Integer init = cellInt(row, colIndex, "初始库存", formatter);
        Long wh = cellLong(row, colIndex, "入库仓库ID", formatter);
        if (init != null && init > 0) {
            if (wh == null || wh <= 0) {
                throw new BusinessException("填写了初始库存时必须填写有效的入库仓库ID");
            }
            dto.setInitialStock(init);
            dto.setWarehouseId(wh);
        } else if (init != null) {
            dto.setInitialStock(init);
        }
        return dto;
    }

    private static String cellStr(Row row, Map<String, Integer> colIndex, String header, DataFormatter formatter) {
        Integer idx = colIndex.get(header);
        if (idx == null) {
            return "";
        }
        Cell cell = row.getCell(idx);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static Integer cellInt(Row row, Map<String, Integer> colIndex, String header, DataFormatter formatter) {
        String s = cellStr(row, colIndex, header, formatter);
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s.replaceAll(",", "").trim().split("\\.")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long cellLong(Row row, Map<String, Integer> colIndex, String header, DataFormatter formatter) {
        String s = cellStr(row, colIndex, header, formatter);
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.parseLong(s.replace(",", "").trim().split("\\.")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal cellDecimal(Row row, Map<String, Integer> colIndex, String header, DataFormatter formatter) {
        Integer idx = colIndex.get(header);
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String s = cell.getStringCellValue().trim().replace(",", "");
                    if (!StringUtils.hasText(s)) {
                        return null;
                    }
                    return new BigDecimal(s);
                case FORMULA:
                    String fs = formatter.formatCellValue(cell).trim().replace(",", "");
                    if (!StringUtils.hasText(fs)) {
                        return null;
                    }
                    return new BigDecimal(fs);
                default:
                    String t = formatter.formatCellValue(cell).trim().replace(",", "");
                    if (!StringUtils.hasText(t)) {
                        return null;
                    }
                    return new BigDecimal(t);
            }
        } catch (Exception e) {
            return null;
        }
    }
}

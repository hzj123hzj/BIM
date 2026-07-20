package com.bmi.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * 读取 Excel(.xlsx / .xls) 食物表。
 * 返回每行字符串数组: {名称, 热量, 蛋白质, 碳水, 脂肪}。
 * 自动识别表头（中文/英文关键字），未识别则按固定列顺序解析。
 */
public final class ExcelUtil {
    private ExcelUtil() {}

    /** 字段顺序: 0=名称 1=热量 2=蛋白质 3=碳水 4=脂肪 */
    public static List<String[]> readFoods(File file) throws Exception {
        List<String[]> all = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {
            // 合并工作簿内所有 Sheet（兼容多 Sheet：如按品牌分表的快餐营养数据）
            int n = wb.getNumberOfSheets();
            for (int i = 0; i < n; i++) {
                Sheet sheet = wb.getSheetAt(i);
                if (sheet == null) continue;
                all.addAll(parseSheet(sheet));
            }
        }
        return all;
    }

    private static List<String[]> parseSheet(Sheet sheet) {
        List<String[]> rows = new ArrayList<>();
        Iterator<Row> it = sheet.rowIterator();
        if (!it.hasNext()) return rows;

        Row first = it.next();
        int[] map = detectHeader(first);
        boolean hasHeader = map != null;
        int[] colMap = hasHeader ? map : new int[]{0, 1, 2, 3, 4};

        if (!hasHeader) {
            rows.add(parseRow(first, colMap));
        }
        while (it.hasNext()) {
            Row r = it.next();
            String[] parsed = parseRow(r, colMap);
            if (parsed[0] == null || parsed[0].trim().isEmpty()) continue; // 跳过空行
            // 跳过“分类/分组”行：名称有值但营养数据全为空（如“🥣 主食谷物”）
            if (parsed[1].trim().isEmpty() && parsed[2].trim().isEmpty()
                    && parsed[3].trim().isEmpty() && parsed[4].trim().isEmpty()) continue;
            rows.add(parsed);
        }
        return rows;
    }

    /** 返回 字段->列索引 映射；若不像表头返回 null */
    private static int[] detectHeader(Row row) {
        int[] map = {-1, -1, -1, -1, -1};
        boolean found = false;
        for (Cell c : row) {
            int f = matchField(cellText(c));
            if (f >= 0) {
                map[f] = c.getColumnIndex();
                found = true;
            }
        }
        return found ? map : null;
    }

    private static int matchField(String h) {
        if (h == null) return -1;
        String s = h.trim().toLowerCase();
        if (s.contains("名称") || s.contains("名字") || s.contains("食物") || s.contains("食品")
                || s.equals("name") || s.equals("food")) return 0;
        if (s.contains("热量") || s.contains("卡路里") || s.contains("千卡")
                || s.contains("calorie") || s.equals("cal") || s.equals("kcal")) return 1;
        if (s.contains("蛋白质") || s.contains("蛋白") || s.equals("protein")) return 2;
        if (s.contains("碳水") || s.contains("碳水化合物") || s.equals("carb") || s.equals("carbs")) return 3;
        if (s.contains("脂肪") || s.equals("fat")) return 4;
        return -1;
    }

    private static String[] parseRow(Row row, int[] colMap) {
        String[] out = new String[5];
        for (int f = 0; f < 5; f++) {
            int ci = colMap[f];
            if (ci < 0) {
                out[f] = "";
                continue;
            }
            Cell c = row.getCell(ci, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            out[f] = cellText(c);
        }
        return out;
    }

    private static String cellText(Cell c) {
        if (c == null) return "";
        CellType t = c.getCellType();
        if (t == CellType.FORMULA) {
            try {
                t = c.getCachedFormulaResultType();
            } catch (Exception e) {
                return "";
            }
        }
        switch (t) {
            case STRING:
                return c.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) return "";
                double v = c.getNumericCellValue();
                return (v == Math.rint(v)) ? String.valueOf((long) v) : String.valueOf(v);
            case BOOLEAN:
                return String.valueOf(c.getBooleanCellValue());
            default:
                return "";
        }
    }
}

package com.bmi.util;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * 读取 Excel(.xlsx / .xls) 食物表，支持嵌入图片（按图片锚定行对应到食物）。
 *
 * 兼容两类模板：
 *  - 简单模板：{名称, 每100g热量, 蛋白质, 碳水, 脂肪}（默认份量 100g）。
 *  - 品牌营养模板（如快餐数据）：按品牌分 Sheet，含「产品名称 / 每份量(g) /
 *    每份热量 / 每100g热量 / 蛋白质 / 碳水 / 脂肪 / 图片」等列。
 *    其中 蛋白质/碳水/脂肪 按「每份」给出，导入时按 每份量 折算成「每100g」，
 *    与现有食物库（calories_per_100g + default_grams）模型保持一致。
 *  图片在「图片」列以嵌入图形式存在，按锚定行号贴回对应食物。
 */
public final class ExcelUtil {
    private ExcelUtil() {}

    /** 单条待导入食物（已折算为每100g + 标准份量 + 图片字节） */
    public static final class FoodImportRow {
        public String name;
        public int portionG;   // 标准份量(克) -> default_grams
        public int cal100;     // 每100g热量
        public double protein; // 每100g
        public double carbs;
        public double fat;
        public byte[] image;
        public FoodImportRow(String name, int portionG, int cal100, double p, double c, double f) {
            this.name = name; this.portionG = portionG; this.cal100 = cal100;
            this.protein = p; this.carbs = c; this.fat = f;
        }
    }

    // 字段编号
    private static final int F_NAME = 0, F_PORTION = 1, F_CAL_SERV = 2, F_CAL100 = 3,
            F_PROT = 4, F_CARB = 5, F_FAT = 6;

    public static List<FoodImportRow> readFoods(File file) throws Exception {
        List<FoodImportRow> all = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {
            int n = wb.getNumberOfSheets();
            boolean multi = n > 1;
            for (int i = 0; i < n; i++) {
                Sheet sheet = wb.getSheetAt(i);
                if (sheet == null) continue;
                List<FoodImportRow> sheetRows = parseSheet(sheet);
                // 多 Sheet（品牌分表）时，用 Sheet 名作品牌前缀，利于 AI 按品牌区分
                String sheetName = sheet.getSheetName();
                String prefix = (multi && needBrandPrefix(sheetName)) ? sheetName + " " : "";
                for (FoodImportRow r : sheetRows) {
                    r.name = prefix + r.name;
                }
                all.addAll(sheetRows);
            }
        }
        return all;
    }

    private static boolean needBrandPrefix(String s) {
        if (s == null) return false;
        String t = s.trim();
        return !t.isEmpty() && !t.matches("(?i)sheet\\d+") && !t.equals("食物") && !t.equals("食物库");
    }

    private static List<FoodImportRow> parseSheet(Sheet sheet) {
        List<FoodImportRow> rows = new ArrayList<>();
        Iterator<Row> it = sheet.rowIterator();
        if (!it.hasNext()) return rows;

        Row first = it.next();
        int[] map = detectHeader(first);
        boolean hasHeader = map != null;
        int[] colMap = hasHeader ? map : new int[]{0, -1, -1, 1, 2, 3, 4};

        List<Integer> rowNums = new ArrayList<>();
        if (!hasHeader) {
            FoodImportRow r = parseRow(first, colMap);
            if (r != null && !r.name.isEmpty()) { rows.add(r); rowNums.add(first.getRowNum() + 1); }
        }
        while (it.hasNext()) {
            Row r = it.next();
            FoodImportRow fr = parseRow(r, colMap);
            if (fr == null || fr.name.isEmpty()) continue; // 跳过分类行/空行
            rows.add(fr);
            rowNums.add(r.getRowNum() + 1);
        }

        // 图片：按锚定行号贴回对应食物
        Map<Integer, byte[]> imgByRow = extractImages(sheet);
        for (int i = 0; i < rowNums.size(); i++) {
            byte[] img = imgByRow.get(rowNums.get(i));
            if (img != null) rows.get(i).image = img;
        }
        return rows;
    }

    /** 返回 字段->列索引 映射；若不像表头返回 null */
    private static int[] detectHeader(Row row) {
        int[] map = {-1, -1, -1, -1, -1, -1, -1};
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
        if (s.contains("名称") || s.contains("名字") || s.contains("产品") || s.contains("食物")
                || s.equals("name") || s.equals("food")) return F_NAME;
        // 热量需先区分 每100g / 每份
        if (s.contains("每100") || s.contains("100g") || s.contains("100克")) return F_CAL100;
        if (s.contains("热量") || s.contains("卡路里") || s.contains("calorie")
                || s.equals("cal") || s.equals("kcal")) return F_CAL_SERV;
        if (s.contains("份量") || s.contains("重量") || s.contains("克数")) return F_PORTION;
        if (s.contains("蛋白质") || s.contains("蛋白") || s.equals("protein")) return F_PROT;
        if (s.contains("碳水") || s.contains("碳水化合物") || s.equals("carb") || s.equals("carbs")) return F_CARB;
        if (s.contains("脂肪") || s.equals("fat")) return F_FAT;
        return -1;
    }

    private static FoodImportRow parseRow(Row row, int[] colMap) {
        String name = cellText(getCell(row, colMap[F_NAME]));
        String portionS = cellText(getCell(row, colMap[F_PORTION]));
        String calServS = colMap[F_CAL_SERV] >= 0 ? cellText(getCell(row, colMap[F_CAL_SERV])) : "";
        String cal100S = colMap[F_CAL100] >= 0 ? cellText(getCell(row, colMap[F_CAL100])) : "";
        String pS = colMap[F_PROT] >= 0 ? cellText(getCell(row, colMap[F_PROT])) : "";
        String cS = colMap[F_CARB] >= 0 ? cellText(getCell(row, colMap[F_CARB])) : "";
        String fS = colMap[F_FAT] >= 0 ? cellText(getCell(row, colMap[F_FAT])) : "";

        int portion = toInt(portionS);
        int calServ = toInt(calServS);
        int cal100 = toInt(cal100S);
        double p = toDouble(pS), c = toDouble(cS), f = toDouble(fS);

        int defG = portion > 0 ? portion : 100;
        int c100 = cal100 > 0 ? cal100
                : (calServ > 0 && portion > 0 ? (int) Math.round(calServ * 100.0 / portion)
                : (calServ > 0 ? calServ : 0));
        double p100 = p > 0 && portion > 0 ? p * 100.0 / portion : p;
        double c100v = c > 0 && portion > 0 ? c * 100.0 / portion : c;
        double f100v = f > 0 && portion > 0 ? f * 100.0 / portion : f;

        return new FoodImportRow(name.trim(), defG, c100, p100, c100v, f100v);
    }

    /** 提取 Sheet 内所有嵌入图片，按锚定的 Excel 行号(1-based) 映射到字节 */
    private static Map<Integer, byte[]> extractImages(Sheet sheet) {
        Map<Integer, byte[]> map = new LinkedHashMap<>();
        if (sheet instanceof XSSFSheet xs) {
            XSSFDrawing drawing = xs.getDrawingPatriarch();
            if (drawing != null) {
                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture pic) {
                        XSSFClientAnchor a = pic.getClientAnchor();
                        int row = (a != null ? a.getRow1() : -1) + 1;
                        byte[] data = pic.getPictureData().getData();
                        if (row > 0 && data != null && data.length > 0) map.put(row, data);
                    }
                }
            }
        } else if (sheet instanceof HSSFSheet hs) {
            HSSFPatriarch dp = hs.getDrawingPatriarch();
            if (dp != null) {
                for (HSSFShape shape : dp.getChildren()) {
                    if (shape instanceof HSSFPicture pic) {
                        HSSFClientAnchor a = pic.getClientAnchor();
                        int row = (a != null ? a.getRow1() : -1) + 1;
                        byte[] data = pic.getPictureData().getData();
                        if (row > 0 && data != null && data.length > 0) map.put(row, data);
                    }
                }
            }
        }
        return map;
    }

    private static Cell getCell(Row row, int ci) {
        if (ci < 0) return null;
        return row.getCell(ci, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private static int toInt(String s) {
        try { return (int) Math.round(Double.parseDouble(s.trim())); }
        catch (Exception e) { return 0; }
    }

    private static double toDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0; }
    }

    private static String cellText(Cell c) {
        if (c == null) return "";
        CellType t = c.getCellType();
        if (t == CellType.FORMULA) {
            try { t = c.getCachedFormulaResultType(); } catch (Exception e) { return ""; }
        }
        switch (t) {
            case STRING: return c.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) return "";
                double v = c.getNumericCellValue();
                return (v == Math.rint(v)) ? String.valueOf((long) v) : String.valueOf(v);
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            default: return "";
        }
    }
}

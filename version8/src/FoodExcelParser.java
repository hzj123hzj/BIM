import java.util.*;

/**
 * 智能解析食物 Excel 导入表。
 * 支持表头识别、分类列、分组标题行（如"汉堡""小食""甜品"）自动填充。
 * 返回 String[] {category, name, calories, protein, carbs, fat}，category 可能为空。
 */
public class FoodExcelParser {

    public static List<String[]> parse(List<String[]> rawRows) {
        List<String[]> out = new ArrayList<>();
        if (rawRows == null || rawRows.isEmpty()) return out;

        // 1. 找出表头行
        int headerIdx = -1;
        int[] cols = null;
        for (int i = 0; i < rawRows.size(); i++) {
            cols = detectHeader(rawRows.get(i));
            if (cols != null) {
                headerIdx = i;
                break;
            }
        }

        int nameIdx, calIdx, proteinIdx, carbsIdx, fatIdx, categoryIdx;
        if (headerIdx >= 0 && cols != null) {
            categoryIdx = cols[0];
            nameIdx = cols[1];
            calIdx = cols[2];
            proteinIdx = cols[3];
            carbsIdx = cols[4];
            fatIdx = cols[5];
        } else {
            // 无表头：从第一个长度>=5的行推断列数
            // 兼容「首行被跳过」或「首行是分组标题行（长度很短）」的情况
            int inferLen = 5;
            for (String[] r : rawRows) {
                if (r != null && r.length >= 5) {
                    inferLen = r.length;
                    break;
                }
            }
            categoryIdx = inferLen >= 6 ? 0 : -1;
            nameIdx = categoryIdx >= 0 ? 1 : 0;
            calIdx = nameIdx + 1;
            proteinIdx = calIdx + 1;
            carbsIdx = proteinIdx + 1;
            fatIdx = carbsIdx + 1;
            headerIdx = -1; // 从头解析
        }

        int start = headerIdx + 1;
        String currentCategory = "";
        for (int i = start; i < rawRows.size(); i++) {
            String[] row = rawRows.get(i);
            if (row == null || row.length == 0) continue;

            // 补齐到至少 nameIdx+1，避免越界
            String[] cells = new String[Math.max(row.length, fatIdx + 1)];
            System.arraycopy(row, 0, cells, 0, row.length);
            for (int j = row.length; j < cells.length; j++) cells[j] = "";

            // 统计非空值
            int nonEmpty = 0;
            int firstNonEmptyIdx = -1;
            for (int j = 0; j < cells.length; j++) {
                if (notEmpty(cells[j])) {
                    nonEmpty++;
                    if (firstNonEmptyIdx < 0) firstNonEmptyIdx = j;
                }
            }
            if (nonEmpty == 0) continue;

            // 只有 1 个非空值：当作分组标题（分类）
            if (nonEmpty == 1 && firstNonEmptyIdx == categoryIdx) {
                currentCategory = cells[categoryIdx].trim();
                continue;
            }

            // 必须含名称
            String name = nameIdx >= 0 && nameIdx < cells.length ? cells[nameIdx].trim() : "";
            if (name.isEmpty()) continue;

            // 分类：优先用当前列，若为空则继承上一个分组标题
            String category = "";
            if (categoryIdx >= 0 && categoryIdx < cells.length && notEmpty(cells[categoryIdx])) {
                category = cells[categoryIdx].trim();
            } else if (categoryIdx >= 0) {
                category = currentCategory;
            }

            // 热量/蛋白质/碳水/脂肪
            String cal = getNum(cells, calIdx);
            String p = getNum(cells, proteinIdx);
            String c = getNum(cells, carbsIdx);
            String f = getNum(cells, fatIdx);
            if (cal.isEmpty() || p.isEmpty() || c.isEmpty() || f.isEmpty()) continue;

            try {
                Integer.parseInt(cal);
                Double.parseDouble(p);
                Double.parseDouble(c);
                Double.parseDouble(f);
            } catch (NumberFormatException e) {
                continue;
            }

            out.add(new String[]{category, name, cal, p, c, f});
        }
        return out;
    }

    /** 返回 int[]{categoryIdx, nameIdx, calIdx, proteinIdx, carbsIdx, fatIdx}; categoryIdx=-1 表示无分类列 */
    private static int[] detectHeader(String[] row) {
        if (row == null || row.length < 5) return null;
        int nameIdx = -1, calIdx = -1, proteinIdx = -1, carbsIdx = -1, fatIdx = -1, categoryIdx = -1;
        for (int i = 0; i < row.length; i++) {
            String v = row[i] == null ? "" : row[i].trim().toLowerCase();
            if (v.isEmpty()) continue;
            if (v.contains("分类") || v.contains("类别") || v.contains("种类")) categoryIdx = i;
            else if (v.contains("产品名称") || v.contains("食物名称") || v.equals("名称") || v.contains("食品")) nameIdx = i;
            else if (v.contains("热量") || v.contains("卡路里") || v.contains("千卡") || v.contains("kcal")) calIdx = i;
            else if (v.contains("蛋白质") || v.contains("蛋白") || v.equals("protein")) proteinIdx = i;
            else if (v.contains("碳水") || v.contains("碳水化合物") || v.equals("carbs") || v.contains("糖类")) carbsIdx = i;
            else if (v.contains("脂肪") || v.contains("油脂") || v.equals("fat")) fatIdx = i;
        }
        if (nameIdx >= 0 && calIdx >= 0 && proteinIdx >= 0 && carbsIdx >= 0 && fatIdx >= 0) {
            return new int[]{categoryIdx, nameIdx, calIdx, proteinIdx, carbsIdx, fatIdx};
        }
        return null;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String getNum(String[] cells, int idx) {
        if (idx < 0 || idx >= cells.length) return "";
        String v = cells[idx] == null ? "" : cells[idx].trim();
        // 去掉单位如 (kcal) (g) 等
        v = v.replaceAll("\\(.*\\)", "").replaceAll("[kK][cC][aA][lL]", "").replaceAll("[gG]", "").replaceAll("[千大]", "").trim();
        return v;
    }
}

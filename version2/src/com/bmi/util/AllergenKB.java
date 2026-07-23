package com.bmi.util;

import java.util.*;

/**
 * 过敏源 → 食物 知识库（映射表）。
 *
 * 用户在健康档案里填写的过敏源可能是「类别词」（如 海鲜、坚果、奶制品），
 * 而识别出的食物名是具体食材（如 三文鱼、花生、牛奶）。仅靠精确子串匹配会漏判
 * （过敏源填「海鲜」却识别到「清蒸鱼」）。本知识库把类别词展开为一组触发关键词，
 * 使匹配具备语义级覆盖。
 *
 * 说明：作为实训项目，知识库以静态映射维护，便于直接阅读与扩展；
 * 若日后要做成可运营配置，可平移为数据库表 allergen_food_map(allergen, keyword)。
 */
public class AllergenKB {

    /** 过敏源类别 → 触发该过敏的食物关键词（首个元素一般为类别名本身）。 */
    private static final Map<String, String[]> MAP = new LinkedHashMap<>();
    static {
        MAP.put("海鲜", new String[]{"海鲜", "鱼", "虾", "蟹", "贝", "鱿鱼", "墨鱼", "章鱼",
                "海参", "海带", "紫菜", "蚝", "蚬", "贻贝", "龙虾", "蟹柳",
                "三文鱼", "金枪鱼", "鳕鱼", "鲈鱼", "带鱼", "虾仁", "鱼丸"});
        MAP.put("坚果", new String[]{"坚果", "花生", "核桃", "杏仁", "腰果", "榛子",
                "开心果", "瓜子", "芝麻", "碧根果", "夏威夷果", "巴旦木"});
        MAP.put("花生", new String[]{"花生", "花生酱", "花生米", "花生碎"});
        MAP.put("奶制品", new String[]{"奶", "牛奶", "酸奶", "奶酪", "奶油", "黄油",
                "冰淇淋", "炼乳", "奶茶", "芝士", "芝士", "奶粉", "牛乳"});
        MAP.put("蛋", new String[]{"蛋", "鸡蛋", "蛋糕", "蛋黄", "蛋白", "蛋挞", "蛋卷", "咸蛋"});
        MAP.put("大豆", new String[]{"大豆", "豆腐", "豆浆", "黄豆", "毛豆", "酱油",
                "纳豆", "味噌", "豆腐干", "豆皮", "千张", "素鸡"});
        MAP.put("麸质", new String[]{"麸质", "面", "面包", "馒头", "面条", "包子", "饺子",
                "蛋糕", "饼干", "麦", "意面", "油条", "披萨", "凉皮", "馄饨", "烧麦"});
        MAP.put("芒果", new String[]{"芒果", "芒果干", "芒果酱"});
    }

    /**
     * 判断食物名是否命中用户过敏源。匹配规则：
     *  1) 食物名直接包含用户填写的过敏源词（精确子串）；
     *  2) 用户填写的是已知类别词时，食物名包含该类别下任一触发关键词。
     * 命中返回冲突的过敏源词，否则返回 null。
     */
    public static String match(String foodName, String userAllergies) {
        if (foodName == null || foodName.isEmpty()) return null;
        if (userAllergies == null || userAllergies.trim().isEmpty()) return null;
        for (String al : userAllergies.split("[,，/、;；\\s]+")) {
            if (al.isEmpty()) continue;
            if (foodName.contains(al)) return al;
            String[] triggers = MAP.get(al);
            if (triggers != null) {
                for (String t : triggers) {
                    if (!t.equals(al) && foodName.contains(t)) return al;
                }
            }
        }
        return null;
    }

    /** 把用户过敏源展开为所有需规避的关键词（含类别触发词），供方案文本扫描使用。 */
    public static List<String> expandTriggers(String userAllergies) {
        List<String> out = new ArrayList<>();
        if (userAllergies == null) return out;
        for (String al : userAllergies.split("[,，/、;；\\s]+")) {
            if (al.isEmpty()) continue;
            out.add(al);
            String[] triggers = MAP.get(al);
            if (triggers != null) {
                for (String t : triggers) if (!t.equals(al)) out.add(t);
            }
        }
        return out;
    }
}

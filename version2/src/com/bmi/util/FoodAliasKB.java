package com.bmi.util;

import java.util.*;

/**
 * 食物同物异名知识库（别名 → 规范名）。
 *
 * 用途：AI 识图返回的食物名常带口语/地域别名（西红柿 / 马铃薯 / 奇异果），
 * 而本地食物库可能以另一名称登记（番茄 / 土豆 / 猕猴桃）。语义上本是同一物，
 * 但名称不同会导致 matchFood 走"模糊候选"甚至"建草稿"，造成重复食物或漏匹配。
 *
 * 本类把 AI 返回名归一到库内最可能使用的规范名，提升识图命中率与入库一致性。
 *
 * 设计取舍：
 *  - 仅在"整词精确相等"时替换（"西红柿"→"番茄"），绝不做子串替换，
 *    避免把"西红柿炒蛋"误改成"番茄炒蛋"（另一道菜），或把"土豆丝"误判成"土豆"。
 *  - 纯静态知识库，不依赖数据库，与 AllergenKB 同构，零迁移成本。
 *  - 局限：仅覆盖"单食物"同物异名，无法处理"菜名级"别名（如 西红柿炒蛋↔番茄炒蛋），
 *    这类需后续在 foods 表加 aliases 列或引入菜名向量匹配。
 */
public class FoodAliasKB {
    private static final Map<String, String[]> MAP = new LinkedHashMap<>();
    static {
        MAP.put("番茄",   new String[]{"西红柿", "蕃茄", "洋柿子"});
        MAP.put("土豆",   new String[]{"马铃薯", "洋芋", "山药蛋"});
        MAP.put("红薯",   new String[]{"地瓜", "番薯", "山芋"});
        MAP.put("猕猴桃", new String[]{"奇异果"});
        MAP.put("牛油果", new String[]{"鳄梨"});
        MAP.put("西柚",   new String[]{"葡萄柚"});
        MAP.put("樱桃",   new String[]{"车厘子"});
        MAP.put("覆盆子", new String[]{"树莓"});
        MAP.put("西兰花", new String[]{"绿菜花", "青花菜"});
        MAP.put("花菜",   new String[]{"菜花", "花椰菜"});
        MAP.put("青椒",   new String[]{"甜椒", "柿子椒"});
        MAP.put("洋葱",   new String[]{"葱头"});
        MAP.put("黄瓜",   new String[]{"青瓜"});
        MAP.put("香菜",   new String[]{"芫荽"});
        MAP.put("包菜",   new String[]{"圆白菜", "卷心菜", "甘蓝", "高丽菜"});
        MAP.put("玉米",   new String[]{"苞谷", "玉蜀黍", "棒子"});
        MAP.put("花生",   new String[]{"长生果", "落花生"});
        MAP.put("章鱼",   new String[]{"八爪鱼"});
        MAP.put("鱿鱼",   new String[]{"枪乌贼", "枪鱼"});
        MAP.put("金针菇", new String[]{"智力菇"});
        MAP.put("圣女果", new String[]{"小番茄", "樱桃番茄"});
        MAP.put("生菜",   new String[]{"叶用莴苣"});
        MAP.put("油麦菜", new String[]{"莜麦菜"});
        MAP.put("苦瓜",   new String[]{"凉瓜"});
        MAP.put("丝瓜",   new String[]{"胜瓜"});
        MAP.put("茄子",   new String[]{"矮瓜", "落苏"});
    }

    private static final Map<String, String> ALIAS_TO_CANON = new HashMap<>();
    static {
        for (Map.Entry<String, String[]> e : MAP.entrySet()) {
            for (String a : e.getValue()) ALIAS_TO_CANON.put(a, e.getKey());
        }
    }

    /** 把 AI 返回的食物名归一到规范名；无对应别名时原样返回。整词精确匹配，绝不子串替换。 */
    public static String canonical(String name) {
        if (name == null) return null;
        String key = name.trim();
        if (key.isEmpty()) return name;
        String canon = ALIAS_TO_CANON.get(key);
        return canon != null ? canon : name;
    }

    /** 调试/展示用：返回全部"规范名 → 别名"映射。 */
    public static Map<String, String[]> all() { return MAP; }
}

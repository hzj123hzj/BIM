package com.bmi.util;

import java.util.*;
import java.text.*;

public class HealthCalculator {

        // ==================== BMI 计算 ====================

        /** 计算 BMI = 体重 / (身高/100)² */
        public static double calcBMI(double weightKg, double heightCm) {
            double h = heightCm / 100.0;
            return weightKg / (h * h);
        }

        /** BMI 分类 (中国标准) */
        public static String classifyBMI(double bmi) {
            if (bmi < 18.5) return "偏瘦";
            if (bmi < 24.0) return "正常";
            if (bmi < 28.0) return "超重";
            return "肥胖";
        }

        // ==================== 每日饮水量目标 ====================

        /** 按体重估算每日饮水目标(ml): 体重(kg) × 35ml, 取整到 50, 范围 1200~3500; 无体重默认 2000 */
        public static int calcDailyWaterGoal(double weightKg) {
            int base = (weightKg > 0) ? (int) Math.round(weightKg * 35 / 50.0) * 50 : 2000;
            if (base < 1200) base = 1200;
            if (base > 3500) base = 3500;
            return base;
        }

        // ==================== BMR 计算 (三种公式) ====================

        /** Harris-Benedict 公式 (区分性别) */
        public static double calcBMR_Harris(double weight, double height, int age, String gender) {
            if ("男".equals(gender)) {
                return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
            } else {
                return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
            }
        }

        /** Mifflin-St Jeor 公式 (区分性别) */
        public static double calcBMR_Mifflin(double weight, double height, int age, String gender) {
            double base = 10 * weight + 6.25 * height - 5 * age;
            return "男".equals(gender) ? base + 5 : base - 161;
        }

        /** 中国营养学会公式 (区分性别) */
        public static double calcBMR_China(double weight, int age, String gender) {
            double base = weight * 24 - age * 5;
            return "男".equals(gender) ? base + 100 : base - 100;
        }

        /** 三公式平均值 */
        public static double calcAvgBMR(double weight, double height, int age, String gender) {
            double h = calcBMR_Harris(weight, height, age, gender);
            double m = calcBMR_Mifflin(weight, height, age, gender);
            double c = calcBMR_China(weight, age, gender);
            return (h + m + c) / 3.0;
        }

        // ==================== TDEE 计算 ====================

        /** 活动系数映射 (兼容 "轻度活动"/"轻度" 两种写法) */
        public static double getActivityFactor(String level) {
            if (level == null) return 1.2;
            // 去掉"活动"二字, 统一用短写法匹配
            String k = level.replace("活动", "").trim();
            switch (k) {
                case "久坐": return 1.2;
                case "轻度": return 1.375;
                case "中度": return 1.55;
                case "重度": return 1.725;
                case "极重度": return 1.9;
                default: return 1.2;
            }
        }

        /** TDEE = BMR × 活动系数 */
        public static double calcTDEE(double bmr, String activityLevel) {
            return bmr * getActivityFactor(activityLevel);
        }

        // ==================== 内脏脂肪评估 ====================

        public static String assessVisceralFat(int level) {
            if (level <= 4) return "正常";
            if (level <= 8) return "偏高";
            return "过高";
        }

        // ==================== 骨骼肌肉量评级 ====================

        public static String assessMuscle(double boneMuscle, double weight, String gender) {
            double standard = "男".equals(gender) ? weight * 0.42 : weight * 0.36;
            double ratio = boneMuscle / standard;
            if (ratio < 0.9) return "偏低";
            if (ratio > 1.1) return "偏高";
            return "正常";
        }

        // ==================== 身体年龄估算 ====================

        public static int calcBodyAge(int age, double bodyFat, double muscleRate, int visceralFat, String gender) {
            int bodyAge = age;
            // 体脂率调整
            boolean male = "男".equals(gender);
            if ((male && bodyFat < 15) || (!male && bodyFat < 22)) bodyAge -= 5;
            else if((male && bodyFat > 25) || (!male && bodyFat > 32)) bodyAge += 5;
            // 肌肉率调整
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate > stdMuscle * 1.1) bodyAge -= 3;
            else if(muscleRate < stdMuscle * 0.9) bodyAge += 3;
            // 内脏脂肪调整
            if (visceralFat <= 4) bodyAge -= 2;
            else if(visceralFat <= 8) bodyAge += 2;
            else bodyAge += 5;
            // 限制在 20-60 之间
            return Math.max(20, Math.min(60, bodyAge));
        }

        // ==================== BMI + 体脂率 综合体质分类 ====================

        /**
         * 完整交叉矩阵分类 (6 种类型)
         * 消瘦型 / 标准型 / 肌肉型 / 超重型 / 肥胖型 / 隐性肥胖型
         */
        public static String classifyBodyType(double bmi, double bodyFat, String gender) {
            boolean male = "男".equals(gender);
            // 体脂率分级
            boolean fatLow = male ? bodyFat < 12 : bodyFat < 20;
            boolean fatNormal = male ? (bodyFat >= 12 && bodyFat <= 25) : (bodyFat >= 20 && bodyFat <= 32);
            boolean fatHigh = male ? bodyFat > 25 : bodyFat > 32;

            // BMI 分级
            if (bmi < 18.5) {
                return fatHigh ? "隐性肥胖型" : "消瘦型";
            } else if (bmi < 24.0) {
                return fatHigh ? "隐性肥胖型" : "标准型";
            } else if (bmi < 28.0) {
                if (fatLow) return "肌肉型";
                return fatHigh ? "肥胖型" : "超重型";
            } else {
                return fatLow ? "肌肉型" : "肥胖型";
            }
        }

        // ==================== 理想体重 ====================

        public static double calcIdealWeight(double heightCm) {
            double h = heightCm / 100.0;
            return h * h * 22;
        }

        // ==================== 标准体重区间 ====================

        /**
         * 标准体重区间 (中国标准 BMI 18.5 ~ 23.9)
         * @return {下限kg, 上限kg}
         */
        public static double[] calcStandardWeightRange(double heightCm) {
            double h = heightCm / 100.0;
            return new double[]{ h * h * 18.5, h * h * 23.9 };
        }

        // ==================== 粗略肥胖/体型分级 (由 BMI 推导, 无需仪器) ====================

        /** 粗略肥胖分级 — 复用 BMI 分类 */
        public static String classifyObesityLevelRough(double bmi) {
            return classifyBMI(bmi);
        }

        /** 粗略体型分级 (由 BMI 推导) */
        public static String classifyBodyShapeRough(double bmi) {
            if (bmi < 18.5) return "消瘦";
            if (bmi < 24.0) return "标准";
            if (bmi < 28.0) return "超重";
            return "肥胖";
        }

        // ==================== 由 BMI 估算体脂率 ====================

        /**
         * Deurenberg 公式: 体脂率% = 1.20×BMI + 0.23×年龄 − 10.8×性别 − 5.4
         * @param gender "男" / "女"
         * @return 估算体脂率 (%)
         */
        public static double estimateBodyFatFromBMI(double bmi, int age, String gender) {
            double sex = "男".equals(gender) ? 1.0 : 0.0;
            return 1.20 * bmi + 0.23 * age - 10.8 * sex - 5.4;
        }

        // ==================== 去脂体重 / 脂肪体重 ====================

        /** 去脂体重 (瘦体重) = 体重 × (1 − 体脂率/100) */
        public static double calcLeanBodyMass(double weight, double bodyFatPercent) {
            return weight * (1 - bodyFatPercent / 100.0);
        }

        /** 脂肪体重 = 体重 × 体脂率/100 */
        public static double calcFatWeight(double weight, double bodyFatPercent) {
            return weight * bodyFatPercent / 100.0;
        }

        // ==================== 身体形态评估 ====================

        /** 腰围身高比 WHtR 评估 */
        public static String assessWHtR(double waist, double height) {
            double whtr = waist / height;
            if (whtr < 0.40) return "偏瘦";
            if (whtr < 0.50) return "正常";
            if (whtr < 0.55) return "腹型肥胖风险";
            return "腹型肥胖";
        }

        /** 体型分类 */
        public static String classifyBodyShape(double waist, String gender) {
            boolean male = "男".equals(gender);
            if ((male && waist >= 90) || (!male && waist >= 85)) return "苹果型(中心性肥胖)";
            if ((male && waist < 85) || (!male && waist < 80)) return "梨型/标准型";
            return "轻度腹型肥胖";
        }

        // ==================== 健康评分 (0-100, 五维加权) ====================

        public static int calcHealthScore(double bmi, double bodyFat, int visceralFat,
                                    double muscleRate, double waterRate, String gender) {
            boolean male = "男".equals(gender);
            int score = 0;

            // 1. BMI 评分 (满分30)
            if (bmi >= 18.5 && bmi < 24.0) score += 30;
            else if((bmi >= 24.0 && bmi < 28.0) || (bmi >= 17.0 && bmi < 18.5)) score += 20;
            else score += 10;

            // 2. 体脂率评分 (满分25)
            boolean fatNormal = male ? (bodyFat >= 12 && bodyFat <= 25) : (bodyFat >= 20 && bodyFat <= 32);
            boolean fatSevere = male ? bodyFat > 30 : bodyFat > 38;
            if (fatNormal) score += 25;
            else if(fatSevere) score += 5;
            else score += 15;

            // 3. 内脏脂肪评分 (满分20)
            if (visceralFat <= 4) score += 20;
            else if(visceralFat <= 8) score += 12;
            else score += 5;

            // 4. 肌肉量评分 (满分15) — 用肌肉率近似
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate >= stdMuscle * 0.9 && muscleRate <= stdMuscle * 1.1) score += 15;
            else if(muscleRate > stdMuscle * 1.1) score += 15;
            else score += 8;

            // 5. 水分率评分 (满分10)
            boolean waterNormal = male ? (waterRate >= 50 && waterRate <= 65) : (waterRate >= 45 && waterRate <= 60);
            if (waterNormal) score += 10;
            else score += 5;

            return score;
        }

        /** 健康评分等级 */
        public static String scoreLevel(int score) {
            if (score >= 90) return "优秀";
            if (score >= 75) return "良好";
            if (score >= 60) return "及格";
            return "需改善";
        }

        // ==================== 运动热量消耗估算 ====================

        /** MET 值表 */
        public static double getMET(String exerciseType) {
            switch (exerciseType) {
                case "快走": return 3.5;
                case "跑步": return 9.0;
                case "游泳": return 7.0;
                case "力量训练": return 5.0;
                case "骑行": return 7.5;
                case "瑜伽": return 3.0;
                case "跳绳": return 12.0;
                case "球类": return 6.0;
                default: return 5.0;
            }
        }

        /** 计算运动消耗热量 = MET × 体重 × 时长(小时) × 强度系数 */
        public static int calcExerciseCalories(String type, int duration, String intensity, double weight) {
            double met = getMET(type);
            double hours = duration / 60.0;
            double factor = "低".equals(intensity) ? 0.85 : "高".equals(intensity) ? 1.15 : 1.0;
            return (int)(met * weight * hours * factor);
        }

        // ==================== 预测算法 ====================

        /**
         * 线性回归趋势预测
         * @param dates 日期列表 (按时间正序)
         * @param values 对应数值列表
         * @param futureDays 预测未来天数
         * @return 预测值, 若数据不足返回 Double.NaN
         */
        public static double predictTrend(List<Date> dates, List<Double> values, int futureDays) {
            int n = dates.size();
            if (n < 3) return Double.NaN;

            // xi = 距第一天的天数
            long baseTime = dates.get(0).getTime();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                double xi = (dates.get(i).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
                double yi = values.get(i);
                sumX += xi; sumY += yi;
                sumXY += xi * yi; sumX2 += xi * xi;
            }
            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) return values.get(n - 1); // 数据无变化

            double k = (n * sumXY - sumX * sumY) / denominator;
            double b = (sumY - k * sumX) / n;

            double lastX = (dates.get(n - 1).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
            return k * (lastX + futureDays) + b;
        }

        /**
         * 趋势判断
         */
        public static String trendDirection(List<Date> dates, List<Double> values) {
            int n = dates.size();
            if (n < 3) return "数据不足";
            long baseTime = dates.get(0).getTime();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            for (int i = 0; i < n; i++) {
                double xi = (dates.get(i).getTime() - baseTime) / (1000.0 * 60 * 60 * 24);
                double yi = values.get(i);
                sumX += xi; sumY += yi;
                sumXY += xi * yi; sumX2 += xi * xi;
            }
            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) return "趋于稳定";
            double k = (n * sumXY - sumX * sumY) / denominator;
            if (Math.abs(k) < 0.01) return "趋于稳定";
            return k > 0 ? "上升" : "下降";
        }

        /**
         * 目标达成日期预测
         * @return 预测天数, -1 表示无法达成, -2 表示进度过慢
         */
        public static int predictGoalDays(double currentWeight, double targetWeight,
                                    double dailyCalorieDeficit, String goalType) {
            if (Math.abs(dailyCalorieDeficit) < 100) return -2;

            double diff = currentWeight - targetWeight;
            if ("减脂".equals(goalType) || "减重".equals(goalType)) {
                if (diff <= 0) return 0; // 已达标
                if (dailyCalorieDeficit <= 0) return -1;
                return (int)Math.ceil((diff * 7700) / dailyCalorieDeficit);
            } else if ("增肌".equals(goalType)) {
                if (diff >= 0) return 0;
                return (int)Math.ceil((Math.abs(diff) * 5500) / Math.abs(dailyCalorieDeficit));
            }
            return -1;
        }

        /**
         * 健康风险评估
         */
        public static String assessRisk(double predictedBMI30) {
            if (predictedBMI30 >= 28.0)
                return "高风险 — 预测30天后BMI将进入肥胖区间, 建议立即调整饮食和运动计划";
            if (predictedBMI30 >= 24.0 || predictedBMI30 < 18.5)
                return "中风险 — 预测30天后BMI偏离正常范围, 需关注体重变化";
            return "低风险 — 预测30天后BMI维持在正常范围, 继续保持";
        }
    }

    // ================================================================
    //                  第四部分: 登录/注册窗口
    // ================================================================

    /** 登录/注册窗口 */

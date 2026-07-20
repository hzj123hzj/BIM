import java.util.*;
import java.text.*;

public class HealthCalculator {

        // ==================== BMI 计算 ====================

        /** 计算 BMI = 体重 / (身高/100)² */
        static double calcBMI(double weightKg, double heightCm) {
            double h = heightCm / 100.0;
            return weightKg / (h * h);
        }

        /** BMI 分类 (中国标准) */
        static String classifyBMI(double bmi) {
            if (bmi < 18.5) return "偏瘦";
            if (bmi < 24.0) return "正常";
            if (bmi < 28.0) return "超重";
            return "肥胖";
        }

        /** 肥胖等级(粗略)，由 BMI 直接判定 (中国标准) */
        static String classifyObesityLevel(double bmi) {
            if (bmi < 18.5) return "偏瘦";
            if (bmi < 24.0) return "正常";
            if (bmi < 28.0) return "超重";
            return "肥胖";
        }

        // ==================== 标准体重范围 (由身高) ====================

        /** 标准体重范围：按 BMI 18.5~24 对应身高反推，返回 [下限kg, 上限kg] */
        static double[] calcStandardWeightRange(double heightCm) {
            double h = heightCm / 100.0;
            double lower = 18.5 * h * h;
            double upper = 24.0 * h * h;
            return new double[]{lower, upper};
        }

        // ==================== 体脂率估算 (无仪器时由 BMI 推算) ====================

        /**
         * Deurenberg 公式：由 BMI、年龄、性别估算体脂率(%)
         * 男性 gender=1，女性 gender=0
         */
        static double estimateBodyFatFromBMI(double bmi, int age, String gender) {
            boolean male = "男".equals(gender);
            double bf = 1.20 * bmi + 0.23 * age - 10.8 * (male ? 1 : 0) - 5.4;
            return Math.max(3, Math.min(60, bf));
        }

        // ==================== 去脂/脂肪重量 ====================

        /** 去脂体重(kg) = 体重 × (1 - 体脂率/100) */
        static double calcLeanMass(double weight, double bodyFatPct) {
            return weight * (1 - bodyFatPct / 100.0);
        }

        /** 脂肪重量(kg) = 体重 × 体脂率/100 */
        static double calcFatMass(double weight, double bodyFatPct) {
            return weight * (bodyFatPct / 100.0);
        }

        // ==================== BMR 计算 (三种公式) ====================

        /** Harris-Benedict 公式 (区分性别) */
        static double calcBMR_Harris(double weight, double height, int age, String gender) {
            if ("男".equals(gender)) {
                return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
            } else {
                return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
            }
        }

        /** Mifflin-St Jeor 公式 (区分性别) */
        static double calcBMR_Mifflin(double weight, double height, int age, String gender) {
            double base = 10 * weight + 6.25 * height - 5 * age;
            return "男".equals(gender) ? base + 5 : base - 161;
        }

        /** 中国营养学会公式 (区分性别) */
        static double calcBMR_China(double weight, int age, String gender) {
            double base = weight * 24 - age * 5;
            return "男".equals(gender) ? base + 100 : base - 100;
        }

        /** 三公式平均值 */
        static double calcAvgBMR(double weight, double height, int age, String gender) {
            double h = calcBMR_Harris(weight, height, age, gender);
            double m = calcBMR_Mifflin(weight, height, age, gender);
            double c = calcBMR_China(weight, age, gender);
            return (h + m + c) / 3.0;
        }

        // ==================== TDEE 计算 ====================

        /** 活动系数映射 */
        static double getActivityFactor(String level) {
            switch (level) {
                case "久坐": return 1.2;
                case "轻度活动": return 1.375;
                case "中度活动": return 1.55;
                case "重度活动": return 1.725;
                case "极重度活动": return 1.9;
                default: return 1.2;
            }
        }

        /** TDEE = BMR × 活动系数 */
        static double calcTDEE(double bmr, String activityLevel) {
            return bmr * getActivityFactor(activityLevel);
        }

        // ==================== 内脏脂肪评估 ====================

        static String assessVisceralFat(int level) {
            if (level <= 4) return "正常";
            if (level <= 8) return "偏高";
            return "过高";
        }

        // ==================== 骨骼肌肉量评级 ====================

        static String assessMuscle(double boneMuscle, double weight, String gender) {
            double standard = "男".equals(gender) ? weight * 0.42 : weight * 0.36;
            double ratio = boneMuscle / standard;
            if (ratio < 0.9) return "偏低";
            if (ratio > 1.1) return "偏高";
            return "正常";
        }

        // ==================== 身体年龄估算 ====================

        static int calcBodyAge(int age, double bodyFat, double muscleRate, int visceralFat, String gender) {
            int bodyAge = age;
            // 体脂率调整
            boolean male = "男".equals(gender);
            if ((male && bodyFat < 15) || (!male && bodyFat < 22)) bodyAge -= 5;
            else if ((male && bodyFat > 25) || (!male && bodyFat > 32)) bodyAge += 5;
            // 肌肉率调整
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate > stdMuscle * 1.1) bodyAge -= 3;
            else if (muscleRate < stdMuscle * 0.9) bodyAge += 3;
            // 内脏脂肪调整
            if (visceralFat <= 4) bodyAge -= 2;
            else if (visceralFat <= 8) bodyAge += 2;
            else bodyAge += 5;
            // 限制在 20-60 之间
            return Math.max(20, Math.min(60, bodyAge));
        }

        // ==================== BMI + 体脂率 综合体质分类 ====================

        /**
         * 完整交叉矩阵分类 (6 种类型)
         * 消瘦型 / 标准型 / 肌肉型 / 超重型 / 肥胖型 / 隐性肥胖型
         */
        static String classifyBodyType(double bmi, double bodyFat, String gender) {
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

        static double calcIdealWeight(double heightCm) {
            double h = heightCm / 100.0;
            return h * h * 22;
        }

        // ==================== 身体形态评估 ====================

        /** 腰围身高比 WHtR 评估 */
        static String assessWHtR(double waist, double height) {
            double whtr = waist / height;
            if (whtr < 0.40) return "偏瘦";
            if (whtr < 0.50) return "正常";
            if (whtr < 0.55) return "腹型肥胖风险";
            return "腹型肥胖";
        }

        /** 体型分类 */
        static String classifyBodyShape(double waist, String gender) {
            boolean male = "男".equals(gender);
            if ((male && waist >= 90) || (!male && waist >= 85)) return "苹果型(中心性肥胖)";
            if ((male && waist < 85) || (!male && waist < 80)) return "梨型/标准型";
            return "轻度腹型肥胖";
        }

        // ==================== 健康评分 (0-100, 五维加权) ====================

        static int calcHealthScore(double bmi, double bodyFat, int visceralFat,
                                    double muscleRate, double waterRate, String gender) {
            boolean male = "男".equals(gender);
            int score = 0;

            // 1. BMI 评分 (满分30)
            if (bmi >= 18.5 && bmi < 24.0) score += 30;
            else if ((bmi >= 24.0 && bmi < 28.0) || (bmi >= 17.0 && bmi < 18.5)) score += 20;
            else score += 10;

            // 2. 体脂率评分 (满分25)
            boolean fatNormal = male ? (bodyFat >= 12 && bodyFat <= 25) : (bodyFat >= 20 && bodyFat <= 32);
            boolean fatSevere = male ? bodyFat > 30 : bodyFat > 38;
            if (fatNormal) score += 25;
            else if (fatSevere) score += 5;
            else score += 15;

            // 3. 内脏脂肪评分 (满分20)
            if (visceralFat <= 4) score += 20;
            else if (visceralFat <= 8) score += 12;
            else score += 5;

            // 4. 肌肉量评分 (满分15) — 用肌肉率近似
            double stdMuscle = male ? 40.0 : 35.0;
            if (muscleRate >= stdMuscle * 0.9 && muscleRate <= stdMuscle * 1.1) score += 15;
            else if (muscleRate > stdMuscle * 1.1) score += 15;
            else score += 8;

            // 5. 水分率评分 (满分10)
            boolean waterNormal = male ? (waterRate >= 50 && waterRate <= 65) : (waterRate >= 45 && waterRate <= 60);
            if (waterNormal) score += 10;
            else score += 5;

            return score;
        }

        /** 健康评分等级 */
        static String scoreLevel(int score) {
            if (score >= 90) return "优秀";
            if (score >= 75) return "良好";
            if (score >= 60) return "及格";
            return "需改善";
        }

        // ==================== 运动热量消耗估算 ====================

        /** MET 值表 */
        static double getMET(String exerciseType) {
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
        static int calcExerciseCalories(String type, int duration, String intensity, double weight) {
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
        static double predictTrend(List<Date> dates, List<Double> values, int futureDays) {
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
        static String trendDirection(List<Date> dates, List<Double> values) {
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
        static int predictGoalDays(double currentWeight, double targetWeight,
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
        static String assessRisk(double predictedBMI30) {
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

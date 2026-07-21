package com.bmi.db;

import com.bmi.util.HealthCalculator;
import com.bmi.util.ImageUtil;
import com.bmi.util.PasswordUtil;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.*;
import java.net.*;
import java.io.*;

public class DBUtil {
    public static String currentUsername = "";
    public static String currentGender = "";
    public static int currentAge = 0;
    public static double currentHeight = 0;
    public static double currentWeight = 0;
    public static double currentWaist = 0;
    public static String currentActivityLevel = "久坐";
    private static final String DB_URL = "jdbc:postgresql://localhost:5433/health_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "12345678";
    public static final DecimalFormat df1 = new DecimalFormat("#0.0");
    public static final DecimalFormat df2 = new DecimalFormat("#0.00");

        /** 获取数据库连接 */
        public static Connection getConnection() throws SQLException {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL JDBC 驱动未找到, 请确认 postgresql-42.7.3.jar 在 classpath 中");
            }
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        /** 测试数据库连接 */
        public static boolean testConnection() {
            try (Connection conn = getConnection()) {
                return conn != null;
            } catch (SQLException e) {
                return false;
            }
        }

        // ==================== 用户相关操作 ====================

        /** 注册用户（含基础属性：性别/年龄/身高/体重/腰围/活动水平） */
        public static boolean registerUser(String username, String password, String gender,
                                     int age, double height, String activityLevel,
                                     double weight, Double waist) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash(password, salt);
            String sql = "INSERT INTO users (username, password, salt, gender, age, height, weight, waist, activity_level) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, salt);
                ps.setString(4, gender);
                ps.setInt(5, age);
                ps.setDouble(6, height);
                if (weight > 0) ps.setDouble(7, weight); else ps.setNull(7, Types.DOUBLE);
                if (waist != null && waist > 0) ps.setDouble(8, waist); else ps.setNull(8, Types.DOUBLE);
                ps.setString(9, activityLevel);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                if (e.getSQLState().equals("23505")) {
                    e.printStackTrace();
                } else {
                    e.printStackTrace();
                }
                return false;
            }
        }

        /** 验证登录 */
        public static boolean loginUser(String username, String password) {
            String sql = "SELECT password, salt, gender, age, height, weight, waist, activity_level FROM users WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password");
                    String salt = rs.getString("salt");
                    if (PasswordUtil.verify(password, salt, hash)) {
                        currentUsername = username;
                        currentGender = rs.getString("gender");
                        currentAge = rs.getInt("age");
                        currentHeight = rs.getDouble("height");
                        currentWeight = rs.getDouble("weight");
                        currentWaist = rs.getDouble("waist");
                        currentActivityLevel = rs.getString("activity_level");
                        if (currentActivityLevel == null) currentActivityLevel = "久坐";
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * 注册时生成初始健康记录：仅写基础属性(体重/腰围) + 由基础算出的 BMI/BMR/TDEE/粗略体型，
         * 仪器测量列(body_fat/water_rate/...) 留 NULL（Postgres 中 CHECK 对 NULL 放行）。
         * 让数据大屏/分析评估在注册后即可见数，无需先去数据录入逐项填仪器值。
         */
        public static boolean saveBaselineHealthRecord(String username, double weight, double height,
                int age, String gender, String activityLevel, Double waist) {
            if (weight <= 0) return false;
            double bmi = HealthCalculator.calcBMI(weight, height);
            double bmr = HealthCalculator.calcAvgBMR(weight, height, age, gender);
            double tdee = HealthCalculator.calcTDEE(bmr, activityLevel);
            String bodyType = HealthCalculator.classifyBMI(bmi); // 粗略体型/肥胖等级
            String sql = "INSERT INTO health_records (username, weight, waist, bmi, bmr, tdee, body_type) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setDouble(2, weight);
                if (waist != null && waist > 0) ps.setDouble(3, waist); else ps.setNull(3, Types.DOUBLE);
                ps.setDouble(4, bmi);
                ps.setDouble(5, bmr);
                ps.setDouble(6, tdee);
                ps.setString(7, bodyType);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        // ==================== 健康记录操作 ====================

        /** 保存健康记录 */
        public static boolean saveHealthRecord(double weight, double bodyFat, double waterRate,
                double proteinRate, double muscleRate, int visceralFat, double boneMuscle,
                double boneMass, double waist) {
            // 计算派生数据
            double bmi = HealthCalculator.calcBMI(weight, currentHeight);
            double bmr = HealthCalculator.calcAvgBMR(weight, currentHeight, currentAge, currentGender);
            double tdee = HealthCalculator.calcTDEE(bmr, currentActivityLevel);
            int bodyAge = HealthCalculator.calcBodyAge(currentAge, bodyFat, muscleRate, visceralFat, currentGender);
            String bodyType = HealthCalculator.classifyBodyType(bmi, bodyFat, currentGender);

            String sql = "INSERT INTO health_records (username, weight, body_fat, water_rate, protein_rate, " +
                         "muscle_rate, visceral_fat, bone_muscle, bone_mass, bmr, tdee, bmi, waist, body_age, body_type) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String updateCheckinSql = "UPDATE users SET checkin_days = (" +
                    "SELECT COUNT(DISTINCT record_date) FROM health_records WHERE username = ?" +
                    ") WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                ps.setString(1, currentUsername);
                ps.setDouble(2, weight);
                ps.setDouble(3, bodyFat);
                ps.setDouble(4, waterRate);
                ps.setDouble(5, proteinRate);
                ps.setDouble(6, muscleRate);
                ps.setInt(7, visceralFat);
                ps.setDouble(8, boneMuscle);
                ps.setDouble(9, boneMass);
                ps.setDouble(10, bmr);
                ps.setDouble(11, tdee);
                ps.setDouble(12, bmi);
                ps.setDouble(13, waist);
                ps.setInt(14, bodyAge);
                ps.setString(15, bodyType);
                ps.executeUpdate();

                try (PreparedStatement ps2 = conn.prepareStatement(updateCheckinSql)) {
                    ps2.setString(1, currentUsername);
                    ps2.setString(2, currentUsername);
                    ps2.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取最新健康记录 */
        public static Map<String, Object> getLatestHealthRecord() {
            String sql = "SELECT * FROM health_records WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("weight", rs.getDouble("weight"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("water_rate", rs.getDouble("water_rate"));
                    map.put("muscle_rate", rs.getDouble("muscle_rate"));
                    map.put("visceral_fat", rs.getInt("visceral_fat"));
                    map.put("bone_muscle", rs.getDouble("bone_muscle"));
                    map.put("bone_mass", rs.getDouble("bone_mass"));
                    map.put("protein_rate", rs.getDouble("protein_rate"));
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    try {
                        map.put("body_type", rs.getString("body_type"));
                    } catch (SQLException ex) {
                        map.put("body_type", "--");
                    }
                    map.put("record_date", rs.getDate("record_date"));
                    return map;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        /** 获取历史健康记录列表 */
        public static List<Map<String, Object>> getHealthRecords(int limit) {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT * FROM health_records WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("record_date", rs.getDate("record_date"));
                    map.put("weight", rs.getDouble("weight"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("water_rate", rs.getDouble("water_rate"));
                    map.put("muscle_rate", rs.getDouble("muscle_rate"));
                    map.put("visceral_fat", rs.getInt("visceral_fat"));
                    map.put("bone_muscle", rs.getDouble("bone_muscle"));
                    map.put("bone_mass", rs.getDouble("bone_mass"));
                    map.put("protein_rate", rs.getDouble("protein_rate"));
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    try {
                        map.put("body_type", rs.getString("body_type"));
                    } catch (SQLException ex) {
                        map.put("body_type", "--");
                    }
                    list.add(map);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 检查今天是否已打卡（有健康记录） */
        public static boolean isCheckedToday() {
            String sql = "SELECT 1 FROM health_records WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                return ps.executeQuery().next();
            } catch (SQLException e) {
                return false;
            }
        }

        // ==================== 运动记录操作 ====================

        /** 保存运动记录 */
        public static boolean saveExerciseRecord(String type, int duration, String intensity, int calories) {
            String sql = "INSERT INTO exercise_records (username, exercise_type, duration, intensity, calories_burned) " +
                         "VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, type);
                ps.setInt(3, duration);
                ps.setString(4, intensity);
                ps.setInt(5, calories);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取今日运动消耗总热量 */
        public static int getTodayExerciseCalories() {
            String sql = "SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }

        /** 获取今日运动记录列表 */
        public static List<String[]> getTodayExerciseList() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT exercise_type, duration, intensity, calories_burned FROM exercise_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("exercise_type"),
                        rs.getInt("duration") + "分钟",
                        rs.getString("intensity"),
                        rs.getInt("calories_burned") + "kcal"
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取当前用户全部运动记录（含日期） */
        public static List<String[]> getExerciseRecordsByUser(String username, int limit) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT record_date, exercise_type, duration, intensity, calories_burned FROM exercise_records " +
                         "WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                while (rs.next()) {
                    list.add(new String[]{
                        sdf.format(rs.getDate("record_date")),
                        rs.getString("exercise_type"),
                        rs.getInt("duration") + "分钟",
                        rs.getString("intensity"),
                        rs.getInt("calories_burned") + "kcal"
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== 饮食记录操作 ====================

        /** 保存饮食记录 */
        public static boolean saveDietRecord(String mealType, String foodName, int calories,
                                       double protein, double carbs, double fat) {
            String sql = "INSERT INTO diet_records (username, meal_type, food_name, calories, protein, carbs, fat) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, mealType);
                ps.setString(3, foodName);
                ps.setInt(4, calories);
                ps.setDouble(5, protein);
                ps.setDouble(6, carbs);
                ps.setDouble(7, fat);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取今日饮食汇总 */
        public static int[] getTodayDietSummary() {
            // 返回 [总热量, 蛋白质×100, 碳水×100, 脂肪×100] (乘100保留精度)
            String sql = "SELECT COALESCE(SUM(calories),0), COALESCE(SUM(protein),0), " +
                         "COALESCE(SUM(carbs),0), COALESCE(SUM(fat),0) FROM diet_records " +
                         "WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new int[]{ rs.getInt(1), (int)(rs.getDouble(2)*100),
                            (int)(rs.getDouble(3)*100), (int)(rs.getDouble(4)*100) };
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new int[]{0, 0, 0, 0};
        }

        /** 获取今日饮食记录明细 */
        public static List<String[]> getTodayDietRecords() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT meal_type, food_name, calories, protein, carbs, fat " +
                         "FROM diet_records WHERE username = ? AND record_date = CURRENT_DATE " +
                         "ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("meal_type"),
                            rs.getString("food_name"),
                            String.valueOf(rs.getInt("calories")),
                            df2.format(rs.getDouble("protein")),
                            df2.format(rs.getDouble("carbs")),
                            df2.format(rs.getDouble("fat"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== 饮水记录相关操作 ====================

        /** 饮水“太少”判定阈值比例（今日总量 < 目标 × 该比例 即提醒） */
        public static final double WATER_LOW_RATIO = 0.5;

        private static boolean waterTablesReady = false;

        /** 自愈合建表：首次调用时创建 water_records / water_goals（不会破坏已有数据） */
        public static void ensureWaterTables() {
            if (waterTablesReady) return;
            String sql1 = "CREATE TABLE IF NOT EXISTS water_records (" +
                    "  id SERIAL PRIMARY KEY," +
                    "  username VARCHAR(50) REFERENCES users(username)," +
                    "  record_date DATE DEFAULT CURRENT_DATE," +
                    "  amount_ml INT CHECK (amount_ml > 0 AND amount_ml <= 3000)," +
                    "  note VARCHAR(100)," +
                    "  created_at TIMESTAMP DEFAULT NOW()" +
                    ")";
            String sql2 = "CREATE TABLE IF NOT EXISTS water_goals (" +
                    "  username VARCHAR(50) PRIMARY KEY REFERENCES users(username)," +
                    "  goal_ml INT CHECK (goal_ml >= 500 AND goal_ml <= 6000)" +
                    ")";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql1);
                stmt.executeUpdate(sql2);
                try { stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_water_username_date ON water_records(username, record_date)"); } catch (SQLException ignore) {}
                waterTablesReady = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /** 记录一次饮水 */
        public static boolean saveWaterRecord(int amountMl, String note) {
            ensureWaterTables();
            if (amountMl <= 0 || amountMl > 3000) return false;
            String sql = "INSERT INTO water_records (username, amount_ml, note) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setInt(2, amountMl);
                ps.setString(3, (note == null || note.trim().isEmpty()) ? null : note.trim());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 今日饮水总量(ml) */
        public static int getTodayWaterTotal() {
            ensureWaterTables();
            String sql = "SELECT COALESCE(SUM(amount_ml),0) FROM water_records " +
                    "WHERE username = ? AND record_date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }

        /** 今日饮水记录明细: {时间, 水量ml, 备注} 按时间倒序 */
        public static List<String[]> getTodayWaterRecords() {
            ensureWaterTables();
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT TO_CHAR(created_at, 'HH24:MI'), amount_ml, note " +
                    "FROM water_records WHERE username = ? AND record_date = CURRENT_DATE " +
                    "ORDER BY created_at DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString(1),
                            String.valueOf(rs.getInt(2)),
                            rs.getString(3) == null ? "" : rs.getString(3)
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 每日饮水目标(ml): 优先用户自定义设定, 否则按体重估算, 再否则默认 2000 */
        public static int getDailyWaterGoal() {
            ensureWaterTables();
            String sql = "SELECT goal_ml FROM water_goals WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("goal_ml");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return HealthCalculator.calcDailyWaterGoal(currentWeight);
        }

        /** 保存用户自定义每日饮水目标(ml) */
        public static boolean saveWaterGoal(int goalMl) {
            ensureWaterTables();
            if (goalMl < 500 || goalMl > 6000) return false;
            String sql = "INSERT INTO water_goals (username, goal_ml) VALUES (?, ?) " +
                    "ON CONFLICT (username) DO UPDATE SET goal_ml = EXCLUDED.goal_ml";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setInt(2, goalMl);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 今日摄水量是否“太少”（低于目标 × WATER_LOW_RATIO） */
        public static boolean isWaterIntakeLow() {
            int goal = getDailyWaterGoal();
            if (goal <= 0) return false;
            return getTodayWaterTotal() < goal * WATER_LOW_RATIO;
        }

        /** 今日是否已有“喝水提醒”通知（按天去重，避免消息中心重复刷屏） */
        public static boolean hasWaterReminderToday() {
            String sql = "SELECT COUNT(*) FROM notifications WHERE receiver = ? AND title = '喝水提醒' " +
                    "AND created_at::date = CURRENT_DATE";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        /** 获取食物列表 */
        public static List<String[]> getAllFoods() {
            ensureFoodColumns();
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT food_name, calories_per_100g, protein, carbs, fat FROM foods WHERE COALESCE(status,'已发布')<>'待确认' ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("food_name"),
                        rs.getInt("calories_per_100g") + "",
                        df2.format(rs.getDouble("protein")),
                        df2.format(rs.getDouble("carbs")),
                        df2.format(rs.getDouble("fat"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== 目标操作 ====================

        /** 保存或更新目标 */
        public static boolean saveGoal(String goalType, double targetValue) {
            // 先检查是否已有目标
            String checkSql = "SELECT id FROM goals WHERE username = ?";
            String updateSql = "UPDATE goals SET goal_type = ?, target_value = ?, start_date = CURRENT_DATE, " +
                               "end_date = NULL, current_stage = 1 WHERE username = ?";
            String insertSql = "INSERT INTO goals (username, goal_type, target_value, start_date) VALUES (?, ?, ?, CURRENT_DATE)";
            try (Connection conn = getConnection()) {
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setString(1, currentUsername);
                if (checkPs.executeQuery().next()) {
                    PreparedStatement ps = conn.prepareStatement(updateSql);
                    ps.setString(1, goalType);
                    ps.setDouble(2, targetValue);
                    ps.setString(3, currentUsername);
                    return ps.executeUpdate() > 0;
                } else {
                    PreparedStatement ps = conn.prepareStatement(insertSql);
                    ps.setString(1, currentUsername);
                    ps.setString(2, goalType);
                    ps.setDouble(3, targetValue);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 更新目标当前阶段 */
        public static boolean updateGoalStage(int stage) {
            String sql = "UPDATE goals SET current_stage = ? WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, stage);
                ps.setString(2, currentUsername);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取指定日期范围内的运动统计 [次数, 总分钟] */
        public static int[] getExerciseStatsBetween(Date start, Date end) {
            int[] result = {0, 0};
            String sql = "SELECT COUNT(*), COALESCE(SUM(duration), 0) FROM exercise_records " +
                         "WHERE username = ? AND record_date >= ? AND record_date < ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setDate(2, new java.sql.Date(start.getTime()));
                ps.setDate(3, new java.sql.Date(end.getTime()));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    result[0] = rs.getInt(1);
                    result[1] = rs.getInt(2);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return result;
        }

        /** 获取目标 */
        public static Map<String, Object> getGoal() {
            String sql = "SELECT * FROM goals WHERE username = ? ORDER BY id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("goal_type", rs.getString("goal_type"));
                    map.put("target_value", rs.getDouble("target_value"));
                    map.put("start_date", rs.getDate("start_date"));
                    map.put("end_date", rs.getDate("end_date"));
                    map.put("current_stage", rs.getInt("current_stage"));
                    return map;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        // ==================== 成就操作 ====================

        /** 检查并授予成就徽章 */
        public static void checkAndGrantAchievements() {
            try (Connection conn = getConnection()) {
                // 连续打卡天数
                int streak = getCheckInStreak(conn);
                if (streak >= 7) grantBadge(conn, "毅力之星");
                if (streak >= 30) grantBadge(conn, "坚持达人");

                // 饮食记录天数
                int dietDays = getDietDays(conn);
                if (dietDays >= 30) grantBadge(conn, "美食家");

                // 运动记录次数
                int exerciseCount = getExerciseCount(conn);
                if (exerciseCount >= 20) grantBadge(conn, "运动健将");

                // 健康评分
                Map<String, Object> latest = getLatestHealthRecord();
                if (latest != null) {
                    int score = HealthCalculator.calcHealthScore(
                        (double)latest.get("bmi"), (double)latest.get("body_fat"),
                        (int)latest.get("visceral_fat"), (double)latest.get("muscle_rate"),
                        (double)latest.get("water_rate"), currentGender);
                    if (score >= 90) grantBadge(conn, "健康标兵");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static int getCheckInStreak(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(DISTINCT record_date) FROM health_records " +
                         "WHERE username = ? AND record_date >= CURRENT_DATE - INTERVAL '30 days'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        private static int getDietDays(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(DISTINCT record_date) FROM diet_records WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        private static int getExerciseCount(Connection conn) throws SQLException {
            String sql = "SELECT COUNT(*) FROM exercise_records WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, currentUsername);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }

        /** 授予徽章（不存在才插入） */
        private static void grantBadge(Connection conn, String badgeName) throws SQLException {
            String checkSql = "SELECT 1 FROM achievements WHERE username = ? AND badge_name = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, currentUsername);
            checkPs.setString(2, badgeName);
            if (!checkPs.executeQuery().next()) {
                String insertSql = "INSERT INTO achievements (username, badge_name) VALUES (?, ?)";
                PreparedStatement insertPs = conn.prepareStatement(insertSql);
                insertPs.setString(1, currentUsername);
                insertPs.setString(2, badgeName);
                insertPs.executeUpdate();
            }
        }

        /** 获取已获得徽章列表 */
        public static List<String[]> getAchievements() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT badge_name, achieved_date FROM achievements WHERE username = ? ORDER BY achieved_date DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{ rs.getString("badge_name"), rs.getDate("achieved_date").toString() });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        // ==================== AI 报告操作 ====================

        /** 保存 AI 报告 */
        public static boolean saveAIReport(String reportType, String content) {
            String sql = "INSERT INTO ai_reports (username, report_type, report_content) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, reportType);
                ps.setString(3, content);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取历史 AI 报告列表 */
        public static List<String[]> getAIReports() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, report_type, generated_at FROM ai_reports WHERE username = ? ORDER BY generated_at DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("report_type"),
                        sdf.format(rs.getTimestamp("generated_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取 AI 报告内容 */
        public static String getAIReportContent(int reportId) {
            String sql = "SELECT report_content FROM ai_reports WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reportId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("report_content");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "";
        }

        // ==================== 管理员操作 ====================

        /** 管理员登录验证 */
        public static boolean loginAdmin(String username, String password) {
            String sql = "SELECT password, salt, role, status FROM admins WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password");
                    String salt = rs.getString("salt");
                    String status = rs.getString("status");
                    if (!"启用".equals(status)) return false;
                    if (PasswordUtil.verify(password, salt, hash)) {
                        currentUsername = username;
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        /** 初始化默认管理员账号 */
        public static void initDefaultAdmin() {
            String sql = "INSERT INTO admins (username, password, salt, role, status) " +
                         "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM admins)";
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash("admin123", salt);
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "admin");
                ps.setString(2, hash);
                ps.setString(3, salt);
                ps.setString(4, "super_admin");
                ps.setString(5, "启用");
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /** 获取所有用户（管理员） */
        public static List<Map<String, Object>> getAllUsers() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT id, username, gender, age, height, activity_level, account_status, " +
                         "created_at, last_login, deleted, " +
                         "(SELECT COUNT(DISTINCT record_date) FROM health_records WHERE username = u.username) AS checkin_days " +
                         "FROM users u ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("username", rs.getString("username"));
                    map.put("gender", rs.getString("gender"));
                    map.put("age", rs.getInt("age"));
                    map.put("height", rs.getDouble("height"));
                    map.put("activity_level", rs.getString("activity_level"));
                    map.put("account_status", rs.getString("account_status"));
                    map.put("created_at", rs.getTimestamp("created_at"));
                    map.put("last_login", rs.getTimestamp("last_login"));
                    map.put("checkin_days", rs.getInt("checkin_days"));
                    map.put("deleted", rs.getBoolean("deleted"));
                    list.add(map);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 更新用户账号状态 */
        public static boolean updateUserStatus(int userId, String status) {
            String sql = "UPDATE users SET account_status = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 软删除用户 */
        public static boolean softDeleteUser(int userId) {
            String sql = "UPDATE users SET deleted = TRUE, account_status = '冻结' WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 硬删除用户 */
        public static boolean hardDeleteUser(int userId) {
            String sql = "DELETE FROM users WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取用户详细健康档案 */
        public static Map<String, Object> getUserHealthProfile(String username) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("username", username);

            // 最新健康记录
            String sql = "SELECT * FROM health_records WHERE username = ? ORDER BY record_date DESC, id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Map<String, Object> latest = new HashMap<>();
                    latest.put("weight", rs.getDouble("weight"));
                    latest.put("body_fat", rs.getDouble("body_fat"));
                    latest.put("bmi", rs.getDouble("bmi"));
                    latest.put("waist", rs.getDouble("waist"));
                    latest.put("record_date", rs.getDate("record_date"));
                    profile.put("latest_record", latest);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // 记录总数
            profile.put("record_count", countByUser("health_records", username));
            profile.put("diet_count", countByUser("diet_records", username));
            profile.put("exercise_count", countByUser("exercise_records", username));
            profile.put("achievement_count", countByUser("achievements", username));

            return profile;
        }

        private static int countByUser(String table, String username) {
            String sql = "SELECT COUNT(*) FROM " + table + " WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }

        /** 获取全局统计数据 */
        public static Map<String, Object> getGlobalStats() {
            Map<String, Object> stats = new HashMap<>();
            try (Connection conn = getConnection()) {
                stats.put("total_users", countQuery(conn, "SELECT COUNT(*) FROM users WHERE deleted = FALSE"));
                stats.put("active_users_7d", countQuery(conn, "SELECT COUNT(DISTINCT username) FROM health_records WHERE record_date >= CURRENT_DATE - INTERVAL '7 days'"));
                stats.put("today_checkin", countQuery(conn, "SELECT COUNT(DISTINCT username) FROM health_records WHERE record_date = CURRENT_DATE"));
                stats.put("avg_bmi", avgQuery(conn, "SELECT AVG(bmi) FROM health_records"));
                stats.put("avg_body_fat", avgQuery(conn, "SELECT AVG(body_fat) FROM health_records"));
                stats.put("abnormal_users", getAbnormalUsers().size());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return stats;
        }

        /** 所有注册并激活的用户列表 */
        public static List<Map<String, Object>> getTotalUsersList() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT username, gender, age, height, activity_level, created_at FROM users WHERE deleted = FALSE ORDER BY username";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", rs.getString("username"));
                    map.put("gender", rs.getString("gender"));
                    map.put("age", rs.getInt("age"));
                    map.put("height", rs.getDouble("height"));
                    map.put("activity_level", rs.getString("activity_level"));
                    map.put("created_at", rs.getTimestamp("created_at"));
                    list.add(map);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }

        /** 最近 7 天活跃（登录或打卡）用户列表 */
        public static List<Map<String, Object>> getActiveUsers7dList() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "WITH latest AS (" +
                    "SELECT username, MAX(record_date) AS last_date, COUNT(*) AS record_count " +
                    "FROM health_records WHERE record_date >= CURRENT_DATE - INTERVAL '7 days' " +
                    "GROUP BY username) " +
                    "SELECT u.username, u.age, u.gender, l.last_date, l.record_count " +
                    "FROM users u JOIN latest l ON u.username = l.username " +
                    "ORDER BY l.last_date DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", rs.getString("username"));
                    map.put("age", rs.getInt("age"));
                    map.put("gender", rs.getString("gender"));
                    map.put("last_date", rs.getDate("last_date"));
                    map.put("record_count", rs.getInt("record_count"));
                    list.add(map);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }

        /** 今日打卡用户列表 */
        public static List<Map<String, Object>> getTodayCheckinUsersList() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "WITH today AS (" +
                    "SELECT username, MAX(record_date) AS record_date, AVG(bmi) AS bmi, " +
                    "COUNT(*) AS record_count FROM health_records WHERE record_date = CURRENT_DATE GROUP BY username) " +
                    "SELECT u.username, u.age, u.gender, t.record_date, t.record_count, t.bmi " +
                    "FROM users u JOIN today t ON u.username = t.username " +
                    "ORDER BY t.record_date DESC, u.username";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", rs.getString("username"));
                    map.put("age", rs.getInt("age"));
                    map.put("gender", rs.getString("gender"));
                    map.put("record_date", rs.getDate("record_date"));
                    map.put("record_count", rs.getInt("record_count"));
                    map.put("bmi", rs.getDouble("bmi"));
                    list.add(map);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }

        /** 有 BMI 记录的用户列表（用于平均 BMI 卡片） */
        public static List<Map<String, Object>> getAvgBMIUsersList() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "WITH latest AS (" +
                    "SELECT username, bmi, weight, body_fat, record_date, " +
                    "ROW_NUMBER() OVER (PARTITION BY username ORDER BY record_date DESC) rn " +
                    "FROM health_records) " +
                    "SELECT u.username, u.age, u.gender, l.bmi, l.weight, l.body_fat, l.record_date " +
                    "FROM users u JOIN latest l ON u.username = l.username AND l.rn = 1 " +
                    "ORDER BY l.bmi DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", rs.getString("username"));
                    map.put("age", rs.getInt("age"));
                    map.put("gender", rs.getString("gender"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("weight", rs.getDouble("weight"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("record_date", rs.getDate("record_date"));
                    list.add(map);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return list;
        }

        private static int countQuery(Connection conn, String sql) throws SQLException {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }

        private static double avgQuery(Connection conn, String sql) throws SQLException {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }

        /** 获取异常用户列表 */
        public static List<Map<String, Object>> getAbnormalUsers() {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "WITH latest AS (" +
                    "SELECT username, weight, bmi, body_fat, record_date, " +
                    "ROW_NUMBER() OVER (PARTITION BY username ORDER BY record_date DESC) rn " +
                    "FROM health_records), " +
                    "earliest AS (" +
                    "SELECT username, weight, record_date, " +
                    "ROW_NUMBER() OVER (PARTITION BY username ORDER BY record_date ASC) rn " +
                    "FROM health_records) " +
                    "SELECT u.username, u.age, u.gender, l.weight AS latest_weight, l.bmi, l.body_fat, " +
                    "l.record_date AS latest_date, e.weight AS earliest_weight, e.record_date AS earliest_date " +
                    "FROM users u JOIN latest l ON u.username = l.username AND l.rn = 1 " +
                    "LEFT JOIN earliest e ON u.username = e.username AND e.rn = 1 " +
                    "WHERE (l.weight - e.weight) > 5 " +
                    "OR l.bmi > 28 OR l.bmi < 18.5";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", rs.getString("username"));
                    map.put("age", rs.getInt("age"));
                    map.put("gender", rs.getString("gender"));
                    map.put("latest_weight", rs.getDouble("latest_weight"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("body_fat", rs.getDouble("body_fat"));
                    map.put("weight_diff", rs.getDouble("latest_weight") - rs.getDouble("earliest_weight"));
                    map.put("reason", rs.getDouble("bmi") > 28 ? "BMI超标" : rs.getDouble("bmi") < 18.5 ? "BMI偏低" : "体重骤变");
                    list.add(map);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取系统配置 */
        public static Map<String, String> getSystemConfig() {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT config_key, config_value FROM system_config";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    map.put(rs.getString("config_key"), rs.getString("config_value"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 更新系统配置 */
        public static boolean updateSystemConfig(String key, String value) {
            String sql = "INSERT INTO system_config (config_key, config_value, description) VALUES (?, ?, '') " +
                         "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = NOW()";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, key);
                ps.setString(2, value);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 记录系统日志 */
        public static void logAction(String type, String operator, String action, String detail) {
            String sql = "INSERT INTO system_logs (log_type, operator, action, detail) VALUES (?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setString(2, operator);
                ps.setString(3, action);
                ps.setString(4, detail);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /** 获取系统日志 */
        public static List<String[]> getSystemLogs(String type) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT log_type, operator, action, detail, created_at FROM system_logs " +
                         "WHERE ? = '' OR log_type = ? ORDER BY created_at DESC LIMIT 200";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setString(2, type);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("log_type"),
                            rs.getString("operator"),
                            rs.getString("action"),
                            rs.getString("detail"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 导出所有用户数据为 CSV 内容字符串 */
        public static String exportUsersCSV() {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,用户名,性别,年龄,身高,活动等级,账号状态,注册时间,打卡天数\n");
            List<Map<String, Object>> users = getAllUsers();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Map<String, Object> u : users) {
                sb.append(u.get("id")).append(",");
                sb.append(u.get("username")).append(",");
                sb.append(u.get("gender")).append(",");
                sb.append(u.get("age")).append(",");
                sb.append(u.get("height")).append(",");
                sb.append(u.get("activity_level")).append(",");
                sb.append(u.get("account_status")).append(",");
                sb.append(u.get("created_at") == null ? "" : sdf.format(u.get("created_at"))).append(",");
                sb.append(u.get("checkin_days")).append("\n");
            }
            return sb.toString();
        }

        /** 导出所有健康打卡记录为 CSV 内容字符串 */
        public static String exportHealthRecordsCSV() {
            StringBuilder sb = new StringBuilder();
            sb.append("ID,用户名,记录日期,体重,体脂率,水分率,肌肉率,内脏脂肪,骨量,BMI,腰围,基础代谢,每日消耗,身体年龄,身体类型\n");
            String sql = "SELECT id, username, record_date, weight, body_fat, water_rate, muscle_rate, " +
                         "visceral_fat, bone_muscle, bmi, waist, bmr, tdee, body_age, body_type " +
                         "FROM health_records ORDER BY record_date DESC, id DESC";
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getInt("id")).append(",");
                    sb.append(rs.getString("username")).append(",");
                    sb.append(rs.getTimestamp("record_date") == null ? "" : sdf.format(rs.getTimestamp("record_date"))).append(",");
                    sb.append(rs.getDouble("weight")).append(",");
                    sb.append(rs.getDouble("body_fat")).append(",");
                    sb.append(rs.getDouble("water_rate")).append(",");
                    sb.append(rs.getDouble("muscle_rate")).append(",");
                    sb.append(rs.getInt("visceral_fat")).append(",");
                    sb.append(rs.getDouble("bone_muscle")).append(",");
                    sb.append(rs.getDouble("bmi")).append(",");
                    sb.append(rs.getDouble("waist")).append(",");
                    sb.append(rs.getDouble("bmr")).append(",");
                    sb.append(rs.getDouble("tdee")).append(",");
                    sb.append(rs.getInt("body_age")).append(",");
                    sb.append(rs.getString("body_type")).append("\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        /** 保存通知消息 */
        public static boolean saveNotification(String sender, String receiver, String title, String content, String type) {
            String sql = "INSERT INTO notifications (sender, receiver, title, content, type) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sender);
                ps.setString(2, receiver);
                ps.setString(3, title);
                ps.setString(4, content);
                ps.setString(5, type);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取所有通知 */
        public static List<String[]> getAllNotifications() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, sender, receiver, title, type, status, created_at FROM notifications ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("sender"),
                            rs.getString("receiver"),
                            rs.getString("title"),
                            rs.getString("type"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取当前用户收到的通知 */
        public static List<String[]> getMyNotifications(String username) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, sender, title, type, status, created_at FROM notifications WHERE receiver = ? ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("sender"),
                            rs.getString("title"),
                            rs.getString("type"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at") != null ? sdf.format(rs.getTimestamp("created_at")) : "-"
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 更新通知状态（标记已读） */
        public static boolean updateNotificationStatus(int id, String status) {
            String sql = "UPDATE notifications SET status = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取单条通知内容 */
        public static String getNotificationContent(int id) {
            String sql = "SELECT content FROM notifications WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("content");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return "";
        }

        /** 获取当前用户未读消息数量 */
        public static int getUnreadNotificationCount(String username) {
            int count = 0;
            String sql = "SELECT COUNT(*) FROM notifications WHERE receiver = ? AND status = '未读'";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) count = rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return count;
        }

        /** 获取所有食物 */
        public static List<String[]> getFoods() {
            ensureFoodColumns();
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, food_name, calories_per_100g, protein, carbs, fat FROM foods WHERE COALESCE(status,'已发布')<>'待确认' ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("food_name"),
                            String.valueOf(rs.getInt("calories_per_100g")),
                            df2.format(rs.getDouble("protein")),
                            df2.format(rs.getDouble("carbs")),
                            df2.format(rs.getDouble("fat"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 添加/更新食物 */
        public static boolean saveFood(int id, String name, int calories, double protein, double carbs, double fat) {
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    String sql = "INSERT INTO foods (food_name, calories_per_100g, protein, carbs, fat) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, calories);
                    ps.setDouble(3, protein);
                    ps.setDouble(4, carbs);
                    ps.setDouble(5, fat);
                    return ps.executeUpdate() > 0;
                } else {
                    String sql = "UPDATE foods SET food_name = ?, calories_per_100g = ?, protein = ?, carbs = ?, fat = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, calories);
                    ps.setDouble(3, protein);
                    ps.setDouble(4, carbs);
                    ps.setDouble(5, fat);
                    ps.setInt(6, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 删除食物 */
        public static boolean deleteFood(int id) {
            String sql = "DELETE FROM foods WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * 批量导入食物（按名称 upsert：存在则更新，不存在则新增）。
         * @param rows 每行 {名称, 热量, 蛋白质, 碳水, 脂肪}
         * @return 结果描述字符串
         */
        public static String importFoods(List<String[]> rows) throws SQLException {
            int inserted = 0, updated = 0, skipped = 0;
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                String sel = "SELECT id FROM foods WHERE food_name = ?";
                String ins = "INSERT INTO foods (food_name, calories_per_100g, protein, carbs, fat) VALUES (?, ?, ?, ?, ?)";
                String upd = "UPDATE foods SET calories_per_100g = ?, protein = ?, carbs = ?, fat = ? WHERE food_name = ?";
                try (PreparedStatement psSel = conn.prepareStatement(sel);
                     PreparedStatement psIns = conn.prepareStatement(ins);
                     PreparedStatement psUpd = conn.prepareStatement(upd)) {
                    for (String[] r : rows) {
                        String name = (r[0] == null) ? "" : r[0].trim();
                        if (name.isEmpty()) { skipped++; continue; }
                        int cal = parseIntSafe(r[1]);
                        double p = parseDblSafe(r[2]);
                        double c = parseDblSafe(r[3]);
                        double f = parseDblSafe(r[4]);
                        psSel.setString(1, name);
                        try (ResultSet rs = psSel.executeQuery()) {
                            if (rs.next()) {
                                psUpd.setInt(1, cal);
                                psUpd.setDouble(2, p);
                                psUpd.setDouble(3, c);
                                psUpd.setDouble(4, f);
                                psUpd.setString(5, name);
                                psUpd.executeUpdate();
                                updated++;
                            } else {
                                psIns.setString(1, name);
                                psIns.setInt(2, cal);
                                psIns.setDouble(3, p);
                                psIns.setDouble(4, c);
                                psIns.setDouble(5, f);
                                psIns.executeUpdate();
                                inserted++;
                            }
                        }
                    }
                }
                conn.commit();
            }
            return String.format("新增 %d 条，更新 %d 条，跳过 %d 条", inserted, updated, skipped);
        }

        /** 食物图片相似度哈希阈值：汉明距离 ≤ 该值视为同一/近似食物 */
        public static final int FOOD_PHASH_THRESHOLD = 10;

        private static boolean foodColumnsReady = false;

        /** 自愈合：首次调用时为 foods 表补 图片/phash/status 列，并把含糊的 calories 重命名为 calories_per_100g（兼容老库，不破坏已有数据） */
        public static void ensureFoodColumns() {
            if (foodColumnsReady) return;
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                try { stmt.executeUpdate("ALTER TABLE foods ADD COLUMN IF NOT EXISTS food_image BYTEA"); } catch (SQLException ignore) {}
                try { stmt.executeUpdate("ALTER TABLE foods ADD COLUMN IF NOT EXISTS food_phash BIGINT"); } catch (SQLException ignore) {}
                try { stmt.executeUpdate("ALTER TABLE foods ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT '已发布'"); } catch (SQLException ignore) {}
                try { stmt.executeUpdate("UPDATE foods SET status='已发布' WHERE status IS NULL"); } catch (SQLException ignore) {}
                // 食物热量语义显式化：calories(含糊) -> calories_per_100g（数据本就是每100g）
                try { stmt.executeUpdate("DO $$ BEGIN IF EXISTS ("
                        + "SELECT 1 FROM information_schema.columns "
                        + "WHERE table_name='foods' AND column_name='calories') "
                        + "THEN ALTER TABLE foods RENAME COLUMN calories TO calories_per_100g; END IF; END $$"); } catch (SQLException ignore) {}
                foodColumnsReady = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /** 带图食物行（含感知哈希与状态）。图片为 BYTEA 原始字节。 */
        public record FoodRow(int id, String name, int cal, double protein, double carbs, double fat,
                              byte[] image, Long phash, String status) {}

        private static FoodRow foodRowFromRs(ResultSet rs) throws SQLException {
            return new FoodRow(
                    rs.getInt("id"),
                    rs.getString("food_name"),
                    rs.getInt("calories_per_100g"),
                    rs.getDouble("protein"),
                    rs.getDouble("carbs"),
                    rs.getDouble("fat"),
                    rs.getBytes("food_image"),
                    (Long) rs.getObject("food_phash"),
                    rs.getString("status"));
        }

        private static final String FOOD_SELECT =
                "SELECT id, food_name, calories_per_100g, protein, carbs, fat, food_image, food_phash, COALESCE(status,'已发布') AS status FROM foods ";

        /** 已发布食物（含图/phash），供管理面板与识图匹配（排除草稿）。 */
        public static List<FoodRow> getFoodsWithImage() {
            ensureFoodColumns();
            List<FoodRow> list = new ArrayList<>();
            String sql = FOOD_SELECT + "WHERE COALESCE(status,'已发布')<>'待确认' ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(foodRowFromRs(rs));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 待确认草稿食物（AI 识图新增，待审核）。 */
        public static List<FoodRow> getDraftFoods() {
            ensureFoodColumns();
            List<FoodRow> list = new ArrayList<>();
            String sql = FOOD_SELECT + "WHERE status='待确认' ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(foodRowFromRs(rs));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 模糊候选：food_name 双向 LIKE（解决「香辣鸡腿堡」↔「塔斯汀香辣鸡腿堡」）。 */
        public static List<FoodRow> searchFoodsFuzzy(String name) {
            ensureFoodColumns();
            List<FoodRow> list = new ArrayList<>();
            if (name == null || name.trim().isEmpty()) return list;
            String n = name.trim();
            String sql = FOOD_SELECT
                    + "WHERE COALESCE(status,'已发布')<>'待确认' AND (food_name ILIKE ? OR ? ILIKE '%'||food_name||'%') ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "%" + n + "%");
                ps.setString(2, n);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(foodRowFromRs(rs));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /**
         * 识图匹配：1) 精确名 → 2) 模糊候选 → 3) pHash 在候选间消歧。
         * 命中返回食物行；无匹配返回 null（调用方应建草稿）。
         */
        public static FoodRow matchFood(String name, long uploadHash) {
            if (name == null || name.trim().isEmpty()) return null;
            String n = name.trim();
            // 1. 精确匹配（忽略大小写/空格由模型命名决定，这里直接相等）
            for (FoodRow fr : getFoodsWithImage()) {
                if (fr.name() != null && fr.name().trim().equalsIgnoreCase(n)) return fr;
            }
            // 2. 模糊候选
            List<FoodRow> cands = searchFoodsFuzzy(n);
            if (cands.isEmpty()) return null;
            if (cands.size() == 1) return cands.get(0); // 唯一候选可信
            // 3. 多个候选 → pHash 消歧
            FoodRow best = null;
            int bestDist = Integer.MAX_VALUE;
            for (FoodRow fr : cands) {
                if (fr.phash() == null) continue;
                int d = ImageUtil.hamming(uploadHash, fr.phash());
                if (d < bestDist) { bestDist = d; best = fr; }
            }
            return (best != null && bestDist <= FOOD_PHASH_THRESHOLD) ? best : null;
        }

        /** 保存食物（带图）。有图时计算 pHash 一并写入；无图则写 NULL。 */
        public static boolean saveFood(int id, String name, int calories, double protein, double carbs, double fat, byte[] image) {
            ensureFoodColumns();
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    Long phash = (image != null && image.length > 0)
                            ? ImageUtil.perceptualHash(ImageUtil.bufferedImageFromBytes(image)) : null;
                    String sql = "INSERT INTO foods (food_name, calories_per_100g, protein, carbs, fat, food_image, food_phash, status) "
                            + "VALUES (?,?,?,?,?,?,?,'已发布')";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, calories);
                    ps.setDouble(3, protein);
                    ps.setDouble(4, carbs);
                    ps.setDouble(5, fat);
                    if (image != null && image.length > 0) {
                        ps.setBytes(6, image);
                        ps.setObject(7, phash);
                    } else {
                        ps.setNull(6, Types.BINARY);
                        ps.setNull(7, Types.BIGINT);
                    }
                    return ps.executeUpdate() > 0;
                } else {
                    if (image != null && image.length > 0) {
                        Long phash = ImageUtil.perceptualHash(ImageUtil.bufferedImageFromBytes(image));
                        String sql = "UPDATE foods SET food_name=?, calories_per_100g=?, protein=?, carbs=?, fat=?, food_image=?, food_phash=? WHERE id=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, name);
                        ps.setInt(2, calories);
                        ps.setDouble(3, protein);
                        ps.setDouble(4, carbs);
                        ps.setDouble(5, fat);
                        ps.setBytes(6, image);
                        ps.setObject(7, phash);
                        ps.setInt(8, id);
                        return ps.executeUpdate() > 0;
                    }
                    // 无新图：保留原图与 phash，仅更新文本字段
                    String sql = "UPDATE foods SET food_name=?, calories_per_100g=?, protein=?, carbs=?, fat=? WHERE id=?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setInt(2, calories);
                    ps.setDouble(3, protein);
                    ps.setDouble(4, carbs);
                    ps.setDouble(5, fat);
                    ps.setInt(6, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 命中但库图为空时，用上传图补充（"相似则加上"）。 */
        public static boolean updateFoodImage(int id, byte[] image) {
            ensureFoodColumns();
            if (image == null || image.length == 0) return false;
            try (Connection conn = getConnection()) {
                Long phash = ImageUtil.perceptualHash(ImageUtil.bufferedImageFromBytes(image));
                String sql = "UPDATE foods SET food_image=?, food_phash=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setBytes(1, image);
                ps.setObject(2, phash);
                ps.setInt(3, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** AI 识图无匹配时，建一条「待确认」草稿（带图+phash），返回新 id。 */
        public static int saveDraftFood(String name, int cal, double p, double c, double f, byte[] image) {
            ensureFoodColumns();
            try (Connection conn = getConnection()) {
                Long phash = (image != null && image.length > 0)
                        ? ImageUtil.perceptualHash(ImageUtil.bufferedImageFromBytes(image)) : null;
                String sql = "INSERT INTO foods (food_name, calories_per_100g, protein, carbs, fat, food_image, food_phash, status) "
                            + "VALUES (?,?,?,?,?,?,?,'待确认') RETURNING id";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, name);
                ps.setInt(2, cal);
                ps.setDouble(3, p);
                ps.setDouble(4, c);
                ps.setDouble(5, f);
                if (image != null && image.length > 0) {
                    ps.setBytes(6, image);
                    ps.setObject(7, phash);
                } else {
                    ps.setNull(6, Types.BINARY);
                    ps.setNull(7, Types.BIGINT);
                }
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;
        }

        /** 审核通过草稿 → 转为已发布。 */
        public static boolean approveFood(int id) {
            ensureFoodColumns();
            String sql = "UPDATE foods SET status='已发布' WHERE id=?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 拒绝草稿 → 删除。 */
        public static boolean rejectFood(int id) {
            return deleteFood(id);
        }

        private static int parseIntSafe(String s) {
            return (int) Math.round(parseDblSafe(s));
        }

        private static double parseDblSafe(String s) {
            if (s == null) return 0;
            String t = s.trim().replaceAll("[^0-9.\\-]", "");
            if (t.isEmpty()) return 0;
            try {
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        /** 获取所有运动库项目 */
        public static List<String[]> getExerciseLibrary() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, exercise_name, exercise_type, calories_per_hour, intensity_level, description FROM exercise_library ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("exercise_name"),
                            rs.getString("exercise_type"),
                            String.valueOf(rs.getInt("calories_per_hour")),
                            rs.getString("intensity_level"),
                            rs.getString("description")
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 保存运动库项目 */
        public static boolean saveExerciseLibrary(int id, String name, String type, int calories, String intensity, String desc) {
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    String sql = "INSERT INTO exercise_library (exercise_name, exercise_type, calories_per_hour, intensity_level, description) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, type);
                    ps.setInt(3, calories);
                    ps.setString(4, intensity);
                    ps.setString(5, desc);
                    return ps.executeUpdate() > 0;
                } else {
                    String sql = "UPDATE exercise_library SET exercise_name = ?, exercise_type = ?, calories_per_hour = ?, intensity_level = ?, description = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, type);
                    ps.setInt(3, calories);
                    ps.setString(4, intensity);
                    ps.setString(5, desc);
                    ps.setInt(6, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 删除运动库项目 */
        public static boolean deleteExerciseLibrary(int id) {
            String sql = "DELETE FROM exercise_library WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取所有健康文章 */
        public static List<String[]> getHealthArticles() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, title, category, status, published_at FROM health_articles ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("published_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取所有已发布健康文章（用户端展示，仅返回已发布状态） */
        public static List<String[]> getPublishedHealthArticles() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, title, category, status, published_at FROM health_articles WHERE status = '已发布' ORDER BY published_at DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("published_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 根据 ID 获取文章正文 */
        public static Map<String, String> getHealthArticleById(int id) {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT title, content, category, published_at FROM health_articles WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    map.put("title", rs.getString("title"));
                    map.put("content", rs.getString("content"));
                    map.put("category", rs.getString("category"));
                    map.put("published_at", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(rs.getTimestamp("published_at")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 保存健康文章 */
        public static boolean saveHealthArticle(int id, String title, String content, String category) {
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    String sql = "INSERT INTO health_articles (title, content, category) VALUES (?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, category);
                    return ps.executeUpdate() > 0;
                } else {
                    String sql = "UPDATE health_articles SET title = ?, content = ?, category = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, title);
                    ps.setString(2, content);
                    ps.setString(3, category);
                    ps.setInt(4, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取 AI 模板 */
        public static List<String[]> getAITemplates() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, template_name, template_type, status, updated_at FROM ai_templates ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("template_name"),
                            rs.getString("template_type"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("updated_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 保存 AI 模板 */
        public static boolean saveAITemplate(int id, String name, String type, String content) {
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    String sql = "INSERT INTO ai_templates (template_name, template_type, prompt_text) VALUES (?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, type);
                    ps.setString(3, content);
                    return ps.executeUpdate() > 0;
                } else {
                    String sql = "UPDATE ai_templates SET template_name = ?, template_type = ?, prompt_text = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, type);
                    ps.setString(3, content);
                    ps.setInt(4, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取单个 AI 模板详情 */
        public static Map<String, String> getAITemplateById(int id) {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT id, template_name, template_type, prompt_text, status, updated_at FROM ai_templates WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                if (rs.next()) {
                    map.put("id", String.valueOf(rs.getInt("id")));
                    map.put("template_name", rs.getString("template_name"));
                    map.put("template_type", rs.getString("template_type"));
                    map.put("prompt_text", rs.getString("prompt_text"));
                    map.put("status", rs.getString("status"));
                    map.put("updated_at", rs.getTimestamp("updated_at") != null ? sdf.format(rs.getTimestamp("updated_at")) : "-");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 获取所有 AI 问答记录（管理员，过滤已标记无效的记录） */
        public static List<String[]> getAIChatRecords() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, username, question, status, created_at FROM ai_chat_records WHERE COALESCE(status,'有效') <> '无效' ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("username"),
                            rs.getString("question"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取所有 AI 饮食推荐记录（管理员，过滤已标记无效的记录） */
        public static List<String[]> getAIDietRecords() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, username, query, status, created_at FROM ai_diet_records WHERE COALESCE(status,'有效') <> '无效' ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("username"),
                            rs.getString("query"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取所有 AI 菜谱生成记录（管理员，过滤已标记无效的记录） */
        public static List<String[]> getAICookbookRecords() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, username, ingredients, flavor, meal, people, status, created_at FROM ai_cookbook_records WHERE COALESCE(status,'有效') <> '无效' ORDER BY id DESC";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("username"),
                            rs.getString("ingredients"),
                            rs.getString("flavor"),
                            rs.getString("meal"),
                            String.valueOf(rs.getInt("people")),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 保存 AI 问答记录 */
        public static boolean saveAIChatRecord(String username, String question, String answer) {
            String sql = "INSERT INTO ai_chat_records (username, question, answer) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, question);
                ps.setString(3, answer);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 保存 AI 饮食推荐记录（自动建表） */
        public static boolean saveAIDietRecord(String username, String query, String result) {
            String createSql = "CREATE TABLE IF NOT EXISTS ai_diet_records (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(50) REFERENCES users(username), " +
                    "query TEXT, " +
                    "result TEXT, " +
                    "status VARCHAR(20) DEFAULT '有效', " +
                    "created_at TIMESTAMP DEFAULT NOW())";
            String alterSql = "ALTER TABLE ai_diet_records ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT '有效'";
            String insertSql = "INSERT INTO ai_diet_records (username, query, result) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                stmt.executeUpdate(createSql);
                stmt.executeUpdate(alterSql);
                ps.setString(1, username);
                ps.setString(2, query);
                ps.setString(3, result);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 保存 AI 菜谱生成记录（自动建表） */
        public static boolean saveAICookbookRecord(String username, String ingredients, String flavor, String meal, int people, String result) {
            String createSql = "CREATE TABLE IF NOT EXISTS ai_cookbook_records (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(50) REFERENCES users(username), " +
                    "ingredients TEXT, " +
                    "flavor VARCHAR(50), " +
                    "meal VARCHAR(20), " +
                    "people INT, " +
                    "result TEXT, " +
                    "status VARCHAR(20) DEFAULT '有效', " +
                    "created_at TIMESTAMP DEFAULT NOW())";
            String alterSql = "ALTER TABLE ai_cookbook_records ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT '有效'";
            String insertSql = "INSERT INTO ai_cookbook_records (username, ingredients, flavor, meal, people, result) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                stmt.executeUpdate(createSql);
                stmt.executeUpdate(alterSql);
                ps.setString(1, username);
                ps.setString(2, ingredients);
                ps.setString(3, flavor);
                ps.setString(4, meal);
                ps.setInt(5, people);
                ps.setString(6, result);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取 AI 使用统计（管理员） */
        public static List<String[]> getAIUsageStats() {
            List<String[]> list = new ArrayList<>();
            String createChat = "CREATE TABLE IF NOT EXISTS ai_chat_records (id SERIAL PRIMARY KEY, username VARCHAR(50) REFERENCES users(username), question TEXT, answer TEXT, status VARCHAR(20) DEFAULT '有效', created_at TIMESTAMP DEFAULT NOW())";
            String createDiet = "CREATE TABLE IF NOT EXISTS ai_diet_records (id SERIAL PRIMARY KEY, username VARCHAR(50) REFERENCES users(username), query TEXT, result TEXT, status VARCHAR(20) DEFAULT '有效', created_at TIMESTAMP DEFAULT NOW())";
            String createCookbook = "CREATE TABLE IF NOT EXISTS ai_cookbook_records (id SERIAL PRIMARY KEY, username VARCHAR(50) REFERENCES users(username), ingredients TEXT, flavor VARCHAR(50), meal VARCHAR(20), people INT, result TEXT, status VARCHAR(20) DEFAULT '有效', created_at TIMESTAMP DEFAULT NOW())";
            String sql = "SELECT u.username, " +
                    "COALESCE(c.chat_count, 0) AS chat_count, " +
                    "COALESCE(d.diet_count, 0) AS diet_count, " +
                    "COALESCE(b.cookbook_count, 0) AS cookbook_count, " +
                    "GREATEST(COALESCE(c.last_time, TIMESTAMP '1970-01-01 00:00:00'), " +
                            "COALESCE(d.last_time, TIMESTAMP '1970-01-01 00:00:00'), " +
                            "COALESCE(b.last_time, TIMESTAMP '1970-01-01 00:00:00')) AS last_time " +
                    "FROM users u " +
                    "LEFT JOIN (SELECT username, COUNT(*) AS chat_count, MAX(created_at) AS last_time FROM ai_chat_records WHERE COALESCE(status,'有效')<>'无效' GROUP BY username) c ON u.username = c.username " +
                    "LEFT JOIN (SELECT username, COUNT(*) AS diet_count, MAX(created_at) AS last_time FROM ai_diet_records WHERE COALESCE(status,'有效')<>'无效' GROUP BY username) d ON u.username = d.username " +
                    "LEFT JOIN (SELECT username, COUNT(*) AS cookbook_count, MAX(created_at) AS last_time FROM ai_cookbook_records WHERE COALESCE(status,'有效')<>'无效' GROUP BY username) b ON u.username = b.username " +
                    "ORDER BY u.username";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                stmt.executeUpdate(createChat);
                stmt.executeUpdate(createDiet);
                stmt.executeUpdate(createCookbook);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    Timestamp lastT = rs.getTimestamp("last_time");
                    String lastStr = (lastT != null && lastT.getTime() > 0) ? sdf.format(lastT) : "-";
                    list.add(new String[]{
                            rs.getString("username"),
                            String.valueOf(rs.getInt("chat_count")),
                            String.valueOf(rs.getInt("diet_count")),
                            String.valueOf(rs.getInt("cookbook_count")),
                            lastStr
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取当前用户的 AI 问答记录（隐藏被管理员标记为无效的记录） */
        public static List<String[]> getAIChatRecordsByUser(String username, int limit) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, question, answer, status, created_at FROM ai_chat_records " +
                         "WHERE username = ? AND COALESCE(status,'有效') <> '无效' ORDER BY id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("question"),
                            rs.getString("answer"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取当前用户的 AI 饮食记录（隐藏被标记为无效的记录） */
        public static List<String[]> getAIDietRecordsByUser(String username, int limit) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, query, result, status, created_at FROM ai_diet_records " +
                         "WHERE username = ? AND COALESCE(status,'有效') <> '无效' ORDER BY id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("query"),
                            rs.getString("result"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 获取当前用户的 AI 菜谱记录（隐藏被标记为无效的记录） */
        public static List<String[]> getAICookbookRecordsByUser(String username, int limit) {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, ingredients, flavor, meal, people, result, status, created_at FROM ai_cookbook_records " +
                         "WHERE username = ? AND COALESCE(status,'有效') <> '无效' ORDER BY id DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setInt(2, limit);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("ingredients"),
                            rs.getString("flavor"),
                            rs.getString("meal"),
                            String.valueOf(rs.getInt("people")),
                            rs.getString("result"),
                            rs.getString("status"),
                            sdf.format(rs.getTimestamp("created_at"))
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return list;
        }

        /** 根据 ID 查询 AI 问答记录详情 */
        public static Map<String, String> getAIChatRecordById(int id) {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT id, username, question, answer, status, created_at FROM ai_chat_records WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                if (rs.next()) {
                    map.put("id", String.valueOf(rs.getInt("id")));
                    map.put("username", rs.getString("username"));
                    map.put("question", rs.getString("question"));
                    map.put("answer", rs.getString("answer"));
                    map.put("status", rs.getString("status"));
                    map.put("created_at", sdf.format(rs.getTimestamp("created_at")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 更新 AI 问答状态（带状态一致校验与重复通知防护） */
        public static boolean updateAIChatStatus(int id, String status) {
            String select = "SELECT status, notified FROM ai_chat_records WHERE id = ?";
            String update = "UPDATE ai_chat_records SET status = ?, notified = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement psSel = conn.prepareStatement(select)) {
                psSel.setInt(1, id);
                ResultSet rs = psSel.executeQuery();
                if (!rs.next()) return false;
                String curStatus = rs.getString("status");
                boolean notified = rs.getBoolean("notified");
                if (status.equals(curStatus)) return false; // 已是目标状态，不重复操作
                boolean newNotified = "无效".equals(status) || notified; // 标记无效时锁定已通知
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, status);
                    ps.setBoolean(2, newNotified);
                    ps.setInt(3, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 根据 ID 查询 AI 饮食推荐记录详情 */
        public static Map<String, String> getAIDietRecordById(int id) {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT id, username, query, result, status, created_at FROM ai_diet_records WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                if (rs.next()) {
                    map.put("id", String.valueOf(rs.getInt("id")));
                    map.put("username", rs.getString("username"));
                    map.put("query", rs.getString("query"));
                    map.put("result", rs.getString("result"));
                    map.put("status", rs.getString("status"));
                    map.put("created_at", sdf.format(rs.getTimestamp("created_at")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 根据 ID 查询 AI 菜谱生成记录详情 */
        public static Map<String, String> getAICookbookRecordById(int id) {
            Map<String, String> map = new HashMap<>();
            String sql = "SELECT id, username, ingredients, flavor, meal, people, result, status, created_at FROM ai_cookbook_records WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                if (rs.next()) {
                    map.put("id", String.valueOf(rs.getInt("id")));
                    map.put("username", rs.getString("username"));
                    map.put("ingredients", rs.getString("ingredients"));
                    map.put("flavor", rs.getString("flavor"));
                    map.put("meal", rs.getString("meal"));
                    map.put("people", String.valueOf(rs.getInt("people")));
                    map.put("result", rs.getString("result"));
                    map.put("status", rs.getString("status"));
                    map.put("created_at", sdf.format(rs.getTimestamp("created_at")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return map;
        }

        /** 更新 AI 饮食推荐状态（带状态一致校验与重复通知防护） */
        public static boolean updateAIDietStatus(int id, String status) {
            String select = "SELECT status, notified FROM ai_diet_records WHERE id = ?";
            String update = "UPDATE ai_diet_records SET status = ?, notified = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement psSel = conn.prepareStatement(select)) {
                psSel.setInt(1, id);
                ResultSet rs = psSel.executeQuery();
                if (!rs.next()) return false;
                String curStatus = rs.getString("status");
                boolean notified = rs.getBoolean("notified");
                if (status.equals(curStatus)) return false;
                boolean newNotified = "无效".equals(status) || notified;
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, status);
                    ps.setBoolean(2, newNotified);
                    ps.setInt(3, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 更新 AI 菜谱生成状态（带状态一致校验与重复通知防护） */
        public static boolean updateAICookbookStatus(int id, String status) {
            String select = "SELECT status, notified FROM ai_cookbook_records WHERE id = ?";
            String update = "UPDATE ai_cookbook_records SET status = ?, notified = ? WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement psSel = conn.prepareStatement(select)) {
                psSel.setInt(1, id);
                ResultSet rs = psSel.executeQuery();
                if (!rs.next()) return false;
                String curStatus = rs.getString("status");
                boolean notified = rs.getBoolean("notified");
                if (status.equals(curStatus)) return false;
                boolean newNotified = "无效".equals(status) || notified;
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, status);
                    ps.setBoolean(2, newNotified);
                    ps.setInt(3, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 获取 AI API 配置（默认使用智谱 GLM-4.7-Flash） */
        public static Map<String, String> getAIApiConfig() {
            Map<String, String> cfg = new HashMap<>();
            cfg.put("provider_name", "zhipu");
            cfg.put("api_key", "");
            cfg.put("model_name", "glm-4.7-flash");
            cfg.put("vision_model", "glm-4v-flash");
            cfg.put("endpoint_url", "https://open.bigmodel.cn/api/paas/v4");
            String sql = "SELECT provider AS provider_name, api_key, model_name, vision_model, endpoint AS endpoint_url FROM ai_api_config WHERE provider = ? ORDER BY id DESC LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "zhipu");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cfg.put("provider_name", rs.getString("provider_name"));
                    cfg.put("api_key", rs.getString("api_key") == null ? "" : rs.getString("api_key"));
                    cfg.put("model_name", rs.getString("model_name") == null ? "glm-4.7-flash" : rs.getString("model_name"));
                    cfg.put("vision_model", rs.getString("vision_model") == null ? "glm-4v-flash" : rs.getString("vision_model"));
                    cfg.put("endpoint_url", rs.getString("endpoint_url") == null ? "https://open.bigmodel.cn/api/paas/v4" : rs.getString("endpoint_url"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return cfg;
        }

        /** 通用 OpenAI 兼容对话调用（智谱 / 硅基流动等任意兼容服务均可） */
        public static String callOpenAIChat(String apiKey, String prompt) throws Exception {
            Map<String, String> cfg = getAIApiConfig();
            String endpoint = cfg.getOrDefault("endpoint_url", "https://open.bigmodel.cn/api/paas/v4");
            String model = cfg.getOrDefault("model_name", "glm-4.7-flash");
            String fullUrl = endpoint.endsWith("/chat/completions") ? endpoint : endpoint + "/chat/completions";
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            String escaped = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            String body = "{\"model\":\"" + model.replace("\\", "\\\\").replace("\"", "\\\"") + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes("UTF-8")); }
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            String resp = response.toString();
            if (code >= 400) return "AI 调用失败 (HTTP " + code + "): " + resp;
            String content = extractContent(resp);
            return content != null ? content : ("AI 返回: " + resp);
        }

        /** 多模态视觉调用：传入图片 base64，让模型识别食物并返回结构化文本 */
        public static String callVision(String apiKey, String visionModel, String prompt, String base64Image) throws Exception {
            Map<String, String> cfg = getAIApiConfig();
            String endpoint = cfg.getOrDefault("endpoint_url", "https://open.bigmodel.cn/api/paas/v4");
            String model = (visionModel == null || visionModel.trim().isEmpty()) ? cfg.getOrDefault("vision_model", "glm-4v-flash") : visionModel.trim();
            String fullUrl = endpoint.endsWith("/chat/completions") ? endpoint : endpoint + "/chat/completions";
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            String p = escJson(prompt);
            String img = base64Image.replace("\\", "\\\\").replace("\"", "\\\"");
            String body = "{\"model\":\"" + escJson(model) + "\","
                    + "\"messages\":[{\"role\":\"user\",\"content\":["
                    + "{\"type\":\"text\",\"text\":\"" + p + "\"},"
                    + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/jpeg;base64," + img + "\"}}"
                    + "]}]}";
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes("UTF-8")); }
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            String resp = response.toString();
            if (code >= 400) return "AI 调用失败 (HTTP " + code + "): " + resp;
            String content = extractContent(resp);
            return content != null ? content : ("AI 返回: " + resp);
        }

        private static String escJson(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        /** 从 OpenAI 兼容响应中提取 message.content（支持转义字符） */
        public static String extractContent(String json) {
            int idx = json.indexOf("\"content\":\"");
            if (idx < 0) {
                int m = json.indexOf("\"message\"");
                if (m >= 0) {
                    int c = json.indexOf("\"content\":\"", m);
                    if (c < 0) return null;
                    idx = c;
                } else {
                    return null;
                }
            }
            int start = idx + 11;
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char ch = json.charAt(i);
                if (ch == '\\') {
                    if (i + 1 < json.length()) {
                        char n = json.charAt(i + 1);
                        switch (n) {
                            case 'n': sb.append('\n'); break;
                            case 't': sb.append('\t'); break;
                            case 'r': sb.append('\r'); break;
                            case '"': sb.append('"'); break;
                            case '\\': sb.append('\\'); break;
                            default: sb.append(n);
                        }
                        i += 2;
                        continue;
                    }
                    sb.append(ch);
                } else if (ch == '"') {
                    break;
                } else {
                    sb.append(ch);
                }
                i++;
            }
            return sb.toString();
        }

        /** 保存 AI API 配置（含视觉模型） */
        public static boolean saveAIApiConfig(String apiKey, String modelName, String visionModel, String endpointUrl) {
            Map<String, String> existing = getAIApiConfig();
            String sql;
            boolean hasRecord = existing != null && !existing.getOrDefault("api_key", "").isEmpty();
            try (Connection conn = getConnection()) {
                if (hasRecord) {
                    sql = "UPDATE ai_api_config SET api_key = ?, model_name = ?, vision_model = ?, endpoint = ?, updated_at = CURRENT_TIMESTAMP WHERE provider = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, apiKey);
                        ps.setString(2, modelName);
                        ps.setString(3, visionModel);
                        ps.setString(4, endpointUrl);
                        ps.setString(5, "zhipu");
                        return ps.executeUpdate() > 0;
                    }
                } else {
                    sql = "INSERT INTO ai_api_config (provider, api_key, model_name, vision_model, endpoint) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, "zhipu");
                        ps.setString(2, apiKey);
                        ps.setString(3, modelName);
                        ps.setString(4, visionModel);
                        ps.setString(5, endpointUrl);
                        return ps.executeUpdate() > 0;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 执行数据库备份（导出 SQL 到文件） */
        public static boolean backupDatabase(String outputPath) {
            try (Connection conn = getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                StringBuilder sb = new StringBuilder();
                sb.append("-- Backup generated at ").append(new Date()).append("\n\n");
                // 简单导出用户表、健康记录、饮食、运动、成就数据
                exportTable(conn, sb, "users");
                exportTable(conn, sb, "health_records");
                exportTable(conn, sb, "diet_records");
                exportTable(conn, sb, "exercise_records");
                exportTable(conn, sb, "goals");
                exportTable(conn, sb, "achievements");
                exportTable(conn, sb, "ai_reports");
                exportTable(conn, sb, "system_config");
                exportTable(conn, sb, "foods");
                exportTable(conn, sb, "exercise_library");
                exportTable(conn, sb, "health_articles");
                exportTable(conn, sb, "message_templates");
                exportTable(conn, sb, "notifications");
                exportTable(conn, sb, "ai_templates");
                exportTable(conn, sb, "ai_chat_records");

                try (FileWriter fw = new FileWriter(outputPath)) {
                    fw.write(sb.toString());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private static void exportTable(Connection conn, StringBuilder sb, String table) throws SQLException {
            sb.append("-- Table: ").append(table).append("\n");
            Statement st = conn.createStatement();
            try {
                ResultSet rs = st.executeQuery("SELECT * FROM " + table);
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                while (rs.next()) {
                    sb.append("INSERT INTO ").append(table).append(" VALUES (");
                    for (int i = 1; i <= colCount; i++) {
                        String val = rs.getString(i);
                        sb.append(val == null ? "NULL" : "'" + val.replace("'", "''") + "'");
                        if (i < colCount) sb.append(",");
                    }
                    sb.append(");\n");
                }
            } catch (SQLException e) {
                // 表可能不存在，跳过
                sb.append("-- skipped or empty\n");
            }
            sb.append("\n");
        }
    }

    // ================================================================
    //                  第三部分: 健康计算工具类
    // ================================================================

    /**
     * 健康计算工具类 — 封装所有健康指标计算逻辑
     * 包含: BMI / BMR(三公式) / TDEE / 身体年龄 / 体质分类 / 健康评分 / 预测
     */

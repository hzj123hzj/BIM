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
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/health_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "12345678";
    public static final DecimalFormat df1 = new DecimalFormat("#0.0");
    public static final DecimalFormat df2 = new DecimalFormat("#0.00");

        /** 获取数据库连接 */
        static Connection getConnection() throws SQLException {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL JDBC 驱动未找到, 请确认 postgresql-42.7.3.jar 在 classpath 中");
            }
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        /** 测试数据库连接 */
        static boolean testConnection() {
            try (Connection conn = getConnection()) {
                return conn != null;
            } catch (SQLException e) {
                return false;
            }
        }

        // ==================== 用户相关操作 ====================

        /** 注册用户 */
        static boolean registerUser(String username, String password, String gender,
                                     int age, double height, double weight, double waist, String activityLevel) {
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
                ps.setDouble(7, weight);
                ps.setDouble(8, waist);
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
        static boolean loginUser(String username, String password) {
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

        // ==================== 健康记录操作 ====================

        /** 保存健康记录 */
        static boolean saveHealthRecord(double weight, double bodyFat, double waterRate,
                double muscleRate, double proteinRate, int visceralFat, double boneMuscle, double subcutaneousFat, double waist) {
            // 计算派生数据
            double bmi = HealthCalculator.calcBMI(weight, currentHeight);
            double bmr = HealthCalculator.calcAvgBMR(weight, currentHeight, currentAge, currentGender);
            double tdee = HealthCalculator.calcTDEE(bmr, currentActivityLevel);
            int bodyAge = HealthCalculator.calcBodyAge(currentAge, bodyFat, muscleRate, visceralFat, currentGender);
            String bodyType = HealthCalculator.classifyBodyType(bmi, bodyFat, currentGender);
            int bodyScore = HealthCalculator.calcHealthScore(bmi, bodyFat, visceralFat, muscleRate, waterRate, currentGender);

            String sql = "INSERT INTO health_records (username, weight, body_fat, water_rate, muscle_rate, " +
                         "protein_rate, visceral_fat, bone_muscle, subcutaneous_fat, bmr, tdee, bmi, waist, body_age, body_score, body_type) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                ps.setDouble(5, muscleRate);
                ps.setDouble(6, proteinRate);
                ps.setInt(7, visceralFat);
                ps.setDouble(8, boneMuscle);
                ps.setDouble(9, subcutaneousFat);
                ps.setDouble(10, bmr);
                ps.setDouble(11, tdee);
                ps.setDouble(12, bmi);
                ps.setDouble(13, waist);
                ps.setInt(14, bodyAge);
                ps.setInt(15, bodyScore);
                ps.setString(16, bodyType);
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

        /** 获取当前用户的 6 项必需属性（系统根数据），优先用户表基线，缺失时回退最新记录 */
        static Map<String, Object> getUserProfile() {
            Map<String, Object> map = new HashMap<>();
            String sql = "SELECT gender, age, height, weight, waist, activity_level FROM users WHERE username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    map.put("gender", rs.getString("gender"));
                    map.put("age", rs.getInt("age"));
                    map.put("height", rs.getDouble("height"));
                    map.put("weight", rs.getDouble("weight"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("activity_level", rs.getString("activity_level"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // 回退：用户表无体重/腰围基线时，用最新健康记录的值
            Map<String, Object> latest = getLatestHealthRecord();
            if (latest != null && !latest.isEmpty()) {
                if (toDouble(map.get("weight")) <= 0 && latest.get("weight") != null) {
                    map.put("weight", latest.get("weight"));
                }
                if (toDouble(map.get("waist")) <= 0 && latest.get("waist") != null) {
                    map.put("waist", latest.get("waist"));
                }
            }
            return map;
        }

        private static double toDouble(Object v) {
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof String) { try { return Double.parseDouble((String) v); } catch (Exception e) { return 0; } }
            return 0;
        }

        /** 获取最新健康记录 */
        static Map<String, Object> getLatestHealthRecord() {
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
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    map.put("protein_rate", rs.getDouble("protein_rate"));
                    map.put("subcutaneous_fat", rs.getDouble("subcutaneous_fat"));
                    map.put("body_score", rs.getInt("body_score"));
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
        static List<Map<String, Object>> getHealthRecords(int limit) {
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
                    map.put("bmr", rs.getDouble("bmr"));
                    map.put("tdee", rs.getDouble("tdee"));
                    map.put("bmi", rs.getDouble("bmi"));
                    map.put("waist", rs.getDouble("waist"));
                    map.put("body_age", rs.getInt("body_age"));
                    map.put("protein_rate", rs.getDouble("protein_rate"));
                    map.put("subcutaneous_fat", rs.getDouble("subcutaneous_fat"));
                    map.put("body_score", rs.getInt("body_score"));
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
        static boolean isCheckedToday() {
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
        static boolean saveExerciseRecord(String type, int duration, String intensity, int calories) {
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
        static int getTodayExerciseCalories() {
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
        static List<String[]> getTodayExerciseList() {
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
        static List<String[]> getExerciseRecordsByUser(String username, int limit) {
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
        static boolean saveDietRecord(String mealType, String foodName, int calories,
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
        static int[] getTodayDietSummary() {
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
        static List<String[]> getTodayDietRecords() {
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

        /** 获取食物列表 */
        static List<String[]> getAllFoods() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT food_name, calories, protein, carbs, fat FROM foods ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                        rs.getString("food_name"),
                        rs.getInt("calories") + "",
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
        static boolean saveGoal(String goalType, double targetValue) {
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
        static boolean updateGoalStage(int stage) {
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
        static int[] getExerciseStatsBetween(Date start, Date end) {
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
        static Map<String, Object> getGoal() {
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
        static void checkAndGrantAchievements() {
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
        static List<String[]> getAchievements() {
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
        static boolean saveAIReport(String reportType, String content) {
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
        static List<String[]> getAIReports() {
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
        static String getAIReportContent(int reportId) {
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
        static boolean loginAdmin(String username, String password) {
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
        static void initDefaultAdmin() {
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
        static List<Map<String, Object>> getAllUsers() {
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
        static boolean updateUserStatus(int userId, String status) {
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
        static boolean softDeleteUser(int userId) {
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
        static boolean hardDeleteUser(int userId) {
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
        static Map<String, Object> getUserHealthProfile(String username) {
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
        static Map<String, Object> getGlobalStats() {
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
        static List<Map<String, Object>> getTotalUsersList() {
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
        static List<Map<String, Object>> getActiveUsers7dList() {
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
        static List<Map<String, Object>> getTodayCheckinUsersList() {
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
        static List<Map<String, Object>> getAvgBMIUsersList() {
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
        static List<Map<String, Object>> getAbnormalUsers() {
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
        static Map<String, String> getSystemConfig() {
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
        static boolean updateSystemConfig(String key, String value) {
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
        static void logAction(String type, String operator, String action, String detail) {
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
        static List<String[]> getSystemLogs(String type) {
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
        static String exportUsersCSV() {
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
        static String exportHealthRecordsCSV() {
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
        static boolean saveNotification(String sender, String receiver, String title, String content, String type) {
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
        static List<String[]> getAllNotifications() {
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
        static List<String[]> getMyNotifications(String username) {
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
        static boolean updateNotificationStatus(int id, String status) {
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
        static String getNotificationContent(int id) {
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
        static int getUnreadNotificationCount(String username) {
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
        static List<String[]> getFoods() {
            List<String[]> list = new ArrayList<>();
            String sql = "SELECT id, COALESCE(category,'') AS category, food_name, calories, protein, carbs, fat FROM foods ORDER BY id";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("category"),
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

        /** 添加/更新食物 */
        static boolean saveFood(int id, String category, String name, int calories, double protein, double carbs, double fat) {
            try (Connection conn = getConnection()) {
                if (id <= 0) {
                    String sql = "INSERT INTO foods (category, food_name, calories, protein, carbs, fat) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, category);
                    ps.setString(2, name);
                    ps.setInt(3, calories);
                    ps.setDouble(4, protein);
                    ps.setDouble(5, carbs);
                    ps.setDouble(6, fat);
                    return ps.executeUpdate() > 0;
                } else {
                    String sql = "UPDATE foods SET category = ?, food_name = ?, calories = ?, protein = ?, carbs = ?, fat = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, category);
                    ps.setString(2, name);
                    ps.setInt(3, calories);
                    ps.setDouble(4, protein);
                    ps.setDouble(5, carbs);
                    ps.setDouble(6, fat);
                    ps.setInt(7, id);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        /** 删除食物 */
        static boolean deleteFood(int id) {
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

        /** 批量导入食物(事务) rows: {category, name, calories, protein, carbs, fat} 或 {name, calories, protein, carbs, fat} */
        static int[] batchInsertFoods(List<String[]> rows) throws SQLException {
            int[] result = new int[]{0, 0};
            if (rows == null || rows.isEmpty()) return result;

            // 判断是 5 列还是 6 列
            int len = rows.get(0).length;
            boolean hasCategory = len >= 6;
            String sql = hasCategory
                ? "INSERT INTO foods (category, food_name, calories, protein, carbs, fat) VALUES (?, ?, ?, ?, ?, ?)"
                : "INSERT INTO foods (food_name, calories, protein, carbs, fat) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (String[] row : rows) {
                        try {
                            if (hasCategory) {
                                ps.setString(1, row[0] == null ? "" : row[0]);
                                ps.setString(2, row[1]);
                                ps.setInt(3, Integer.parseInt(row[2]));
                                ps.setDouble(4, Double.parseDouble(row[3]));
                                ps.setDouble(5, Double.parseDouble(row[4]));
                                ps.setDouble(6, Double.parseDouble(row[5]));
                            } else {
                                ps.setString(1, row[0]);
                                ps.setInt(2, Integer.parseInt(row[1]));
                                ps.setDouble(3, Double.parseDouble(row[2]));
                                ps.setDouble(4, Double.parseDouble(row[3]));
                                ps.setDouble(5, Double.parseDouble(row[4]));
                            }
                            ps.addBatch();
                            result[0]++;
                        } catch (Exception parseEx) {
                            result[1]++;
                        }
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            return result;
        }
        static List<String[]> getExerciseLibrary() {
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
        static boolean saveExerciseLibrary(int id, String name, String type, int calories, String intensity, String desc) {
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
        static boolean deleteExerciseLibrary(int id) {
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

        /** 批量导入运动库(事务) rows: {name, type, caloriesPerHour, intensity, desc} */
        static int[] batchInsertExercises(List<String[]> rows) throws SQLException {
            String sql = "INSERT INTO exercise_library (exercise_name, exercise_type, calories_per_hour, intensity_level, description) " +
                         "VALUES (?, ?, ?, ?, ?) ON CONFLICT (exercise_name) DO NOTHING";
            int ok = 0, fail = 0;
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (String[] row : rows) {
                        try {
                            ps.setString(1, row[0]);
                            ps.setString(2, row[1]);
                            ps.setInt(3, Integer.parseInt(row[2]));
                            ps.setString(4, row[3]);
                            ps.setString(5, row[4]);
                            ps.addBatch();
                            ok++;
                        } catch (Exception parseEx) {
                            fail++;
                        }
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            return new int[]{ok, fail};
        }
        static List<String[]> getHealthArticles() {
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
        static List<String[]> getPublishedHealthArticles() {
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
        static Map<String, String> getHealthArticleById(int id) {
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
        static boolean saveHealthArticle(int id, String title, String content, String category) {
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
        static List<String[]> getAITemplates() {
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
        static boolean saveAITemplate(int id, String name, String type, String content) {
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
        static Map<String, String> getAITemplateById(int id) {
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
        static List<String[]> getAIChatRecords() {
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
        static List<String[]> getAIDietRecords() {
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
        static List<String[]> getAICookbookRecords() {
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
        static boolean saveAIChatRecord(String username, String question, String answer) {
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
        static boolean saveAIDietRecord(String username, String query, String result) {
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
        static boolean saveAICookbookRecord(String username, String ingredients, String flavor, String meal, int people, String result) {
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
        static List<String[]> getAIUsageStats() {
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
        static List<String[]> getAIChatRecordsByUser(String username, int limit) {
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
        static List<String[]> getAIDietRecordsByUser(String username, int limit) {
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
        static List<String[]> getAICookbookRecordsByUser(String username, int limit) {
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
        static Map<String, String> getAIChatRecordById(int id) {
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
        static boolean updateAIChatStatus(int id, String status) {
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
        static Map<String, String> getAIDietRecordById(int id) {
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
        static Map<String, String> getAICookbookRecordById(int id) {
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
        static boolean updateAIDietStatus(int id, String status) {
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
        static boolean updateAICookbookStatus(int id, String status) {
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
        static Map<String, String> getAIApiConfig() {
            Map<String, String> cfg = new HashMap<>();
            cfg.put("provider_name", "zhipu");
            cfg.put("api_key", "");
            cfg.put("model_name", "glm-4.7-flash");
            cfg.put("vision_model", "glm-4v-flash");
            cfg.put("endpoint_url", "https://open.bigmodel.cn/api/paas/v4");
            String sql = "SELECT provider_name, api_key, model_name, vision_model, endpoint_url FROM ai_api_config WHERE provider_name = ? ORDER BY id DESC LIMIT 1";
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
        static String callOpenAIChat(String apiKey, String prompt) throws Exception {
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
        static String callVision(String apiKey, String visionModel, String prompt, String base64Image) throws Exception {
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
        static String extractContent(String json) {
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
        static boolean saveAIApiConfig(String apiKey, String modelName, String visionModel, String endpointUrl) {
            Map<String, String> existing = getAIApiConfig();
            String sql;
            boolean hasRecord = existing != null && !existing.getOrDefault("api_key", "").isEmpty();
            try (Connection conn = getConnection()) {
                if (hasRecord) {
                    sql = "UPDATE ai_api_config SET api_key = ?, model_name = ?, vision_model = ?, endpoint_url = ?, updated_at = CURRENT_TIMESTAMP WHERE provider_name = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, apiKey);
                        ps.setString(2, modelName);
                        ps.setString(3, visionModel);
                        ps.setString(4, endpointUrl);
                        ps.setString(5, "zhipu");
                        return ps.executeUpdate() > 0;
                    }
                } else {
                    sql = "INSERT INTO ai_api_config (provider_name, api_key, model_name, vision_model, endpoint_url) VALUES (?, ?, ?, ?, ?)";
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
        static boolean backupDatabase(String outputPath) {
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

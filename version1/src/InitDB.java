import java.sql.*;

/**
 * 数据库初始化工具
 * 自动创建 health_db 数据库和所有表, 插入80种食物数据
 * 用法: java -cp .;postgresql-42.7.3.jar InitDB
 */
public class InitDB {
    // 先连默认的 postgres 数据库 (用于创建新数据库)
    static final String ADMIN_URL = "jdbc:postgresql://localhost:5432/postgres";
    static final String DB_USER = "postgres";
    static final String DB_PASS = "12345678";
    static final String DB_NAME = "health_db";

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL JDBC 驱动未找到! 请确保 postgresql-42.7.3.jar 在当前目录");
            return;
        }

        // 第一步: 连接 postgres 数据库, 创建 health_db (如果不存在)
        System.out.println("正在连接 PostgreSQL 服务器...");
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, DB_USER, DB_PASS)) {
            System.out.println("连接成功!");

            // 检查 health_db 是否已存在
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getCatalogs();
            boolean dbExists = false;
            while (rs.next()) {
                if (DB_NAME.equals(rs.getString(1))) {
                    dbExists = true;
                    break;
                }
            }
            rs.close();

            if (dbExists) {
                System.out.println("数据库 " + DB_NAME + " 已存在, 跳过创建");
            } else {
                // PostgreSQL 不允许在事务中创建数据库, 需要关闭自动提交
                conn.setAutoCommit(true);
                String sql = "CREATE DATABASE " + DB_NAME;
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("数据库 " + DB_NAME + " 创建成功!");
                }
            }
        } catch (SQLException e) {
            System.out.println("连接失败: " + e.getMessage());
            return;
        }

        // 第二步: 连接 health_db, 创建所有表
        String healthUrl = "jdbc:postgresql://localhost:5432/" + DB_NAME;
        System.out.println("\n正在连接 " + DB_NAME + " 并创建表...");
        try (Connection conn = DriverManager.getConnection(healthUrl, DB_USER, DB_PASS)) {
            conn.setAutoCommit(true);

            // 用户表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) UNIQUE NOT NULL," +
                "  password VARCHAR(255) NOT NULL," +
                "  salt VARCHAR(64) NOT NULL DEFAULT ''," +
                "  gender VARCHAR(10)," +
                "  age INT CHECK (age > 0 AND age < 150)," +
                "  height DECIMAL(5,2) CHECK (height > 0)," +
                "  activity_level VARCHAR(20) DEFAULT '久坐'," +
                "  created_at TIMESTAMP DEFAULT NOW()" +
                ")");
            System.out.println("  [OK] users 表");

            // 健康记录表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS health_records (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  record_date DATE DEFAULT CURRENT_DATE," +
                "  weight DECIMAL(5,2) CHECK (weight > 0 AND weight < 500)," +
                "  body_fat DECIMAL(4,2) CHECK (body_fat >= 0 AND body_fat <= 100)," +
                "  water_rate DECIMAL(4,2) CHECK (water_rate >= 0 AND water_rate <= 100)," +
                "  muscle_rate DECIMAL(4,2) CHECK (muscle_rate >= 0 AND muscle_rate <= 100)," +
                "  visceral_fat INT CHECK (visceral_fat >= 0 AND visceral_fat <= 30)," +
                "  bone_muscle DECIMAL(5,2)," +
                "  bmr DECIMAL(6,2)," +
                "  tdee DECIMAL(6,2)," +
                "  bmi DECIMAL(4,2)," +
                "  body_age INT," +
                "  waist DECIMAL(5,2)," +
                "  body_type VARCHAR(20)," +
                "  created_at TIMESTAMP DEFAULT NOW()" +
                ")");
            System.out.println("  [OK] health_records 表");

            // 兼容旧表: 如果没有 body_type 字段则添加
            try {
                execUpdate(conn, "ALTER TABLE health_records ADD COLUMN IF NOT EXISTS body_type VARCHAR(20)");
                System.out.println("  [OK] body_type 字段已确认");
            } catch (SQLException e) {
                System.out.println("  [INFO] body_type 字段检查: " + e.getMessage());
            }

            // 运动记录表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS exercise_records (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  record_date DATE DEFAULT CURRENT_DATE," +
                "  exercise_type VARCHAR(50)," +
                "  duration INT CHECK (duration > 0)," +
                "  calories_burned INT CHECK (calories_burned >= 0)," +
                "  intensity VARCHAR(10) CHECK (intensity IN ('低','中','高'))" +
                ")");
            System.out.println("  [OK] exercise_records 表");

            // 目标表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS goals (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  goal_type VARCHAR(20)," +
                "  target_value DECIMAL(5,2)," +
                "  start_date DATE DEFAULT CURRENT_DATE," +
                "  end_date DATE" +
                ")");
            System.out.println("  [OK] goals 表");

            // 饮食记录表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS diet_records (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  record_date DATE DEFAULT CURRENT_DATE," +
                "  meal_type VARCHAR(10) CHECK (meal_type IN ('早餐','午餐','晚餐','加餐'))," +
                "  food_name VARCHAR(100)," +
                "  calories INT," +
                "  protein DECIMAL(5,2)," +
                "  carbs DECIMAL(5,2)," +
                "  fat DECIMAL(5,2)" +
                ")");
            System.out.println("  [OK] diet_records 表");

            // 成就表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS achievements (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  badge_name VARCHAR(50)," +
                "  achieved_date DATE DEFAULT CURRENT_DATE" +
                ")");
            System.out.println("  [OK] achievements 表");

            // AI报告表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS ai_reports (" +
                "  id SERIAL PRIMARY KEY," +
                "  username VARCHAR(50) REFERENCES users(username)," +
                "  report_type VARCHAR(20)," +
                "  report_content TEXT," +
                "  created_at TIMESTAMP DEFAULT NOW()" +
                ")");
            System.out.println("  [OK] ai_reports 表");

            // 食物数据库表
            execUpdate(conn,
                "CREATE TABLE IF NOT EXISTS foods (" +
                "  id SERIAL PRIMARY KEY," +
                "  food_name VARCHAR(100)," +
                "  calories INT," +
                "  protein DECIMAL(5,2)," +
                "  carbs DECIMAL(5,2)," +
                "  fat DECIMAL(5,2)" +
                ")");
            System.out.println("  [OK] foods 表");

            // 创建索引
            execUpdate(conn, "CREATE INDEX IF NOT EXISTS idx_hr_username_date ON health_records(username, record_date)");
            execUpdate(conn, "CREATE INDEX IF NOT EXISTS idx_dr_username_date ON diet_records(username, record_date)");
            execUpdate(conn, "CREATE INDEX IF NOT EXISTS idx_er_username_date ON exercise_records(username, record_date)");
            System.out.println("  [OK] 索引创建");

            // 检查 foods 表是否已有数据
            boolean hasFoodData = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet checkRs = stmt.executeQuery("SELECT COUNT(*) FROM foods")) {
                if (checkRs.next() && checkRs.getInt(1) > 0) {
                    hasFoodData = true;
                }
            }

            if (!hasFoodData) {
                System.out.println("\n正在插入80种食物数据...");
                insertFoods(conn);
                System.out.println("  [OK] 80种食物数据插入完成");
            } else {
                System.out.println("\n食物数据已存在, 跳过插入");
            }

            System.out.println("\n========================================");
            System.out.println("数据库初始化完成! 可以启动 HealthSystem 了");
            System.out.println("运行命令: java -Dfile.encoding=UTF-8 -cp .;postgresql-42.7.3.jar HealthSystem");
            System.out.println("========================================");

        } catch (SQLException e) {
            System.out.println("建表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void execUpdate(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private static void insertFoods(Connection conn) throws SQLException {
        String sql = "INSERT INTO foods (food_name, calories, protein, carbs, fat) VALUES (?, ?, ?, ?, ?)";
        String[][] foods = {
            {"米饭","116","2.6","25.9","0.3"}, {"面条","137","4.5","28.5","0.5"},
            {"馒头","221","7.0","47.0","1.0"}, {"包子","220","7.5","38.0","4.0"},
            {"饺子","250","8.0","30.0","10.0"}, {"油条","388","6.9","43.0","20.0"},
            {"豆浆","33","3.0","1.5","1.8"}, {"牛奶","54","3.0","4.5","3.0"},
            {"鸡蛋","144","13.0","1.5","9.0"}, {"鸡胸肉","133","31.0","0.0","2.8"},
            {"鸡腿肉","181","24.0","0.0","9.0"}, {"猪肉瘦","143","20.0","0.0","6.0"},
            {"猪肉肥","395","14.0","0.0","37.0"}, {"牛肉瘦","125","26.0","0.0","2.0"},
            {"牛肉肥","332","18.0","0.0","28.0"}, {"羊肉","205","20.0","0.0","13.0"},
            {"鱼肉","113","20.0","0.0","3.0"}, {"虾肉","85","18.0","0.0","1.0"},
            {"豆腐","81","8.0","3.5","4.0"}, {"豆腐干","140","16.0","5.0","6.0"},
            {"豆浆粉","422","18.0","55.0","12.0"}, {"白菜","13","1.5","2.0","0.1"},
            {"菠菜","23","2.5","3.0","0.3"}, {"西兰花","34","3.0","5.0","0.5"},
            {"菜花","25","2.0","4.0","0.3"}, {"黄瓜","15","0.8","3.0","0.1"},
            {"西红柿","18","0.9","4.0","0.2"}, {"茄子","25","1.0","5.0","0.2"},
            {"青椒","22","1.0","4.5","0.2"}, {"土豆","77","2.0","17.0","0.1"},
            {"红薯","86","1.6","20.0","0.1"}, {"山药","57","1.5","12.0","0.1"},
            {"南瓜","23","1.0","5.0","0.1"}, {"冬瓜","12","0.4","2.5","0.1"},
            {"萝卜","16","0.6","3.5","0.1"}, {"胡萝卜","41","1.0","9.5","0.2"},
            {"洋葱","40","1.1","9.0","0.1"}, {"大蒜","149","6.0","33.0","0.5"},
            {"姜","80","1.8","17.0","0.8"}, {"苹果","52","0.3","14.0","0.2"},
            {"香蕉","89","1.1","23.0","0.3"}, {"橙子","47","0.9","12.0","0.1"},
            {"西瓜","30","0.6","7.5","0.1"}, {"哈密瓜","34","0.5","8.0","0.1"},
            {"葡萄","69","0.7","18.0","0.2"}, {"草莓","32","0.7","7.5","0.3"},
            {"蓝莓","57","0.7","14.5","0.3"}, {"桃子","39","0.9","10.0","0.1"},
            {"梨","42","0.4","10.0","0.1"}, {"李子","46","0.7","12.0","0.3"},
            {"樱桃","50","1.0","12.0","0.3"}, {"芒果","60","0.8","15.0","0.4"},
            {"火龙果","60","1.2","14.0","0.4"}, {"猕猴桃","61","1.0","15.0","0.5"},
            {"柠檬","29","1.1","6.0","0.3"}, {"核桃","654","15.0","14.0","65.0"},
            {"花生","567","26.0","16.0","49.0"}, {"瓜子","600","24.0","12.0","53.0"},
            {"黑芝麻","559","17.0","12.0","50.0"}, {"小米","358","9.0","75.0","3.0"},
            {"玉米","112","3.5","22.0","1.5"}, {"燕麦","389","17.0","66.0","7.0"},
            {"荞麦","337","13.0","71.0","3.0"}, {"意面","131","5.0","25.0","0.5"},
            {"蛋糕","348","5.0","45.0","17.0"}, {"面包","265","9.0","50.0","3.0"},
            {"饼干","450","6.0","65.0","18.0"}, {"巧克力","546","4.0","60.0","32.0"},
            {"冰淇淋","207","3.5","24.0","11.0"}, {"酸奶","72","3.5","8.0","3.0"},
            {"奶酪","360","25.0","2.0","28.0"}, {"黄油","717","0.5","0.0","81.0"},
            {"植物油","884","0.0","0.0","100.0"}, {"酱油","60","2.0","10.0","0.0"},
            {"醋","18","0.4","4.0","0.0"}, {"蜂蜜","304","0.3","82.0","0.0"},
            {"白糖","387","0.0","100.0","0.0"}, {"红糖","380","0.0","95.0","0.0"},
            {"盐","0","0.0","0.0","0.0"}, {"茶叶","0","0.0","0.0","0.0"},
            {"咖啡","1","0.1","0.0","0.0"}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] food : foods) {
                pstmt.setString(1, food[0]);
                pstmt.setInt(2, Integer.parseInt(food[1]));
                pstmt.setDouble(3, Double.parseDouble(food[2]));
                pstmt.setDouble(4, Double.parseDouble(food[3]));
                pstmt.setDouble(5, Double.parseDouble(food[4]));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}

import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/health_db";
        String user = "postgres";
        String pass = "12345678";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // 查看表结构
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet cols = meta.getColumns(null, "public", "health_records", null);
            System.out.println("health_records 表字段:");
            while (cols.next()) {
                System.out.println("  - " + cols.getString("COLUMN_NAME") + " " + cols.getString("TYPE_NAME"));
            }

            // 查看所有用户
            System.out.println("\n用户列表:");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, gender, age, height FROM users")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString("username") + " | " + rs.getString("gender") +
                            " | " + rs.getInt("age") + "岁 | " + rs.getDouble("height") + "cm");
                }
            }

            // 查看所有健康记录
            System.out.println("\n健康记录列表:");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, record_date, weight, bmi, body_type FROM health_records ORDER BY id")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString("username") + " | " + rs.getDate("record_date") +
                            " | 体重:" + rs.getDouble("weight") + " | BMI:" + rs.getDouble("bmi") +
                            " | 类型:" + rs.getString("body_type"));
                }
            }

            // 查看记录总数
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM health_records")) {
                rs.next();
                System.out.println("\n总记录数: " + rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

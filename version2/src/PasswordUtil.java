import java.security.*;
import java.util.*;

public class PasswordUtil {

        /** 生成随机盐值 (16字节 → 32位Hex字符串) */
        static String generateSalt() {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return bytesToHex(salt);
        }

        /** SHA-256 加盐哈希 */
        static String hash(String password, String salt) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update((password + salt).getBytes("UTF-8"));
                return bytesToHex(md.digest());
            } catch (Exception e) {
                throw new RuntimeException("密码加密失败", e);
            }
        }

        /** 验证密码 */
        static boolean verify(String password, String salt, String hash) {
            return hash(password, salt).equals(hash);
        }

        /** 字节数组转 Hex 字符串 */
        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    // ================================================================
    //                    第二部分: 数据库工具类
    // ================================================================

    /**
     * 数据库工具类 — 封装所有 JDBC 操作
     * 全部使用 PreparedStatement 防止 SQL 注入
     */

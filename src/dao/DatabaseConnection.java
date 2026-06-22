package asrama.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final Properties CONFIG = new Properties();

    static {
        try (FileInputStream input = new FileInputStream("config/database.properties")) {
            CONFIG.load(input);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException ex) {
            throw new ExceptionInInitializerError("Gagal memuat konfigurasi database: " + ex.getMessage());
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password", "")
        );
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }
}

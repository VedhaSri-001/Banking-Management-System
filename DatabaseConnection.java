package banking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection con = null;

    public static Connection getConnection() {
        if (con != null) return con; // reuse connection if already created
        try {
            String mysqlJDBCDriver = "com.mysql.cj.jdbc.Driver";
            String url = "jdbc:mysql://127.0.0.1:3306/bank";
            String user = "your_username";
            String pass = "your_password";
            Class.forName(mysqlJDBCDriver);
            con = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Connection Failed!");
            e.printStackTrace();
        }
        return con;
    }
}



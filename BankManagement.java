package banking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BankManagement {

    private static final int NULL = 0;
    private static final Logger LOGGER = Logger.getLogger(BankManagement.class.getName());

    static Connection con = DatabaseConnection.getConnection();

    public static boolean createAccount(String name, int passCode, int ac_no, int balance) {
        if (name == null || name.isEmpty() || passCode == NULL || ac_no <= 0) {
            System.out.println("All Fields Required!");
            return false;
        }

        String sql = "INSERT INTO customer (ac_no, cname, balance, pass_code) VALUES (?, ?, ?, ?)";

        try(PreparedStatement pst = con.prepareStatement(sql)){
            pst.setInt(1,ac_no);
            pst.setString(2,name);
            pst.setInt(3,balance);
            pst.setInt(4,passCode);

            int result = pst.executeUpdate();
            if (result == 1) {
                System.out.println(name + ", Now You Can Login!");
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Account Number or Username Not Available!");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred during account creation", e);
        }
        return false;
    }

    public static boolean loginAccount(String name, int passCode) {
        if (name == null || name.isEmpty() || passCode == NULL) {
            System.out.println("All Fields Required!");
            return false;
        }

        String sql = "SELECT * FROM customer WHERE cname = ? AND pass_code = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setInt(2, passCode);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
                    int ch;
                    int senderAc = rs.getInt("ac_no");

                    while (true) {
                        try {
                            System.out.println("\nHello, " + rs.getString("cname"));
                            System.out.println("1) Transfer Money");
                            System.out.println("2) View Balance");
                            System.out.println("3) Log Out");
                            System.out.print("Enter Choice: ");
                            ch = Integer.parseInt(sc.readLine());

                            switch (ch) {
                                case 1:
                                    System.out.print("Enter Receiver A/c No: ");
                                    int receiveAc = Integer.parseInt(sc.readLine());
                                    System.out.print("Enter Amount: ");
                                    int amt = Integer.parseInt(sc.readLine());

                                    if (transferMoney(senderAc, receiveAc, amt)) {
                                        System.out.println("MSG: Money Sent Successfully!\n");
                                    } else {
                                        System.out.println("ERR: Transfer Failed!\n");
                                    }
                                    break;

                                case 2:
                                    getBalance(senderAc);
                                    break;

                                case 3:
                                    System.out.println("Logged Out Successfully!\n");
                                    return true;

                                default:
                                    System.out.println("ERR: Enter Valid Input!\n");
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Exception occurred in user menu", e);
                            System.out.println("ERR: Invalid Input!");
                        }
                    }
                } else {
                    System.out.println("Invalid Username or Password!");
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred during login", e);
        }
        return false;
    }

    public static void getBalance(int acNo) {
        String sql = "SELECT * FROM customer WHERE ac_no = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, acNo);
            try (ResultSet rs = pst.executeQuery()) {
                System.out.println("-----------------------------------------------------------");
                System.out.printf("%12s %10s %10s\n", "Account No", "Name", "Balance");
                while (rs.next()) {
                    System.out.printf("%12d %10s %10d.00\n", rs.getInt("ac_no"), rs.getString("cname"), rs.getInt("balance"));
                }
                System.out.println("-----------------------------------------------------------\n");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while fetching balance", e);
        }
    }

    public static boolean transferMoney(int sender_ac, int receiver_ac, int amount) {
        if (receiver_ac == NULL || amount == NULL) {
            System.out.println("All Fields Required!");
            return false;
        }

        try {
            con.setAutoCommit(false);

            // Check sender balance
            String sql = "SELECT balance FROM customer WHERE ac_no = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, sender_ac);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Sender Account Not Found!");
                        con.rollback();
                        return false;
                    }
                    int currentBalance = rs.getInt("balance");
                    if (currentBalance < amount) {
                        System.out.println("Insufficient Balance!");
                        con.rollback();
                        return false;
                    }
                }
            }

            // Check receiver exists
            sql = "SELECT * FROM customer WHERE ac_no = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, receiver_ac);
                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Receiver Account Number Invalid!");
                        con.rollback();
                        return false;
                    }
                }
            }

            // Debit sender
            sql = "UPDATE customer SET balance = balance - ? WHERE ac_no = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, amount);
                pst.setInt(2, sender_ac);
                pst.executeUpdate();
            }

            // Credit receiver
            sql = "UPDATE customer SET balance = balance + ? WHERE ac_no = ?";
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, amount);
                pst.setInt(2, receiver_ac);
                pst.executeUpdate();
            }

            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Transfer Failed", e);
            try {
                if (con != null) {
                    con.rollback();
                    con.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed", ex);
            }
        }
        return false;
    }
}

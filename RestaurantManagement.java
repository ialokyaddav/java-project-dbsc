import java.sql.*;
import java.util.Scanner;

public class RestaurantManagement {

    // Change only the password below
    static final String URL  = "jdbc:mysql://localhost:3306/restaurant?createDatabaseIfNotExist=true";
    static final String USER = "root";
    static final String PASS = "Anish&alok123";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Database connected!");
            setupTables(con);

            while (true) {
                System.out.println("\n===== RESTAURANT MANAGEMENT =====");
                System.out.println("1. Add menu item");
                System.out.println("2. View menu");
                System.out.println("3. Update item price");
                System.out.println("4. Delete menu item");
                System.out.println("5. Take order and print bill");
                System.out.println("6. View sales report");
                System.out.println("7. Exit");
                System.out.print("Choice: ");

                int choice = readInt();
                try {
                    switch (choice) {
                        case 1: addItem(con); break;
                        case 2: viewMenu(con); break;
                        case 3: updatePrice(con); break;
                        case 4: deleteItem(con); break;
                        case 5: placeOrder(con); break;
                        case 6: salesReport(con); break;
                        case 7:
                            System.out.println("Bye!");
                            return;
                        default:
                            System.out.println("Please choose 1 to 7.");
                    }
                } catch (SQLException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    // Creates the tables on first run and adds a few sample dishes
    static void setupTables(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS menu ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "name VARCHAR(50) NOT NULL, "
                    + "price DECIMAL(8,2) NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                    + "order_id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "item_name VARCHAR(50), "
                    + "qty INT, "
                    + "line_total DECIMAL(10,2), "
                    + "order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM menu");
            rs.next();
            if (rs.getInt(1) == 0) {
                st.executeUpdate("INSERT INTO menu (name, price) VALUES "
                        + "('Paneer Butter Masala', 220), ('Veg Biryani', 180), "
                        + "('Butter Naan', 40), ('Masala Dosa', 120), ('Cold Coffee', 90)");
                System.out.println("Sample menu added.");
            }
        }
    }

    static void addItem(Connection con) throws SQLException {
        System.out.print("Item name: ");
        String name = sc.nextLine();
        System.out.print("Price: ");
        double price = readDouble();
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO menu (name, price) VALUES (?, ?)")) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
            System.out.println("Item added.");
        }
    }

    static void viewMenu(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM menu ORDER BY id")) {
            System.out.println("\nID   ITEM                      PRICE");
            System.out.println("------------------------------------");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-4d %-24s %8.2f%n",
                        rs.getInt("id"), rs.getString("name"), rs.getDouble("price"));
            }
            if (!any) System.out.println("Menu is empty.");
        }
    }

    static void updatePrice(Connection con) throws SQLException {
        System.out.print("Item ID: ");
        int id = readInt();
        System.out.print("New price: ");
        double price = readDouble();
        try (PreparedStatement ps = con.prepareStatement("UPDATE menu SET price = ? WHERE id = ?")) {
            ps.setDouble(1, price);
            ps.setInt(2, id);
            System.out.println(ps.executeUpdate() > 0 ? "Price updated." : "Item not found.");
        }
    }

    static void deleteItem(Connection con) throws SQLException {
        System.out.print("Item ID: ");
        int id = readInt();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM menu WHERE id = ?")) {
            ps.setInt(1, id);
            System.out.println(ps.executeUpdate() > 0 ? "Item deleted." : "Item not found.");
        }
    }

    static void placeOrder(Connection con) throws SQLException {
        double subtotal = 0;
        StringBuilder bill = new StringBuilder();

        while (true) {
            System.out.print("Item ID (0 to finish): ");
            int id = readInt();
            if (id == 0) break;

            try (PreparedStatement ps = con.prepareStatement("SELECT name, price FROM menu WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Item not found.");
                        continue;
                    }
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");

                    System.out.print("Quantity: ");
                    int qty = readInt();
                    if (qty <= 0) {
                        System.out.println("Quantity must be at least 1.");
                        continue;
                    }

                    double line = price * qty;
                    try (PreparedStatement ins = con.prepareStatement(
                            "INSERT INTO orders (item_name, qty, line_total) VALUES (?, ?, ?)")) {
                        ins.setString(1, name);
                        ins.setInt(2, qty);
                        ins.setDouble(3, line);
                        ins.executeUpdate();
                    }
                    subtotal += line;
                    bill.append(String.format("%-22s %2d x %7.2f = %9.2f%n", name, qty, price, line));
                }
            }
        }

        if (subtotal == 0) {
            System.out.println("No items ordered.");
            return;
        }
        double gst = subtotal * 0.05;
        System.out.println("\n========== BILL ==========");
        System.out.print(bill);
        System.out.println("--------------------------");
        System.out.printf("Subtotal : %10.2f%n", subtotal);
        System.out.printf("GST (5%%) : %10.2f%n", gst);
        System.out.printf("TOTAL    : %10.2f%n", subtotal + gst);
        System.out.println("==========================");
    }

    static void salesReport(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT item_name, SUM(qty) AS sold, SUM(line_total) AS revenue "
                             + "FROM orders GROUP BY item_name ORDER BY revenue DESC")) {
            System.out.println("\nITEM                     SOLD    REVENUE");
            System.out.println("-----------------------------------------");
            double total = 0;
            boolean any = false;
            while (rs.next()) {
                any = true;
                total += rs.getDouble("revenue");
                System.out.printf("%-24s %4d %10.2f%n",
                        rs.getString("item_name"), rs.getInt("sold"), rs.getDouble("revenue"));
            }
            if (!any) {
                System.out.println("No orders yet.");
            } else {
                System.out.printf("Total sales (before GST): %.2f%n", total);
            }
        }
    }

    static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Enter a whole number: ");
            }
        }
    }

    static double readDouble() {
        while (true) {
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Enter a number: ");
            }
        }
    }
}

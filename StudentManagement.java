import java.sql.*;
import java.util.Scanner;

public class StudentManagement {

    // Apna MySQL password yahan daalo
    static final String URL  = "jdbc:mysql://localhost:3306/college";
    static final String USER = "root";
    static final String PASS = "Anish&alok123";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Database connected!");

            while (true) {
                System.out.println("\n===== STUDENT MANAGEMENT =====");
                System.out.println("1. Add Student");
                System.out.println("2. View All Students");
                System.out.println("3. Search Student by ID");
                System.out.println("4. Update Marks");
                System.out.println("5. Delete Student");
                System.out.println("6. Exit");
                System.out.print("Choice: ");

                int choice = readInt();
                try {
                    switch (choice) {
                        case 1: addStudent(con); break;
                        case 2: viewAll(con); break;
                        case 3: searchStudent(con); break;
                        case 4: updateMarks(con); break;
                        case 5: deleteStudent(con); break;
                        case 6:
                            System.out.println("Bye!");
                            return;
                        default:
                            System.out.println("Galat choice, 1 se 6 ke beech chuno.");
                    }
                } catch (SQLException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection fail: " + e.getMessage());
        }
    }

    static void addStudent(Connection con) throws SQLException {
        System.out.print("ID: ");
        int id = readInt();
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Marks: ");
        int marks = readInt();

        String sql = "INSERT INTO student (id, name, marks) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setInt(3, marks);
            ps.executeUpdate();
            System.out.println("Student add ho gaya.");
        }
    }

    static void viewAll(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM student ORDER BY id")) {
            System.out.println("\nID\tNAME\t\tMARKS");
            System.out.println("-----------------------------");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println(rs.getInt("id") + "\t" + rs.getString("name")
                        + "\t\t" + rs.getInt("marks"));
            }
            if (!any) System.out.println("Koi student nahi mila.");
        }
    }

    static void searchStudent(Connection con) throws SQLException {
        System.out.print("ID: ");
        int id = readInt();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM student WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("ID: " + rs.getInt("id")
                            + ", Name: " + rs.getString("name")
                            + ", Marks: " + rs.getInt("marks"));
                } else {
                    System.out.println("Is ID ka student nahi mila.");
                }
            }
        }
    }

    static void updateMarks(Connection con) throws SQLException {
        System.out.print("ID: ");
        int id = readInt();
        System.out.print("Naye Marks: ");
        int marks = readInt();
        try (PreparedStatement ps = con.prepareStatement("UPDATE student SET marks = ? WHERE id = ?")) {
            ps.setInt(1, marks);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Marks update ho gaye." : "Is ID ka student nahi mila.");
        }
    }

    static void deleteStudent(Connection con) throws SQLException {
        System.out.print("ID: ");
        int id = readInt();
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM student WHERE id = ?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Student delete ho gaya." : "Is ID ka student nahi mila.");
        }
    }

    // Number ke liye safe input (galat likhne par crash nahi hoga)
    static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Sirf number daalo: ");
            }
        }
    }
}
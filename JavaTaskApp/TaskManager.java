import java.sql.*;
import java.util.*;

// Task model
class Task {
    String title, description, status;
    int priority;

    Task(String title, String desc, int priority, String status) {
        this.title = title;
        this.description = desc;
        this.priority = priority;
        this.status = status;
    }
}

public class TaskManager {
    static final String URL = "jdbc:mysql://localhost:3306/taskdb"; //Database URL
    static final String USER = "root";       //  MySQL username
    static final String PASS = "Ruchijoshi@24";   // MySQL password

    private final Scanner sc = new Scanner(System.in);
    private LinkedList<Task> taskList = new LinkedList<>();
    private Stack<Task> undoStack = new Stack<>();

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Add a new task
    public void addTask() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter task title: ");
        String title = sc.nextLine();
        System.out.print("Enter description: ");
        String desc = sc.nextLine();
        System.out.print("Enter priority (1-10): ");
        int priority = sc.nextInt();
        sc.nextLine();

        Task newTask = new Task(title, desc, priority, "pending");
        taskList.add(newTask);
        undoStack.push(newTask);

        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tasks (title, description, priority, status) VALUES (?, ?, ?, ?)");
            ps.setString(1, title);
            ps.setString(2, desc);
            ps.setInt(3, priority);
            ps.setString(4, "pending");
            ps.executeUpdate();
            System.out.println(" Task added and saved to database.");
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        } finally {
            sc.close();
        }
    }

    // Display all tasks
    public void displayTasks() {
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tasks ORDER BY priority DESC")) {

            System.out.println("\n" + "=".repeat(70));
            System.out.println("TASK LIST (Sorted by Priority)");
            System.out.println("=".repeat(70));
            System.out.printf("%-20s %-10s %-10s %-30s%n", "Title", "Priority", "Status", "Description");
            System.out.println("-".repeat(70));

            while (rs.next()) {
                System.out.printf("%-20s %-10d %-10s %-30s%n",
                        rs.getString("title"),
                        rs.getInt("priority"),
                        rs.getString("status"),
                        rs.getString("description"));
            }
            System.out.println("=".repeat(70));

        } catch (SQLException e) {
            System.out.println(" Error fetching tasks: " + e.getMessage());
        }
    }

    // Delete a task
    public void deleteTask() {
        System.out.print("Enter title of task to delete: ");
        String title = sc.nextLine();

        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM tasks WHERE title = ?");
            ps.setString(1, title);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println(" Task deleted successfully.");
            else
                System.out.println(" Task not found.");
        } catch (SQLException e) {
            System.out.println(" Error deleting: " + e.getMessage());
        }
    }

    // Update a task
    public void updateTask() {
        System.out.print("Enter title of task to update: ");
        String title = sc.nextLine();

        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM tasks WHERE title = ?");
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println(" Task not found.");
                return;
            }

            System.out.print("Enter new description (or press Enter to keep current): ");
            String desc = sc.nextLine();
            if (desc.isEmpty()) desc = rs.getString("description");

            System.out.print("Enter new priority (1-10, or 0 to keep current): ");
            int priority = sc.nextInt();
            sc.nextLine();
            if (priority == 0) priority = rs.getInt("priority");

            PreparedStatement upd = con.prepareStatement(
                "UPDATE tasks SET description = ?, priority = ? WHERE title = ?");
            upd.setString(1, desc);
            upd.setInt(2, priority);
            upd.setString(3, title);
            upd.executeUpdate();
            System.out.println(" Task updated successfully.");

        } catch (SQLException e) {
            System.out.println(" Error updating: " + e.getMessage());
        }
    }

    // Mark complete
    public void markComplete() {
        System.out.print("Enter title of task to mark complete: ");
        String title = sc.nextLine();

        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE tasks SET status = 'done' WHERE title = ?");
            ps.setString(1, title);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println(" Task marked complete!");
            else
                System.out.println(" Task not found.");
        } catch (SQLException e) {
            System.out.println(" Error: " + e.getMessage());
        }
    }

    // Undo last added task
    public void undoLastOperation() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo!");
            return;
        }
        Task last = undoStack.pop();
        try (Connection con = connect()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM tasks WHERE title = ?");
            ps.setString(1, last.title);
            ps.executeUpdate();
            System.out.println(" Undid last addition: " + last.title);
        } catch (SQLException e) {
            System.out.println(" Error undoing: " + e.getMessage());
        }
    }

    // Menu
    public void run() {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("           TASK MANAGER MENU");
            System.out.println("=".repeat(50));
            System.out.println("1. Add Task");
            System.out.println("2. Delete Task");
            System.out.println("3. Update Task");
            System.out.println("4. Mark Task Complete");
            System.out.println("5. Display All Tasks");
            System.out.println("6. Undo Last Operation");
            System.out.println("7. Exit");
            System.out.println("=".repeat(50));
            System.out.print("Enter your choice (1-7): ");

            int choice = sc.nextInt();
            sc.nextLine();
            switch (choice) {
                case 1 -> addTask();
                case 2 -> deleteTask();
                case 3 -> updateTask();
                case 4 -> markComplete();
                case 5 -> displayTasks();
                case 6 -> undoLastOperation();
                case 7 -> {
                    System.out.println(" Thank you for using Task Manager!");
                    return;
                }
                default -> System.out.println("Invalid choice! Please enter between 1–7.");
            }
        }
    }

    public static void main(String[] args) {
        TaskManager tm = new TaskManager();
        tm.run();
    }
}

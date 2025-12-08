package com.example;

import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             Scanner scanner = new Scanner(System.in)
        ) {
            boolean loggedIn = login(scanner,connection);

            if (!loggedIn) {
                System.out.println("Invalid username or password");
                return;
            }

            mainMenuLoop(scanner, connection);



        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Todo: Starting point for your code
    }
    private boolean login(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        String sql = "select * from account where name = ? and password = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }

    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    private void mainMenuLoop(Scanner scanner, Connection connection) throws SQLException {
        boolean running  = true;
        while (running) {
            printMenu();

            String choice = scanner.nextLine().trim();

            //Hanterar vilka menyval som användaren gör
            switch (choice) {
                case "0" -> running = false;
                case "1" -> listMoonMissions(connection);
                case "2" -> getMissionById(scanner, connection);
                case "3" -> countMissionsByYear(scanner, connection);
                case "4" -> createAccount(scanner, connection);
                case "5" -> updateAccountPassword(scanner, connection);
                case "6" -> deleteAccount(scanner, connection);


                    default -> System.out.println("Invalid option");
            }
        }
    }

    //Skriver ut menyn
    private void printMenu() {
        System.out.println("------------Main menu------------");
        System.out.println("1) List moon missions");
        System.out.println("2) Get a moon mission by mission:id");
        System.out.println("3) Count missions for a given year");
        System.out.println("4) Create an account");
        System.out.println("5) Update an account");
        System.out.println("6) Delete an account");
        System.out.println("0) Exit");
        System.out.println("---------------------------------");
        System.out.print("Choose: ");

    }

    //Listar alla rymduppdrag
    private void listMoonMissions(Connection connection) throws SQLException {
        String sql = "select spacecraft from moon_mission";

        try (var ps = connection.prepareStatement(sql);
            var rs = ps.executeQuery()) {

            //Loopa igenom alla rader och skriv ut namnet på rymdfarkosten
            while (rs.next()) {
                String spacecraft = rs.getString("spacecraft");
                System.out.println(spacecraft);
            }
        }
    }

    //Hämta ett specifikt uppdrag baserat på mission_id
    private void getMissionById(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("mission id: ");
        String input = scanner.nextLine();

        int id;
        try {
            id = Integer.parseInt(input); //Kontrollerar att id är ett nummer
        } catch (NumberFormatException e) {
            System.out.println("Invalid id");
            return;
        }

        String sql = "select * from moon_mission where mission_id = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    //Läs alla kolumner och skriv ut dem
                    System.out.println("Spacecraft: " +  rs.getString("spacecraft"));
                    System.out.println("Launch date: " +  rs.getString("launch_date"));
                    System.out.println("Carrier rocket: " +  rs.getString("carrier_rocket"));
                    System.out.println("operator: " +   rs.getString("operator"));
                    System.out.println("Mission type: " + rs.getString("mission_type"));
                    System.out.println("Outcome: " +  rs.getString("outcome"));
                } else {
                    System.out.println("No mission found");
                }
            }
        }

    }

    //Räknar hur många uppdrag som skedde ett visst år
    private void countMissionsByYear(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("Year: ");
        String input = scanner.nextLine();

        int year;

        try {
            year = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid year");
            return;
        }
        String sql = "select count(*) from moon_mission where year(launch_date) = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setInt(1, year);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("Mission in " + year + ": " + count);
                }
            }
        }
    }

    //Skapa ett ntt konto i databasen
    private void createAccount(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("First name: ");
        String firstName = scanner.nextLine();

        System.out.print("Last name: ");
        String lastName = scanner.nextLine();

        System.out.print("SSN: ");
        String ssn = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        //Skapa ett användarnamn baserat på första 3 bokstäverna av för- och efternamn
        String usernamePart1 = firstName.length() >= 3 ? firstName.substring(0, 3) : firstName;
        String usernamePart2 = lastName.length() >= 3 ? lastName.substring(0, 3) : lastName;
        String name = usernamePart1 + usernamePart2;

        String sql = "insert into account (name, password, first_name, last_name, ssn) values (?, ?, ?, ?, ?)";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, password);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, ssn);

            ps.executeUpdate();
        }
        System.out.println("Account created with username: " + name);
    }

    //Uppdatera lösenord för ett konto baserat på user_id
    private void updateAccountPassword(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("User id: ");
        String input = scanner.nextLine();

        int userId;
        try {
            userId = Integer.parseInt(input);
        }catch (NumberFormatException e) {
            System.out.println("Invalid id");
            return;
        }
        System.out.print("New password: ");
        String newPassword = scanner.nextLine();

        String sql = "update account set password = ? where user_id = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Password updated");
            } else {
                System.out.println("No password found for user id: " + userId);
            }
        }

    }

    //Ta bort ett konto basserat på user_id
    private void deleteAccount(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("User id: ");
        String input = scanner.nextLine();

        int userId;
        try {
            userId = Integer.parseInt(input);
        }catch (NumberFormatException e) {
            System.out.println("Invalid id");
            return;
        }

        String sql = "delete from account where user_id = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Account deleted");
            } else  {
                System.out.println("No account found for user id: " + userId);
            }
        }
    }
}

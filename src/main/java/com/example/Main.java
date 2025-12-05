package com.example;

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
                System.out.println("Invalid login");
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

            switch (choice) {
                case "0" -> running = false;
                case "1" -> listMoonMissions(connection);
                case "2" -> getMissionById(scanner, connection);


                    default -> System.out.println("Invalid option");
            }
        }
    }
    private void printMenu() {
        System.out.println("1) List moon missions");
        System.out.println("2) Get a moon mission by mission:id");
        System.out.println("3) Count missions for a given year");
        System.out.println("4) Create an account");
        System.out.println("5) Update an account");
        System.out.println("6) Delete an account");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }
    private void listMoonMissions(Connection connection) throws SQLException {
        String sql = "select spacecraft from moon_mission";

        try (var ps = connection.prepareStatement(sql);
            var rs = ps.executeQuery()) {

            while (rs.next()) {
                String spacecraft = rs.getString("spacecraft");
                System.out.println(spacecraft);
            }
        }
    }
    private void getMissionById(Scanner scanner, Connection connection) throws SQLException {
        System.out.print("mission id: ");
        String input = scanner.nextLine();

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid id");
            return;
        }

        String sql = "select * from moon_mission where mission_id = ?";

        try (var ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String spacecraft = rs.getString("spacecraft");
                    String launchDate = rs.getString("launch_date");
                    String rocket = rs.getString("carrier_rocket");
                    String operator = rs.getString("operator");
                    String type = rs.getString("mission_type");
                    String outcome = rs.getString("outcome");

                    System.out.println("Spacecraft: " +  spacecraft);
                    System.out.println("Launch date: " + launchDate);
                    System.out.println("Carrier rocket: " + rocket);
                    System.out.println("Operator: " + operator);
                    System.out.println("Mission type: " + type);
                    System.out.println("Outcome: " + outcome);

                } else {
                    System.out.println("No mission found");
                }
            }
        }
    }
}

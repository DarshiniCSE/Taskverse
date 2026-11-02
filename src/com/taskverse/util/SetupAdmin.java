package com.taskverse.util;

import com.taskverse.dao.UserDAO;
import com.taskverse.dao.UserDAOImpl;
import com.taskverse.model.User;
import com.taskverse.security.PasswordHasher;

import java.io.Console;
import java.util.Scanner;

/**
 * SetupAdmin
 * Secure, local command-line utility for initial one-time owner account creation.
 * 
 * Guarantees:
 * - Only creates an account if NO OWNER exists in the system.
 * - Adopts all unowned legacy records to the initial owner.
 * - Never logs or exposes password plaintext.
 * - Verifies database state and record counts before reporting success.
 * 
 * Usage:
 *   java -cp ... com.taskverse.util.SetupAdmin [username] [email]
 */
public class SetupAdmin {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       TASKVERSE — INITIAL OWNER SETUP CLI       ");
        System.out.println("=================================================");

        try {
            DBConnection.loadConfiguration();
            String testStatus = DBConnection.testConnection();
            if (!testStatus.startsWith("SUCCESS")) {
                System.err.println("[ERROR] Database connection failed: " + testStatus);
                System.exit(1);
            }

            UserDAO userDAO = new UserDAOImpl();

            // 1. Enforce singleton owner rule
            int existingOwners = userDAO.getOwnerCount();
            if (existingOwners > 0) {
                System.err.println("[ABORTED] An owner account already exists in the system.");
                System.err.println("          Setup utility can only be used once to establish the initial owner.");
                System.exit(2);
            }

            String username = null;
            String email = null;
            char[] password = null;

            Console console = System.console();

            if (args.length >= 2) {
                username = args[0].trim();
                email = args[1].trim();
            }

            if (username == null || username.isEmpty()) {
                if (console != null) {
                    username = console.readLine("Enter Owner Username (3-30 chars): ").trim();
                } else {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Enter Owner Username (3-30 chars): ");
                    username = scanner.nextLine().trim();
                }
            }

            if (email == null || email.isEmpty()) {
                if (console != null) {
                    email = console.readLine("Enter Owner Email: ").trim();
                } else {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Enter Owner Email: ");
                    email = scanner.nextLine().trim();
                }
            }

            if (console != null) {
                password = console.readPassword("Enter Owner Password (min 8 chars): ");
                char[] confirm = console.readPassword("Confirm Owner Password: ");
                if (!java.util.Arrays.equals(password, confirm)) {
                    System.err.println("[ERROR] Passwords do not match.");
                    System.exit(3);
                }
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter Owner Password (min 8 chars): ");
                String p1 = scanner.nextLine();
                System.out.print("Confirm Owner Password: ");
                String p2 = scanner.nextLine();
                if (!p1.equals(p2)) {
                    System.err.println("[ERROR] Passwords do not match.");
                    System.exit(3);
                }
                password = p1.toCharArray();
            }

            if (password == null || password.length < 8) {
                System.err.println("[ERROR] Password must be at least 8 characters long.");
                System.exit(4);
            }

            System.out.println("[*] Hashing password with PBKDF2WithHmacSHA256 (310,000 iterations)...");
            PasswordHasher.HashResult hash = PasswordHasher.hashPassword(password);
            java.util.Arrays.fill(password, '\0'); // Wipe plaintext memory

            User owner = new User();
            owner.setUsername(username);
            owner.setEmail(email.toLowerCase());
            owner.setPasswordHash(hash.getHashBase64());
            owner.setPasswordSalt(hash.getSaltBase64());
            owner.setRole("OWNER");
            owner.setActive(true);

            System.out.println("[*] Creating owner record in database...");
            User created = userDAO.createUser(owner);

            System.out.println("[*] Adopting unowned legacy records...");
            int adoptedCount = userDAO.adoptLegacyRecords(created.getUserId());

            // Verification
            User verified = userDAO.getUserById(created.getUserId());
            int newOwnerCount = userDAO.getOwnerCount();

            if (verified != null && newOwnerCount == 1) {
                System.out.println("=================================================");
                System.out.println("          SETUP COMPLETED SUCCESSFULLY!          ");
                System.out.println("=================================================");
                System.out.println(" Owner ID      : " + created.getUserId());
                System.out.println(" Username      : " + created.getUsername());
                System.out.println(" Email         : " + created.getEmail());
                System.out.println(" Role          : " + created.getRole());
                System.out.println(" Legacy Assets : " + adoptedCount + " records adopted");
                System.out.println("=================================================");
            } else {
                System.err.println("[WARNING] Setup finished but state verification returned unexpected counts.");
            }

        } catch (Exception e) {
            System.err.println("[FATAL] Owner setup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(5);
        }
    }
}

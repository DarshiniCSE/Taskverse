package com.taskverse.dao;

import com.taskverse.model.User;
import java.sql.SQLException;

/**
 * UserDAO Interface
 * Defines persistence contract for User accounts, authentication data,
 * and legacy record adoption.
 */
public interface UserDAO {

    /**
     * Inserts a new user record.
     * @param user user entity with username, email, passwordHash, passwordSalt, role
     * @return User populated with generated userId
     * @throws SQLException on database error (e.g. duplicate username/email)
     */
    User createUser(User user) throws SQLException;

    /**
     * Retrieves a user by normalized email.
     * @param email user email address
     * @return User object or null if not found
     * @throws SQLException on database error
     */
    User getUserByEmail(String email) throws SQLException;

    /**
     * Retrieves a user by username.
     * @param username username string
     * @return User object or null if not found
     * @throws SQLException on database error
     */
    User getUserByUsername(String username) throws SQLException;

    /**
     * Retrieves a user by ID.
     * @param userId user ID
     * @return User object or null if not found
     * @throws SQLException on database error
     */
    User getUserById(int userId) throws SQLException;

    /**
     * Returns total count of users with role 'OWNER'.
     * @return count of owners
     * @throws SQLException on database error
     */
    int getOwnerCount() throws SQLException;

    /**
     * Adopts unowned legacy tasks, notes, categories, and task points,
     * assigning them to the specified owner user ID.
     * @param ownerUserId owner user ID
     * @return total number of adopted records
     * @throws SQLException on database error
     */
    int adoptLegacyRecords(int ownerUserId) throws SQLException;
}

package com.example.sis.util;

/**
 * Constants used across test classes
 */
public class TestConstants {
    
    // Test Role IDs
    public static final Integer TEST_SUPER_ADMIN_ROLE_ID = 1;
    public static final Integer TEST_CENTER_ADMIN_ROLE_ID = 2;
    public static final Integer TEST_TEACHER_ROLE_ID = 3;
    public static final Integer TEST_STUDENT_ROLE_ID = 4;
    public static final Integer TEST_INACTIVE_ROLE_ID = 5;
    
    // Test Role Codes
    public static final String TEST_ROLE_CODE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String TEST_ROLE_CODE_CENTER_ADMIN = "CENTER_ADMIN";
    public static final String TEST_ROLE_CODE_TEACHER = "TEACHER";
    public static final String TEST_ROLE_CODE_STUDENT = "STUDENT";
    
    // Test Permission IDs
    public static final Integer TEST_PERMISSION_READ_STUDENT = 1;
    public static final Integer TEST_PERMISSION_CREATE_STUDENT = 2;
    public static final Integer TEST_PERMISSION_UPDATE_STUDENT = 3;
    public static final Integer TEST_PERMISSION_DELETE_STUDENT = 4;
    public static final Integer TEST_PERMISSION_READ_ROLE = 5;
    public static final Integer TEST_PERMISSION_CREATE_ROLE = 6;
    
    // Test User IDs
    public static final Integer TEST_USER_SUPER_ADMIN = 1;
    public static final Integer TEST_USER_CENTER_ADMIN = 2;
    public static final Integer TEST_USER_TEACHER = 3;
    public static final Integer TEST_USER_STUDENT = 4;
    public static final Integer TEST_USER_REGULAR = 5;
    
    // Test Center IDs
    public static final Integer TEST_CENTER_ID_1 = 1;
    public static final Integer TEST_CENTER_ID_2 = 2;
    
    // Test User Usernames
    public static final String TEST_USERNAME_SUPER_ADMIN = "superadmin";
    public static final String TEST_USERNAME_CENTER_ADMIN = "centeradmin";
    public static final String TEST_USERNAME_TEACHER = "teacher01";
    public static final String TEST_USERNAME_STUDENT = "student01";
    
    // Mock JWT Subject
    public static final String TEST_JWT_SUBJECT = "test-user";
    
    // Other constants
    public static final String TEST_ASSIGNED_BY = "test-system";
    
    private TestConstants() {
        // Utility class
    }
}

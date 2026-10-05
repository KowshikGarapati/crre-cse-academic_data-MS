package com.crre.academicdms;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    @Test
    void generateAndVerifyHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String adminPass = "Admin@123";
        String studentPass = "Student@123";
        String facultyPass = "Faculty@123";

        String adminHash = encoder.encode(adminPass);
        String studentHash = encoder.encode(studentPass);
        String facultyHash = encoder.encode(facultyPass);

        System.out.println("ADMIN_HASH=" + adminHash);
        System.out.println("STUDENT_HASH=" + studentHash);
        System.out.println("FACULTY_HASH=" + facultyHash);
        assertTrue(encoder.matches(adminPass, adminHash));
        assertTrue(encoder.matches(studentPass, studentHash));
        assertTrue(encoder.matches(facultyPass, facultyHash));
    }
}

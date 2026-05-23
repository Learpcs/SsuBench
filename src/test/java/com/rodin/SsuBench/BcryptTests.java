package com.rodin.SsuBench;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BcryptTests {

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    private final int MAX_PASSWORD_SIZE = 72;
    private final int BCRYPT_OUTPUT_SIZE = 60;

    String testCase(String password) {
        return bCryptPasswordEncoder.encode(password);
    }

    @Test
    public void maxPasswordValidLengthTest() {
        testCase("a".repeat(MAX_PASSWORD_SIZE));
    }

    @Test
    public void minPasswordLengthTest() {
        assertThrows(IllegalArgumentException.class, () -> testCase("a".repeat(MAX_PASSWORD_SIZE + 1)));
    }

    @RepeatedTest(5)
    public void OutputTestSize() {
        String randomString = UUID.randomUUID().toString();
        assertEquals(testCase(randomString).length(), BCRYPT_OUTPUT_SIZE);
    }

    @RepeatedTest(5)
    public void AllAsciiTest() {
        String randomString = UUID.randomUUID().toString();
        assertTrue(StringUtils.isAsciiPrintable(testCase(randomString)));
    }

    @Test
    public void TestIfSpaceIsPrintable() {
        assertTrue(StringUtils.isAsciiPrintable(" ".repeat(MAX_PASSWORD_SIZE)));
    }
}

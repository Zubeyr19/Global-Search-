package com.globalsearch.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordValidator
 */
class PasswordValidatorTest {

    @Test
    @DisplayName("Should accept strong valid password")
    void testStrongValidPassword() {
        String strongPassword = "MyP@ssw0rd123!";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(strongPassword);

        assertNotNull(strength);
        assertTrue(strength.getScore() >= 60, "Strong password should score at least 60");
    }

    @Test
    @DisplayName("Should reject password with no uppercase")
    void testNoUppercase() {
        String password = "myp@ssw0rd123!";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        assertTrue(strength.getScore() < 80, "Password without uppercase should have lower score");
        assertTrue(strength.getSuggestions().contains("uppercase") ||
                   strength.getSuggestions().isEmpty());
    }

    @Test
    @DisplayName("Should reject password with no lowercase")
    void testNoLowercase() {
        String password = "MYP@SSW0RD123!";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        assertTrue(strength.getScore() < 80);
    }

    @Test
    @DisplayName("Should reject password with no digit")
    void testNoDigit() {
        String password = "MyP@ssword!";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        assertTrue(strength.getScore() < 80);
    }

    @Test
    @DisplayName("Should reject password with no special character")
    void testNoSpecialCharacter() {
        String password = "MyPassword123";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        assertTrue(strength.getScore() < 80);
    }

    @Test
    @DisplayName("Should reject password that is too short")
    void testTooShort() {
        String password = "MyP@s1";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        assertTrue(strength.getScore() < 40, "Short password should have low score");
    }

    @Test
    @DisplayName("Should reject common passwords")
    void testCommonPassword() {
        String[] commonPasswords = {"password123", "admin123", "12345678"};

        for (String password : commonPasswords) {
            PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);
            assertTrue(strength.getScore() < 60, "Common password should have lower score: " + password);
        }
    }

    @Test
    @DisplayName("Should detect sequential characters")
    void testSequentialCharacters() {
        String password = "MyP@ssw0rd123abc";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        // Password with sequential characters should get penalty
        assertTrue(strength.getScore() < 100);
    }

    @Test
    @DisplayName("Should detect repeated characters")
    void testRepeatedCharacters() {
        String password = "MyP@sssw0rd111";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        // Password with repeated characters should get penalty
        assertTrue(strength.getScore() < 100);
    }

    @Test
    @DisplayName("Should handle null password")
    void testNullPassword() {
        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(null);

        assertNotNull(strength);
        assertEquals(0, strength.getScore());
        assertEquals("Very Weak", strength.getStrength());
    }

    @Test
    @DisplayName("Should handle empty password")
    void testEmptyPassword() {
        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength("");

        assertNotNull(strength);
        assertEquals(0, strength.getScore());
        assertEquals("Very Weak", strength.getStrength());
    }

    @Test
    @DisplayName("Should rate very strong password highly")
    void testVeryStrongPassword() {
        String veryStrongPassword = "Xk9#mP2$nQ7@vL5!wR8&";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(veryStrongPassword);

        assertNotNull(strength);
        assertTrue(strength.getScore() >= 70, "Very strong password should score highly");
        assertTrue(strength.getStrength().equals("Strong") ||
                   strength.getStrength().equals("Very Strong"));
    }

    @Test
    @DisplayName("Should provide appropriate strength labels")
    void testStrengthLabels() {
        // Very Weak
        assertEquals("Very Weak", PasswordValidator.calculateStrength("abc").getStrength());

        // Weak
        PasswordValidator.PasswordStrength weak = PasswordValidator.calculateStrength("abcdefgh");
        assertTrue(weak.getStrength().equals("Weak") || weak.getStrength().equals("Very Weak"));

        // Strong
        String strongPass = "MyP@ssw0rd123!Long";
        assertTrue(PasswordValidator.calculateStrength(strongPass).getScore() >= 60);
    }

    @Test
    @DisplayName("Should give suggestions for weak passwords")
    void testPasswordSuggestions() {
        String weakPassword = "abc123";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(weakPassword);

        assertNotNull(strength.getSuggestions());
        assertFalse(strength.getSuggestions().isEmpty());
    }

    @Test
    @DisplayName("Should accept passwords with various special characters")
    void testVariousSpecialCharacters() {
        String[] validPasswords = {
            "MyP@ssw0rd!",
            "MyP#ssw0rd1",
            "MyP$ssw0rd1",
            "MyP%ssw0rd1",
            "MyP&ssw0rd1",
            "MyP*ssw0rd1"
        };

        for (String password : validPasswords) {
            PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);
            assertTrue(strength.getScore() > 30, "Password with special char should have decent score: " + password);
        }
    }

    @Test
    @DisplayName("Should penalize passwords with keyboard patterns")
    void testKeyboardPatterns() {
        String password = "qwertyuiop123!A";

        PasswordValidator.PasswordStrength strength = PasswordValidator.calculateStrength(password);

        // Keyboard pattern should reduce score
        assertTrue(strength.getScore() < 100);
    }

    @Test
    @DisplayName("Should reward unique character diversity")
    void testCharacterDiversity() {
        String diversePassword = "A1b2C3d4E5f6!@#$";
        String lessNDiv密码ePassword = "AAAA1111!!!!";

        PasswordValidator.PasswordStrength diverse = PasswordValidator.calculateStrength(diversePassword);
        PasswordValidator.PasswordStrength lessDiverse = PasswordValidator.calculateStrength(lessDivPassword);

        assertTrue(diverse.getScore() > lessDiverse.getScore(),
                   "More diverse password should score higher");
    }

    @Test
    @DisplayName("Should reward longer passwords")
    void testPasswordLength() {
        String shortPassword = "MyP@ss1!";
        String longPassword = "MyP@ssw0rdIsVeryLong123!";

        PasswordValidator.PasswordStrength shortStrength = PasswordValidator.calculateStrength(shortPassword);
        PasswordValidator.PasswordStrength longStrength = PasswordValidator.calculateStrength(longPassword);

        assertTrue(longStrength.getScore() > shortStrength.getScore(),
                   "Longer password should score higher");
    }
}

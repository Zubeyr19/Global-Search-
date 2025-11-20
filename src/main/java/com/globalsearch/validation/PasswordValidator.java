package com.globalsearch.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password validator that enforces strong password policies
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character (@$!%*?&)
 * - No common passwords
 */
@Slf4j
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    // Common weak passwords to block
    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "password123", "12345678", "qwerty", "abc123",
        "letmein", "welcome", "monkey", "dragon", "master",
        "admin", "admin123", "root", "toor", "pass"
    );

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            addConstraintViolation(context, "Password cannot be null");
            return false;
        }

        List<String> errors = new ArrayList<>();

        // Check length
        if (password.length() < MIN_LENGTH) {
            errors.add(String.format("Password must be at least %d characters long", MIN_LENGTH));
        }

        if (password.length() > MAX_LENGTH) {
            errors.add(String.format("Password must not exceed %d characters", MAX_LENGTH));
        }

        // Check for uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        // Check for lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            errors.add("Password must contain at least one special character (@$!%*?&#^()_+-=[]{};\\':\"|,.<>/?)");
        }

        // Check against common passwords
        String lowerPassword = password.toLowerCase();
        for (String commonPassword : COMMON_PASSWORDS) {
            if (lowerPassword.contains(commonPassword)) {
                errors.add("Password contains commonly used words or patterns");
                break;
            }
        }

        // Check for sequential characters (123, abc, etc.)
        if (containsSequentialCharacters(password)) {
            errors.add("Password contains sequential characters");
        }

        // Check for repeated characters (aaa, 111, etc.)
        if (containsRepeatedCharacters(password, 3)) {
            errors.add("Password contains too many repeated characters");
        }

        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            for (String error : errors) {
                addConstraintViolation(context, error);
            }
            return false;
        }

        return true;
    }

    /**
     * Calculate password strength score (0-100)
     */
    public static PasswordStrength calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(0, "Very Weak", "Password is empty");
        }

        int score = 0;
        List<String> suggestions = new ArrayList<>();

        // Length scoring (max 25 points)
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 5;

        // Character variety (max 40 points)
        if (UPPERCASE_PATTERN.matcher(password).find()) {
            score += 10;
        } else {
            suggestions.add("Add uppercase letters");
        }

        if (LOWERCASE_PATTERN.matcher(password).find()) {
            score += 10;
        } else {
            suggestions.add("Add lowercase letters");
        }

        if (DIGIT_PATTERN.matcher(password).find()) {
            score += 10;
        } else {
            suggestions.add("Add numbers");
        }

        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            score += 10;
        } else {
            suggestions.add("Add special characters");
        }

        // Complexity bonuses (max 35 points)
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= 8) score += 10;
        if (uniqueChars >= 12) score += 10;
        if (uniqueChars >= 16) score += 5;

        // Penalty for common patterns
        if (containsSequentialCharacters(password)) {
            score -= 10;
            suggestions.add("Avoid sequential characters");
        }

        if (containsRepeatedCharacters(password, 3)) {
            score -= 10;
            suggestions.add("Avoid repeated characters");
        }

        // Check against common passwords
        String lowerPassword = password.toLowerCase();
        for (String commonPassword : COMMON_PASSWORDS) {
            if (lowerPassword.contains(commonPassword)) {
                score -= 20;
                suggestions.add("Avoid common words");
                break;
            }
        }

        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));

        String strength;
        if (score >= 80) strength = "Very Strong";
        else if (score >= 60) strength = "Strong";
        else if (score >= 40) strength = "Medium";
        else if (score >= 20) strength = "Weak";
        else strength = "Very Weak";

        return new PasswordStrength(score, strength, String.join(", ", suggestions));
    }

    private static boolean containsSequentialCharacters(String password) {
        String lower = password.toLowerCase();
        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);

            // Check for sequential numbers or letters
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
            // Check for reverse sequential
            if (c2 == c1 - 1 && c3 == c2 - 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsRepeatedCharacters(String password, int maxRepeats) {
        for (int i = 0; i <= password.length() - maxRepeats; i++) {
            char c = password.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < maxRepeats; j++) {
                if (password.charAt(i + j) != c) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) {
                return true;
            }
        }
        return false;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }

    /**
     * Password strength result
     */
    public static class PasswordStrength {
        private final int score;
        private final String strength;
        private final String suggestions;

        public PasswordStrength(int score, String strength, String suggestions) {
            this.score = score;
            this.strength = strength;
            this.suggestions = suggestions;
        }

        public int getScore() {
            return score;
        }

        public String getStrength() {
            return strength;
        }

        public String getSuggestions() {
            return suggestions;
        }
    }
}

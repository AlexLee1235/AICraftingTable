package com.watermelon0117.aicraft.gpt;

final class ItemIdSanitizer {
    private ItemIdSanitizer() {
    }

    static String sanitize(String id) {
        if (id == null) {
            return null;
        }

        StringBuilder sanitized = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char character = id.charAt(i);
            if (isAllowed(character)) {
                sanitized.append(character);
            }
        }
        return sanitized.toString();
    }

    private static boolean isAllowed(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == ' '
                || character == '.'
                || character == '+'
                || character == '-';
    }
}

package com.nspay.services;

import java.security.SecureRandom;

/** Generates valid Romanian-format IBANs for this demo. */
public final class IbanGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] BANK_CODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private IbanGenerator() { }

    public static String generate() {
        StringBuilder bban = new StringBuilder("RO");
        bban.append("00");
        for (int i = 0; i < 4; i++) bban.append(BANK_CODE[RANDOM.nextInt(BANK_CODE.length)]);
        for (int i = 0; i < 16; i++) bban.append(RANDOM.nextInt(10));
        int checkDigits = 98 - mod97(bban.substring(4) + "RO00");
        bban.replace(2, 4, String.format("%02d", checkDigits));
        return bban.toString();
    }

    private static int mod97(String value) {
        int remainder = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String digits = Character.isLetter(c)
                    ? Integer.toString(Character.toUpperCase(c) - 'A' + 10)
                    : Character.toString(c);
            for (int j = 0; j < digits.length(); j++) {
                remainder = (remainder * 10 + digits.charAt(j) - '0') % 97;
            }
        }
        return remainder;
    }
}

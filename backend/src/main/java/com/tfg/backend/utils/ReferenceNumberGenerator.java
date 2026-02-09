package com.tfg.backend.utils;

import java.security.SecureRandom;

public class ReferenceNumberGenerator {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    //Reference number sample: REF-9X2A-M4J1
    public static String generateOrderReferenceNumber() {
        StringBuilder sb = new StringBuilder("OR-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public static String generateProductReferenceNumber() {
        StringBuilder sb = new StringBuilder("PR-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public static String generateShopReferenceNumber() {
        StringBuilder sb = new StringBuilder("TI-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append("-");
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}

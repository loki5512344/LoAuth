package dev.loki.loAuth.common.util;

import java.security.SecureRandom;

public final class CodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private CodeGenerator() {}

    public static String generate(final int length) {
        if (length < 4 || length > 16)
            throw new IllegalArgumentException("Code length must be between 4 and 16");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}

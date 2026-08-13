package com.sherwin.sherwinmart.util;

import org.mindrot.jbcrypt.BCrypt;

/** Password hashing helper — jBCrypt only, per Section 2 rule 2 (no plaintext, no MD5/SHA1). */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean matches(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }
}

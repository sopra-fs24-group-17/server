package ch.uzh.ifi.hase.soprafs24.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Service
public class PasswordService {

    private static String securePassword;
    private static final Random RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz?_%&/()[]*#";

    public String generateRandomPassword(int length) {
        StringBuilder randomPassword = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            randomPassword.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return randomPassword.toString();
    }

    public String securePassword(String password) {
        Argon2 argon2 = getArgon2Instance();
        try {
            return argon2.hash(2, 6123, 1, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }

    private Argon2 getArgon2Instance() {
        return Argon2Factory.create();
    }

    public boolean verifyPassword(String hash, String password) {
        Argon2 argon2 = getArgon2Instance();
        try {
            // Verify the password against the hash
            return argon2.verify(hash, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }
}

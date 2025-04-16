package dev.abhay7.skribbl.client.crypto;

// import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class PBKDF2Helper {
    public static final int SECURE_ITERATIONS = 1_000_000;

    public static SecretKey deriveKeyingMaterial(String password, byte[] salt, int iterations, int keyLength) {
        // Represents the initial keying material, like a user-entered password
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);

        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey result = secretKeyFactory.generateSecret(pbeKeySpec);
            
            // need to reformat for AES

            return new SecretKeySpec(result.getEncoded(), "AES");
        }
        catch (Exception e) {
            System.out.println("A fatal crypto error while deriving a key w/ PBKDF2 occurred: " + e.getMessage());
            System.exit(-1);
            return null;
        }
    }
}

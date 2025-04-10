package dev.abhay7.skribbl.client.Crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public final class AESGCMHelper {
    public static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    public static byte[] generateIV() {
        byte[] result = new byte[IV_LENGTH_BYTES];

        try {
            SecureRandom.getInstanceStrong().nextBytes(result);

            return result;
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("A fatal crypto error while deriving an IV for AES-GCM: " + e.getMessage());
            System.exit(-1);
            return null;
        }
    }

    public static byte[] encrypt(byte[] plainText, SecretKey key, byte[] iv, byte[] aad) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

            if (aad != null) {
                // Add optional additional authenticated data
                // This is not encrypted, but it IS checked for integrity
                // This MUST be called before cipher.update or doFinal
                cipher.updateAAD(aad);
            }
            
            return cipher.doFinal(plainText);
        }
        catch (Exception e) {
            System.out.println("A fatal crypto error while encrypting data with AES-GCM occurred: " + e.getMessage());
            System.exit(-1);
            return null;
        }   
    }

    public static byte[] decrypt(byte[] aad, byte[] iv, byte[] cipherText, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            if (aad != null) {
                // Add optional additional authenticated data
                // This is not encrypted, but it IS checked for integrity
                // This MUST be called before cipher.update or doFinal
                // It must be supplied during decryption, too
                cipher.updateAAD(aad);
            }
            
            return cipher.doFinal(cipherText);
        }
        catch (Exception e) {
            System.out.println("A fatal crypto error while encrypting data with AES-GCM occurred: " + e.getMessage());
            System.exit(-1);
            return null;
        }
    }
}

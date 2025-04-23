package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.security.auth.DestroyFailedException;

public class PWDSignalSingleRatchetCipherEngine extends PWDSignalCipherEngine {
    // The KDF algorithm we use
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    // Its standrad practice to have 32 zero bytes when u don't need a salt
    // 32 = length of SHA256 (basis of our HKDF)
    private static final byte[] HKDF_ZERO_SALT = new byte[32];

    // Need to separate the receiving and sending chains so they represent differnet domains
    // Send Chain means sending for the initiator (so everything's relative to initiator)
    private static final byte[] HKDF_SEND_CHAIN_INIT_INFO = "PWDSignalSingleRatchetCipherEngine_SendChainInit".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_RECEIVE_CHAIN_INIT_INFO = "PWDSignalSingleRatchetCipherEngine_ReceiveChainInit".getBytes(StandardCharsets.UTF_8);

    // The size of our chain key, in bytes
    private static final int CHAIN_KEY_SIZE = 32;

    // We take in a current chain key, and produce from that 2 keys: a new chain key and the message key
    // We need differnet info tags to separate domains
    private static final byte[] HKDF_RATCHET_STEP_CHAIN_KEY_INFO = "PWDSignalSingleRatchetCipherEngine_RatchetStepChainKey".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_RATCHET_STEP_MESSAGE_KEY_INFO = "PWDSignalSingleRatchetCipherEngine_RatchetStepMessageKey".getBytes(StandardCharsets.UTF_8);

    // Represents the length of a key for AES GCM in bytes (32 * 8 = 256 bits)
    private static final int AES_GCM_KEY_SIZE = 32;

    // Represents the size of the nonce for AES GCM in bytes (12 * 8 = 96 bits)
    private static final int AES_GCM_NONCE_SIZE = 12;

    // Represnts the length of the AES GCM tag in bits
    private static final int AES_GCM_TAG_SIZE_BITS = 128;

    // The current sending chain key
    private SecretKey sendChainKey;
    
    // The current receiving chain key
    private SecretKey receiveChainKey;

    // Source of entropy
    private final SecureRandom random;

    public PWDSignalSingleRatchetCipherEngine(BigInteger sharedSecretKey, boolean isConnectionInitiator) throws Exception {
        super(sharedSecretKey, isConnectionInitiator);

        // get our entropy source
        this.random = SecureRandom.getInstanceStrong();

        // Turn the shared secret key into bytes, and sort-of zero the BigInteger afterwards
        byte[] sharedSecretKeyBytes = sharedSecretKey.toByteArray();
        sharedSecretKey = BigInteger.ZERO;

        try {
            KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);

            // JPAKE shared secret keys are not uniform and aren't PRFs
            AlgorithmParameterSpec params = HKDFParameterSpec.ofExtract()
                                                            .addIKM(sharedSecretKeyBytes)
                                                            .addSalt(HKDF_ZERO_SALT)
                                                            .thenExpand(HKDF_SEND_CHAIN_INIT_INFO, CHAIN_KEY_SIZE);

            // Derive the send-chain key; its algorithm name will just be HKDF_ALGORITHM becuz i couldn't think of anything else that makes sense
            SecretKey sendChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);

            // reset the KDF
            hkdf = KDF.getInstance(HKDF_ALGORITHM);

            params = HKDFParameterSpec.ofExtract()
                                    .addIKM(sharedSecretKeyBytes)
                                    .addSalt(HKDF_ZERO_SALT)
                                    .thenExpand(HKDF_RECEIVE_CHAIN_INIT_INFO, CHAIN_KEY_SIZE);

            // Derive the receive chain key
            SecretKey receiveChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);
            
            // This is the only asymettric part of this algorithm
            // Sending chain key of initiator is the receiving chain key of the receiver
            if (isConnectionInitiator) {
                this.sendChainKey = sendChainKey;
                this.receiveChainKey = receiveChainKey;
            }
            else {
                this.sendChainKey = receiveChainKey;
                this.receiveChainKey = sendChainKey;
            }
        }
        finally {
            // When we're done, we can clear the raw keying material byte[]
            for (int i = 0; i < sharedSecretKeyBytes.length; ++i) sharedSecretKeyBytes[i] = 0;
        }
    }

    @Override
    public byte[] encryptSendPacket(byte[] message, int offset, int length) throws Exception {
        KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);

        AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(sendChainKey, HKDF_RATCHET_STEP_CHAIN_KEY_INFO, CHAIN_KEY_SIZE);
        SecretKey newSendChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);
        
        hkdf = KDF.getInstance(HKDF_ALGORITHM);
        params = HKDFParameterSpec.expandOnly(sendChainKey, HKDF_RATCHET_STEP_MESSAGE_KEY_INFO, AES_GCM_KEY_SIZE);
        SecretKey messageKey = hkdf.deriveKey("AES", params);

        // destroy the old chain key
        destroySecretKey(sendChainKey);
        // replace it with the new one
        sendChainKey = newSendChainKey;
        
        // generate random nonce
        byte[] nonce = new byte[AES_GCM_NONCE_SIZE];
        random.nextBytes(nonce);

        // encrypt with AES GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, nonce);
        
        cipher.init(Cipher.ENCRYPT_MODE, messageKey, gcmParameterSpec);

        // if (aad != null) {
        //     // Add optional additional authenticated data
        //     // This is not encrypted, but it IS checked for integrity
        //     // This MUST be called before cipher.update or doFinal
        //     cipher.updateAAD(aad);
        // }
        
        // encrypt our message
        byte[] cipherText = cipher.doFinal(message, offset, length);

        // message key no longer needed
        destroySecretKey(messageKey);

        // prefix the nonce
        byte[] result = new byte[cipherText.length + AES_GCM_NONCE_SIZE];
        System.arraycopy(nonce, 0, result, 0, AES_GCM_NONCE_SIZE);
        System.arraycopy(cipherText, 0, result, AES_GCM_NONCE_SIZE, cipherText.length);

        return result;
    }

    @Override
    public byte[] decryptReceivePacket(byte[] cipherText, int offset, int length) throws Exception {
        // must at least have the nonce in it
        if (length < AES_GCM_NONCE_SIZE) {
            throw new AEADBadTagException("Ciphertext too short");
        }

        KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);

        AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(receiveChainKey, HKDF_RATCHET_STEP_CHAIN_KEY_INFO, CHAIN_KEY_SIZE);
        SecretKey newReceiveChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);

        hkdf = KDF.getInstance(HKDF_ALGORITHM);
        params = HKDFParameterSpec.expandOnly(receiveChainKey, HKDF_RATCHET_STEP_MESSAGE_KEY_INFO, AES_GCM_KEY_SIZE);
        SecretKey messageKey = hkdf.deriveKey("AES", params);

        // old receive chain key no longer needed
        destroySecretKey(receiveChainKey);
        receiveChainKey = newReceiveChainKey;

        byte[] nonce = Arrays.copyOfRange(cipherText, offset, offset + AES_GCM_NONCE_SIZE);
        byte[] actualCipherText = Arrays.copyOfRange(cipherText, offset + AES_GCM_NONCE_SIZE, offset + length);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_SIZE_BITS, nonce);
            
            cipher.init(Cipher.DECRYPT_MODE, messageKey, gcmParameterSpec);

            // if (aad != null) {
            //     // Add optional additional authenticated data
            //     // This is not encrypted, but it IS checked for integrity
            //     // This MUST be called before cipher.update or doFinal
            //     // It must be supplied during decryption, too
            //     cipher.updateAAD(aad);
            // }
            
            return cipher.doFinal(actualCipherText);
        }
        finally {
            // always destroy the messageKey
            destroySecretKey(messageKey);
        }
    }

    private static void destroySecretKey(SecretKey key) {
        try {
            if (!key.isDestroyed()) {
                key.destroy();
            }
        }
        catch (DestroyFailedException e) {
            // ignore...
            // some SecretKey's don't implement the destroy method
            // another example of java being terrible...
        }
    }
}

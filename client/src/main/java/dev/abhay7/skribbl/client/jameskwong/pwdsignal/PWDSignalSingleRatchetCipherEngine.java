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

import dev.abhay7.skribbl.client.crypto.AESGCMHelper;

public class PWDSignalSingleRatchetCipherEngine extends PWDSignalCipherEngine {
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    // Its standrad practice to have 32 zero bytes when u don't need a salt
    // 32 = length of SHA256
    private static final byte[] HKDF_ZERO_SALT = new byte[32];

    // Need to separate the receiving and sending chains so they represent differnet domains
    // Send Chain means sending for the initiator (so everything's relative to initiator)
    private static final byte[] HKDF_SEND_CHAIN_INIT_INFO = "PWDSignalSingleRatchetCipherEngine_SendChainInit".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_RECEIVE_CHAIN_INIT_INFO = "PWDSignalSingleRatchetCipherEngine_ReceiveChainInit".getBytes(StandardCharsets.UTF_8);

    private static final int CHAIN_KEY_SIZE = 32;

    private static final byte[] HKDF_RATCHET_STEP_CHAIN_KEY_INFO = "PWDSignalSingleRatchetCipherEngine_RatchetStepChain".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_RATCHET_STEP_MESSAGE_KEY_INFO = "PWDSignalSingleRatchetCipherEngine_RatchetStepMessage".getBytes(StandardCharsets.UTF_8);

    private static final int AES_GCM_KEY_SIZE = 32;

    private static final int AES_GCM_NONCE_SIZE = 12; // bytes (96 bits)
    private static final int AES_GCM_TAG_LENGTH_BITS = 128; // bits

    private SecretKey sendChainKey;
    private SecretKey receiveChainKey;

    private final SecureRandom random;

    public PWDSignalSingleRatchetCipherEngine(BigInteger sharedSecretKey, boolean isConnectionInitiator) throws Exception {
        super(sharedSecretKey, isConnectionInitiator);

        this.random = SecureRandom.getInstanceStrong();

        byte[] sharedSecretKeyBytes = sharedSecretKey.toByteArray();
        sharedSecretKey = BigInteger.ZERO;

        try {
            KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);

            AlgorithmParameterSpec params = HKDFParameterSpec.ofExtract()
                                                            .addIKM(sharedSecretKeyBytes)
                                                            .addSalt(HKDF_ZERO_SALT)
                                                            .thenExpand(HKDF_SEND_CHAIN_INIT_INFO, CHAIN_KEY_SIZE);

            SecretKey sendChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);

            hkdf = KDF.getInstance(HKDF_ALGORITHM);

            params = HKDFParameterSpec.ofExtract()
                                    .addIKM(sharedSecretKeyBytes)
                                    .addSalt(HKDF_ZERO_SALT)
                                    .thenExpand(HKDF_RECEIVE_CHAIN_INIT_INFO, CHAIN_KEY_SIZE);

            SecretKey receiveChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);
            
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

        sendChainKey.destroy();
        sendChainKey = newSendChainKey;

        byte[] nonce = new byte[AES_GCM_NONCE_SIZE];
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, nonce);
        
        cipher.init(Cipher.ENCRYPT_MODE, messageKey, gcmParameterSpec);

        // if (aad != null) {
        //     // Add optional additional authenticated data
        //     // This is not encrypted, but it IS checked for integrity
        //     // This MUST be called before cipher.update or doFinal
        //     cipher.updateAAD(aad);
        // }
        
        byte[] cipherText = cipher.doFinal(message, offset, length);
        messageKey.destroy();

        byte[] result = new byte[cipherText.length + AES_GCM_NONCE_SIZE];
        System.arraycopy(nonce, 0, result, 0, AES_GCM_NONCE_SIZE);
        System.arraycopy(cipherText, 0, result, AES_GCM_NONCE_SIZE, cipherText.length);

        return result;
    }

    @Override
    public byte[] decryptReceivePacket(byte[] cipherText, int offset, int length) throws Exception {
        if (length < AES_GCM_NONCE_SIZE) {
            throw new AEADBadTagException("Ciphertext too short");
        }

        KDF hkdf = KDF.getInstance(HKDF_ALGORITHM);

        AlgorithmParameterSpec params = HKDFParameterSpec.expandOnly(receiveChainKey, HKDF_RATCHET_STEP_CHAIN_KEY_INFO, CHAIN_KEY_SIZE);
        SecretKey newReceiveChainKey = hkdf.deriveKey(HKDF_ALGORITHM, params);

        hkdf = KDF.getInstance(HKDF_ALGORITHM);
        params = HKDFParameterSpec.expandOnly(receiveChainKey, HKDF_RATCHET_STEP_MESSAGE_KEY_INFO, AES_GCM_KEY_SIZE);
        SecretKey messageKey = hkdf.deriveKey("AES", params);

        receiveChainKey.destroy();
        receiveChainKey = newReceiveChainKey;

        byte[] nonce = Arrays.copyOfRange(cipherText, offset, offset + AES_GCM_NONCE_SIZE);
        byte[] actualCipherText = Arrays.copyOfRange(cipherText, offset + AES_GCM_NONCE_SIZE, offset + length);

        try {
            byte[] result = AESGCMHelper.decrypt(null, nonce, actualCipherText, messageKey);
            return result;
        }
        finally {
            // always destroy the messageKey
            messageKey.destroy();
        }
    }   
}

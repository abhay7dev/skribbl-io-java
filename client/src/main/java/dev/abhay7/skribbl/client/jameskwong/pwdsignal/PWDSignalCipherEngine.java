package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

import java.math.BigInteger;

public abstract class PWDSignalCipherEngine {
    public PWDSignalCipherEngine(BigInteger sharedSecretKey, boolean isConnectionInitiator) throws Exception {

    }

    public abstract byte[] encryptSendPacket(byte[] message, int offset, int length) throws Exception;

    public abstract byte[] decryptReceivePacket(byte[] cipherText, int offset, int length) throws Exception;

    public byte[] encryptSendPacket(byte[] message) throws Exception {
        return encryptSendPacket(message, 0, message.length);
    }

    public byte[] decryptReceivePacket(byte[] cipherText) throws Exception {
        return decryptReceivePacket(cipherText, 0, cipherText.length);
    }
}

package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.json.JSONObject;

public class PWDSignalSession {
    private static final JPAKEPrimeOrderGroup jpakePrimeOrderGroup = JPAKEPrimeOrderGroups.NIST_3072;

    private PWDSignalSessionState state;

    private final SecureRandom random;

    private final String jpakeUserID;
    private final JPAKEParticipant jpake;

    public PWDSignalSession(String password) throws NoSuchAlgorithmException {
        random = SecureRandom.getInstanceStrong();

        jpakeUserID = nextHexString(random, 32);
        log("JPAKE User Identifier: " + jpakeUserID);

        jpake = new JPAKEParticipant(jpakeUserID, password.toCharArray(), jpakePrimeOrderGroup, SHA256Digest.newInstance(), random);
        
        state = PWDSignalSessionState.INITIALIZED;
    }

    public byte[] createPayload1() throws IllegalStateException {
        validateState(PWDSignalSessionState.PAYLOAD_1_CREATED);

        JPAKERound1Payload payload = jpake.createRound1PayloadToSend();
        
        JSONObject json = new JSONObject();
        json.put("participantId", payload.getParticipantId());
        json.put("gx1", payload.getGx1());
        json.put("gx2", payload.getGx2());
        json.put("kpx1", payload.getKnowledgeProofForX1());
        json.put("kpx2", payload.getKnowledgeProofForX2());

        byte[] result = encodeJSONObject(json);

        state = PWDSignalSessionState.PAYLOAD_1_CREATED;
        
        return result;
    }

    // MARK: SecureRandom helpers

    private static String nextHexString(SecureRandom random, int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    // MARK: Misc helpers

    private void validateState(PWDSignalSessionState newState) {
        if (state.getNumericalValue() >= newState.getNumericalValue()) {
            throw new IllegalStateException("We're trying to do an operation which moves us into " + newState.toString() + ", but we're already at " + this.state + " which is ahead!");
        }
    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static byte[] encodeJSONObject(JSONObject object) {
        String jsonStr = object.toString();
        byte[] jsonBytes = jsonStr.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[jsonBytes.length + 4];
        writeIntLE(jsonBytes.length, result, 0);
        System.arraycopy(jsonBytes, 0, result, 4, jsonBytes.length);

        return result;
    }


    // These 2 helpers methods are not my code; It is from the internet
    /**
     * Encodes a 32-bit integer into the byte array in little-endian order,
     * starting at the specified offset.
     *
     * @param value  the integer value to encode
     * @param buffer the destination byte array
     * @param offset the starting position in the array to write the bytes
     * @throws IllegalArgumentException if the buffer is too small
     */
    private static void writeIntLE(int value, byte[] buffer, int offset) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (offset < 0 || offset + 4 > buffer.length) {
            throw new IllegalArgumentException("Buffer too small or invalid offset: " + offset);
        }

        // Write the least significant byte first
        buffer[offset]     = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /**
     * Decodes a 32-bit integer from the byte array in little-endian order,
     * starting at the specified offset.
     *
     * @param buffer the source byte array
     * @param offset the starting position in the array to read the bytes
     * @return the decoded integer value
     * @throws IllegalArgumentException if the buffer is too small
     */
    private static int readIntLE(byte[] buffer, int offset) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (offset < 0 || offset + 4 > buffer.length) {
            throw new IllegalArgumentException("Buffer too small or invalid offset: " + offset);
        }

        // Combine bytes starting from least significant
        return ((buffer[offset] & 0xFF)) |
               ((buffer[offset + 1] & 0xFF) << 8) |
               ((buffer[offset + 2] & 0xFF) << 16) |
               ((buffer[offset + 3] & 0xFF) << 24);
    }
}

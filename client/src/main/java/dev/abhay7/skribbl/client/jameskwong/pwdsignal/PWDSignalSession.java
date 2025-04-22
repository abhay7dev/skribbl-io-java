package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound3Payload;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.json.JSONArray;
import org.json.JSONObject;

public class PWDSignalSession {
    // 128 bits of security
    private static final JPAKEPrimeOrderGroup jpakePrimeOrderGroup = JPAKEPrimeOrderGroups.NIST_3072;

    // State machine
    private PWDSignalSessionState state;

    // Source of entropy
    private final SecureRandom random;

    // Participant ID for JPAKE
    private final String jpakeUserID;
    private final JPAKEParticipant jpake;

    // The raw, shared secret key derived immediately after JPAKE
    // Do not use this as raw keying material--run it through a KDF first
    private BigInteger sharedSecretKey;

    public PWDSignalSession(String password) throws NoSuchAlgorithmException {
        random = SecureRandom.getInstanceStrong();

        jpakeUserID = nextHexString(random, 32);
        log("JPAKE User Identifier: " + jpakeUserID);

        jpake = new JPAKEParticipant(jpakeUserID, password.toCharArray(), jpakePrimeOrderGroup, SHA256Digest.newInstance(), random);

        sharedSecretKey = null;

        state = PWDSignalSessionState.INITIALIZED;
    }

    // Returns the state of the PWDSignal session
    public PWDSignalSessionState getState() {
        return state;
    }

    public byte[] createPayload1() throws IllegalStateException {
        if (state.getNumericalValue() < PWDSignalSessionState.INITIALIZED.getNumericalValue()) throw new IllegalStateException("Need to have initializeed the session before creating payload 1");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_1_CREATED.getNumericalValue()) {
            throw new IllegalStateException("Already created payload 1");
        }

        JPAKERound1Payload payload = jpake.createRound1PayloadToSend();
        
        JSONObject json = new JSONObject();

        json.put("participantId", payload.getParticipantId());
        putBigInteger(json, "gx1", payload.getGx1());
        putBigInteger(json, "gx2", payload.getGx2());
        putBigIntegerArray(json, "kpx1", payload.getKnowledgeProofForX1());
        putBigIntegerArray(json, "kpx2", payload.getKnowledgeProofForX2());

        byte[] result = encodeJSONObject(json);

        state = PWDSignalSessionState.PAYLOAD_1_CREATED;    
        return result;
    }

    public void acceptPayload1(byte[] data, int offset) throws Exception {
        if (state.getNumericalValue() < PWDSignalSessionState.PAYLOAD_1_CREATED.getNumericalValue()) throw new IllegalStateException("Need to have created payload1 before receiving one");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_1_VALIDATED.getNumericalValue()) {
            throw new IllegalStateException("Already accepted payload 1 before");
        }     

        try {
            JSONObject object = decodeJSONObject(data, offset);

            String participantId = object.getString("participantId");
            BigInteger gx1 = getBigInteger(object, "gx1");
            BigInteger gx2 = getBigInteger(object, "gx2");
            BigInteger[] kpx1 = getBigIntegerArray(object, "kpx1");
            BigInteger[] kpx2 = getBigIntegerArray(object, "kpx2");

            JPAKERound1Payload payload = new JPAKERound1Payload(participantId, gx1, gx2, kpx1, kpx2);
            jpake.validateRound1PayloadReceived(payload);

            // success!
            state = PWDSignalSessionState.PAYLOAD_1_VALIDATED;
        }
        catch (Exception exception) {
            // failure :(
            state = PWDSignalSessionState.PAYLOAD_1_FAILED;

            throw exception;
        }
    }

    public byte[] createPayload2() throws IllegalStateException {
        if (state.getNumericalValue() < PWDSignalSessionState.PAYLOAD_1_VALIDATED.getNumericalValue())
            throw new IllegalStateException("Need to have validated payload 1 before creating payload 2");
        if (state.getNumericalValue() == PWDSignalSessionState.PAYLOAD_1_FAILED.getNumericalValue())
            throw new IllegalStateException("Payload 1 validation failed; cannot create payload 2");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_2_CREATED.getNumericalValue())
            throw new IllegalStateException("Already created payload 2");
    
        JPAKERound2Payload payload = jpake.createRound2PayloadToSend();
    
        JSONObject json = new JSONObject();
        json.put("participantId", payload.getParticipantId());
        putBigInteger(json, "a", payload.getA());
        putBigIntegerArray(json, "kpx2s", payload.getKnowledgeProofForX2s());
    
        byte[] result = encodeJSONObject(json);

        state = PWDSignalSessionState.PAYLOAD_2_CREATED;

        return result;
    }
    
    public void acceptPayload2(byte[] data, int offset) throws Exception {
        if (state.getNumericalValue() < PWDSignalSessionState.PAYLOAD_2_CREATED.getNumericalValue())
            throw new IllegalStateException("Need to have created payload2 before receiving one");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_2_VALIDATED.getNumericalValue())
            throw new IllegalStateException("Already accepted payload 2 before");
    
        try {
            JSONObject object = decodeJSONObject(data, offset);
            String participantId = object.getString("participantId");
            BigInteger a = getBigInteger(object, "a");
            BigInteger[] kpx2s = getBigIntegerArray(object, "kpx2s");
    
            JPAKERound2Payload payload = new JPAKERound2Payload(participantId, a, kpx2s);
            jpake.validateRound2PayloadReceived(payload);
    
            sharedSecretKey = jpake.calculateKeyingMaterial();
            state = PWDSignalSessionState.PAYLOAD_2_VALIDATED;
        } catch (Exception e) {
            state = PWDSignalSessionState.PAYLOAD_2_FAILED;
            throw e;
        }
    }
    
    public byte[] createPayload3() throws IllegalStateException {
        if (state.getNumericalValue() < PWDSignalSessionState.PAYLOAD_2_VALIDATED.getNumericalValue())
            throw new IllegalStateException("Need to have validated payload 2 before creating payload 3");
        if (state.getNumericalValue() == PWDSignalSessionState.PAYLOAD_2_FAILED.getNumericalValue())
            throw new IllegalStateException("Payload 2 validation failed; cannot create payload 3");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_3_CREATED.getNumericalValue())
            throw new IllegalStateException("Already created payload 3");
    
        JPAKERound3Payload payload = jpake.createRound3PayloadToSend(sharedSecretKey);
    
        JSONObject json = new JSONObject();
        json.put("participantId", payload.getParticipantId());
        putBigInteger(json, "macTag", payload.getMacTag());
    
        byte[] result = encodeJSONObject(json);

        state = PWDSignalSessionState.PAYLOAD_3_CREATED;

        return result;
    }
    
    public void acceptPayload3(byte[] data, int offset) throws Exception {
        if (state.getNumericalValue() < PWDSignalSessionState.PAYLOAD_3_CREATED.getNumericalValue())
            throw new IllegalStateException("Need to have created payload3 before receiving one");
        if (state.getNumericalValue() >= PWDSignalSessionState.PAYLOAD_3_VALIDATED.getNumericalValue())
            throw new IllegalStateException("Already accepted payload 3 before");
    
        try {
            JSONObject object = decodeJSONObject(data, offset);
            String participantId = object.getString("participantId");
            BigInteger macTag = getBigInteger(object, "macTag");
    
            JPAKERound3Payload payload = new JPAKERound3Payload(participantId, macTag);
            jpake.validateRound3PayloadReceived(payload, sharedSecretKey);
    
            state = PWDSignalSessionState.PAYLOAD_3_VALIDATED;
        } catch (Exception e) {
            state = PWDSignalSessionState.PAYLOAD_3_FAILED;
            throw e;
        }
    }

    // MARK: Misc helpers

    // Don't log sensitive info...
    private static void log(String str) {
        if (true) {
            System.out.println(str);
        }
        else {
            // do nothing; don't log
        }
    }

    // Returns a hex string representing numBytes of data
    private static String nextHexString(SecureRandom random, int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    // MARK: Byte buffer helpers

    // Decodes a JSONObject from a length-prefixed array of bytes
    private static JSONObject decodeJSONObject(byte[] data, int offset) throws Exception {
        int length = readIntLE(data, offset);
        if (length > 1024 * 1024) {
            throw new Exception("packet length of " + length + " is over the limit");
        }

        if (offset + 4 + length > data.length) {
            throw new Exception("Provided data array does not have the entirety of the JSONObject");
        }
        
        String jsonString = new String(data, offset + 4, length, StandardCharsets.UTF_8);

        return new JSONObject(jsonString);
    }

    // Returns a byte[] with the JSONObject encoded into it (prefixed with 4 byte length integer)
    private static byte[] encodeJSONObject(JSONObject object) {
        String jsonStr = object.toString();
        byte[] jsonBytes = jsonStr.getBytes(StandardCharsets.UTF_8);

        // have to do this because IDK how big the JSON data will be

        byte[] result = new byte[jsonBytes.length + 4];
        writeIntLE(jsonBytes.length, result, 0);
        
        System.arraycopy(jsonBytes, 0, result, 4, jsonBytes.length);

        return result;
    }

    // Helpers for big integer and json
    private static void putBigInteger(JSONObject obj, String key, BigInteger value) {
        if (value == null) {
            throw new NullPointerException("Cannot put null BigInteger for " + key);
        }

        // explicitly store as string
        obj.put(key, value.toString());
    }

    private static BigInteger getBigInteger(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) {
            throw new NullPointerException("Missing or null BigInteger for " + key);
        }

        return new BigInteger(obj.getString(key));
    }

    private static void putBigIntegerArray(JSONObject obj, String key, BigInteger[] values) {
        if (values == null) {
            throw new NullPointerException("Cannot put null BigInteger array for " + key);
        }

        JSONArray array = new JSONArray();

        for (int i = 0; i < values.length; i++) {
            BigInteger bi = values[i];
            if (bi == null) {
                throw new NullPointerException("BigInteger array contains null value at index " + i);
            }

            // turn to string always
            array.put(bi.toString());
        }

        // finally add the array to the JSONObject
        obj.put(key, array);
    }

    private static BigInteger[] getBigIntegerArray(JSONObject obj, String key) {
        if (!obj.has(key) || obj.isNull(key)) {
            throw new NullPointerException("Missing or null BigInteger array for " + key);
        }

        JSONArray array = obj.getJSONArray(key);
        BigInteger[] result = new BigInteger[array.length()];

        for (int i = 0; i < result.length; i++) {
            if (array.isNull(i)) {
                throw new NullPointerException("Null BigInteger at index " + i);
            }

            result[i] = new BigInteger(array.getString(i));
        }

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

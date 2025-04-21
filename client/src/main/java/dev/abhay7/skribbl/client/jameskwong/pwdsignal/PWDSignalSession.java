package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.digests.SHA256Digest;

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

    

    // MARK: SecureRandom helpers

    private static String nextHexString(SecureRandom random, int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);

        return HexFormat.of().formatHex(bytes);
    }

    // MARK: Misc helpers

    private static void log(String str) {
        System.out.println(str);
    }
}

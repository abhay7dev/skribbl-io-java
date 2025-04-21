package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

public enum PWDSignalSessionState {
    INITIALIZED(0),

    PAYLOAD_1_CREATED (10),
    PAYLOAD_1_VALIDATED (20),
    PAYLOAD_1_FAILED (30),

    PAYLOAD_2_CREATED (40),
    PAYLOAD_2_VALIDATED (50),
    PAYLOAD_2_FAILED (60),

    PAYLOAD_3_CREATED (70),
    PAYLOAD_3_VALIDATED (80),
    PAYLOAD_3_FAILED (90);

    private int numericalValue;

    PWDSignalSessionState(int numericalValue) {
        this.numericalValue = numericalValue;
    }

    public int getNumericalValue() {
        return numericalValue;
    }
}

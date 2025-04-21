package dev.abhay7.skribbl.client.jameskwong.pwdsignal;

public enum PWDSignalSessionState {
    INITIALIZED,

    PAYLOAD_1_VALIDATED,
    PAYLOAD_1_FAILED,

    PAYLOAD_2_VALIDATED,
    PAYLOAD_2_FAILED,
    
    PAYLOAD_3_VALIDATED,
    PAYLOAD_3_FAILED
}

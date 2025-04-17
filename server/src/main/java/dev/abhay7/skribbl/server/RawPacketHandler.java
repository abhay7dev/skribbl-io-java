package dev.abhay7.skribbl.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class RawPacketHandler {

    // Default MAX_RAW_PACKET_LENGTH is 16 MB (16 * 1024 * 1024)
    private static int MAX_RAW_PACKET_LENGTH = 16777216;
    private static byte LENGTH_BYTES_LENGTH = 4; // Default length indicator is 4 bytes

    /**
     * @return A JSON string that can be parsed by org.json or a similar library
    */
    public static String readRawPacket(InputStream reader) throws IOException {
        // We read 4 bytes of data from the InputStream are store it in a byte array
        byte[] lengthBytesLE = new byte[RawPacketHandler.LENGTH_BYTES_LENGTH];
        readNBytes(reader, lengthBytesLE, RawPacketHandler.LENGTH_BYTES_LENGTH);
        
        // We then convert those bytes to an integer, because those first 4 bytes represents the length of the overall JSON package.
        int length = littleEndianBytesToInt(lengthBytesLE);

        // We make sure the length is valid
        if (length > RawPacketHandler.MAX_RAW_PACKET_LENGTH) {
            throw new IOException("Raw packet is way to large: " + length);
        }

        // Create a new byte array to hold the actual JSON data, and then read those bytes
        byte[] jsonData = new byte[length];
        readNBytes(reader, jsonData, length);

        // Interpret data into a UTF-8 that we can actually use
        return new String(jsonData, StandardCharsets.UTF_8);
    }

    // **STILL NEED TO CALL writer.flush() after calling this!**
    public static void bufferRawPacket(OutputStream writer, String toSend) throws IOException {
        bufferRawPacket(writer, toSend.getBytes(StandardCharsets.UTF_8));
    }
    public static void bufferRawPacket(OutputStream writer, byte[] toSend) throws IOException {
        byte[] lengthBytes = new byte[4];
        intToLittleEndianBytes(toSend.length, lengthBytes);
        writeNBytes(writer, lengthBytes, 4);
        writeNBytes(writer, toSend, toSend.length);
    }

    // Reads "N" bytes into arr starting at index offset
    private static void readNBytes(InputStream reader, byte[] arr, int count) throws IOException {
        int read = 0;
        while (read < count) {
            int adder = reader.read(arr, read, count - read);
            if (adder == -1) {
                throw new IOException("EOF when reading " + count + " bytes");
            }
            read += adder;
        }
    }

    // OutputStream.write(byte[], off, len) blocks until all len bytes are written.
    private static void writeNBytes(OutputStream writer, byte[] arr, int count) throws IOException {
        int written = 0;
        while (written < count) {
            writer.write(arr, written, count - written);
            written = count;
        }
    }

    // For integers of length > 255, they're stored as more than one byte; the order of these bytes matter.
    // For example, you could say the first byte in a 2 byte array is the "LEAST SIGNIFICANT" part of it
    // That is little endianness, and that is what we expect the LENGTH in a packet to be
    private static int littleEndianBytesToInt(byte[] bytes) {
        if (bytes.length < 4) throw new IllegalArgumentException("Specified byte array to parse is too short.");

        // [1111 0000] [1111 0000] [1111 0000] [1111 0000]
        // [byte 1]    [byte 2]    [byte 3]    [byte 4]

        // 0xFF is needed as a sign extension because bytes by default are treated as signed (-128->127), but we need to make them unsigned (0->255) to accurately interpret them.
        return (bytes[0] & 0xFF) |
                ((bytes[1] & 0xFF) << 8) |
                ((bytes[2] & 0xFF) << 16) |
                ((bytes[3] & 0xFF) << 24);
    }

    // Convert an integer to Little Endian Bytes, storing the resultant values in the bytes[] array
    private static void intToLittleEndianBytes(int value, byte[] bytes) {
        if (bytes.length < 4) throw new IllegalArgumentException("Byte array too short (need 4 bytes at offset)");
        
        bytes[0] = (byte) (value & 0xFF);
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        bytes[2] = (byte) ((value >> 16) & 0xFF);
        bytes[3] = (byte) ((value >> 24) & 0xFF);
    }

    
}

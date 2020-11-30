package io.adobe.prometheus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BigEndianBitSetTest {

    @Test
    @DisplayName("Basic test from the Prometheus use-case")
    void readBits() throws Exception {
        var stream = new byte[] {
                -30, -96, -75, -19, -82, 93, 63, -16,
                0, 0, 0, 0, 0, 0, -24, 7,
                64, 7, -81, -8, -112, 3, -53, -2,
                36, 0, -6, -1, -59, 127, -2, -65,
                -1, 95, -8, -94, 39, 86, -40, -56,
                10, -59, -1, -110, 0, -128
        };
        var bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(48);
        long result = bitSet.readBits(64);
        double val = java.lang.Double.longBitsToDouble(result);
        assertEquals(1.0, val);
    }

    //whole word from one word
    @Test
    @DisplayName("Read whole word from one word")
    void readBitsWholeWordOneWord() {

    }

    @Test
    @DisplayName("Read first bit from one word")
    void readFirstBitFromOneWord() throws Exception {
        var stream = new byte[] {-30, -96, -75, -19, -82, 93, 63, -16};
        var bitSet = BigEndianBitSet.valueOf(stream);
        long result = bitSet.readBits(1);
    }
    //first/last/Nth bit from one word
    //last bit from one word and first from another
    //less than 64 bits resulting in negative
    //64 bits resulting in negative
    //64 bits from 2 words
    //more that 64 bits from 2 words => exception?
    //more bits than left from 1 and from 2 words
    //last bits from last word

    @Test
    void readBitsFast() {
    }

    @Test
    @DisplayName("Read first bit from a word")
    void readFirstBit() throws Exception {
        //1110001010100000101101011110110110101110010111010011111111110000
        var stream = new byte[] {-30, -96, -75, -19, -82, 93, 63, -16};
        var bitSet = BigEndianBitSet.valueOf(stream);
        assertTrue(bitSet.readBit());

        //0000000010100000101101011110110110101110010111010011111111110000
        stream = new byte[] {0, -96, -75, -19, -82, 93, 63, -16};
        bitSet = BigEndianBitSet.valueOf(stream);
        assertFalse(bitSet.readBit());
    }

    @Test
    @DisplayName("Read last bit from a full word")
    void readLastBit() throws Exception {
        //0000000010100000101101011110110110101110010111010011111111110000
        var stream = new byte[] {0, -96, -75, -19, -82, 93, 63, -16};
        var bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(63);
        assertFalse(bitSet.readBit());

        //0000000010100000101101011110110110101110010111010011111100000001
        stream = new byte[] {0, -96, -75, -19, -82, 93, 63, 1};
        bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(63);
        assertTrue(bitSet.readBit());
    }

    @Test
    @DisplayName("Read first & last bit from a shorter word")
    void readBitShorterWord() throws Exception {
        //0000000010100000101101011110110100000000000000000000000000000000
        var stream = new byte[] {0, -96, -75, -19};
        var bitSet = BigEndianBitSet.valueOf(stream);
        assertFalse(bitSet.readBit());

        //11100010 10100000 10110101 11101101 00000000000000000000000000000000
        stream = new byte[] {-30, -96, -75, -19};
        bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(31);
        assertTrue(bitSet.readBit());

        //00000000 10100000 10110101 00000001 00000000000000000000000000000000
        stream = new byte[] {0, -96, -75, 1};
        bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(31);
        assertTrue(bitSet.readBit());
    }

    @Test
    @DisplayName("Read a bit outside of the range of the shorter word")
    void readBitShorterWordException() throws Exception {
        var stream = new byte[] {0, -96, -75, -19};
        var bitSet = BigEndianBitSet.valueOf(stream);
        Exception exception = assertThrows(Exception.class, () -> {
            bitSet.readBits(64);
            bitSet.readBit();
        });
        assertTrue(exception.getMessage().contains("Index is larger than available"));
    }


    @Test
    @DisplayName("Read one bit from a word")
    void readBit() throws Exception {
        //00000101 00000000 00000000 00000000 00000000 00000000 00000000 00000000
        var stream = new byte[] {5};
        var bitSet = BigEndianBitSet.valueOf(stream);
        bitSet.readBits(5);
        assertTrue(bitSet.readBit());
        assertFalse(bitSet.readBit());
        assertTrue(bitSet.readBit());
    }
}
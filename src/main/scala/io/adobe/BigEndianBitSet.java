package io.adobe;

import java.util.Arrays;

public class BigEndianBitSet {

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;

    private int bitsUsed;

    private int bitsOffset = 0;

    /**
     * The number of words in the logical size of this BitSet.
     */
    private transient int wordsInUse;

    /**
     * Returns a new bit set containing all the bits in the given byte array.
     *
     * @param bytes a byte array containing a big-endian
     *        representation of a sequence of bits to be used as the
     *        initial bits of the new bit set
     * @return a {@code BitSet} containing all the bits in the byte array
     */
    public static BigEndianBitSet valueOf(byte[] bytes) {
        return initWords(bytes);
    }

    /**
     * Returns a new bit set containing all the bits in the given long array.
     *
     * @param longs a long array containing a big-endian representation
     *        of a sequence of bits to be used as the initial bits of the
     *        new bit set
     * @return a {@code BitSet} containing all the bits in the long array
     */
    public static BigEndianBitSet valueOf(long[] longs) {
        int n;
        for (n = longs.length; n > 0 && longs[n - 1] == 0; n--)
            ;
        return new BigEndianBitSet(Arrays.copyOf(longs, n), longs.length * BITS_PER_WORD);
    }

    /**
     * Creates a bit set using words as the internal representation.
     * The last word (if there is one) must be non-zero.
     */
    private BigEndianBitSet(long[] words, int bitsUsed) {
        this.words = words;
        wordsInUse = words.length;
        this.bitsUsed = bitsUsed;
    }

    /**
     * Retrieve length of the bits used
     * @return int
     */
    public int length() {
        return bitsUsed;
    }

    /**
     * Retrieve current bit offset
     * @return int
     */
    public int getBitsOffset() {
        return bitsOffset;
    }

    /**
     * Skip current bit offset and start from the beginning
     */
    public void rewind() {
        bitsOffset = 0;
    }

    /**
     * Compose big-endian representation of bytes in the bit set
     *
     * @param unsignedBytes byte[]
     * @return BigEndianBitSet
     */
    private static BigEndianBitSet initWords(byte[] unsignedBytes) {
        int length = (int)Math.ceil(unsignedBytes.length / 8.0);
        long[] result = new long[length];
        var current = 0;
        var currentCounter = 0;
        var i = 0;
        var s = 7 * 8;

        while (i < unsignedBytes.length) {
            long next = unsignedBytes[i] & 0xff;
            next <<= s;

            result[current] |= next;
            s -= 8;
            i += 1;
            currentCounter += 1;
            if (currentCounter > 7) {
                currentCounter = 0;
                current += 1;
                s = 7 * 8;
            }
        }
        return new BigEndianBitSet(result, unsignedBytes.length * 8);
    }

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    public boolean readBit() throws Exception {
        int firstWord = bitsOffset / BITS_PER_WORD;
        if (firstWord >= wordsInUse) {
            throw new Exception("Index is larger than available");
        }
        int startFrom = bitsOffset % BITS_PER_WORD;
        int shift = BITS_PER_WORD - startFrom - 1;
        boolean bit = ((words[firstWord] >> shift) & 1) == 1;
        bitsOffset += 1;
        return bit;
    }

    /**
     * @TODO load from the stream of bits rather than a static byte array
     * @TODO rename to StreamBitSet
     * @TODO use constants to indicate Big Endian order
     */

    /**
     * Returns a new {@code BigEndianBitSet} composed of bits from this {@code BigEndianBitSet}
     * from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param  nbits number of bits to read
     * @return a new {@code BigEndianBitSet} from a range of this {@code BitSet}
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *         or {@code toIndex} is negative, or {@code fromIndex} is
     *         larger than {@code toIndex}
     */
    public long readBits(int nbits) throws Exception {
        var bitsRead = nbits;
        int firstWord = bitsOffset / BITS_PER_WORD;
        if (firstWord >= wordsInUse) {
            throw new Exception("Index is larger than available");
        }
        int startFrom = bitsOffset % BITS_PER_WORD;
        int valid = BITS_PER_WORD - startFrom;

        if (nbits > 64) {
            throw new Exception("Requested bits does not fit to the long type");
        }

        if (nbits <= valid) {
            return readBitsFast(nbits);
        }

        long bitmask = WORD_MASK << startFrom;

        // We have to read all remaining valid bits from the current buffer and a part from the next one.
        nbits -= valid;

        long result;
        var current = firstWord;
        result = (words[current] << startFrom) & bitmask;

        current++;

        if (current > wordsInUse) {
            throw new Exception("Index is larger than available");
        }

        int bitsLeftInSecondWord = BITS_PER_WORD - nbits;
        bitmask = WORD_MASK << bitsLeftInSecondWord;
        result = (result | ((words[current] & bitmask) >>> valid)) >>> (bitsLeftInSecondWord - valid);

        bitsOffset += bitsRead;
        return result;
    }

    // readBitsFast is like readBits but can return io.EOF if the internal buffer is empty.
    // If it returns io.EOF, the caller should retry reading bits calling readBits().
    // This function must be kept small and a leaf in order to help the compiler inlining it
    // and further improve performances.
    public long readBitsFast(int nbits) {
        var bitsRead = nbits;
        int valid = BITS_PER_WORD - bitsOffset % BITS_PER_WORD;
        if (nbits > valid) {
            return 0;
        }
        valid -= nbits;

        long bitmask = (WORD_MASK >>> valid) >>> bitsOffset;
        int current = bitsOffset / BITS_PER_WORD;
        long result = (words[current] >>> valid) & bitmask;
        bitsOffset += bitsRead;
        return result;
    }
}

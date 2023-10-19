package com.itextpdf.barcodes.qrcode;

import com.itextpdf.barcodes.qrcode.Version;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Encoder {
    private static final int[] ALPHANUMERIC_TABLE = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 44, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1};
    static final String DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";

    private Encoder() {
    }

    private static int calculateMaskPenalty(ByteMatrix matrix) {
        return 0 + MaskUtil.applyMaskPenaltyRule1(matrix) + MaskUtil.applyMaskPenaltyRule2(matrix) + MaskUtil.applyMaskPenaltyRule3(matrix) + MaskUtil.applyMaskPenaltyRule4(matrix);
    }

    public static void encode(String content, ErrorCorrectionLevel ecLevel, QRCode qrCode) throws WriterException {
        encode(content, ecLevel, (Map<EncodeHintType, Object>) null, qrCode);
    }

    public static void encode(String content, ErrorCorrectionLevel ecLevel, Map<EncodeHintType, Object> hints, QRCode qrCode) throws WriterException {
        CharacterSetECI eci;
        String encoding = hints == null ? null : (String) hints.get(EncodeHintType.CHARACTER_SET);
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        int desiredMinVersion = (hints == null || hints.get(EncodeHintType.MIN_VERSION_NR) == null) ? 1 : ((Integer) hints.get(EncodeHintType.MIN_VERSION_NR)).intValue();
        if (desiredMinVersion < 1) {
            desiredMinVersion = 1;
        }
        if (desiredMinVersion > 40) {
            desiredMinVersion = 40;
        }
        Mode mode = chooseMode(content, encoding);
        BitVector dataBits = new BitVector();
        appendBytes(content, mode, dataBits, encoding);
        initQRCode(dataBits.sizeInBytes(), ecLevel, desiredMinVersion, mode, qrCode);
        BitVector headerAndDataBits = new BitVector();
        if (mode == Mode.BYTE && !"ISO-8859-1".equals(encoding) && (eci = CharacterSetECI.getCharacterSetECIByName(encoding)) != null) {
            appendECI(eci, headerAndDataBits);
        }
        appendModeInfo(mode, headerAndDataBits);
        appendLengthInfo(mode.equals(Mode.BYTE) ? dataBits.sizeInBytes() : content.length(), qrCode.getVersion(), mode, headerAndDataBits);
        headerAndDataBits.appendBitVector(dataBits);
        terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);
        BitVector finalBits = new BitVector();
        interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(), qrCode.getNumRSBlocks(), finalBits);
        ByteMatrix matrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
        qrCode.setMaskPattern(chooseMaskPattern(finalBits, qrCode.getECLevel(), qrCode.getVersion(), matrix));
        MatrixUtil.buildMatrix(finalBits, qrCode.getECLevel(), qrCode.getVersion(), qrCode.getMaskPattern(), matrix);
        qrCode.setMatrix(matrix);
        if (!qrCode.isValid()) {
            throw new WriterException("Invalid QR code: " + qrCode.toString());
        }
    }

    static int getAlphanumericCode(int code) {
        int[] iArr = ALPHANUMERIC_TABLE;
        if (code < iArr.length) {
            return iArr[code];
        }
        return -1;
    }

    public static Mode chooseMode(String content) {
        return chooseMode(content, (String) null);
    }

    public static Mode chooseMode(String content, String encoding) {
        if ("Shift_JIS".equals(encoding)) {
            return isOnlyDoubleByteKanji(content) ? Mode.KANJI : Mode.BYTE;
        }
        boolean hasNumeric = false;
        boolean hasAlphanumeric = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c >= '0' && c <= '9') {
                hasNumeric = true;
            } else if (getAlphanumericCode(c) == -1) {
                return Mode.BYTE;
            } else {
                hasAlphanumeric = true;
            }
        }
        if (hasAlphanumeric) {
            return Mode.ALPHANUMERIC;
        }
        if (hasNumeric) {
            return Mode.NUMERIC;
        }
        return Mode.BYTE;
    }

    private static boolean isOnlyDoubleByteKanji(String content) {
        try {
            byte[] bytes = content.getBytes("Shift_JIS");
            int length = bytes.length;
            if (length % 2 != 0) {
                return false;
            }
            for (int i = 0; i < length; i += 2) {
                int byte1 = bytes[i] & 255;
                if ((byte1 < 129 || byte1 > 159) && (byte1 < 224 || byte1 > 235)) {
                    return false;
                }
            }
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private static int chooseMaskPattern(BitVector bits, ErrorCorrectionLevel ecLevel, int version, ByteMatrix matrix) throws WriterException {
        int minPenalty = Integer.MAX_VALUE;
        int bestMaskPattern = -1;
        for (int maskPattern = 0; maskPattern < 8; maskPattern++) {
            MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
            int penalty = calculateMaskPenalty(matrix);
            if (penalty < minPenalty) {
                minPenalty = penalty;
                bestMaskPattern = maskPattern;
            }
        }
        return bestMaskPattern;
    }

    private static void initQRCode(int numInputBytes, ErrorCorrectionLevel ecLevel, int desiredMinVersion, Mode mode, QRCode qrCode) throws WriterException {
        qrCode.setECLevel(ecLevel);
        qrCode.setMode(mode);
        for (int versionNum = desiredMinVersion; versionNum <= 40; versionNum++) {
            Version version = Version.getVersionForNumber(versionNum);
            int numBytes = version.getTotalCodewords();
            Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
            int numEcBytes = ecBlocks.getTotalECCodewords();
            int numRSBlocks = ecBlocks.getNumBlocks();
            int numDataBytes = numBytes - numEcBytes;
            if (numDataBytes >= numInputBytes + 3) {
                qrCode.setVersion(versionNum);
                qrCode.setNumTotalBytes(numBytes);
                qrCode.setNumDataBytes(numDataBytes);
                qrCode.setNumRSBlocks(numRSBlocks);
                qrCode.setNumECBytes(numEcBytes);
                qrCode.setMatrixWidth(version.getDimensionForVersion());
                return;
            }
        }
        throw new WriterException("Cannot find proper rs block info (input data too big?)");
    }

    static void terminateBits(int numDataBytes, BitVector bits) throws WriterException {
        int capacity = numDataBytes << 3;
        if (bits.size() <= capacity) {
            for (int i = 0; i < 4 && bits.size() < capacity; i++) {
                bits.appendBit(0);
            }
            int numBitsInLastByte = bits.size() % 8;
            if (numBitsInLastByte > 0) {
                int numPaddingBits = 8 - numBitsInLastByte;
                for (int i2 = 0; i2 < numPaddingBits; i2++) {
                    bits.appendBit(0);
                }
            }
            if (bits.size() % 8 == 0) {
                int numPaddingBytes = numDataBytes - bits.sizeInBytes();
                for (int i3 = 0; i3 < numPaddingBytes; i3++) {
                    if (i3 % 2 == 0) {
                        bits.appendBits(236, 8);
                    } else {
                        bits.appendBits(17, 8);
                    }
                }
                if (bits.size() != capacity) {
                    throw new WriterException("Bits size does not equal capacity");
                }
                return;
            }
            throw new WriterException("Number of bits is not a multiple of 8");
        }
        throw new WriterException("data bits cannot fit in the QR Code" + bits.size() + " > " + capacity);
    }

    static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes, int numDataBytes, int numRSBlocks, int blockID, int[] numDataBytesInBlock, int[] numECBytesInBlock) throws WriterException {
        if (blockID < numRSBlocks) {
            int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
            int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
            int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
            int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
            int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
            int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
            int numEcBytesInGroup2 = (numTotalBytesInGroup1 + 1) - numDataBytesInGroup2;
            if (numEcBytesInGroup1 != numEcBytesInGroup2) {
                throw new WriterException("EC bytes mismatch");
            } else if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
                throw new WriterException("RS blocks mismatch");
            } else if (numTotalBytes != ((numDataBytesInGroup1 + numEcBytesInGroup1) * numRsBlocksInGroup1) + ((numDataBytesInGroup2 + numEcBytesInGroup2) * numRsBlocksInGroup2)) {
                throw new WriterException("Total bytes mismatch");
            } else if (blockID < numRsBlocksInGroup1) {
                numDataBytesInBlock[0] = numDataBytesInGroup1;
                numECBytesInBlock[0] = numEcBytesInGroup1;
            } else {
                numDataBytesInBlock[0] = numDataBytesInGroup2;
                numECBytesInBlock[0] = numEcBytesInGroup2;
            }
        } else {
            throw new WriterException("Block ID too large");
        }
    }

    static void interleaveWithECBytes(BitVector bits, int numTotalBytes, int numDataBytes, int numRSBlocks, BitVector result) throws WriterException {
        int i = numTotalBytes;
        int i2 = numDataBytes;
        int i3 = numRSBlocks;
        BitVector bitVector = result;
        if (bits.sizeInBytes() == i2) {
            List<BlockPair> blocks = new ArrayList<>(i3);
            int dataBytesOffset = 0;
            int maxNumDataBytes = 0;
            int maxNumEcBytes = 0;
            for (int i4 = 0; i4 < i3; i4++) {
                int[] numDataBytesInBlock = new int[1];
                int[] numEcBytesInBlock = new int[1];
                getNumDataBytesAndNumECBytesForBlockID(numTotalBytes, numDataBytes, numRSBlocks, i4, numDataBytesInBlock, numEcBytesInBlock);
                ByteArray dataBytes = new ByteArray();
                dataBytes.set(bits.getArray(), dataBytesOffset, numDataBytesInBlock[0]);
                ByteArray ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
                blocks.add(new BlockPair(dataBytes, ecBytes));
                maxNumDataBytes = Math.max(maxNumDataBytes, dataBytes.size());
                maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.size());
                dataBytesOffset += numDataBytesInBlock[0];
            }
            if (i2 == dataBytesOffset) {
                for (int i5 = 0; i5 < maxNumDataBytes; i5++) {
                    for (int j = 0; j < blocks.size(); j++) {
                        ByteArray dataBytes2 = blocks.get(j).getDataBytes();
                        if (i5 < dataBytes2.size()) {
                            bitVector.appendBits(dataBytes2.mo25269at(i5), 8);
                        }
                    }
                }
                for (int i6 = 0; i6 < maxNumEcBytes; i6++) {
                    for (int j2 = 0; j2 < blocks.size(); j2++) {
                        ByteArray ecBytes2 = blocks.get(j2).getErrorCorrectionBytes();
                        if (i6 < ecBytes2.size()) {
                            bitVector.appendBits(ecBytes2.mo25269at(i6), 8);
                        }
                    }
                }
                if (i != result.sizeInBytes()) {
                    throw new WriterException("Interleaving error: " + i + " and " + result.sizeInBytes() + " differ.");
                }
                return;
            }
            throw new WriterException("Data bytes does not match offset");
        }
        throw new WriterException("Number of bits and data bytes does not match");
    }

    static ByteArray generateECBytes(ByteArray dataBytes, int numEcBytesInBlock) {
        int numDataBytes = dataBytes.size();
        int[] toEncode = new int[(numDataBytes + numEcBytesInBlock)];
        for (int i = 0; i < numDataBytes; i++) {
            toEncode[i] = dataBytes.mo25269at(i);
        }
        new ReedSolomonEncoder(GF256.QR_CODE_FIELD).encode(toEncode, numEcBytesInBlock);
        ByteArray ecBytes = new ByteArray(numEcBytesInBlock);
        for (int i2 = 0; i2 < numEcBytesInBlock; i2++) {
            ecBytes.set(i2, toEncode[numDataBytes + i2]);
        }
        return ecBytes;
    }

    static void appendModeInfo(Mode mode, BitVector bits) {
        bits.appendBits(mode.getBits(), 4);
    }

    static void appendLengthInfo(int numLetters, int version, Mode mode, BitVector bits) throws WriterException {
        int numBits = mode.getCharacterCountBits(Version.getVersionForNumber(version));
        if (numLetters <= (1 << numBits) - 1) {
            bits.appendBits(numLetters, numBits);
            return;
        }
        throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
    }

    static void appendBytes(String content, Mode mode, BitVector bits, String encoding) throws WriterException {
        if (mode.equals(Mode.NUMERIC)) {
            appendNumericBytes(content, bits);
        } else if (mode.equals(Mode.ALPHANUMERIC)) {
            appendAlphanumericBytes(content, bits);
        } else if (mode.equals(Mode.BYTE)) {
            append8BitBytes(content, bits, encoding);
        } else if (mode.equals(Mode.KANJI)) {
            appendKanjiBytes(content, bits);
        } else {
            throw new WriterException("Invalid mode: " + mode);
        }
    }

    static void appendNumericBytes(String content, BitVector bits) {
        int length = content.length();
        int i = 0;
        while (i < length) {
            int num1 = content.charAt(i) - '0';
            if (i + 2 < length) {
                bits.appendBits((num1 * 100) + ((content.charAt(i + 1) - '0') * 10) + (content.charAt(i + 2) - '0'), 10);
                i += 3;
            } else if (i + 1 < length) {
                bits.appendBits((num1 * 10) + (content.charAt(i + 1) - '0'), 7);
                i += 2;
            } else {
                bits.appendBits(num1, 4);
                i++;
            }
        }
    }

    static void appendAlphanumericBytes(String content, BitVector bits) throws WriterException {
        int length = content.length();
        int i = 0;
        while (i < length) {
            int code1 = getAlphanumericCode(content.charAt(i));
            if (code1 == -1) {
                throw new WriterException();
            } else if (i + 1 < length) {
                int code2 = getAlphanumericCode(content.charAt(i + 1));
                if (code2 != -1) {
                    bits.appendBits((code1 * 45) + code2, 11);
                    i += 2;
                } else {
                    throw new WriterException();
                }
            } else {
                bits.appendBits(code1, 6);
                i++;
            }
        }
    }

    static void append8BitBytes(String content, BitVector bits, String encoding) throws WriterException {
        try {
            byte[] bytes = content.getBytes(encoding);
            for (byte appendBits : bytes) {
                bits.appendBits(appendBits, 8);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new WriterException(uee.toString());
        }
    }

    static void appendKanjiBytes(String content, BitVector bits) throws WriterException {
        try {
            byte[] bytes = content.getBytes("Shift_JIS");
            int length = bytes.length;
            int i = 0;
            while (i < length) {
                int code = ((bytes[i] & 255) << 8) | (bytes[i + 1] & 255);
                int subtracted = -1;
                if (code >= 33088 && code <= 40956) {
                    subtracted = code - 33088;
                } else if (code >= 57408 && code <= 60351) {
                    subtracted = code - 49472;
                }
                if (subtracted != -1) {
                    bits.appendBits(((subtracted >> 8) * 192) + (subtracted & 255), 13);
                    i += 2;
                } else {
                    throw new WriterException("Invalid byte sequence");
                }
            }
        } catch (UnsupportedEncodingException uee) {
            throw new WriterException(uee.toString());
        }
    }

    private static void appendECI(CharacterSetECI eci, BitVector bits) {
        bits.appendBits(Mode.ECI.getBits(), 4);
        bits.appendBits(eci.getValue(), 8);
    }
}

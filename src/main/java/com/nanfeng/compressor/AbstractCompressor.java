package com.nanfeng.compressor;

/**
 * @author 楠枫
 */
public abstract class AbstractCompressor implements Compressor {

    protected byte bitStringToByte(String str) {
        if (null == str) {
            throw new RuntimeException("when bit string convert to byte, Object can not be null!");
        }
        if (8 != str.length()) {
            throw new RuntimeException("bit string'length must be 8");
        }
        try {
            if (str.charAt(0) == '0') {
                return (byte) Integer.parseInt(str, 2);
            } else if (str.charAt(0) == '1') {
                return (byte) (Integer.parseInt(str, 2) - 256);
            }
        } catch (NumberFormatException e) {
            System.out.println(str);
            throw new RuntimeException("bit string convert to byte failed, byte String must only include 0 and 1!");
        }
        return 0;
    }

    protected String byteToBit(byte b) {
        return ""
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)
                + (byte) ((b >> 1) & 0x1) + (byte) ((b) & 0x1);
    }

}

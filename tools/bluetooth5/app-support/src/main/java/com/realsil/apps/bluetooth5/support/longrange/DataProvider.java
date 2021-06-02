package com.realsil.apps.bluetooth5.support.longrange;


import com.realsil.sdk.core.logger.ZLogger;
import com.realsil.sdk.core.utility.DataConverter;
import com.realsil.sdk.core.utility.StringUtils;

/**
 *
 * @author bingshanguxue
 * @date 01/12/2017
 */

public class DataProvider {
    private static final int SEQ_HEADER_LENGTH = 2;

    /**
     * 0 ~ 255
     */
    private static final int STREAM_SIZE_256 = 256;

    private static final int STREAM_DATA_0_255_EX_SIZE = 256 * 5;
    private static byte[] STREAM_DATA_0_255_EX;

    static {
        STREAM_DATA_0_255_EX = new byte[STREAM_DATA_0_255_EX_SIZE];
        for (int i = 0; i < STREAM_DATA_0_255_EX_SIZE; i++) {
            STREAM_DATA_0_255_EX[i] = (byte) (i % STREAM_SIZE_256);
        }
    }

    /**
     * 生成随机数据
     *
     * @param sequenceId 序号
     * @param packetLen     数据总长度
     */
    public static byte[] generateRandomData(int sequenceId, long packetLen) {
        byte[] data;
        //每次发送2个字节(sequenceId)+18个字节（data）
        byte[] header = new byte[SEQ_HEADER_LENGTH];
        header[0] = (byte) ((sequenceId >> 8) & 0xff);
        header[1] = (byte) (sequenceId & 0xff);

        if (packetLen > SEQ_HEADER_LENGTH) {
            byte[] random = DataConverter.str2Bytes(StringUtils.genNonceStringByLength(packetLen - SEQ_HEADER_LENGTH));
            int randomLen = random.length;
            data = new byte[randomLen + SEQ_HEADER_LENGTH];
            System.arraycopy(header, 0, data, 0, SEQ_HEADER_LENGTH);
            System.arraycopy(random, 0, data, SEQ_HEADER_LENGTH, randomLen);
        } else {
            //minmum length is HEADER_LENGTH
            data = new byte[SEQ_HEADER_LENGTH];
            System.arraycopy(header, 0, data, 0, 2);
        }
        return data;
    }

    /**
     * 生成生成随机数据（包的前两个字节是序号）
     *
     * @param sequenceId  序号
     * @param seqLen  序号长度
     * @param packetLen   数据包长度
     */
    public static byte[] generateRandomWithSeq(int sequenceId, int seqLen, long packetLen) {
        byte[] header = new byte[seqLen];
        for (int i = seqLen-1; i >=0; i--) {
            header[i] = (byte) ((sequenceId >> (8 * i)) & 0xff);
        }

        byte[] data;

        if (packetLen > seqLen) {
            data = new byte[(int) packetLen];
            int desPos = 0;
            System.arraycopy(header, 0, data, 0, seqLen);
            desPos += seqLen;

            long randomLen = packetLen - seqLen;
            byte[] random = DataConverter.str2Bytes(StringUtils.genNonceStringByLength(randomLen));
            System.arraycopy(random, 0, data, desPos, (int) randomLen);
        } else {
            data = new byte[seqLen];
            System.arraycopy(header, 0, data, 0, seqLen);
        }
        return data;
    }

    public static byte[] generateStreamWithSeq(int sequenceId, int streamIndex, long packetLen) {
        return generateStreamWithSeq(sequenceId, SEQ_HEADER_LENGTH, streamIndex, packetLen);
    }

    /**
     * 生成 0 ~ 255 连续数据（包的前两个字节是序号）
     *
     * @param sequenceId  序号
     * @param seqLen  序号长度
     * @param offset stream起始编号
     * @param packetLen   数据包长度
     */
    public static byte[] generateStreamWithSeq(int sequenceId, int seqLen, int offset, long packetLen) {
        byte[] header = new byte[seqLen];
        for (int i = seqLen-1; i >=0; i--) {
            header[i] = (byte) ((sequenceId >> (8 * i)) & 0xff);
        }

        offset %= STREAM_SIZE_256;

        byte[] data;

        if (packetLen > seqLen) {
            data = new byte[(int) packetLen];
            int desPos = 0;
            System.arraycopy(header, 0, data, 0, seqLen);
            desPos += seqLen;

            long streamLen = packetLen - seqLen;

            if (offset + streamLen <= STREAM_DATA_0_255_EX_SIZE) {
                System.arraycopy(STREAM_DATA_0_255_EX, offset, data, desPos, (int) streamLen);
            } else {
                int len1 = STREAM_DATA_0_255_EX_SIZE - offset;
                System.arraycopy(STREAM_DATA_0_255_EX, offset, data, desPos, len1);
                desPos += len1;

                int len2 = (int) (streamLen - len1);
                int len3 = 0;
                do {
                    len3 = Math.min(len2, STREAM_DATA_0_255_EX_SIZE);
                    System.arraycopy(STREAM_DATA_0_255_EX, 0, data, desPos, len3);
                    desPos += len3;
                    len2 -= len3;
                } while (len2 > 0);
            }
        } else {
            //minmum length is HEADER_LENGTH
            data = new byte[seqLen];
            System.arraycopy(header, 0, data, 0, seqLen);
        }
        return data;
    }

    public static byte[] generateStream(int offset, long packetLen) {
        try {
            byte[] data = new byte[(int) packetLen];

            offset %= STREAM_SIZE_256;
            if (offset + packetLen <= STREAM_DATA_0_255_EX_SIZE) {
                System.arraycopy(STREAM_DATA_0_255_EX, offset, data, 0, (int) packetLen);
            } else {
                int len1 = STREAM_DATA_0_255_EX_SIZE - offset;
                System.arraycopy(STREAM_DATA_0_255_EX, offset, data, 0, len1);

                int desPos = len1;
                int len2 = (int) (packetLen - len1);
                int len3 = 0;
                do {
                    len3 = Math.min(len2, STREAM_DATA_0_255_EX_SIZE);
                    System.arraycopy(STREAM_DATA_0_255_EX, 0, data, desPos, len3);
                    desPos += len3;
                    len2 -= len3;
                } while (len2 > 0);
            }

            return data;
        } catch (Exception e) {
            ZLogger.e(e.toString());
            return null;
        }
    }

}

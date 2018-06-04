package com.bigweiyan.util;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TimeSeriesRawIO {
    private RandomAccessFile file;
    private int seriesLen;
    private int totalSeries;
    private int lineLength;
    private byte[] buffer;
    private int blockPos;
    private int blockNo;
    private int blockSize;
    private boolean isRead;
    private int type;

    public static final int HEADER = 8;
    public static final int TYPE_EXCEPTION = 0;
    public static final int TYPE_PURE_DATA = 1;
    public TimeSeriesRawIO(String fileName, boolean isRead, int dataType, int blockSize) throws IOException{
        if (!isRead) throw new IllegalStateException("write series should use another constructor");
        this.isRead = true;
        this.type = dataType;
        this.blockSize = blockSize;
        this.blockPos = 0;
        this.blockNo = -1;
        this.file = new RandomAccessFile(fileName, "r");
        this.seriesLen = file.readInt();
        this.totalSeries = file.readInt();
        if (dataType == TYPE_EXCEPTION) this.lineLength = 4 + 8 * this.seriesLen;
        else if (dataType == TYPE_PURE_DATA) this.lineLength = 8 * this.seriesLen;
        this.buffer = new byte[lineLength * blockSize];
    }

    public TimeSeriesRawIO(String fileName, boolean isRead, int dataType, int blockSize, int seriesLen) throws IOException{
        this.isRead = isRead;
        this.type = dataType;
        this.blockSize = blockSize;
        this.blockPos = 0;
        this.blockNo = -1;
        this.file = new RandomAccessFile(fileName, isRead ? "r" : "rw");
        if (isRead) {
            this.seriesLen = file.readInt();
            this.totalSeries = file.readInt();

        } else {
            this.seriesLen = seriesLen;
            this.totalSeries = 0;
            this.file.writeInt(seriesLen);
            this.file.writeInt(0);
        }
        if (dataType == TYPE_EXCEPTION) this.lineLength = 4 + 8 * this.seriesLen;
        else if (dataType == TYPE_PURE_DATA) this.lineLength = 8 * this.seriesLen;
        this.buffer = new byte[lineLength * blockSize];
    }

    public void bufferedWrite(double[] data) throws IOException{
        if (isRead) throw new IllegalStateException("read mode not allowed write operation");
        if (type == TYPE_EXCEPTION) throw new IllegalStateException("data type is exception, use bufferedWriteException instead");
        for (int i = 0; i < seriesLen; i++) {
            BitTool.doubleToBytes(data[i], buffer, lineLength * blockPos + i * 8);
        }
        totalSeries++;
        blockPos++;
        if (blockPos == blockSize) {
            file.write(buffer);
            blockPos = 0;
        }
    }

    public double[] bufferedRead(int pos) throws IOException{
        if (!isRead) throw new IllegalStateException("write mode not allowed read operation");
        if (type == TYPE_EXCEPTION) throw new IllegalStateException("data type is exception, use bufferedReadException instead");
        if (pos >= totalSeries || pos < 0)
            throw new IllegalArgumentException("position should not be negative and less than total series numbers");
        double[] result = new double[seriesLen];
        int expectedBlock = pos / blockSize;
        if (expectedBlock != blockNo) {
            blockNo = expectedBlock;
            file.seek(HEADER + blockNo * blockSize * lineLength);
            file.read(buffer);
        }
        blockPos = pos % blockSize;
        for (int i = 0; i < seriesLen; i++) {
            result[i] = BitTool.bytesToDouble(buffer, blockPos * lineLength + i * 8);
        }
        return result;
    }

    public void bufferedWriteException(double data[], int id) throws IOException {
        if (isRead) throw new IllegalStateException("read mode not allowed write operation");
        if (type == TYPE_PURE_DATA) throw new IllegalStateException("data type is pure data, use bufferedWrite instead");
        BitTool.intToBytes(id, buffer, blockPos * lineLength);
        for (int i = 0; i < seriesLen; i++) {
            BitTool.doubleToBytes(data[i], buffer, blockPos * lineLength + i * 8 + 4);
        }
        totalSeries++;
        blockPos++;
        if (blockPos == blockSize) {
            file.write(buffer);
            blockPos = 0;
        }
    }

    public Pair<double[], Integer> bufferedReadException(int pos) throws IOException{
        if (!isRead) throw new IllegalStateException("write mode not allowed read operation");
        if (type == TYPE_PURE_DATA) throw new IllegalStateException("data type is pure data, use bufferedRead instead");
        if (pos >= totalSeries || pos < 0)
            throw new IllegalArgumentException("position should not be negative and less than total series numbers");
        double[] exception = new double[seriesLen];
        int id = 0;
        int expectedBlock = pos / blockSize;
        if (expectedBlock != blockNo) {
            blockNo = expectedBlock;
            file.seek(HEADER + blockNo * blockSize * lineLength);
            file.read(buffer);
        }
        blockPos = pos % blockSize;
        id = BitTool.bytesToInt(buffer, blockPos * lineLength);
        for (int i = 0; i < seriesLen; i++) {
            exception[i] = BitTool.bytesToDouble(buffer, blockPos * lineLength + i * 8 + 4);
        }
        return new Pair<>(exception,id);
    }

    public int getTotalSeries() {
        return totalSeries;
    }

    public void close() {
        try {
            if (!isRead) {
                if (blockPos != 0) {
                    file.write(buffer, 0, blockPos * lineLength);
                }
                file.seek(4);
                file.writeInt(totalSeries);
            }
            file.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

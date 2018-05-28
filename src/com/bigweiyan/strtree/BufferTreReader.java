package com.bigweiyan.strtree;

import com.bigweiyan.TimeSeriesIndexer;
import com.bigweiyan.util.BitTool;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferTreReader {
    private RandomAccessFile file;
    private int degree;
    private int lmbrDim;
    private int totalLength;
    private byte[] buffer;
    private int blockSize;
    private int blockPos;
    private int blockNo;
    private final int lineLength;
    public BufferTreReader(String indexPath, int bufferSize) throws IOException {
        file = new RandomAccessFile(indexPath + ".tre", "r");
        byte buffer[] = new byte[12];
        file.read(buffer);
        lmbrDim = BitTool.bytesToInt(buffer, 0);
        degree = BitTool.bytesToInt(buffer, 4);
        totalLength = BitTool.bytesToInt(buffer, 8);
        blockSize = bufferSize;
        this.lineLength = 1 + lmbrDim * 20 + degree * 4;
        this.buffer = new byte[lineLength * blockSize];
        blockNo = -1;
        blockPos = 0;
    }

    public void seek(int pos) throws IOException{
        int expectBlock = pos / blockSize;
        if (blockNo != expectBlock) {
            blockNo = expectBlock;
            file.seek(TimeSeriesIndexer.TREE_FILE_HEAD + lineLength * blockNo * blockSize);
            file.read(buffer);
        }
        blockPos = lineLength * (pos % blockSize);
    }

    public double readDouble() {
        double result = BitTool.bytesToDouble(buffer, blockPos);
        blockPos += 8;
        return result;
    }
    public byte readByte() {
        blockPos++;
        return buffer[blockPos - 1];
    }
    public int readInt(){
        int result = BitTool.bytesToInt(buffer, blockPos);
        blockPos += 4;
        return result;
    }

    public int getDegree() {
        return degree;
    }

    public int getLmbrDim() {
        return  lmbrDim;
    }

    public void close() throws IOException{
        file.close();
    }
}

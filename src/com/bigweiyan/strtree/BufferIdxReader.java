package com.bigweiyan.strtree;

import com.bigweiyan.TimeSeriesIndexer;
import com.bigweiyan.util.BitTool;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferIdxReader {
    private int size;
    private int lmbrDim;
    private RandomAccessFile file;
    private int blockSize;
    private int blockNo;
    private int blockPos;
    private final int lineLength;
    private byte[] buffer;
    public BufferIdxReader(String indexPath, int lmbrDim, int bufferLen) throws IOException {
        file = new RandomAccessFile(indexPath + ".idx", "r");
        size = file.readInt();
        this.lmbrDim = lmbrDim;
        blockSize = bufferLen;
        lineLength = 4 + lmbrDim * 20;
        buffer = new byte[lineLength * blockSize];
    }

    public void seek(int pos) throws IOException{
        int expectBlock = pos / blockSize;
        if (blockNo != expectBlock) {
            blockNo = expectBlock;
            file.seek(TimeSeriesIndexer.INDEX_FILE_HEAD + lineLength * blockNo * blockSize);
            file.read(buffer);
        }
        blockPos = lineLength * (pos % blockSize);
    }

    public double readDouble() {
        double result = BitTool.bytesToDouble(buffer, blockPos);
        blockPos += 8;
        return result;
    }

    public int readInt(){
        int result = BitTool.bytesToInt(buffer, blockPos);
        blockPos += 4;
        return result;
    }

    public void close() throws IOException{
        file.close();
    }
}

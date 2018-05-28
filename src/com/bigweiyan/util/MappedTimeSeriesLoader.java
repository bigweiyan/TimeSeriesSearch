package com.bigweiyan.util;

import java.io.IOException;
import java.io.RandomAccessFile;

public class MappedTimeSeriesLoader {
    private int[] map;
    private String fileName;
    private TimeSeriesRawIO rawReader;
    public MappedTimeSeriesLoader(String fileName) throws IOException{
        this.fileName = fileName;
        rawReader = new TimeSeriesRawIO(fileName + ".odt", true, TimeSeriesRawIO.TYPE_PURE_DATA, 1000);
        loadMap();
    }

    private void loadMap() throws IOException{
        RandomAccessFile file = new RandomAccessFile(fileName + ".map", "r");
        int fileLength = (int)(file.length() / 4) - 1;
        map = new int[file.readInt()];
        byte buffer[] = new byte[8000];
        for (int i = 0; i < fileLength / 2000; i++) {
            file.read(buffer);
            for (int j = 0; j < 2000; j++) {
                map[BitTool.bytesToInt(buffer, j * 4)] = i * 2000 + j;
            }
        }
        int currentBlock = fileLength / 2000;
        int rest = fileLength - currentBlock * 2000;
        file.read(buffer);
        for (int i = 0; i < rest; i++) {
            map[BitTool.bytesToInt(buffer, i * 4)] = currentBlock * 2000 + i;
        }
        file.close();
    }

    public double[] getTSFromKey(int key) throws IOException{
        return rawReader.bufferedRead(map[key]);
    }

    public void close() {
        rawReader.close();
    }
}

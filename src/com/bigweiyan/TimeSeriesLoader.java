package com.bigweiyan;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TimeSeriesLoader {
    private int[] map;
    private String fileName;
    private TimeSeriesRawReader rawReader;
    public TimeSeriesLoader(String fileName) {
        this.fileName = fileName;
        rawReader = new TimeSeriesRawReader(fileName + ".odt", "r", TimeSeriesRawReader.TYPE_MAPPED_DATA);
        loadMap();
    }

    private void loadMap() {
        try {
            RandomAccessFile file = new RandomAccessFile(fileName + ".map", "r");
            int length = (int)(file.length() / 4) - 1;
            map = new int[file.readInt()];
            for (int i = 0; i < length; i++) {
                map[file.readInt()] = i;
            }
            file.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public double[] getTSFromKey(int key) {
        return rawReader.read(map[key]);
    }

    public void close() {
        rawReader.close();
    }
}

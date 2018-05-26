package com.bigweiyan;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TimeSeriesLoader {
    private int[] map;
    private String fileName;
    private TimeSeriesIO io;
    public TimeSeriesLoader(String fileName) {
        this.fileName = fileName;
        io = new TimeSeriesIO(fileName + ".odt", "r");
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
        return io.read(map[key]);
    }

    public void close() {
        io.close();
    }
}

package com.bigweiyan;

import com.bigweiyan.util.BitTool;
import com.bigweiyan.util.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TimeSeriesRawReader {
    private RandomAccessFile file;
    private int seriesLen = 0;
    private int totalSeries = 0;
    private int lineLength;
    private boolean isRead;
    public static final int HEADER = 8;
    public static final int TYPE_EXCEPTION = 0;
    public static final int TYPE_MAPPED_DATA = 1;
    public TimeSeriesRawReader(String fileName, String mode, int dataType) {
        try {
            file = new RandomAccessFile(fileName, mode);
            if (mode.equals("r")) {
                seriesLen = file.readInt();
                totalSeries = file.readInt();
                isRead = true;
                if (dataType == TYPE_EXCEPTION) lineLength = 4 + 8 * seriesLen;
                else if (dataType == TYPE_MAPPED_DATA) lineLength = 8 * seriesLen;
            } else if (mode.equals("rw")) {
                file.writeInt(0);
                file.writeInt(0);
                isRead = false;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(double[] data) {
        seriesLen = data.length;
        totalSeries++;
        byte tmp[] = new byte[seriesLen * 8];
        try {
            for (int i = 0; i < seriesLen; i++) {
                BitTool.doubleToBytes(data[i], tmp, i * 8);
            }
            file.write(tmp);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveAllException(double[][] data, int[] id) {
        seriesLen = data[0].length;
        totalSeries = data.length;
        lineLength = seriesLen * 8 + 4;
        try {
            byte tmp[] = new byte[lineLength * 10];
            int current = 0;
            for (int i = 0; i < totalSeries; i++) {
                BitTool.intToBytes(id[i], tmp, current * lineLength);
                for (int j = 0; j < seriesLen; j++) {
                    BitTool.doubleToBytes(data[i][j], tmp, current * lineLength + j * 8 + 4);
                }
                current ++;
                if (current == 10) {
                    file.write(tmp);
                    current = 0;
                }
            }
            if (current != 0) {
                file.write(tmp, 0, current * lineLength);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll(double[][] data) {
        seriesLen = data[0].length;
        totalSeries = data.length;
        byte tmp[] = new byte[seriesLen * 10 * 8];
        int current = 0;
        try {
            file.seek(HEADER);
            for (int i = 0; i < totalSeries; i++) {
                for (int j = 0; j < seriesLen; j++) {
                    BitTool.doubleToBytes(data[i][j], tmp, (current * seriesLen + j) * 8);
                }
                current ++;
                if (current == 10) {
                    file.write(tmp);
                    current = 0;
                }
            }
            if (current != 0) {
                file.write(tmp, 0, current * seriesLen);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double[][] readAll() {
        double series[][] = null;
        byte tmp[] = new byte[seriesLen * 10 * 8];
        try {
            file.seek(HEADER);
            series = new double[totalSeries][seriesLen];
            for (int i = 0; i < totalSeries / 10; i++) {
                file.read(tmp);
                for (int j = 0; j < 10; j++) {
                    for (int k = 0; k < seriesLen; k++) {
                        series[i * 10 + j][k] = BitTool.bytesToDouble(tmp, (j * seriesLen  + k) * 8);
                    }
                }
            }
            file.read(tmp);
            int current = totalSeries / 10 * 10;
            int left = totalSeries - current;
            for (int i = 0; i < left; i++) {
                for (int k = 0; k < seriesLen; k++) {
                    series[current + i][k] = BitTool.bytesToDouble(tmp, (i * seriesLen  + k) * 8);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return series;
    }

    public double[] read(int pos) {
        double series[] = null;
        try {
            byte buffer[] = new byte[lineLength];
            file.seek(pos * 8 * seriesLen + HEADER);
            series = new double[seriesLen];
            file.read(buffer);
            for (int j = 0; j < seriesLen; j++) {
                series[j] = BitTool.bytesToDouble(buffer, j * 8);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return series;
    }

    public Pair<double[][], Integer[]> readExceptions(){
        double series[][] = null;
        Integer[] id = null;
        try {
            file.seek(HEADER);
            byte tmp[] = new byte[lineLength * 10];
            id = new Integer[totalSeries];
            series = new double[totalSeries][seriesLen];
            for (int i = 0; i < totalSeries / 10; i++) {
                file.read(tmp);
                for (int j = 0; j < 10; j++) {
                    id[i * 10 + j] = BitTool.bytesToInt(tmp, lineLength * j);
                    for (int k = 0; k < seriesLen; k++) {
                        series[i * 10 + j][k] = BitTool.bytesToDouble(tmp, lineLength * j + 4 + k * 8);
                    }
                }
            }
            file.read(tmp);
            int current = totalSeries / 10 * 10;
            int left = totalSeries - current;
            for (int i = 0; i < left; i++) {
                id[current + i] = BitTool.bytesToInt(tmp, lineLength * i);
                for (int k = 0; k < seriesLen; k++) {
                    series[current + i][k] = BitTool.bytesToDouble(tmp, lineLength * i + 4 + k * 8);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return new Pair<>(series, id);
    }

    public void close() {
        try {
            if (!isRead) {
                file.seek(0);
                file.writeInt(seriesLen);
                file.writeInt(totalSeries);
            }
            file.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

package com.bigweiyan;

import com.bigweiyan.util.BitTool;
import com.bigweiyan.util.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TimeSeriesRawReader {
    private RandomAccessFile file;
    private int seriesLen = 0;
    private int totalSeries = 0;
    private boolean isRead;
    public static final int HEADER = 8;
    public static final int TYPE_EXCEPTION = 0;
    public static final int TYPE_MAPPEDDATA = 1;
    public TimeSeriesRawReader(String fileName, String mode, int dataType) {
        try {
            file = new RandomAccessFile(fileName, mode);
            if (mode.equals("r")) {
                seriesLen = file.readInt();
                totalSeries = file.readInt();
                isRead = true;
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
        try {
            for (int i = 0; i < seriesLen; i++) {
                file.writeDouble(data[i]);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saveAllException(double[][] data, int[] id) {
        seriesLen = data[0].length;
        totalSeries = data.length;
        try {
            for (int i = 0; i < totalSeries; i++){
                file.writeInt(id[i]);
                for (int j = 0; j < seriesLen; j++) {
                    file.writeDouble(data[i][j]);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll(double[][] data) {
        seriesLen = data[0].length;
        totalSeries = data.length;
        try {
            file.seek(HEADER);
            for (int i = 0; i < totalSeries; i++) {
                for (int j = 0; j < seriesLen; j++) {
                    file.writeDouble(data[i][j]);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double[][] readAll() {
        double series[][] = null;
        try {
            file.seek(HEADER);
            series = new double[totalSeries][seriesLen];
            for (int i = 0; i < totalSeries; i++){
                for (int j = 0; j < seriesLen; j++) {
                    series[i][j] = file.readDouble();
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return series;
    }

    public double[] read(int pos) {
        double series[] = null;
        int lineLength = 8 * seriesLen;
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
        int lineLength = seriesLen * 8 + 4;
        try {
            file.seek(HEADER);
            byte buffer[] = new byte[lineLength];
            id = new Integer[totalSeries];
            series = new double[totalSeries][seriesLen];
            for (int i = 0; i < totalSeries; i++) {
                file.read(buffer);
                id[i] = BitTool.bytesToInt(buffer, 0);
                for (int j = 0; j < seriesLen; j++) {
                    series[i][j] = BitTool.bytesToDouble(buffer, 4 + j * 8);
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

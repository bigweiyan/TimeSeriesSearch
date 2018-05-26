package com.bigweiyan;

import com.bigweiyan.util.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class TimeSeriesIO {
    private RandomAccessFile file;
    private int seriesLen = 0;
    private int totalSeries = 0;
    private boolean isRead;
    public static final int HEADER = 8;
    public TimeSeriesIO(String fileName, String mode) {
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
        FileChannel channel = file.getChannel();
        try {
            file.seek(pos * 8 * seriesLen + HEADER);
            series = new double[seriesLen];
            for (int i = 0; i < seriesLen; i++){
                series[i] = file.readDouble();
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
            id = new Integer[totalSeries];
            series = new double[totalSeries][seriesLen];
            for (int i = 0; i < totalSeries; i++){
                id[i] = file.readInt();
                for (int j = 0; j < seriesLen; j++) {
                    series[i][j] = file.readDouble();
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

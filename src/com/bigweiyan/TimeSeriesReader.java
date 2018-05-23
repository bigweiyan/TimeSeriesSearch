package com.bigweiyan;

import java.util.ArrayList;
import java.util.Scanner;

public class TimeSeriesReader {
    private String divider;
    private int seriesLen;

    public TimeSeriesReader(String divider) {
        this.divider = divider;
        this.seriesLen = 100;
    }

    /**
     * Time Series Data Reader
     * @param divider the divider between two numbers
     * @param seriesLen information of series length(not have to be exactly)
     */
    public TimeSeriesReader(String divider, int seriesLen) {
        this.divider = divider;
        this.seriesLen = seriesLen;
    }

    public double[] readSeries(String input) {
        ArrayList<Double> temp = new ArrayList<>(seriesLen);
        String cells[] = input.split(divider);
        for (String cell:cells) {
            cell = cell.trim();
            if (cell.equals("")) continue;
            temp.add(Double.parseDouble(cell));
        }
        seriesLen = temp.size();
        double result[] = new double[seriesLen];
        for (int i = 0; i < seriesLen; i++) {
            result[i] = temp.get(i);
        }
        return result;
    }

    public double[] readSeries(Scanner scanner){
        double result[] = null;
        if(scanner.hasNext()) {
            result = readSeries(scanner.nextLine());
        }
        if (result != null && result.length > 0) {
            return result;
        } else {
            return null;
        }
    }

    public double[] readSeriesAndNormalize(String input) {
        ArrayList<Double> temp = new ArrayList<>(seriesLen);
        String cells[] = input.split(divider);
        double sum = 0;
        double squareSum = 0;
        for (String cell:cells) {
            cell = cell.trim();
            if (cell.equals("")) continue;
            double current = Double.parseDouble(cell);
            sum += current;
            squareSum += current * current;
            temp.add(current);
        }

        seriesLen = temp.size();
        double result[] = new double[seriesLen];
        double mean = sum / seriesLen;
        double std = Math.sqrt(squareSum / seriesLen - mean * mean);
        for (int i = 0; i < seriesLen; i++) {
            result[i] = (temp.get(i) - mean) / std;
        }
        return result;
    }

    public double[] readSeriesAndNormalize(Scanner scanner) {
        ArrayList<Double> temp = new ArrayList<>();
        while (scanner.hasNext()) {
            temp.add(scanner.nextDouble());
        }
        seriesLen = temp.size();
        double result[] = new double[seriesLen];
        double sum = 0;
        double squareSum = 0;
        for (int i = 0; i < seriesLen; i++) {
            double d = temp.get(i);
            sum += d;
            squareSum += d * d;
        }
        double mean = sum / seriesLen;
        double std = Math.sqrt(squareSum / seriesLen - mean * mean);
        for (int i = 0; i < seriesLen; i++) {
            result[i] = (temp.get(i) - mean) / std;
        }
        return result;
    }
}

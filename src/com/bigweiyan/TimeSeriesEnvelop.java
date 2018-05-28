package com.bigweiyan;

import java.util.ArrayDeque;
import java.util.Deque;

public class TimeSeriesEnvelop {
    public double[] upperEnvelop;
    public double[] lowerEnvelop;
    public int width;

    /**
     * Create an envelop for time series
     * @param timeSeries the time series to be enveloped
     * @param rate width rate of envelop
     */
    public TimeSeriesEnvelop(double[] timeSeries, float rate) {
        if (rate > 1){
            width = timeSeries.length;
        } else if (rate < 1.0 / timeSeries.length){
            width = 0;
        } else {
            width = (int) Math.floor(rate * timeSeries.length);
        }
        createEnvelop(timeSeries);
    }

    /**
     * Create an envelop for time series
     * @param timeSeries the time series to be enveloped
     * @param width width of envelop
     */
    public TimeSeriesEnvelop(double[] timeSeries, int width) {
        this.width = width;
        createEnvelop(timeSeries);
    }

    /**
     * Create an envelop for time series; this method is reusable
     * @param timeSeries the time series to be enveloped
     */
    public void createEnvelop(double[] timeSeries) {
        // 根据传入参数判断是否进行数组重用
        if (upperEnvelop == null || upperEnvelop.length != timeSeries.length) {
            upperEnvelop = new double[timeSeries.length];
            lowerEnvelop = new double[timeSeries.length];
        }
        // 存储了填充窗口最大值的值在时间序列中所在的位置
        Deque<Integer> upperDeque = new ArrayDeque<>(2 * width + 2);
        // 存储了填充窗口最小值的值在时间序列中所在的位置
        Deque<Integer> lowerDeque = new ArrayDeque<>(2 * width + 2);
        upperDeque.offerLast(0);
        lowerDeque.offerLast(0);
        // i是窗口右边缘的位置，因此i-width-1是窗口左边缘以外的第一个位置（即该位置的窗口内不包含当前值）
        for (int i = 1; i < timeSeries.length; i++) {
            if (i > width) {
                upperEnvelop[i - 1 - width] = timeSeries[upperDeque.getFirst()];
                lowerEnvelop[i - 1 - width] = timeSeries[lowerDeque.getFirst()];
            }
            if (timeSeries[i] > timeSeries[i - 1]) {
                upperDeque.pollLast();
                while (! upperDeque.isEmpty() && timeSeries[i] > timeSeries[upperDeque.getLast()]) {
                    upperDeque.pollLast();
                }
            } else {
                lowerDeque.pollLast();
                while (! lowerDeque.isEmpty() && timeSeries[i] < timeSeries[lowerDeque.getLast()]) {
                    lowerDeque.pollLast();
                }
            }
            upperDeque.offerLast(i);
            lowerDeque.offerLast(i);
            // deque中下一个填充的值已经离开了窗口
            if (i == 2 * width + 1 + upperDeque.getFirst())
                upperDeque.pollFirst();
            else if (i == 2 * width + 1 + lowerDeque.getFirst())
                lowerDeque.pollFirst();
        }

        // 将最后的窗口计算完毕[n-width-1 : n]
        for (int i = timeSeries.length; i < timeSeries.length + width + 1; i++) {
            upperEnvelop[i - 1 - width] = timeSeries[upperDeque.getFirst()];
            lowerEnvelop[i - 1 - width] = timeSeries[lowerDeque.getFirst()];
            if (i - upperDeque.getFirst() >= 2 * width + 1)
                upperDeque.pollFirst();
            if (i - lowerDeque.getFirst() >= 2 * width + 1)
                lowerDeque.pollFirst();
        }
    }
}

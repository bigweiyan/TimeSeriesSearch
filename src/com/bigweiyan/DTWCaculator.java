package com.bigweiyan;

import com.bigweiyan.strtree.STRTree;

public class DTWCaculator {
    private int queryLen;
    private double bestSoFar;
    private int kimCount;
    private int keogh1Count;
    private int keogh2Count;
    private int dtwCount;
    private double keogh1Bound[];
    private double keogh2Bound[];
    private double choosedBound[];
    private int width;

    public DTWCaculator(int queryLen, int width) {
        this.queryLen = queryLen;
        this.width = width;
        keogh1Bound = new double[queryLen];
        keogh2Bound = new double[queryLen];
        choosedBound = new double[queryLen];
        bestSoFar = Double.MAX_VALUE;
    }

    /**
     *
     */
    public void reset() {
        bestSoFar = Double.MAX_VALUE;
        kimCount = 0;
        dtwCount = 0;
        keogh1Count = 0;
        keogh2Count = 0;
    }

    /**
     *
     * @param query 待查时间序列
     * @param candidate 已查时间序列
     * @return 查找结果在原始数据中的下标。如果返回-1，则表示之前调用match方法返回过更好的值
     */
    public int matchQueryWithLongRawSeries(TimeSeries query, double[] candidate) {
        // 为了减小浮点误差，每隔一个epoch重新计算累积值
        final int EPOCH = 100000;
        int bufferLen = 0;
        // 保存了下一个将要读入的元素
        int candiLoc = 0;

        kimCount = 0;
        keogh1Count = 0;
        keogh2Count = 0;
        dtwCount = 0;

        // pool存储了顺序的candidate中的raw data，于是我们可以因此连续访问数组获得cand
        double[] pool = new double[2 * queryLen];
        // cand为pool中的连续的一段数据，但是进行了normalization
        double[] normedC = new double[queryLen];
        double[] buffer = new double[EPOCH];
        double[] lastBuffer;

        boolean done = false;
        boolean firstRun = true;
        TimeSeriesEnvelop envelop = null;
        int result = -1;
        // 主要过程
        while (!done) {
            /*
                数据读取和位置标注
             */
            if (firstRun) {
                firstRun = false;
            }else {
                // 从第二轮开始, 前一轮的末尾元素需要回到初始
                candiLoc -= queryLen;
                candiLoc += 1;
            }

            // 向buffer传递值
            if (candiLoc + EPOCH <= candidate.length) {
                System.arraycopy(candidate, candiLoc, buffer, 0, EPOCH);
                bufferLen = EPOCH;
                candiLoc += EPOCH;
                // 当前buffer内第i个元素在原始candidate的位置是candiLoc - EPOCH + i
            }else {
                bufferLen = candidate.length - candiLoc;
                System.arraycopy(candidate, candiLoc, buffer, 0, bufferLen);
                done = true;
                // 当前buffer内第i个元素在原始candidate的位置是candiLoc + i
            }

            // buffer长度不足以进行dtw运算
            if (bufferLen < queryLen) {
                break;
            } else if(bufferLen != EPOCH){
                // buffer长度足以进行dtw，但是不足以进行下一轮
                lastBuffer = new double[bufferLen];
                System.arraycopy(buffer, 0, lastBuffer, 0, bufferLen);
                buffer = lastBuffer;
            }
            /*
                创建envelop
             */
            if (envelop == null) {
                envelop = new TimeSeriesEnvelop(buffer, width);
            }else {
                envelop.createEnvelop(buffer);
            }

            double sum = 0;
            double squareSum = 0;
            for (int i = 0; i < bufferLen; i++) {
                /*
                  记录正则化信息
                 */
                // data是正在匹配的序列的最后一个元素
                double data = buffer[i];
                sum += data;
                squareSum += data * data;
                // 为了读取时更高效
                pool[i % queryLen] = data;
                pool[i % queryLen + queryLen] = data;
                // 当data读取到第len-1个元素时，闭区间[0:len-1]即是正在计算的子序列
                if (i >= queryLen - 1) {
                    // 动态正则化参数
                    double mean = sum / queryLen;
                    double std = Math.sqrt(squareSum / queryLen - mean * mean);
                    /*
                        记录当前正在处理的序列
                     */
                    // 表示了当前子序列第一个元素在buffer中的位置
                    int startLoc = i - queryLen + 1;
                    // 表示了当前子序列第一个元素在pool中的位置
                    int j = startLoc % queryLen;
                    // calculate lb_kim
                    if (lbKimOnRaw(query, buffer, startLoc, mean, std) > bestSoFar) {
                        kimCount++; // use lb_kim trim
                        sum -= buffer[startLoc];
                        squareSum -= buffer[startLoc] * buffer[startLoc];
                        continue;
                    }

                    // calculate lb_keogh
                    double lb_keogh1 = lbKeogh1OnRaw(query, buffer, startLoc, mean, std);
                    if (lb_keogh1 > bestSoFar) {
                        keogh1Count++; //use lb_keogh trim
                        sum -= buffer[startLoc];
                        squareSum -= buffer[startLoc] * buffer[startLoc];
                        continue;
                    }
                    // normalize data
                    for (int k = 0; k < queryLen; k++) {
                        normedC[k] = (pool[j + k] - mean) / std;
                    }
                    // calculate lb_keogh2
                    double lb_keogh2 = lbKeogh2OnRaw(query, envelop, startLoc, mean, std);
                    if (lb_keogh2 > bestSoFar) {
                        keogh2Count++;
                        sum -= buffer[startLoc];
                        squareSum -= buffer[startLoc] * buffer[startLoc];
                        continue;
                    }

                    if (lb_keogh1 > lb_keogh2) {
                        choosedBound[queryLen - 1] = keogh1Bound[queryLen - 1];
                        for (int k = queryLen - 2; k >= 0; k--) {
                            choosedBound[k] = choosedBound[k + 1] + keogh1Bound[k];
                        }
                    }else {
                        choosedBound[queryLen - 1] = keogh2Bound[queryLen - 1];
                        for (int k = queryLen - 2; k >= 0; k--) {
                            choosedBound[k] = choosedBound[k + 1] + keogh2Bound[k];
                        }
                    }

                    double dist = DTW(query, normedC, choosedBound);

                    if (dist < bestSoFar) {
                        bestSoFar = dist;
                        result = done ? candiLoc : candiLoc - EPOCH;
                        result += startLoc;
                    }
                    dtwCount++;
                    sum -= buffer[startLoc];
                    squareSum -= buffer[startLoc] * buffer[startLoc];
                }
            }
        }

        int total = dtwCount + kimCount + keogh1Count + keogh2Count;
        System.out.println("kimCount:" + (kimCount * 1.0 / total));
        System.out.println("keogh1Count:" + (keogh1Count * 1.0 / total));
        System.out.println("keogh2Count:" + (keogh2Count * 1.0 / total));
        System.out.println("dtwCount:" + (dtwCount * 1.0 / total));
        return result;
    }

    public boolean matchQueryWithRawSeries(TimeSeries query, double[] candidate) {
        double sum = 0;
        double squareSum = 0;
        for (int i = 0; i < queryLen; i++) {
            sum += candidate[i];
            squareSum += candidate[i] * candidate[i];
        }
        double mean = sum / queryLen;
        double std = Math.sqrt(squareSum / queryLen - mean * mean);

        if (lbKimOnRaw(query, candidate, 0, mean, std) > bestSoFar) {
            kimCount++; // use lb_kim trim
            return false;
        }
        // calculate lb_keogh
        double lb_keogh1 = lbKeogh1OnRaw(query, candidate, 0, mean, std);
        if (lb_keogh1 > bestSoFar) {
            keogh1Count++; //use lb_keogh trim
            return false;
        }
        TimeSeriesEnvelop envelop = new TimeSeriesEnvelop(candidate, width);
        double lb_keogh2 = lbKeogh2OnRaw(query, envelop, 0, mean, std);
        if (lb_keogh2 > bestSoFar) {
            keogh2Count++;
            return false;
        }
        dtwCount++;
        if (lb_keogh1 > lb_keogh2) {
            choosedBound[queryLen - 1] = keogh1Bound[queryLen - 1];
            for (int k = queryLen - 2; k >= 0; k--) {
                choosedBound[k] = choosedBound[k + 1] + keogh1Bound[k];
            }
        }else {
            choosedBound[queryLen - 1] = keogh2Bound[queryLen - 1];
            for (int k = queryLen - 2; k >= 0; k--) {
                choosedBound[k] = choosedBound[k + 1] + keogh2Bound[k];
            }
        }
        double dist = DTW(query, candidate, choosedBound);
        if (dist < bestSoFar) {
            bestSoFar = dist;
            return true;
        }
        return false;
    }

    /**
     * Check if this candidate is the nearest time series of query.
     * You should run this method for each series in your data set,
     * And make sure call reset if you want to change the query.<br/>
     * example:<br/>
     * TimeSeries nearest = null; <br/>
     * for (TimeSeries c: dataSet) { <br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;nearest = matchQueryWithNormedSeries(query, c) ? c : nearest;<br/>}
     * @param query
     * @param candidate
     * @return if this candidate is your nearest candidate
     */
    public boolean matchQueryWithNormedSeries(TimeSeries query, TimeSeries candidate) {
        if (lbKimOnNormed(query, candidate.data, 0) > bestSoFar) {
            kimCount++; // use lb_kim trim
            return false;
        }
        // calculate lb_keogh
        double lb_keogh1 = lbKeogh1OnNormed(query, candidate.data, 0);
        if (lb_keogh1 > bestSoFar) {
            keogh1Count++; //use lb_keogh trim
            return false;
        }
        double lb_keogh2 = lbKeogh2OnNormed(query, candidate.envelop, 0);
        if (lb_keogh2 > bestSoFar) {
            keogh2Count++;
            return false;
        }
        dtwCount++;
        if (lb_keogh1 > lb_keogh2) {
            choosedBound[queryLen - 1] = keogh1Bound[queryLen - 1];
            for (int k = queryLen - 2; k >= 0; k--) {
                choosedBound[k] = choosedBound[k + 1] + keogh1Bound[k];
            }
        }else {
            choosedBound[queryLen - 1] = keogh2Bound[queryLen - 1];
            for (int k = queryLen - 2; k >= 0; k--) {
                choosedBound[k] = choosedBound[k + 1] + keogh2Bound[k];
            }
        }
        double dist = DTW(query, candidate.data, choosedBound);
        if (dist < bestSoFar) {
            bestSoFar = dist;
            return true;
        }
        return false;
    }

    public int treeSearch(TimeSeries query, STRTree tree) {
        return -1;
    }

    private double lbKimOnRaw(TimeSeries query, double[] rawC, int cStart, double mean, double std){
        double result;
        double first0 = (rawC[cStart] - mean) / std;
        double last0 = (rawC[cStart + queryLen - 1] - mean) / std;
        result = distance(query.data[0], first0) + distance(query.data[queryLen - 1], last0);
        if (result >= bestSoFar)
            return result;

        // perform first DTW op
        double first1 = (rawC[cStart + 1] - mean) / std;
        double dis = min(distance(first0, query.data[1]), distance(first1, query.data[0]));
        dis = min(dis, distance(first1, query.data[1]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        //perform last DTW op
        double last1 = (rawC[cStart + queryLen - 2] - mean) / std;
        dis = min(distance(last0, query.data[queryLen - 2]), distance(last1, query.data[queryLen - 1]));
        dis = min(dis, distance(last1, query.data[queryLen - 2]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        // perform second DTW op
        double first2 = (rawC[cStart + 2] - mean) / std;
        dis = min(distance(first2, query.data[0]), distance(first2, query.data[1]));
        dis = min(dis, distance(first2, query.data[2]));
        dis = min(dis, distance(first1, query.data[2]));
        dis = min(dis, distance(first0, query.data[2]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        //perform second last DTW op
        double last2 = (rawC[cStart + queryLen - 3] - mean) / std;
        dis = min(distance(last2, query.data[queryLen - 1]), distance(last2, query.data[queryLen - 2]));
        dis = min(dis, distance(last2, query.data[queryLen - 3]));
        dis = min(dis, distance(last1, query.data[queryLen - 3]));
        dis = min(dis, distance(last0, query.data[queryLen - 3]));
        result += dis;
        return result;
    }
    private double lbKimOnNormed(TimeSeries query, double[] normedC, int cStart){
        double result;
        double first0 = normedC[cStart];
        double last0 = normedC[cStart + queryLen - 1];
        result = distance(query.data[0], first0) + distance(query.data[queryLen - 1], last0);
        if (result >= bestSoFar)
            return result;

        // perform first DTW op
        double first1 = normedC[cStart + 1];
        double dis = min(distance(first0, query.data[1]), distance(first1, query.data[0]));
        dis = min(dis, distance(first1, query.data[1]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        //perform last DTW op
        double last1 = normedC[cStart + queryLen - 2];
        dis = min(distance(last0, query.data[queryLen - 2]), distance(last1, query.data[queryLen - 1]));
        dis = min(dis, distance(last1, query.data[queryLen - 2]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        // perform second DTW op
        double first2 = normedC[cStart + 2];
        dis = min(distance(first2, query.data[0]), distance(first2, query.data[1]));
        dis = min(dis, distance(first2, query.data[2]));
        dis = min(dis, distance(first1, query.data[2]));
        dis = min(dis, distance(first0, query.data[2]));
        result += dis;
        if (result >= bestSoFar)
            return result;

        //perform second last DTW op
        double last2 = normedC[cStart + queryLen - 3];
        dis = min(distance(last2, query.data[queryLen - 1]), distance(last2, query.data[queryLen - 2]));
        dis = min(dis, distance(last2, query.data[queryLen - 3]));
        dis = min(dis, distance(last1, query.data[queryLen - 3]));
        dis = min(dis, distance(last0, query.data[queryLen - 3]));
        result += dis;
        return result;
    }
    private double lbKeogh1OnRaw(TimeSeries query, double[] rawC, int cStart, double mean, double std) {
        double result = 0;
        double x, dis;
        for (int i = 0; i <queryLen && result < bestSoFar; i++) {
            x = (rawC[cStart + query.order[i]] - mean) / std;
            dis = 0;
            if (x > query.orderedUpperEnvelop[i])
                dis = distance(x, query.orderedUpperEnvelop[i]);
            else if (x < query.orderedLowerEnvelop[i])
                dis = distance(x, query.orderedLowerEnvelop[i]);
            result += dis;
            keogh1Bound[query.order[i]] = dis;
        }
        return result;
    }
    private double lbKeogh1OnNormed(TimeSeries query, double[] normedC, int cStart) {
        double result = 0;
        double x, dis;
        for (int i = 0; i <queryLen && result < bestSoFar; i++) {
            x = normedC[cStart + query.order[i]];
            dis = 0;
            if (x > query.orderedUpperEnvelop[i])
                dis = distance(x, query.orderedUpperEnvelop[i]);
            else if (x < query.orderedLowerEnvelop[i])
                dis = distance(x, query.orderedLowerEnvelop[i]);
            result += dis;
            keogh1Bound[query.order[i]] = dis;
        }
        return result;
    }
    private double lbKeogh2OnRaw(TimeSeries query, TimeSeriesEnvelop rawEnvelop, int cStart, double mean, double std) {
        double result = 0;
        double uu, ll, dis;
        for (int i = 0; i < queryLen && result < bestSoFar; i++) {
            uu = (rawEnvelop.upperEnvelop[cStart + query.order[i]] - mean) / std;
            ll = (rawEnvelop.lowerEnvelop[cStart + query.order[i]] - mean) / std;
            dis = 0;
            if (query.orderedData[i] > uu) {
                dis = distance(query.orderedData[i], uu);
            }else if (query.orderedData[i] < ll) {
                dis = distance(query.orderedData[i], ll);
            }
            result += dis;
            keogh2Bound[query.order[i]] = dis;
        }
        return result;
    }
    private double lbKeogh2OnNormed(TimeSeries query, TimeSeriesEnvelop normedEnvelop, int cStart) {
        double result = 0;
        double uu, ll, dis;
        for (int i = 0; i < queryLen && result < bestSoFar; i++) {
            uu = normedEnvelop.upperEnvelop[cStart + query.order[i]];
            ll = normedEnvelop.lowerEnvelop[cStart + query.order[i]];
            dis = 0;
            if (query.orderedData[i] > uu) {
                dis = distance(query.orderedData[i], uu);
            }else if (query.orderedData[i] < ll) {
                dis = distance(query.orderedData[i], ll);
            }
            result += dis;
            keogh2Bound[query.order[i]] = dis;
        }
        return result;
    }

    public void setBestSoFar(double bestSoFar) {
        this.bestSoFar = bestSoFar;
    }
    public double getBestSoFar() {
        return bestSoFar;
    }

    private double DTW(TimeSeries query, double[] normedC, double[] bound) {
        double costPrev[] = new double[width * 2 + 1];
        double cost[] = new double[width * 2 + 1];
        double minCost;
        double x, y, z;
        for (int i = 0; i < 2 * width + 1; i++) {
            cost[i] = Double.MAX_VALUE;
            costPrev[i] = Double.MAX_VALUE;
        }
        int k = 0;
        for (int i = 0; i < queryLen; i++) {
            k = max(0, width - i);
            minCost = Double.MAX_VALUE;

            for (int j = max(0, i - width); j <= min(queryLen - 1, i + width); j++, k++) {
                if (i == 0 && j == 0) {
                    cost[k] = distance(query.data[0], normedC[0]);
                    minCost = cost[k];
                    continue;
                }
                if (j - 1 < 0 || k - 1 < 0) y = Double.MAX_VALUE;
                else y = cost[k - 1];
                if (i - 1 < 0 || k + 1 > 2 * width) x = Double.MAX_VALUE;
                else x = costPrev[k + 1];
                if (i - 1 < 0 || j - 1 < 0) z = Double.MAX_VALUE;
                else z = costPrev[k];

                cost[k] = min(min(x,y),z) + distance(query.data[i], normedC[j]);

                if (cost[k] < minCost) {
                    minCost = cost[k];
                }
            }

            if (i + width < queryLen - 1 && minCost + bound[i + width + 1] >= bestSoFar) {
                return minCost + bound[i + width + 1];
            }
            double cost_tmp[] = cost;
            cost = costPrev;
            costPrev = cost_tmp;

        }
        k--;
        return costPrev[k];
    }

    private double distance(double a, double b) {
        return (a - b) * (a - b);
    }
    private double min(double a, double b){
        return a < b ? a : b;
    }
    private int min(int a, int b) {
        return a < b ? a : b;
    }
    private int max(int a, int b) {
        return a > b ? a : b;
    }
}

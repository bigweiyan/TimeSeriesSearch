package com.bigweiyan.strtree;

import com.bigweiyan.util.Pair;
import com.bigweiyan.TimeSeriesEnvelop;

import java.util.List;

public class LMBRHelper {
    private int segment = 0;
    /**
     * store (segment + 1) element so we can access last element to get length of raw
     */
    private int startPos[] = null;
    private int shiftArray[] = null;
    /**
     * for each dimension pair, the difference of the pair should less than this threshold so we can get reasonable lower
     * bound<br/>
     * when this threshold is conflict with <b>usageThreshold</b> ,we give up on this one
     */
    private float diffThreshold = 5;

    /**
     * for each dimension pair, we should use more than usageThreshold percentage of envelop in the span so we can get
     * reasonable lower bound<br/>
     * when this threshold is conflict with <b>diffThreshold</b> ,we make sure this one
     */
    private float usageThreshold = 1;

    public LMBRHelper(int length, int segment) {
        if (length <= 0 || segment > length) {
            throw new IllegalArgumentException("Length should greater than both zero and segment");
        }
        this.segment = segment;
        startPos = new int[segment + 1];
        for (int i = 1; i < segment; i++) {
            startPos[i] = (int)(length * 1.0 * i / segment);
        }
        startPos[0] = 0;
        startPos[segment] = length;
    }

    public void setThreshold(float diffThreshold, float usageThreshold) {
        this.diffThreshold = diffThreshold;
        this.usageThreshold = usageThreshold;
    }

    public LMBR createLMBR(TimeSeriesEnvelop envelop) {
        if (envelop.upperEnvelop.length != startPos[segment]) {
            throw new IllegalArgumentException("You should use the same length series as your LMBRHelper's");
        }
        double upper[] = new double[segment];
        double lower[] = new double[segment];
        int weight[] = new int[segment];
        double tmpMax, tmpMin;
        for (int i = 0; i < segment; i++) {
            tmpMax =  - Double.MAX_VALUE;
            tmpMin = Double.MAX_VALUE;
            int usageTh = (int)(usageThreshold * startPos[i + 1] - startPos[i]);
            for (int j = startPos[i]; j < startPos[i + 1]; j++) {
                if (tmpMax < envelop.upperEnvelop[j]) tmpMax = envelop.upperEnvelop[j];
                if (tmpMin > envelop.lowerEnvelop[j]) tmpMin = envelop.lowerEnvelop[j];
                if (tmpMax - tmpMin > diffThreshold && j - startPos[i] > usageTh) break;
                upper[i] = tmpMax;
                lower[i] = tmpMin;
                weight[i] = j - startPos[i] + 1;
            }
        }
        LMBR result = new LMBR();
        result.lower = lower;
        result.upper = upper;
        result.weights = weight;
        return result;
    }

    /**
     * test whether the envelop be a LMBR or exception. if one of dimension's span is too large to be a LMBR, then it
     * would become an exception
     * @param envelopInput envelop to be tested
     * @param idInput the id of this envelop
     * @param lmbrOutput if pass the test, it's LMBR and id will add to this
     * @param exceptionOutput if not pass the test, it's id will add to this
     */
    public void testLMBR(TimeSeriesEnvelop envelopInput, int idInput, List<Pair<Integer, LMBR>> lmbrOutput,
                         List<Integer> exceptionOutput) {
        if (envelopInput.upperEnvelop.length != startPos[segment]) {
            throw new IllegalArgumentException("You should use the same length series as your LMBRHelper's");
        }
        double upper[] = new double[segment];
        double lower[] = new double[segment];
        int weight[] = new int[segment];
        double tmpMax, tmpMin;
        for (int i = 0; i < segment; i++) {
            tmpMax =  - Double.MAX_VALUE;
            tmpMin = Double.MAX_VALUE;
            int usageTh = (int)(usageThreshold * (startPos[i + 1] - startPos[i]));
            for (int j = startPos[i]; j < startPos[i + 1]; j++) {
                if (tmpMax < envelopInput.upperEnvelop[j]) tmpMax = envelopInput.upperEnvelop[j];
                if (tmpMin > envelopInput.lowerEnvelop[j]) tmpMin = envelopInput.lowerEnvelop[j];
                if (tmpMax - tmpMin > diffThreshold) {
                    if (j - startPos[i] < usageTh) {
                        exceptionOutput.add(idInput);
                        return;
                    }else {
                        break;
                    }
                }
                upper[i] = tmpMax;
                lower[i] = tmpMin;
                weight[i] = j - startPos[i] + 1;
            }
        }
        LMBR result = new LMBR();
        result.lower = lower;
        result.upper = upper;
        result.weights = weight;
        lmbrOutput.add(new Pair<>(idInput, result));
    }

    public void testManyLMBR(TimeSeriesEnvelop envelopInput, int idInput, List<List<Pair<Integer, LMBR>>> lmbrOutput,
                         List<Integer> exceptionOutput) {
        if (envelopInput.upperEnvelop.length != startPos[segment]) {
            throw new IllegalArgumentException("You should use the same length series as your LMBRHelper's");
        }
        if (shiftArray == null) {
            int shiftCount = lmbrOutput.size();
            int minSegmentLen = startPos[segment] / segment;
            if (shiftCount >= minSegmentLen) {
                throw new IllegalArgumentException("there's to many shifts");
            }
            shiftArray = new int[shiftCount];
            for (int i = 0; i < shiftCount; i++) {
                shiftArray[i] = (int)(minSegmentLen * 1.0 * i / shiftCount);
            }
        }

        LMBR bestLMBR = null;
        int bestResult = 0;
        int bestNo = 0;
        for (int i = 0; i < shiftArray.length; i++) {
            LMBR lmbr = new LMBR();
            int tmp = testLMBRWithShift(envelopInput, shiftArray[i], lmbr, bestResult);
            if (tmp > bestResult) {
                bestResult = tmp;
                bestLMBR = lmbr;
                bestNo = i;
            }
        }
        if (bestResult == 0) {
            exceptionOutput.add(idInput);
        }else {
            lmbrOutput.get(bestNo).add(new Pair<>(idInput, bestLMBR));
        }
    }

    private int testLMBRWithShift(TimeSeriesEnvelop envelop, int shift, LMBR lmbrOutput, int bestSoFar) {
        double upper[] = new double[segment];
        double lower[] = new double[segment];
        int weight[] = new int[segment];
        double tmpMax, tmpMin;
        int shiftPos[] = new int[segment + 1];
        for (int i = 0; i < segment + 1; i++) {
            shiftPos[i] = startPos[i] + shift;
        }
        int result = 0;
        for (int i = 0; i < segment - 1; i++) {
            tmpMax = - Double.MAX_VALUE;
            tmpMin = Double.MAX_VALUE;
            int usageTh = (int)(usageThreshold * (shiftPos[i + 1] - shiftPos[i]));
            for (int j = shiftPos[i]; j < shiftPos[i + 1]; j++) {
                if (tmpMax < envelop.upperEnvelop[j]) tmpMax = envelop.upperEnvelop[j];
                if (tmpMin > envelop.lowerEnvelop[j]) tmpMin = envelop.lowerEnvelop[j];
                if (tmpMax - tmpMin > diffThreshold) {
                    if (j - shiftPos[i] < usageTh) {
                        return 0;
                    }else {
                        break;
                    }
                }
                upper[i] = tmpMax;
                lower[i] = tmpMin;
                weight[i] = j - shiftPos[i] + 1;
            }
            result += weight[i];
            if (bestSoFar - result > shiftPos[segment] - shiftPos[i + 1]) {
                return 0;
            }
        }
        int length = startPos[segment];
        tmpMax = - Double.MAX_VALUE;
        tmpMin = Double.MAX_VALUE;
        int usageTh = (int)(usageThreshold * (shiftPos[segment] - shiftPos[segment - 1]));
        for (int j = shiftPos[segment - 1]; j < shiftPos[segment]; j++) {
            if (tmpMax < envelop.upperEnvelop[j % length]) tmpMax = envelop.upperEnvelop[j % length];
            if (tmpMin > envelop.lowerEnvelop[j % length]) tmpMin = envelop.lowerEnvelop[j % length];
            if (tmpMax - tmpMin > diffThreshold) {
                if (j - shiftPos[segment - 1] < usageTh) {
                    return 0;
                }else {
                    break;
                }
            }
            upper[segment - 1] = tmpMax;
            lower[segment - 1] = tmpMin;
            weight[segment - 1] = j - shiftPos[segment - 1] + 1;
        }
        result += weight[segment - 1];
        lmbrOutput.upper = upper;
        lmbrOutput.lower = lower;
        lmbrOutput.weights = weight;
        return result;
    }
}

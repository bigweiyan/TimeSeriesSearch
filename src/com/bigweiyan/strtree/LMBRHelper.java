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
    /**
     * for each dimension pair, the difference of the pair should less than this threshold so we can get reasonable lower
     * bound<br/>
     * when this threshold is conflict with <b>usageThreshold</b> ,we give up on this one
     */
    private double diffThreshold = 5;

    /**
     * for each dimension pair, we should use more than usageThreshold percentage of envelop in the span so we can get
     * reasonable lower bound<br/>
     * when this threshold is conflict with <b>diffThreshold</b> ,we make sure this one
     */
    private double usageThreshold = 1;

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

    public void setThreshold(double diffThreshold, double usageThreshold) {
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
            int usageTh = (int)(usageThreshold * startPos[i + 1] - startPos[i]);
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
}

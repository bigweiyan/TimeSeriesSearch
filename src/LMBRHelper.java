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
            System.out.println("" + i + " " + startPos[i]);
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
}

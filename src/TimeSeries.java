import java.util.ArrayList;

public class TimeSeries {
    public double data[];
    public TimeSeriesEnvelop envelop = null;
    public double orderedData[] = null;
    public double orderedLowerEnvelop[] = null;
    public double orderedUpperEnvelop[] = null;
    public int order[] = null;
    public int bandWidth;

    public TimeSeries(double timeSeries[], float bandRate) {
        this.data = timeSeries;
        if (bandRate >= 1) {
            bandWidth = data.length;
        }else if (bandRate < 1.0 / timeSeries.length) {
            bandWidth = 0;
        }else{
            bandWidth = (int) Math.floor(bandRate * timeSeries.length);
        }
    }

    public void initQuery() {
        ArrayList<DataIndex> indexes = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            indexes.add(new DataIndex(data[i], i));
        }
        indexes.sort((DataIndex o1, DataIndex o2) -> {
                double result = Math.abs(o2.value) - Math.abs(o1.value);
                if (result > 0)
                    return  1;
                else if (result < 0)
                    return -1;
                else
                    return 0;
                });

        order = new int[data.length];
        orderedData = new double[data.length];
        envelop = new TimeSeriesEnvelop(data, bandWidth);
        orderedLowerEnvelop = new double[data.length];
        orderedUpperEnvelop = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            order[i] = indexes.get(i).index;
            orderedData[i] = data[order[i]];
            orderedLowerEnvelop[i] = getLowerEnvelop()[order[i]];
            orderedUpperEnvelop[i] = getUpperEnvelop()[order[i]];
        }
    }

    public double[] getLowerEnvelop() {
        return envelop.lowerEnvelop;
    }

    public double[] getUpperEnvelop() {
        return envelop.upperEnvelop;
    }

    public int getLength() {
        return data.length;
    }

    private class DataIndex {
        double value;
        int index;
        DataIndex(double value, int index) {
            this.value = value;
            this.index = index;
        }
    }
}

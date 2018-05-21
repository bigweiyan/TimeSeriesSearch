import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

public class TestMain {
    public static void main(String[] args){
        testQuery();
    }

    public static void testReader() {
        TimeSeriesReader reader = new TimeSeriesReader(" ");
        String input = "  0.0000000e+000 -4.3256481e-001 -2.0981492e+000 -1.9728169e+000 -1.6851405e+000 -2.8316118e+000" +
                " -1.6406963e+000 -4.5153215e-001 -4.8916542e-001 -1.6187306e-001  1.2766081e-002 -1.7394250e-001";
        double result[] = reader.readSeries(input);
        for (double r :result) {
            System.out.println(r);
        }
    }

    public static void testEnvelop() {
        TimeSeries timeSeries = new TimeSeries(new double[]{6,7,8,9,10,11,10,9,8,7,6,5,4,3,2,1,2,3,4,5,6}, 0.1f);
        timeSeries.initQuery();
    }

    public static void testQuery(){
        TimeSeriesReader reader = new TimeSeriesReader(" ");
        TimeSeries query = null;
        double[] cand = null;
        try {
            FileInputStream fileInputStream = new FileInputStream("Query.txt");
            Scanner scanner = new Scanner(fileInputStream);
            query = new TimeSeries(reader.readSeriesAndNormalize(scanner), 0.05f);
            scanner.close();
            fileInputStream.close();
            fileInputStream = new FileInputStream("Data.txt");
            scanner = new Scanner(fileInputStream);
            cand = reader.readSeries(scanner);
            scanner.close();
            fileInputStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }

        if (query == null || cand == null){
            return;
        }
        query.initQuery();
        DTWCaculator caculator = new DTWCaculator(query.getLength(), query.bandWidth);
        int result = caculator.matchQueryWithLongRawSeries(query, cand);
        System.out.println(result);
    }
}

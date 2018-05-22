import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

public class TestMain {
    public static void main(String[] args){
        testLMBR();
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

    public static void testLMBR() {
        double data[] = new double[]{-0.71052,-1.1833,-1.3724,
                -1.5931,-1.467,-1.3724,
                -1.0888,0.045967,0.92853,
                1.0861,1.2753,0.96005,
                0.61333,0.014447,-0.64748,
                -0.26923,-0.20619,0.61333,
                1.3698,1.4644,1.0546,
                0.58181,0.17205,-0.26923};
        LMBRHelper helper = new LMBRHelper(data.length,8);
        helper.setThreshold(1.5, 0.5);
        TimeSeriesEnvelop envelop = new TimeSeriesEnvelop(data, 0);
        LMBR mbr = helper.createLMBR(envelop);

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
        query.initAsQuery(false);
        DTWCaculator caculator = new DTWCaculator(query.getLength(), query.bandWidth);
        boolean result = caculator.matchQueryWithRawSeries(query, query.data);
        System.out.println(caculator.getBestSoFar());
    }

    public static void testIO() {
        try {
            Date date = new Date();
            long current = date.getTime();
            FileInputStream inputStream = new FileInputStream("Data.txt");
            Scanner scanner = new Scanner(inputStream);
            double cand[] = new double[1000000];
            int i = 0;
            while (scanner.hasNext()){
                cand[i] = scanner.nextDouble();
                i++;
            }
            System.out.println("phase 1 time: " + Long.toString(new Date().getTime() - current));
            date = new Date();
            current = date.getTime();
            double sum = 0;
            double squareSum = 0;
            for (int j = 0; j < cand.length; j++) {
                sum += cand[j];
                squareSum += cand[j] * cand[j];
            }
            double mean = sum / cand.length;
            double std = Math.sqrt(squareSum / cand.length - mean * mean);
            for (int j = 0; j < cand.length; j++) {
                cand[j] = (cand[j] - mean) / std;
            }
            System.out.println("phase 2 time: " +  Long.toString(new Date().getTime() - current));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testManyQuery() {
        final int TRAIN_SIZE = 1000;
        final int TEST_SIZE = 8236;
        final int LEN = 1024;
        final float WINDOW = 0.16f;
        final String TRAIN_NAME = "Data/StarLightCurves_TEST";
        final String TEST_NAME = "Data/StarLightCurves_TRAIN";
        TimeSeries test[] = new TimeSeries[TEST_SIZE];
        int testClass[] = new int[TEST_SIZE];
        TimeSeries train[] = new TimeSeries[TRAIN_SIZE];
        int trainClass[] = new int[TRAIN_SIZE];
        try {
            Scanner scanner = new Scanner(new FileInputStream(TRAIN_NAME));
            double data[] = null;
            for (int i = 0; i < TEST_SIZE; i++){
                data = new double[LEN];
                String[] line = scanner.nextLine().split(",");
                for (int j = 0; j < LEN; j++) {
                    data[j] = Double.parseDouble(line[j + 1]);
                }
                test[i] = new TimeSeries(data, WINDOW);
                testClass[i] = Integer.parseInt(line[0]);
            }
            scanner.close();
            scanner = new Scanner(new FileInputStream(TEST_NAME));
            for (int i = 0; i < TRAIN_SIZE; i++) {
                data = new double[LEN];
                String[] line = scanner.nextLine().split(",");
                for (int j = 0; j < LEN; j++) {
                    data[j] = Double.parseDouble(line[j + 1]);
                }
                train[i] = new TimeSeries(data, WINDOW);
                trainClass[i] = Integer.parseInt(line[0]);
            }
            scanner.close();
        }catch (IOException e){
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < TEST_SIZE; i++) {
            test[i].initAsQuery(false);
        }

        for (int i = 0; i < TRAIN_SIZE; i++) {
            train[i].initAsCand(false);
        }

        int wrongCount = 0;
        DTWCaculator caculator = new DTWCaculator(LEN, train[0].bandWidth);
        for (int i = 0; i < TEST_SIZE; i++) {
            caculator.reset();
            TimeSeries query = test[i];
            int result = 0;
            for (int j = 0; j < TRAIN_SIZE; j++) {
                if (caculator.matchQueryWithNormedSeries(query, train[j])) {
                    result = j;
                }
            }
            if (testClass[i] != trainClass[result]) {
                wrongCount ++;
            }
            System.out.println(i);
        }
        System.out.print("wrong rate: ");
        System.out.println(wrongCount * 1.0 / TEST_SIZE);

    }
}

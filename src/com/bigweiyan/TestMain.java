package com.bigweiyan;

import com.bigweiyan.strtree.LMBR;
import com.bigweiyan.strtree.LMBRHelper;
import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.MappedTimeSeriesLoader;
import com.bigweiyan.util.Pair;
import com.bigweiyan.util.TimeSeriesParser;
import com.bigweiyan.util.TimeSeriesRawIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class TestMain {
    public static void main(String[] args) throws IOException{
        testIndex();
        testQueryTree("Data50000");
        testQuery("Data50000");

    }

    public static void testReader() {
        TimeSeriesParser reader = new TimeSeriesParser(" ");
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
        helper.setThreshold(1.5f, 0.5f);
        TimeSeriesEnvelop envelop = new TimeSeriesEnvelop(data, 0);
        LMBR mbr = helper.createLMBR(envelop);

    }

    public static void testQuery(String fileName){
        TimeSeriesParser reader = new TimeSeriesParser(" ");
        TimeSeries query = null;
        double[] cand = null;
        try {
            Date date = new Date();
            FileInputStream fileInputStream = new FileInputStream("D:/query/query.txt");
            Scanner scanner = new Scanner(fileInputStream);
            query = new TimeSeries(reader.readSeriesAndNormalize(scanner), 0.1f);
            scanner.close();
            fileInputStream.close();
            query.initAsQuery(false);
            DTWCalculator caculator = new DTWCalculator(query.getLength(), query.bandWidth);
            TimeSeriesRawIO timeSeriesReader = new TimeSeriesRawIO("D:/index/" + fileName + ".raw", true,
                    TimeSeriesRawIO.TYPE_PURE_DATA, 1);
            int totalSeries = timeSeriesReader.getTotalSeries();
            int result = -1;
            TimeSeries candidate;
            for (int i = 0; i < totalSeries; i++) {
                cand = timeSeriesReader.bufferedRead(i);
                candidate = new TimeSeries(cand, query.bandWidth);
                candidate.initAsCand(false);
                if (caculator.matchQueryWithNormedSeries(query, candidate)) {
                    result = i;
                }
            }
            timeSeriesReader.close();
            System.out.println("trillion result:" + result);
            System.out.println("trillion time:" + Long.toString(new Date().getTime() - date.getTime()));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testQueryTree(String fileName) throws IOException{
        System.out.println("----------read index----------");
        Date start = new Date();
        STRTreeHelper helper = new STRTreeHelper(40);
        TimeSeriesParser reader = new TimeSeriesParser(" ");
        TimeSeries query = null;
        STRTree tree = null;
        try {
            FileInputStream fileInputStream = new FileInputStream("D:/query/query.txt");
            Scanner scanner = new Scanner(fileInputStream);
            query = new TimeSeries(reader.readSeriesAndNormalize(scanner), 0.1f);
            scanner.close();
            fileInputStream.close();
            query.initAsQuery(false);
            tree = helper.generateTreeFromFile("D:/index",fileName);
        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("index io time:" + Long.toString(new Date().getTime() - start.getTime()));
        System.out.println("----------query----------");
        Date date = new Date();
        DTWCalculator calculator = new DTWCalculator(query.getLength(), query.bandWidth);
        TimeSeriesRawIO io = new TimeSeriesRawIO("D:/index/" + fileName +".edt", true, TimeSeriesRawIO.TYPE_EXCEPTION, 1);
        int result = 0;
        for (int i = 0; i < io.getTotalSeries(); i++) {
            Pair<double[], Integer> pair = io.bufferedReadException(i);
            TimeSeries candidate = new TimeSeries(pair.getKey(), 0.1f);
            candidate.initAsCand(false);
            if (calculator.matchQueryWithNormedSeries(query, candidate)) {
                result = pair.getValue();
            }
        }
        System.out.println("exp search time:" + Long.toString(new Date().getTime() - date.getTime()));
        MappedTimeSeriesLoader loader = new MappedTimeSeriesLoader("D:/index/" + fileName);
        Date searchTime = new Date();
        int result2 = calculator.treeSearch(query, tree, loader);
        loader.close();
        System.out.println("tree search time:" + Long.toString(new Date().getTime() - searchTime.getTime()));
        if (result2 != -1) {
            System.out.println("my method tree result:" + result2);
        }else {
            System.out.println("my method exp result:" + result);
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
        DTWCalculator caculator = new DTWCalculator(LEN, train[0].bandWidth);
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

    public static void testIndex() throws IOException{
        TimeSeriesIndexer indexer = new TimeSeriesIndexer(10, 80);
        indexer.creatIndex("D:/output","L256S500K", "D:/index", 4.2f,
                0.02f, 0.1f, false, " ");
    }
}

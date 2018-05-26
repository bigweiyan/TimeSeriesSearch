package com.bigweiyan;

import com.bigweiyan.strtree.LMBR;
import com.bigweiyan.strtree.LMBRHelper;
import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.Pair;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class TestMain {
    public static void main(String[] args){
        testQuery();
        testIndex();
        testQueryTree();
//        testIO();
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

    public static void testSTRTree() {

        double data[][] = new double[16][2];
        for (int i = 0; i < 16; i++) {
            data[15 - i][0] = i / 4;
            data[15 - i][1] = i % 4;
        }
        ArrayList<double[]> arrayList = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            arrayList.add(data[i]);
        }
        ArrayList<TimeSeriesEnvelop> envelops = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            envelops.add(new TimeSeriesEnvelop(data[i], 0));
        }
        ArrayList<Pair<Integer, LMBR>> pairs = new ArrayList<>();
        LMBRHelper lmbrHelper = new LMBRHelper(2, 2);
        lmbrHelper.setThreshold(1, 1);
        for (int i = 0; i < envelops.size(); i++) {
            pairs.add(new Pair<>(i, lmbrHelper.createLMBR(envelops.get(i))));
        }
        STRTreeHelper strTreeHelper = new STRTreeHelper(6);
        try {
            STRTree tree = strTreeHelper.generateTreeFromMemory(pairs);
            TimeSeriesIndexer indexer = new TimeSeriesIndexer(2,6);
            indexer.outputTree("D:/index", "test", tree, arrayList);
            indexer.printIndex("D:/index", "test");
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public static void testQuery(){
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
            fileInputStream = new FileInputStream("D:/test/Data00000");
            scanner = new Scanner(fileInputStream);
            int current = -1;
            int result = -1;
            TimeSeries candidate;
            while (scanner.hasNext()) {
                current++;
                cand = reader.readSeries(scanner);
                candidate = new TimeSeries(cand, query.bandWidth);
                candidate.initAsCand(false);
                if (caculator.matchQueryWithNormedSeries(query, candidate)) {
                    result = current;
                }
            }
            scanner.close();
            fileInputStream.close();
            System.out.println("trillion result:" + result);
            System.out.println("trillion time:" + Long.toString(new Date().getTime() - date.getTime()));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void testQueryTree() {
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
            tree = helper.generateTreeFromFile("D:/index","Data00000");
        }catch (IOException e) {
            e.printStackTrace();
        }
        Date date = new Date();
        DTWCalculator calculator = new DTWCalculator(query.getLength(), query.bandWidth);
        TimeSeriesRawReader io = new TimeSeriesRawReader("D:/index/Data00000.edt", "r", TimeSeriesRawReader.TYPE_EXCEPTION);
        Pair<double[][], Integer[]> exceptions = io.readExceptions();
        int result = 0;
        for (int i = 0; i < exceptions.getKey().length; i++) {
            TimeSeries candidate = new TimeSeries(exceptions.getKey()[i], 0.1f);
            candidate.initAsCand(false);
            if (calculator.matchQueryWithNormedSeries(query, candidate)) {
                result = exceptions.getValue()[i];
            }
        }
        System.out.println("exp search time:" + Long.toString(new Date().getTime() - date.getTime()));
        TimeSeriesLoader loader = new TimeSeriesLoader("D:/index/Data00000");
        Date searchTime = new Date();
        int result2 = calculator.treeSearch(query, tree, loader);
        System.out.println("tree search time:" + Long.toString(new Date().getTime() - searchTime.getTime()));
        if (result2 != -1) {
            System.out.println("my method tree:" + result2);
        }else {
            System.out.println("my method exp:" + result);
        }

    }

    public static void testIO() {
        TimeSeriesIndexer indexer = new TimeSeriesIndexer(10,20);
        try {
            indexer.printIndex("D:/index", "ItalyPowerDemand_TRAIN");
        }catch (IOException e) {
            e.printStackTrace();
        }
        TimeSeriesRawReader timeSeriesRawReader = new TimeSeriesRawReader("D:/index/ItalyPowerDemand_TRAIN.edt", "r", TimeSeriesRawReader.TYPE_EXCEPTION);
        Pair<double[][], Integer[]> pairs = timeSeriesRawReader.readExceptions();
        for (int i = 0; i < pairs.getValue().length; i++) {
            System.out.println("key: " + pairs.getValue()[i]);
            for (int j = 0; j < pairs.getKey()[0].length; j++) {
                System.out.print(pairs.getKey()[i][j] + " ");
            }
            System.out.println("\n");
        }
        timeSeriesRawReader.close();
        timeSeriesRawReader = new TimeSeriesRawReader("D:/index/ItalyPowerDemand_TRAIN.odt", "r", TimeSeriesRawReader.TYPE_MAPPEDDATA);
        double[][] orderedData = timeSeriesRawReader.readAll();
        timeSeriesRawReader.close();
        for (int i = 0; i < orderedData.length; i++) {
            System.out.println(i);
            for (int j = 0; j < orderedData[0].length; j++) {
                System.out.print(orderedData[i][j] + " ");
            }
            System.out.println("\n");
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

    public static void testIndex(){
        TimeSeriesIndexer indexer = new TimeSeriesIndexer(10, 40);
        indexer.creatIndex("D:/test", "D:/index", 4.0f,
                0.03f, 0.1f, false, " ");
    }
}

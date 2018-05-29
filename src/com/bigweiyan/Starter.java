package com.bigweiyan;

import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.MappedTimeSeriesLoader;
import com.bigweiyan.util.Pair;
import com.bigweiyan.util.TimeSeriesParser;
import com.bigweiyan.util.TimeSeriesRawIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

public class Starter {
    LBRSConfiguration configuration;
    String inputFolder;
    String dataName;
    String indexFolder;
    public Starter(LBRSConfiguration configuration, String inputFolder, String dataName, String indexFolder) {
        this.configuration = configuration;
        this.dataName = dataName;
        this.indexFolder = indexFolder;
        this.inputFolder = inputFolder;
    }

    public void LBRSSearch(String queryFileName) throws IOException{
        System.out.println("----------read index----------");
        Date start = new Date();
        STRTreeHelper helper = new STRTreeHelper(configuration.getTreeDegree());
        TimeSeriesParser reader = new TimeSeriesParser(configuration.getDevider());
        TimeSeries query = null;
        STRTree tree = null;
        tree = helper.generateTreeFromFile(indexFolder,dataName);
        System.out.println("index io time:" + Long.toString(new Date().getTime() - start.getTime()));
        System.out.println("----------query-lmrs----------");
        FileInputStream fileInputStream = new FileInputStream(queryFileName);
        Scanner scanner = new Scanner(fileInputStream);
        long expSearchTime = 0;
        long treeSearchTime = 0;
        DTWCalculator calculator = null;
        TimeSeriesRawIO io = new TimeSeriesRawIO(indexFolder + "/" + dataName + ".edt", true, TimeSeriesRawIO.TYPE_EXCEPTION, 1);
        MappedTimeSeriesLoader loader = new MappedTimeSeriesLoader(indexFolder + "/" + dataName);
        while (scanner.hasNext()) {
            Date date = new Date();
            query = new TimeSeries(reader.readSeriesAndNormalize(scanner.nextLine()), configuration.getBandRate());
            query.initAsQuery(false);
            if (calculator == null) calculator = new DTWCalculator(query.getLength(), query.bandWidth);
            calculator.reset();
            int result = 0;
            for (int i = 0; i < io.getTotalSeries(); i++) {
                Pair<double[], Integer> pair = io.bufferedReadException(i);
                TimeSeries candidate = new TimeSeries(pair.getKey(), configuration.getBandRate());
                candidate.initAsCand(false);
                if (calculator.matchQueryWithNormedSeries(query, candidate)) {
                    result = pair.getValue();
                }
            }
            expSearchTime += new Date().getTime() - date.getTime();
            Date searchTime = new Date();
            int result2 = calculator.treeSearch(query, tree, loader);
            if (result2 != -1) {
                System.out.println("result(tree):" + result2);
            } else {
                System.out.println("result(exp):" + result);
            }
            treeSearchTime += new Date().getTime() - searchTime.getTime();
        }
        io.close();
        loader.close();
        System.out.println("exp time:" + expSearchTime + "ms, tree time:" + treeSearchTime+ "ms, total time:" + (treeSearchTime + expSearchTime));
        scanner.close();
        fileInputStream.close();
    }

    public void trillionSearch(String queryFileName) {
        System.out.println("----------query-trillion----------");
        TimeSeriesParser reader = new TimeSeriesParser(configuration.getDevider());
        TimeSeries query = null;
        double[] cand = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(queryFileName);
            Scanner scanner = new Scanner(fileInputStream);
            Date date = new Date();
            DTWCalculator calculator = null;
            while (scanner.hasNext()) {
                query = new TimeSeries(reader.readSeriesAndNormalize(scanner.nextLine()), configuration.getBandRate());
                query.initAsQuery(false);
                if (calculator == null) calculator = new DTWCalculator(query.getLength(), query.bandWidth);
                calculator.reset();
                TimeSeriesRawIO timeSeriesReader = new TimeSeriesRawIO(indexFolder + "/" + dataName + ".raw", true,
                        TimeSeriesRawIO.TYPE_PURE_DATA, 1);
                int totalSeries = timeSeriesReader.getTotalSeries();
                int result = -1;
                TimeSeries candidate;
                for (int i = 0; i < totalSeries; i++) {
                    cand = timeSeriesReader.bufferedRead(i);
                    candidate = new TimeSeries(cand, query.bandWidth);
                    candidate.initAsCand(false);
                    if (calculator.matchQueryWithNormedSeries(query, candidate)) {
                        result = i;
                    }
                }
                timeSeriesReader.close();
                System.out.println("trillion result:" + result);
            }
            scanner.close();
            fileInputStream.close();
            System.out.println("trillion time:" + Long.toString(new Date().getTime() - date.getTime()));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void index()throws IOException {
        TimeSeriesIndexer indexer = new TimeSeriesIndexer(configuration.getLmbrDim(), configuration.getTreeDegree());
        indexer.creatIndex(inputFolder,dataName, indexFolder, configuration.getDiffThreshold(),
                configuration.getUsageThreshold(), configuration.getBandRate(), configuration.isHasLable(), configuration.getDevider());
    }
}

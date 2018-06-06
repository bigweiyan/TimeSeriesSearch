package com.bigweiyan;

import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.MappedTimeSeriesLoader;
import com.bigweiyan.util.Pair;
import com.bigweiyan.util.TimeSeriesParser;
import com.bigweiyan.util.TimeSeriesRawIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public void LBRSSearch(String queryFileName, boolean hasLable) throws IOException{
        int shiftNum = configuration.getShiftNum();
        System.out.println("----------read index----------");
        Date start = new Date();
        STRTreeHelper helper = new STRTreeHelper(configuration.getTreeDegree());
        TimeSeriesParser reader = new TimeSeriesParser(configuration.getDevider());
        TimeSeries query = null;
        List<STRTree> trees = new ArrayList<>(shiftNum);
        for (int i = 0; i < shiftNum; i++) {
            trees.add(helper.generateTreeFromFile(indexFolder, dataName + "." + i));
        }
        System.out.println("index io time:" + Long.toString(new Date().getTime() - start.getTime()));
        System.out.println("----------query-lmrs----------");
        FileInputStream fileInputStream = new FileInputStream(queryFileName);
        Scanner scanner = new Scanner(fileInputStream);
        long expSearchTime = 0;
        long treeSearchTime = 0;
        DTWCalculator calculator = null;
        TimeSeriesRawIO io = new TimeSeriesRawIO(indexFolder + "/" + dataName + ".edt", true, TimeSeriesRawIO.TYPE_EXCEPTION, 1);

        List<MappedTimeSeriesLoader> loaders = new ArrayList<>(shiftNum);
        for (int i = 0; i < shiftNum; i++) {
            loaders.add(new MappedTimeSeriesLoader(indexFolder + "/" + dataName + "." + i));
        }
        int querySize = 0;
        while (scanner.hasNext()) {
            querySize++;
            Date searchTime = new Date();
            String queryLine = scanner.nextLine();
            if (hasLable) {
                queryLine = queryLine.split(configuration.getDevider(),2)[1];
            }
            query = new TimeSeries(reader.readSeriesAndNormalize(queryLine), configuration.getBandRate());
            query.initAsQuery(false);
            if (calculator == null) calculator = new DTWCalculator(query.getLength(), query.bandWidth);
            calculator.reset();
            int result = -1;
            for (int i = 0; i < io.getTotalSeries(); i++) {
                Pair<double[], Integer> pair = io.bufferedReadException(i);
                TimeSeries candidate = new TimeSeries(pair.getKey(), configuration.getBandRate());
                candidate.initAsCand(false);
                if (calculator.matchQueryWithNormedSeries(query, candidate, false)) {
                    result = pair.getValue();
                }
            }
            expSearchTime += new Date().getTime() - searchTime.getTime();
            Date date = new Date();
            int treeNum = 0;
            int treeResult = -1;
            int minSegmentLen = query.getLength() / configuration.getLmbrDim();
            for (int i = 0; i < shiftNum; i++) {
                int tmp = calculator.shiftTreeSearch(query, trees.get(i), (int)(minSegmentLen * 1.0 * i / shiftNum), loaders.get(i));
                if (tmp != -1) {
                    treeResult = tmp;
                    treeNum = i;
                }
            }
            treeSearchTime += new Date().getTime() - date.getTime();
            if (treeResult != -1) {
                System.out.println("result(tree " + treeNum + "):" + treeResult);
            } else {
                System.out.println("result(exp):" + result);
            }

        }
        io.close();
        for (MappedTimeSeriesLoader loader:loaders) {
            loader.close();
        }
        System.out.print("exp time:" + expSearchTime + "ms, tree time:" + treeSearchTime+ "ms, total time:" + (treeSearchTime + expSearchTime));
        System.out.println(", avg time:" + (treeSearchTime + expSearchTime) / querySize);
        System.out.println("dtw count:"+calculator.leafCount + ", lb_lmbr count:" + calculator.nodeCount);
        scanner.close();
        fileInputStream.close();
    }

    public void UCRSearch(String queryFileName, boolean hasLable, boolean useUSP) {
        System.out.println("----------query-UCR----------");
        TimeSeriesParser reader = new TimeSeriesParser(configuration.getDevider());
        TimeSeries query = null;
        double[] cand = null;
        int querySize = 0;
        try {
            FileInputStream fileInputStream = new FileInputStream(queryFileName);
            Scanner scanner = new Scanner(fileInputStream);
            Date date = new Date();
            DTWCalculator calculator = null;
            while (scanner.hasNext()) {
                querySize++;
                String queryLine = scanner.nextLine();
                if (hasLable) {
                    queryLine = queryLine.split(configuration.getDevider(),2)[1];
                }
                query = new TimeSeries(reader.readSeriesAndNormalize(queryLine), configuration.getBandRate());
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
                    if (calculator.matchQueryWithNormedSeries(query, candidate, useUSP)) {
                        result = i;
                    }
                }
                timeSeriesReader.close();
                System.out.println("trillion result:" + result);
            }
            scanner.close();
            fileInputStream.close();
            long useTime = new Date().getTime() - date.getTime();
            System.out.println("trillion time:" + Long.toString(useTime) + ", avg time:" + useTime / querySize);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void index()throws IOException {
        TimeSeriesIndexer indexer = new TimeSeriesIndexer(configuration.getLmbrDim(), configuration.getTreeDegree());
        indexer.creatIndex(inputFolder,dataName, indexFolder, configuration);
    }
}

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
        while (scanner.hasNext()) {
            Date date = new Date();
            String queryLine = scanner.nextLine();
            if (hasLable) {
                queryLine = queryLine.split(configuration.getDevider(),2)[1];
            }
            query = new TimeSeries(reader.readSeriesAndNormalize(queryLine), configuration.getBandRate());
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
            if (treeResult != -1) {
                System.out.println("result(tree " + treeNum + "):" + treeResult);
            } else {
                System.out.println("result(exp):" + result);
            }
            treeSearchTime += new Date().getTime() - searchTime.getTime();
        }
        io.close();
        for (MappedTimeSeriesLoader loader:loaders) {
            loader.close();
        }
        System.out.println("exp time:" + expSearchTime + "ms, tree time:" + treeSearchTime+ "ms, total time:" + (treeSearchTime + expSearchTime));
        System.out.println("dtw count:"+calculator.leafCount + ", lb_lmbr count:" + calculator.nodeCount);
        scanner.close();
        fileInputStream.close();
    }

    public void trillionSearch(String queryFileName, boolean hasLable) {
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
        indexer.creatIndex(inputFolder,dataName, indexFolder, configuration);
    }
}

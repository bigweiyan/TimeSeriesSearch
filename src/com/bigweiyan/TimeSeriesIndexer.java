package com.bigweiyan;

import com.bigweiyan.strtree.LMBR;
import com.bigweiyan.strtree.LMBRHelper;
import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.BitTool;
import com.bigweiyan.util.Pair;
import com.bigweiyan.util.TimeSeriesParser;
import com.bigweiyan.util.TimeSeriesRawIO;

import java.io.*;
import java.util.*;

public class TimeSeriesIndexer {
    public static final int TYPE_INNER_NODE = 0;
    public static final int TYPE_LEAF_NODE = 1;
    public static final int TREE_FILE_HEAD = 12;
    public static final int INDEX_FILE_HEAD = 4;
    private int lmbrDim;
    private int treeDegree;
    public long bufferIOTime = 0;
    public long myIOTime = 0;
    public TimeSeriesIndexer(int lmbrDim, int treeDegree) {
        this.lmbrDim = lmbrDim;
        this.treeDegree = treeDegree;
    }

    public void creatIndex(String inputFolder, String indexFolder, float diffThreshold, float usageThreshold,
                           float bandRate, boolean haveLabel, String divider) throws IOException{
        File file = new File(inputFolder);
        for (File dataFile:file.listFiles()) {
            if (dataFile.isFile()) {
                // input data
                System.out.println("-----------create index-----------");
                Date date = new Date();
                ArrayList<Integer> exceptionIDs = new ArrayList<>();
                ArrayList<Pair<Integer, LMBR>> lmbrAndIds = new ArrayList<>();
                ArrayList<String> labels = new ArrayList<>();
                int seriesLength = 0;
                Scanner scanner = new Scanner(new FileInputStream(dataFile));
                TimeSeriesParser reader = new TimeSeriesParser(divider);
                String line;
                TimeSeriesRawIO rawWriter;
                TimeSeriesEnvelop currentEnvelop;
                double currentData[];
                LMBRHelper lmbrHelper;
                int dataSize = 0;
                // read data, split it into lmbrs and exceptions and then write it back

                if (scanner.hasNext()) {
                    line = scanner.nextLine();
                    if (line.isEmpty()) break;
                    if (haveLabel) {
                        String[] lines = line.split(divider, 2);
                        line = lines[1];
                        labels.add(lines[0]);
                    }
                    currentData = reader.readSeries(line);
                    seriesLength = currentData.length;
                    rawWriter = new TimeSeriesRawIO(indexFolder + "/" + dataFile.getName() + ".raw", false,
                            TimeSeriesRawIO.TYPE_PURE_DATA, 1, seriesLength);
                    rawWriter.bufferedWrite(currentData);
                    currentEnvelop = new TimeSeriesEnvelop(currentData, bandRate);
                    lmbrHelper = new LMBRHelper(currentEnvelop.lowerEnvelop.length, lmbrDim);
                    lmbrHelper.setThreshold(diffThreshold, usageThreshold);
                    lmbrHelper.testLMBR(currentEnvelop, dataSize, lmbrAndIds, exceptionIDs);
                    dataSize++;
                    while (scanner.hasNext()){
                        line = scanner.nextLine();
                        if (line.isEmpty()) break;
                        if (haveLabel) {
                            String[] lines = line.split(divider, 2);
                            line = lines[1];
                            labels.add(lines[0]);
                        }
                        currentData = reader.readSeries(line);
                        rawWriter.bufferedWrite(currentData);
                        currentEnvelop = new TimeSeriesEnvelop(currentData, bandRate);
                        lmbrHelper.testLMBR(currentEnvelop, dataSize, lmbrAndIds, exceptionIDs);
                        dataSize++;
                    }
                    rawWriter.close();
                }else {
                    throw new IllegalArgumentException("file format isn't right");
                }
                scanner.close();
                System.out.println("exceptions: " + exceptionIDs.size() + ", mbrs: " + lmbrAndIds.size());
                System.out.println("split time:"+Long.toString(new Date().getTime() - date.getTime()));


                // save exceptions
                date = new Date();
                TimeSeriesRawIO exceptionsWriter = new TimeSeriesRawIO(indexFolder + "/" + dataFile.getName() + ".edt",
                        false, TimeSeriesRawIO.TYPE_EXCEPTION, 1, seriesLength);
                TimeSeriesRawIO rawReader = new TimeSeriesRawIO(indexFolder + "/" + dataFile.getName() + ".raw",
                        true, TimeSeriesRawIO.TYPE_PURE_DATA, 1);
                for (int i = 0; i < exceptionIDs.size(); i++) {
                    int pos = exceptionIDs.get(i);
                    exceptionsWriter.bufferedWriteException(rawReader.bufferedRead(pos), pos);
                }
                exceptionsWriter.close();
                System.out.println("exception write time:"+Long.toString(new Date().getTime() - date.getTime()));

                //save classes
                if (haveLabel) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(indexFolder + "/" + dataFile.getName() + ".clz"), 2048);
                    writer.write(labels.get(0));
                    for (int i = 0; i < labels.size(); i++) {
                        writer.write(divider);
                        writer.write(labels.get(i));
                    }
                    writer.flush();
                    writer.close();
                }

                // create and save tree
                date = new Date();
                STRTreeHelper strTreeHelper = new STRTreeHelper(treeDegree);
                STRTree tree = strTreeHelper.generateTreeFromMemory(lmbrAndIds);
                System.out.println("tree generate time:"+Long.toString(new Date().getTime() - date.getTime())
                        + ", split time:" + strTreeHelper.splitTime + ", sort time:" + strTreeHelper.sortTime
                        + ", create object time:" + strTreeHelper.objectTime);
                date = new Date();
                outputTree(indexFolder, dataFile.getName(), tree, rawReader, dataSize, seriesLength);
                rawReader.close();
                System.out.println("index write time:"+Long.toString(new Date().getTime() - date.getTime())
                        + ", bufferIOTime:" + bufferIOTime + ", myIOTime:" + myIOTime);
            }
        }
    }

    public void outputTree(String indexFolder, String indexName, STRTree tree, TimeSeriesRawIO rawReader, int dataSize,
                           int seriesLength) throws IOException {
        int size = 500;
        int lineLength = lmbrDim * 20 + treeDegree * 4 + 1;
        BufferedOutputStream treSteam = new BufferedOutputStream(new FileOutputStream(indexFolder + "/" + indexName + ".tre"), lineLength * size);
        byte tmp[] = new byte[12];
        BitTool.intToBytes(lmbrDim, tmp, 0);
        BitTool.intToBytes(treeDegree, tmp, 4);
        for (int i = 0; i < 4; i++)
            tmp[8 + i] = 0;
        treSteam.write(tmp);
        Deque<Pair<STRTree, Integer>> nodeQueue = new LinkedList<>();
        List<Pair<Integer, LMBR>> seriesList = new LinkedList<>();
        nodeQueue.addLast(new Pair<>(tree, 0));
        int totalLength = 0;
        while (!nodeQueue.isEmpty()) {
            Pair<STRTree, Integer> next = nodeQueue.peekFirst();
            totalLength = next.getValue() + 1;
            if (next.getKey().getClass() == STRTree.class) {
                STRTree node = next.getKey();
                if (node.isLeaf) {
                    outPutLeafNode(treSteam, node, seriesList);
                }else {
                    outPutInnerNode(treSteam, node, nodeQueue);
                }
            }
            nodeQueue.pollFirst();
        }
        treSteam.flush();
        treSteam.close();
        RandomAccessFile treFile = new RandomAccessFile(indexFolder + "/" + indexName + ".tre", "rw");
        treFile.seek(8);
        treFile.writeInt(totalLength);
        treFile.close();

        lineLength = lmbrDim * 20 + 4;
        BufferedOutputStream idxStream = new BufferedOutputStream(
                new FileOutputStream(indexFolder + "/" + indexName + ".idx"), lineLength * size);
        BufferedOutputStream mapStream = new BufferedOutputStream(
                new FileOutputStream(indexFolder + "/" + indexName + ".map"), 4 * size);
        byte intTemp[] = new byte[4];
        BitTool.intToBytes(dataSize, intTemp, 0);
        mapStream.write(intTemp);
        BitTool.intToBytes(seriesList.size(), intTemp, 0);
        idxStream.write(intTemp);
        TimeSeriesRawIO timeSeriesRawIO = new TimeSeriesRawIO(indexFolder + "/" + indexName + ".odt",
                false, TimeSeriesRawIO.TYPE_PURE_DATA, 1, seriesLength);
        for (int i = 0; i < seriesList.size(); i++) {
            outPutLMBR(idxStream, seriesList.get(i),mapStream);
            Date date = new Date();
            timeSeriesRawIO.bufferedWrite(rawReader.bufferedRead(seriesList.get(i).getKey()));
            myIOTime += new Date().getTime() - date.getTime();
        }
        timeSeriesRawIO.close();
        idxStream.flush();
        idxStream.close();
        mapStream.flush();
        mapStream.close();
    }

    private void outPutInnerNode(BufferedOutputStream nodeStream, STRTree innerNode,
                                 Deque<Pair<STRTree, Integer>> nodeQueue) throws IOException {
        int lineLength = 1 + 20 * lmbrDim + treeDegree * 4;
        byte[] tmp = new byte[lineLength];
        tmp[0] = TYPE_INNER_NODE;
        int pos = 1;
        for (int i = 0; i < lmbrDim; i++) {
            BitTool.doubleToBytes(innerNode.lmbr.upper[i], tmp, pos);
            BitTool.doubleToBytes(innerNode.lmbr.lower[i], tmp, pos + 8);
            BitTool.intToBytes(innerNode.lmbr.weights[i], tmp, pos + 16);
            pos += 20;
        }
        int lastId = nodeQueue.peekLast().getValue();
        for (int i = 0; i < innerNode.children.length; i++) {
            nodeQueue.addLast(new Pair<>(innerNode.children[i], lastId + i + 1));
            BitTool.intToBytes(lastId + i + 1, tmp, pos);
            pos += 4;
        }
        for (int i = innerNode.children.length; i <treeDegree; i++) {
            BitTool.intToBytes(-1, tmp, pos);
            pos += 4;
        }
        Date date = new Date();
        nodeStream.write(tmp);
        bufferIOTime += new Date().getTime() - date.getTime();
    }

    private void outPutLeafNode(BufferedOutputStream nodeStream, STRTree leafNode,
                                List<Pair<Integer, LMBR>> seriesQueue) throws IOException {
        int lineLength = 1 + 20 * lmbrDim + treeDegree * 4;
        byte tmp[] = new byte[lineLength];
        tmp[0] = TYPE_LEAF_NODE;
        int pos = 1;
        for (int i = 0; i < lmbrDim; i++) {
            BitTool.doubleToBytes(leafNode.lmbr.upper[i], tmp, pos);
            BitTool.doubleToBytes(leafNode.lmbr.lower[i], tmp, pos + 8);
            BitTool.intToBytes(leafNode.lmbr.weights[i], tmp, pos + 16);
            pos += 20;
        } // this is lmbrDim * 20 bytes
        int seriesID = seriesQueue.size();
        for (int i = 0; i < leafNode.series.length; i++) {
            seriesQueue.add(leafNode.series[i]);
            BitTool.intToBytes(seriesID + i, tmp, pos);
            pos += 4;
        }
        for (int i = leafNode.series.length; i <treeDegree; i++) {
            BitTool.intToBytes(-1, tmp, pos);
            pos += 4;
        } // this is degree * 4 bytes
        Date date = new Date();
        nodeStream.write(tmp);
        bufferIOTime += new Date().getTime() - date.getTime();
    }

    private void outPutLMBR(BufferedOutputStream lmbrStream, Pair<Integer, LMBR> series, BufferedOutputStream mapStream) throws IOException{
        byte tmp[] = new byte[4 + lmbrDim * 20];
        BitTool.intToBytes(series.getKey(), tmp, 0);
        int pos = 4;
        for (int i = 0; i < lmbrDim; i++) {
            BitTool.doubleToBytes(series.getValue().upper[i], tmp, pos);
            BitTool.doubleToBytes(series.getValue().lower[i], tmp, pos + 8);
            BitTool.intToBytes(series.getValue().weights[i], tmp, pos + 16);
            pos += 20;
        }
        Date date = new Date();
        lmbrStream.write(tmp);
        byte intTmp[] = new byte[4];
        BitTool.intToBytes(series.getKey(), intTmp, 0);
        mapStream.write(intTmp);
        bufferIOTime += new Date().getTime() - date.getTime();
    }

    public void printIndex(String indexFolder, String indexName) throws IOException{
        RandomAccessFile file = new RandomAccessFile(indexFolder + "/" + indexName + ".tre", "r");
        int dim = file.readInt();
        int degree = file.readInt();

        System.out.println("dim: " + dim + "  degree: " + degree);
        int totalLength = file.readInt();
        for (int i = 0; i < totalLength; i++) {
            System.out.print("id: " +i);
            byte type = file.readByte();
            if (type == TYPE_INNER_NODE) {
                System.out.println(" INNER_NODE");
            }else if (type == TYPE_LEAF_NODE) {
                System.out.println(" LEAF_NODE");
            }
            double[] upper = new double[dim];
            double[] lower = new double[dim];
            int[] weight = new int[dim];
            System.out.print("upper: ");
            for (int j = 0; j < dim; j++) {
                upper[j] = file.readDouble();
                System.out.print(upper[j] + " ");
                lower[j] = file.readDouble();
                weight[j] = file.readInt();
            }
            System.out.print("\nlower: ");
            for (int j = 0; j < dim; j++) {
                System.out.print(lower[j] + " ");
            }
            System.out.print("\nweight: ");
            for (int j = 0; j < dim; j++) {
                System.out.print(weight[j] + " ");
            }
            System.out.print("\nchildId: ");
            for (int j = 0; j < degree; j++) {
                System.out.print(file.readInt() + " ");
            }
            System.out.println("\n");
        }
        file.close();

        file = new RandomAccessFile(indexFolder + "/" + indexName + ".idx", "r");
        totalLength = file.readInt();
        for (int i =0; i < totalLength; i++) {
            System.out.print("i: " + i);
            System.out.println(", key: " + file.readInt());
            double[] upper = new double[dim];
            double[] lower = new double[dim];
            int[] weight = new int[dim];
            System.out.print("upper: ");
            for (int j = 0; j < dim; j++) {
                upper[j] = file.readDouble();
                System.out.print(upper[j] + " ");
                lower[j] = file.readDouble();
                weight[j] = file.readInt();
            }
            System.out.print("\nlower: ");
            for (int j = 0; j < dim; j++) {
                System.out.print(lower[j] + " ");
            }
            System.out.print("\nweight: ");
            for (int j = 0; j < dim; j++) {
                System.out.print(weight[j] + " ");
            }
            System.out.println();
        }
        file.close();
    }
}

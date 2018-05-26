package com.bigweiyan;

import com.bigweiyan.strtree.LMBR;
import com.bigweiyan.strtree.LMBRHelper;
import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.strtree.STRTreeHelper;
import com.bigweiyan.util.Pair;

import java.io.*;
import java.util.*;

public class TimeSeriesIndexer {
    public static final int TYPE_INNER_NODE = 0;
    public static final int TYPE_LEAF_NODE = 1;
    public static final int TREE_FILE_HEAD = 12;
    public static final int INDEX_FILE_HEAD = 4;
    private int lmbrDim;
    private int treeDegree;
    public TimeSeriesIndexer(int lmbrDim, int treeDegree) {
        this.lmbrDim = lmbrDim;
        this.treeDegree = treeDegree;
    }

    public void creatIndex(String inputFolder, String indexFolder, float diffThreshold, float usageThreshold,
                           float bandRate, boolean haveLabel, String divider) {
        ArrayList<String> labels = null;
        File file = new File(inputFolder);
        for (File dataFile:file.listFiles()) {
            if (dataFile.isFile()) {
                // input data
                ArrayList<double[]> data = new ArrayList<>();
                try {
                    Scanner scanner = new Scanner(new FileInputStream(dataFile));
                    TimeSeriesParser reader = new TimeSeriesParser(divider);
                    labels = new ArrayList<>();
                    String line;
                    while (scanner.hasNext()){
                        line = scanner.nextLine();
                        if (line.isEmpty()) break;
                        if (haveLabel) {
                            String[] lines = line.split(divider, 2);
                            line = lines[1];
                            labels.add(lines[0]);
                        }
                        data.add(reader.readSeries(line));
                    }
                    scanner.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
                // create envelop
                TimeSeriesEnvelop envelops[] = new TimeSeriesEnvelop[data.size()];
                for (int i = 0; i < envelops.length; i++) {
                    envelops[i] = new TimeSeriesEnvelop(data.get(i), bandRate);
                }
                // create lmbrs and exceptions
                ArrayList<Integer> exceptions = new ArrayList<>(envelops.length);
                ArrayList<Pair<Integer, LMBR>> lmbrs = new ArrayList<>(envelops.length);
                LMBRHelper lmbrHelper = new LMBRHelper(envelops[0].lowerEnvelop.length, lmbrDim);
                lmbrHelper.setThreshold(diffThreshold, usageThreshold);
                for (int i = 0; i < envelops.length; i++) {
                    lmbrHelper.testLMBR(envelops[i], i, lmbrs, exceptions);
                }
                System.out.println("exceptions: " + exceptions.size() + ", mbrs: " + lmbrs.size());
                // save exceptions
                double[][] exception = new double[exceptions.size()][data.get(0).length];
                int[] ids = new int[exceptions.size()];
                for (int i = 0; i < exceptions.size(); i++) {
                    ids[i] = exceptions.get(i);
                    System.arraycopy(data.get(ids[i]), 0, exception[i], 0, data.get(0).length);
                }
                TimeSeriesIO timeSeriesIO = new TimeSeriesIO(indexFolder + "/" + dataFile.getName() + ".edt", "rw");
                timeSeriesIO.saveAllException(exception,ids);
                timeSeriesIO.close();

                try {
                    //save classes
                    if (haveLabel) {
                        FileWriter writer = new FileWriter(indexFolder + "/" + dataFile.getName() + ".clz");
                        writer.write(labels.get(0));
                        for (int i = 0; i < labels.size(); i++) {
                            writer.write(divider);
                            writer.write(labels.get(i));
                        }
                        writer.close();
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }

                // create and save tree
                STRTreeHelper strTreeHelper = new STRTreeHelper(treeDegree);
                STRTree tree = strTreeHelper.generateTreeFromMemory(lmbrs);
                try {
                    outputTree(indexFolder, dataFile.getName(), tree, data);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void outputTree(String indexFolder, String indexName, STRTree tree, ArrayList<double[]> datas) throws IOException {
        RandomAccessFile treFile = new RandomAccessFile(indexFolder + "/" + indexName + ".tre", "rw");
        treFile.writeInt(lmbrDim);
        treFile.writeInt(treeDegree);
        treFile.writeInt(0);
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
                    outPutLeafNode(treFile, node, seriesList);
                }else {
                    outPutInnerNode(treFile, node, nodeQueue);
                }
            }
            nodeQueue.pollFirst();
        }
        treFile.seek(8);
        treFile.writeInt(totalLength);
        treFile.close();

        RandomAccessFile idxFile = new RandomAccessFile(indexFolder + "/" + indexName + ".idx", "rw");
        RandomAccessFile mapFile = new RandomAccessFile(indexFolder + "/" + indexName + ".map", "rw");
        mapFile.writeInt(datas.size());
        idxFile.writeInt(seriesList.size());
        TimeSeriesIO timeSeriesIO = new TimeSeriesIO(indexFolder + "/" + indexName + ".odt", "rw");
        for (int i = 0; i < seriesList.size(); i++) {
            outPutLMBR(idxFile, seriesList.get(i),mapFile);
            timeSeriesIO.save(datas.get(seriesList.get(i).getKey()));
        }
        timeSeriesIO.close();
        idxFile.close();
        mapFile.close();
    }

    private void outPutInnerNode(RandomAccessFile file, STRTree innerNode,
                                 Deque<Pair<STRTree, Integer>> nodeQueue) throws IOException {
        file.writeByte(TYPE_INNER_NODE);
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(innerNode.lmbr.upper[i]);
            file.writeDouble(innerNode.lmbr.lower[i]);
            file.writeInt(innerNode.lmbr.weights[i]);
        }
        int lastId = nodeQueue.peekLast().getValue();
        for (int i = 0; i < innerNode.children.length; i++) {
            nodeQueue.addLast(new Pair<>(innerNode.children[i], lastId + i + 1));
            file.writeInt(lastId + i + 1);
        }
        for (int i = innerNode.children.length; i <treeDegree; i++) {
            file.writeInt(-1);
        }
    }

    private void outPutLeafNode(RandomAccessFile file, STRTree leafNode,
                                List<Pair<Integer, LMBR>> seriesQueue) throws IOException {
        file.writeByte(TYPE_LEAF_NODE); // this is 1 byte
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(leafNode.lmbr.upper[i]);
            file.writeDouble(leafNode.lmbr.lower[i]);
            file.writeInt(leafNode.lmbr.weights[i]);
        } // this is lmbrDim * 20 bytes
        int seriesID = seriesQueue.size();
        for (int i = 0; i < leafNode.series.length; i++) {
            seriesQueue.add(leafNode.series[i]);
            file.writeInt(seriesID + i);
        }
        for (int i = leafNode.series.length; i <treeDegree; i++) {
            file.writeInt(-1);
        } // this is degree * 4 bytes
    }

    private void outPutLMBR(RandomAccessFile file, Pair<Integer, LMBR> series, RandomAccessFile mapFile) throws IOException{
        file.writeInt(series.getKey());
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(series.getValue().upper[i]);
            file.writeDouble(series.getValue().lower[i]);
            file.writeInt(series.getValue().weights[i]);
        }
        mapFile.writeInt(series.getKey());
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

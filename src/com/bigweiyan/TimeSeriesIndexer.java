package com.bigweiyan;

import com.bigweiyan.strtree.LMBR;
import com.bigweiyan.strtree.STRTree;
import com.bigweiyan.util.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class TimeSeriesIndexer {
    private static final int TYPE_INNER_NODE = 0;
    private static final int TYPE_LEAF_NODE = 1;
    private static final int TYPE_SERIES = 2;
    private int lmbrDim;
    private int treeDegree;
    public TimeSeriesIndexer(int lmbrDim, int treeDegree) {
        this.lmbrDim = lmbrDim;
        this.treeDegree = treeDegree;
    }

    public void creatIndex(String inputFolder, String indexFolder) {

    }

    public void outputTree(String indexFolder, int indexNo, STRTree tree) throws IOException {
        RandomAccessFile file = new RandomAccessFile(indexFolder + "/" + indexNo + ".idx", "rw");
        int lineSize = 5 + 16 * lmbrDim + 4 * treeDegree;
        file.writeInt(lmbrDim);
        file.writeInt(treeDegree);
        file.writeInt(0);
        Deque<Pair<Object, Integer>> todo = new LinkedList<>();
        todo.addLast(new Pair<>(tree, 0));
        int totalLength = 0;
        while (!todo.isEmpty()) {
            Pair<Object, Integer> next = todo.peekFirst();
            totalLength = next.getValue() + 1;
            if (next.getKey().getClass() == STRTree.class) {
                STRTree node = (STRTree) next.getKey();
                if (node.isLeaf) {
                    outPutLeafNode(file, next.getValue(), node, todo);
                }else {
                    outPutInnerNode(file, next.getValue(), node, todo);
                }
            }
            if (next.getKey().getClass() == Pair.class) {
                outPutLMBR(file, next.getValue(), (Pair<Integer, LMBR>) next.getKey());
            }
            todo.pollFirst();
        }
        file.seek(8);
        file.writeInt(totalLength);
        file.close();
    }

    private void outPutInnerNode(RandomAccessFile file, int id, STRTree innerNode, Deque<Pair<Object, Integer>> todo)
            throws IOException {
        file.writeInt(id);
        file.writeByte(TYPE_INNER_NODE);
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(innerNode.lmbr.upper[i]);
            file.writeDouble(innerNode.lmbr.lower[i]);
        }
        int lastId = todo.peekLast().getValue();
        for (int i = 0; i < innerNode.children.length; i++) {
            todo.addLast(new Pair<>(innerNode.children[i], lastId + i + 1));
            file.writeInt(lastId + i + 1);
        }
        for (int i = innerNode.children.length; i <treeDegree; i++) {
            file.writeInt(-1);
        }
    }

    private void outPutLeafNode(RandomAccessFile file, int id, STRTree leafNode, Deque<Pair<Object, Integer>> todo)
            throws IOException {
        file.writeInt(id);
        file.writeByte(TYPE_LEAF_NODE);
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(leafNode.lmbr.upper[i]);
            file.writeDouble(leafNode.lmbr.lower[i]);
        }
        int lastId = todo.peekLast().getValue();
        for (int i = 0; i < leafNode.series.length; i++) {
            todo.addLast(new Pair<>(leafNode.series[i], lastId + i + 1));
            file.writeInt(lastId + i + 1);
        }
        for (int i = leafNode.series.length; i <treeDegree; i++) {
            file.writeInt(-1);
        }
    }

    private void outPutLMBR(RandomAccessFile file, int id, Pair<Integer, LMBR> series) throws IOException{
        file.writeInt(id);
        file.writeByte(TYPE_SERIES);
        for (int i = 0; i < lmbrDim; i++) {
            file.writeDouble(series.getValue().upper[i]);
            file.writeDouble(series.getValue().lower[i]);
        }
        file.writeInt(series.getKey());
        for (int i = 1; i < treeDegree; i++) {
            file.writeInt(-1);
        }
    }

    public STRTree getIndex(String indexFolder, int indexNo) throws IOException{
        RandomAccessFile file = new RandomAccessFile(indexFolder + "/" + indexNo + ".idx", "r");
        int dim = file.readInt();
        int degree = file.readInt();
        System.out.println("dim: " + dim + "  degree: " + degree);
        int totalLength = file.readInt();
        for (int i = 0; i < totalLength; i++) {
            System.out.print("id: " +file.readInt());
            byte type = file.readByte();
            if (type == TYPE_INNER_NODE) {
                System.out.println(" INNER_NODE");
            }else if (type == TYPE_LEAF_NODE) {
                System.out.println(" LEAF_NODE");
            }else if (type == TYPE_SERIES) {
                System.out.println(" SERIES");
            }
            double[] upper = new double[dim];
            double[] lower = new double[dim];
            System.out.print("upper: ");
            for (int j = 0; j < dim; j++) {
                upper[j] = file.readDouble();
                System.out.print(upper[j] + " ");
                lower[j] = file.readDouble();
            }
            System.out.print("\nlower: ");
            for (int j = 0; j < dim; j++) {
                System.out.print(lower[j] + " ");
            }
            if (type == TYPE_SERIES)
                System.out.print("\nseriesId: ");
            else
                System.out.print("\nchildId: ");
            for (int j = 0; j < degree; j++) {
                System.out.print(file.readInt() + " ");
            }
            System.out.println("\n");
        }
        file.close();
        return null;
    }
}

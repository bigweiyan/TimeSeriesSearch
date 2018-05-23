package com.bigweiyan.strtree;

import com.bigweiyan.util.Pair;

import java.util.ArrayList;

public class STRTreeHelper {
    /**
     * max children numbers held by each node
     */
    private int degree;

    public STRTreeHelper(int degree) {
        this.degree = degree;
    }

    public STRTree generateTreeFromMemory(ArrayList<Pair<Integer, LMBR>> mbrs) {
        /**
         * how many page would this tree's leaf need. <br/> one page is at most <b>degree</b> mbrs
         */
        int pages = (int)Math.ceil(mbrs.size() * 1.0 / degree);
        int mbrDim = mbrs.get(0).getValue().upper.length;
        for (int i = 0; i < mbrs.size(); i++) {
            mbrs.get(i).getValue().center = new double[mbrDim];
            for (int j = 0; j < mbrDim; j++) {
                mbrs.get(i).getValue().center[j] =mbrs.get(i).getValue().upper[j] + mbrs.get(i).getValue().lower[j];
            }
        }
        return recursiveGenerateFromMemory(mbrs, 0, pages);
    }

    private STRTree recursiveGenerateFromMemory(ArrayList<Pair<Integer, LMBR>> mbrWithIds, int currentDim, int usePages) {
        STRTree node = null;
        int mbrDim = mbrWithIds.get(0).getValue().upper.length;
        if (mbrWithIds.size() <= degree || currentDim == mbrDim - 1) {
            ArrayList<STRTree> trees = createLeafSegment(mbrWithIds, mbrDim);
            if (trees.size() == 1) {
                node = trees.get(0);
            }else {
                node = createTreeSegment(trees, mbrDim);
            }
        } else {
            mbrWithIds.sort((Pair<Integer, LMBR> a, Pair<Integer, LMBR> b) ->{
                if (a.getValue().center[currentDim] > b.getValue().center[currentDim]) return 1;
                else return -1;
            });
            // the number of untreated dimension
            int leftDim = mbrDim - currentDim;
            // the pages will split to many parts and each part will form a tree node
            // for example: 16 pages for 2 dim, the first partLen should be 16^(1/2) = 4 pages, and second partLen
            // should be 4^0 = 1 page
            int partLen = (int)Math.ceil(Math.pow(usePages, (leftDim - 1.0) / leftDim));
            // create parameter for next recursion
            int currentPart =  -1;
            ArrayList<ArrayList<Pair<Integer, LMBR>>> paras = new ArrayList<>(degree);
            ArrayList<Pair<Integer, LMBR>> currentPara = null;
            for (int i = 0; i < mbrWithIds.size(); i++) {
                if (i % (partLen * degree) == 0) {
                    currentPara = new ArrayList<>(partLen * degree);
                    paras.add(currentPara);
                    currentPart ++;
                }
                paras.get(currentPart).add(mbrWithIds.get(i));
            }
            ArrayList<STRTree> nodes = new ArrayList<>();
            for (ArrayList<Pair<Integer, LMBR>> para : paras) {
                nodes.add(recursiveGenerateFromMemory(para, currentDim + 1, partLen));
            }
            node = createTreeSegment(nodes, mbrDim);
        }
        return node;
    }

    /**
     * create tree nodes and each tree contains no more than degree LMBR
     * @param pairs (id, LMBR) pairs
     * @param mbrDim the number of dimension each mbr have
     * @return tree nodes list
     */
    private ArrayList<STRTree> createLeafSegment(ArrayList<Pair<Integer, LMBR>> pairs, int mbrDim) {
        int resultSize = (int)Math.ceil(pairs.size() * 1.0 / degree);
        ArrayList<STRTree> result = new ArrayList<>(resultSize);
        for (int n = 0; n < resultSize; n++){
            int childSize = degree;
            if (n == resultSize - 1) {
                childSize = pairs.size() - n * degree;
            }
            STRTree node = new STRTree();
            // create bigger lmbr to contain all mbrs
            double nodeUpper[] = new double[mbrDim];
            double nodeLower[] = new double[mbrDim];
            int nodeWeight[] = new int[mbrDim];
            for (int i = 0; i < mbrDim; i++) {
                nodeUpper[i] = -Double.MAX_VALUE;
                nodeLower[i] = Double.MAX_VALUE;
                nodeWeight[i] = Integer.MAX_VALUE;
            }
            double[] currentLower, currentUpper;
            int[] currentWeight;
            for (int i = n * degree; i < n * degree + childSize; i++) {
                currentLower = pairs.get(i).getValue().lower;
                currentUpper = pairs.get(i).getValue().upper;
                currentWeight = pairs.get(i).getValue().weights;
                for (int j = 0; j < mbrDim; j++) {
                    if (currentUpper[j] > nodeUpper[j]) nodeUpper[j] = currentUpper[j];
                    if (currentLower[j] < nodeLower[j]) nodeLower[j] = currentLower[j];
                    if (currentWeight[j] < nodeWeight[j]) nodeWeight[j] = currentWeight[j];
                }
            }
            // fill node elements
            LMBR lmbr = new LMBR();
            lmbr.lower = nodeLower;
            lmbr.upper = nodeUpper;
            lmbr.weights = nodeWeight;
            node.lmbr = lmbr;
            node.isLeaf = true;
            node.series = new Pair[childSize];
            for (int i = n * degree; i < n * degree + childSize; i++) {
                LMBR tmpLmbr = new LMBR();
                int id = pairs.get(i).getKey();
                tmpLmbr.lower = new double[mbrDim];
                System.arraycopy(pairs.get(i).getValue().lower, 0, tmpLmbr.lower, 0, mbrDim);
                tmpLmbr.upper = new double[mbrDim];
                System.arraycopy(pairs.get(i).getValue().upper, 0, tmpLmbr.upper, 0, mbrDim);
                tmpLmbr.weights = new int[mbrDim];
                System.arraycopy(pairs.get(i).getValue().weights, 0, tmpLmbr.weights, 0, mbrDim);
                node.series[i%degree] = new Pair<>(id, tmpLmbr);
            }
            result.add(node);
        }
        return result;
    }

    /**
     * create tree root and each node have more than degree son
     * @param trees tree list
     * @param mbrDim the number of dimension each mbr have
     * @return the root of this tree list
     */
    private STRTree createTreeSegment(ArrayList<STRTree> trees, int mbrDim) {
        if (trees.size() == 1) return trees.get(0);
        ArrayList<STRTree> inputTree = trees;
        ArrayList<STRTree> outputTree;
        ArrayList<STRTree> batch = new ArrayList<>();
        while (inputTree.size() != 1) {
            outputTree = new ArrayList<>();
            batch.clear();
            for (int j = 0; j < inputTree.size(); j++) {
                batch.add(inputTree.get(j));
                if (batch.size() == degree) {
                    outputTree.add(mergeTree(batch, mbrDim));
                    batch.clear();
                }
            }
            if (batch.size() > 0) outputTree.add(mergeTree(batch, mbrDim));
            inputTree = outputTree;
        }
        return inputTree.get(0);

    }

    private STRTree mergeTree(ArrayList<STRTree> trees, int mbrDim) {
        if (trees.size() == 1) return trees.get(0);
        STRTree node = new STRTree();
        // construct the node
        int childrenNum = trees.size();
        double nodeUpper[] = new double[mbrDim];
        double nodeLower[] = new double[mbrDim];
        int nodeWeight[] = new int[mbrDim];
        for (int i = 0; i < mbrDim; i++) {
            nodeUpper[i] = -Double.MAX_VALUE;
            nodeLower[i] = Double.MAX_VALUE;
            nodeWeight[i] = Integer.MAX_VALUE;
        }
        double[] currentLower, currentUpper;
        int[] currentWeight;
        for (int i = 0; i < childrenNum; i++) {
            currentLower = trees.get(i).lmbr.lower;
            currentUpper = trees.get(i).lmbr.upper;
            currentWeight = trees.get(i).lmbr.weights;
            for (int j = 0; j < mbrDim; j++) {
                if (currentUpper[j] > nodeUpper[j]) nodeUpper[j] = currentUpper[j];
                if (currentLower[j] < nodeLower[j]) nodeLower[j] = currentLower[j];
                if (currentWeight[j] < nodeWeight[j]) nodeWeight[j] = currentWeight[j];
            }
        }
        STRTree children[] = new STRTree[childrenNum];
        for (int i = 0; i < childrenNum; i++) {
            children[i] = trees.get(i);
        }
        // fill node elements
        LMBR lmbr = new LMBR();
        lmbr.lower = nodeLower;
        lmbr.upper = nodeUpper;
        lmbr.weights = nodeWeight;
        node.lmbr = lmbr;
        node.isLeaf = false;
        node.children = children;
        return node;
    }

}

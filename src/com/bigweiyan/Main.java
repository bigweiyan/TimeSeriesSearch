package com.bigweiyan;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        LBRSConfiguration configuration = new LBRSConfiguration();
        configuration.setBandRate(0.05f);
        configuration.setDevider(" ");
        configuration.setDiffThreshold(3.35f);
        configuration.setHasLable(false);
        configuration.setLmbrDim(10);
        configuration.setTreeDegree(80);
        configuration.setUsageThreshold(0.03f);
        configuration.setShiftNum(2);
        Starter starter = new Starter(configuration, "D:/output1000", "Data50000","D:/index");
        starter.index();
        starter.LBRSSearch("D:/query/query1000.txt", false);
        //starter.trillionSearch("D:/query/query1000.txt", false);
    }
}

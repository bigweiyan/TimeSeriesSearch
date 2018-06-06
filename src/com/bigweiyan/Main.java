package com.bigweiyan;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        LBRSConfiguration configuration = new LBRSConfiguration();
        configuration.setBandRate(0.1f);
        configuration.setDevider(" ");
        configuration.setDiffThreshold(4.0f);
        configuration.setHasLable(false);
        configuration.setLmbrDim(10);
        configuration.setTreeDegree(80);
        configuration.setUsageThreshold(0.01f);
        configuration.setShiftNum(2);
        Starter starter = new Starter(configuration, "D:/input", "Multi50000Len1000","D:/index/mbr10");
        starter.index();
        starter.LBRSSearch("D:/query/query1000.txt", false);
        //starter.UCRSearch("D:/query/StarLightCurves", true, false);
        //starter.UCRSearch("D:/query/query1000.txt", false, true);
    }
}

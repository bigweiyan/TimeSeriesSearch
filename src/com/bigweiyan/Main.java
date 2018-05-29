package com.bigweiyan;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        LBRSConfiguration configuration = new LBRSConfiguration();
        configuration.setBandRate(0.1f);
        configuration.setDevider(" ");
        configuration.setDiffThreshold(4.2f);
        configuration.setHasLable(false);
        configuration.setLmbrDim(5);
        configuration.setTreeDegree(80);
        configuration.setUsageThreshold(0.02f);
        Starter starter = new Starter(configuration, "D:/output", "L256S500K","D:/index");
        starter.index();
        starter.LBRSSearch("D:/query/query256.txt");
        starter.trillionSearch("D:/query/query256.txt");
    }
}

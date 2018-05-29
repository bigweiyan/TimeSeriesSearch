package com.bigweiyan;

public class LBRSConfiguration {
    private float diffThreshold = 5.0f;
    private float usageThreshold = 0.3f;
    private float bandRate = 0.1f;
    private String devider = " ";
    private int lmbrDim = 8;
    private int treeDegree = 40;

    public boolean isHasLable() {
        return hasLable;
    }

    public void setHasLable(boolean hasLable) {
        this.hasLable = hasLable;
    }

    private boolean hasLable = false;
    public float getDiffThreshold() {
        return diffThreshold;
    }

    public void setDiffThreshold(float diffThreshold) {
        this.diffThreshold = diffThreshold;
    }

    public float getUsageThreshold() {
        return usageThreshold;
    }

    public void setUsageThreshold(float usageThreshold) {
        this.usageThreshold = usageThreshold;
    }

    public float getBandRate() {
        return bandRate;
    }

    public void setBandRate(float bandRate) {
        this.bandRate = bandRate;
    }

    public String getDevider() {
        return devider;
    }

    public void setDevider(String devider) {
        this.devider = devider;
    }

    public int getLmbrDim() {
        return lmbrDim;
    }

    public void setLmbrDim(int lmbrDim) {
        this.lmbrDim = lmbrDim;
    }

    public int getTreeDegree() {
        return treeDegree;
    }

    public void setTreeDegree(int treeDegree) {
        this.treeDegree = treeDegree;
    }
}

package com.adria.adria_kyc_integration.adriascancinlibrary;

public class DetectedValues {
    private String value;
    private int occurence;

    public DetectedValues(String value, int occurence) {
        this.value = value;
        this.occurence = occurence;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getOccurence() {
        return occurence;
    }

    public void setOccurence(int occurence) {
        this.occurence = occurence;
    }
}

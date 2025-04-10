package com.adria.adria_kyc_integration.uploadutils;

import com.google.gson.annotations.SerializedName;

public class ResponseModel {


    @SerializedName("info_code")
    private int info_code;
    @SerializedName("probability")
    private double probability;
    @SerializedName("match")
    private boolean match;
    @SerializedName("info")
    private String info;
    @SerializedName("status")
    private String status;

    public ResponseModel(String status, String info, boolean match, float probability, int info_code) {
        this.status = status;
        this.info = info;
        this.match = match;
        this.probability = probability;
        this.info_code = info_code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }

    public int getInfo_code() {
        return info_code;
    }

    public void setInfo_code(int info_code) {
        this.info_code = info_code;
    }
}

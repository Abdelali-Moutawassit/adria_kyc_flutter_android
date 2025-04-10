package com.adria.adria_kyc_integration.uploadutils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelP {


    @Expose
    @SerializedName("info_code")
    private int info_code;
    @Expose
    @SerializedName("probability_level")
    private String probability_level;
    @Expose
    @SerializedName("probability_msg")
    private String probability_msg;
    @Expose
    @SerializedName("match")
    private boolean match;
    @Expose
    @SerializedName("info")
    private String info;
    @Expose
    @SerializedName("status")
    private String status;


    public int getInfo_code() {
        return info_code;
    }

    public void setInfo_code(int info_code) {
        this.info_code = info_code;
    }

    public String getProbability_level() {
        return probability_level;
    }

    public void setProbability_level(String probability_level) {
        this.probability_level = probability_level;
    }

    public boolean isMatch() {
        return match;
    }

    public String getProbability_msg() {
        return probability_msg;
    }

    public void setProbability_msg(String probability_msg) {
        this.probability_msg = probability_msg;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

package com.vocalink.bacs;

public class Reason {
    private String conditionSummary;
    private String conditionReason;

    public Reason(String conditionSummary, String conditionReason) {
        this.conditionSummary = conditionSummary;
        this.conditionReason = conditionReason;
    }

    public void setConditionSummary(String conditionSummary) {
        this.conditionSummary = conditionSummary;
    }

    public void setConditionReason(String conditionReason) {
        this.conditionReason = conditionReason;
    }

    public String getConditionSummary() {
        return conditionSummary;
    }

    public String getConditionReason() {
        return conditionReason;
    }

    @Override
    public String toString() {
        return "conditionSummary='" + conditionSummary + "\n" +
                "conditionReason='" + conditionReason;
    }
}

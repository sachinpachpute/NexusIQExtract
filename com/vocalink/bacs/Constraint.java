package com.vocalink.bacs;

import java.util.List;
import java.util.Objects;

public class Constraint {
    private String constrintId;
    private String constraintName;
    private List<Reason> reasons;

    public Constraint(String constrintId, String constraintName) {
        this.constrintId = constrintId;
        this.constraintName = constraintName;
    }

    public String getConstrintId() {
        return constrintId;
    }

    public void setConstrintId(String constrintId) {
        this.constrintId = constrintId;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public List<Reason> getReasons() {
        return reasons;
    }

    public void setReasons(List<Reason> reasons) {
        this.reasons = reasons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return Objects.equals(constrintId, that.constrintId) && Objects.equals(constraintName, that.constraintName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constrintId, constraintName);
    }
}

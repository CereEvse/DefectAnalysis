package com.defectanalysis.model;

import java.util.List;

public class BatchData {
    private List<Integer> batchSizes; // Размеры партий
    private List<Integer> defectCounts; // Количество бракованных деталей

    public BatchData() {}

    public BatchData(List<Integer> batchSizes, List<Integer> defectCounts) {
        this.batchSizes = batchSizes;
        this.defectCounts = defectCounts;
    }

    // Геттеры и сеттеры
    public List<Integer> getBatchSizes() {
        return batchSizes;
    }

    public void setBatchSizes(List<Integer> batchSizes) {
        this.batchSizes = batchSizes;
    }

    public List<Integer> getDefectCounts() {
        return defectCounts;
    }

    public void setDefectCounts(List<Integer> defectCounts) {
        this.defectCounts = defectCounts;
    }
}
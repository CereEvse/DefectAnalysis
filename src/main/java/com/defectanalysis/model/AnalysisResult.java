package com.defectanalysis.model;

import java.util.Map;

public class AnalysisResult {
    private String distributionType; // Тип распределения
    private double chiSquareValue; // Значение хи-квадрат
    private double criticalValue; // Критическое значение
    private boolean hypothesisAccepted; // Принята ли гипотеза
    private Map<Integer, Double> expectedDistribution; // Ожидаемое распределение
    private Map<Integer, Double> observedDistribution; // Наблюдаемое распределение

    public AnalysisResult() {}

    // Геттеры и сеттеры
    public String getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(String distributionType) {
        this.distributionType = distributionType;
    }

    public double getChiSquareValue() {
        return chiSquareValue;
    }

    public void setChiSquareValue(double chiSquareValue) {
        this.chiSquareValue = chiSquareValue;
    }

    public double getCriticalValue() {
        return criticalValue;
    }

    public void setCriticalValue(double criticalValue) {
        this.criticalValue = criticalValue;
    }

    public boolean isHypothesisAccepted() {
        return hypothesisAccepted;
    }

    public void setHypothesisAccepted(boolean hypothesisAccepted) {
        this.hypothesisAccepted = hypothesisAccepted;
    }

    public Map<Integer, Double> getExpectedDistribution() {
        return expectedDistribution;
    }

    public void setExpectedDistribution(Map<Integer, Double> expectedDistribution) {
        this.expectedDistribution = expectedDistribution;
    }

    public Map<Integer, Double> getObservedDistribution() {
        return observedDistribution;
    }

    public void setObservedDistribution(Map<Integer, Double> observedDistribution) {
        this.observedDistribution = observedDistribution;
    }
}
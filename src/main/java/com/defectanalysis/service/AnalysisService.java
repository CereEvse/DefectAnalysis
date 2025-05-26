package com.defectanalysis.service;

import com.defectanalysis.model.AnalysisResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    public AnalysisResult analyzeDefectData(List<Integer> batchSizes, List<Integer> defectCounts) {
        if (batchSizes == null || defectCounts == null || batchSizes.size() != defectCounts.size()) {
            throw new IllegalArgumentException("Размеры партий и количество брака должны быть указаны и совпадать по количеству");
        }

        // Анализ распределения Пуассона
        Map<String, Object> poissonResult = StatsCalculator.calculatePoissonDistribution(defectCounts);

        // Подготовка результатов
        AnalysisResult result = new AnalysisResult();
        result.setDistributionType("Пуассона");
        result.setChiSquareValue((double) poissonResult.get("chiSquare"));
        result.setCriticalValue(0.05);
        result.setHypothesisAccepted((boolean) poissonResult.get("isPoisson"));
        result.setExpectedDistribution((Map<Integer, Double>) poissonResult.get("expectedProb"));
        result.setObservedDistribution((Map<Integer, Double>) poissonResult.get("observedProb"));

        return result;
    }
}
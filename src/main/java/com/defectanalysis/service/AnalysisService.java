package com.defectanalysis.service;

import com.defectanalysis.model.AnalysisResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private static final Map<String, String> DISTRIBUTION_NAMES = Map.of(
            "POISSON", "Пуассона",
            "BINOMIAL", "Биномиальное",
            "NORMAL", "Нормальное",
            "EXPONENTIAL", "Экспоненциальное"
    );

    public AnalysisResult analyzeDefectData(
            List<Integer> batchSizes,
            List<Integer> defectCounts,
            String distributionType,
            double significanceLevel) {

        String russianName = DISTRIBUTION_NAMES.getOrDefault(distributionType, distributionType);

        try {
            StatsCalculator.DistributionType type =
                    StatsCalculator.DistributionType.valueOf(distributionType.toUpperCase());

            StatsCalculator.DistributionTestResult testResult = StatsCalculator.testDistribution(
                    defectCounts,
                    StatsCalculator.DistributionType.valueOf(distributionType),
                    significanceLevel,
                    "BINOMIAL".equals(distributionType) ? batchSizes : null
            );

            return convertToAnalysisResult(testResult, russianName);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Ошибка при анализе данных: " + e.getMessage(), e);
        }
    }

    private AnalysisResult convertToAnalysisResult(
            StatsCalculator.DistributionTestResult testResult,
            String distributionType) {

        AnalysisResult result = new AnalysisResult();
        result.setDistributionType(distributionType);
        result.setChiSquareValue(testResult.chiSquare());
        result.setCriticalValue(testResult.criticalValue());
        result.setHypothesisAccepted(testResult.hypothesisAccepted());
        result.setExpectedDistribution(testResult.expectedProb());
        result.setObservedDistribution(testResult.observedProb());
        result.setDistributionParams(testResult.distributionParams());
        result.setWarningMessage(testResult.warningMessage());

        return result;
    }
}
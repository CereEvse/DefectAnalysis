package com.defectanalysis.service;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class StatsCalculator {

    public static Map<String, Object> calculatePoissonDistribution(List<Integer> defectCounts) {
        Map<String, Object> result = new HashMap<>();

        // Расчет лямбда (среднее количество брака)
        double lambda = defectCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // Создание карты наблюдаемых частот
        Map<Integer, Integer> observedFreq = new HashMap<>();
        for (int count : defectCounts) {
            observedFreq.put(count, observedFreq.getOrDefault(count, 0) + 1);
        }

        // Расчет ожидаемых частот (распределение Пуассона)
        PoissonDistribution poisson = new PoissonDistribution(lambda);
        Map<Integer, Double> expectedFreq = new HashMap<>();
        int maxObserved = observedFreq.keySet().stream().max(Integer::compare).orElse(0);

        for (int i = 0; i <= maxObserved; i++) {
            expectedFreq.put(i, poisson.probability(i) * defectCounts.size());
        }

        // Подготовка данных для критерия хи-квадрат
        long[] observed = new long[maxObserved + 1];
        double[] expected = new double[maxObserved + 1];

        for (int i = 0; i <= maxObserved; i++) {
            observed[i] = observedFreq.getOrDefault(i, 0);
            expected[i] = expectedFreq.getOrDefault(i, 0.0);
        }

        // Проверка гипотезы хи-квадрат
        ChiSquareTest chiSquareTest = new ChiSquareTest();
        double chiSquare = chiSquareTest.chiSquare(expected, observed);
        double pValue = chiSquareTest.chiSquareTest(expected, observed);
        boolean isPoisson = pValue > 0.05; // Уровень доверия 95%

        // Конвертация в вероятности
        Map<Integer, Double> expectedProb = new HashMap<>();
        Map<Integer, Double> observedProb = new HashMap<>();

        for (Map.Entry<Integer, Double> entry : expectedFreq.entrySet()) {
            expectedProb.put(entry.getKey(), entry.getValue() / defectCounts.size());
        }

        for (Map.Entry<Integer, Integer> entry : observedFreq.entrySet()) {
            observedProb.put(entry.getKey(), (double)entry.getValue() / defectCounts.size());
        }

        result.put("lambda", lambda);
        result.put("chiSquare", chiSquare);
        result.put("pValue", pValue);
        result.put("isPoisson", isPoisson);
        result.put("expectedProb", expectedProb);
        result.put("observedProb", observedProb);

        return result;
    }
}
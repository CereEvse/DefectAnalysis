package com.defectanalysis.service;

import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import java.util.*;
import java.util.stream.Collectors;

public final class StatsCalculator {
    private static final int HISTOGRAM_BINS = 10;
    private static final int MIN_CATEGORIES = 2;
    private static final int MIN_EXPECTED_COUNT = 5;

    public enum DistributionType {
        POISSON, BINOMIAL, NORMAL, EXPONENTIAL
    }

    public record DistributionTestResult(
            double chiSquare,
            double pValue,
            double criticalValue,
            boolean hypothesisAccepted,
            Map<Integer, Double> expectedProb,
            Map<Integer, Double> observedProb,
            Map<String, Double> distributionParams,
            String warningMessage
    ) {}

    public static DistributionTestResult testDistribution(
            List<Integer> values,
            DistributionType distributionType,
            double alpha,
            List<Integer> batchSizes) {

        Objects.requireNonNull(values, "Данные не могут быть null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Данные не могут быть пустыми");
        }
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("Уровень значимости должен быть между 0 и 1");
        }

        try {
            return switch (distributionType) {
                case POISSON -> testPoisson(values, alpha);
                case BINOMIAL -> {
                    Objects.requireNonNull(batchSizes, "Размеры партий обязательны для биномиального распределения");
                    yield testBinomial(values, batchSizes, alpha);
                }
                case NORMAL -> testNormal(values, alpha);
                case EXPONENTIAL -> testExponential(values, alpha);
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ошибка при анализе " + distributionType + ": " + e.getMessage());
        }
    }

    private static DistributionTestResult testPoisson(List<Integer> values, double alpha) {
        double lambda = calculateMean(values);
        if (lambda <= 0) {
            throw new IllegalArgumentException("Среднее значение (λ) должно быть положительным");
        }

        PoissonDistribution dist = new PoissonDistribution(lambda);
        return performDiscreteTest(
                values,
                dist::probability,
                alpha,
                Map.of("lambda", lambda),
                "Пуассона (λ=" + String.format("%.2f", lambda) + ")"
        );
    }

    private static DistributionTestResult testBinomial(
            List<Integer> defectCounts,
            List<Integer> batchSizes,
            double alpha) {

        validateBinomialInput(defectCounts, batchSizes);
        double p = calculateBinomialP(defectCounts, batchSizes);
        int n = (int) batchSizes.stream().mapToInt(Integer::intValue).average().orElse(1);

        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("Вероятность дефекта должна быть между 0 и 1");
        }

        BinomialDistribution dist = new BinomialDistribution(n, p);
        return performDiscreteTest(
                defectCounts,
                dist::probability,
                alpha,
                Map.of("p", p, "n", (double) n),
                "Биномиальное (n=" + n + ", p=" + String.format("%.2f", p) + ")"
        );
    }

    private static DistributionTestResult testNormal(List<Integer> values, double alpha) {
        double mean = calculateMean(values);
        double stdDev = calculateStdDev(values, mean);

        if (stdDev <= 0) {
            throw new IllegalArgumentException("Стандартное отклонение должно быть положительным");
        }

        NormalDistribution dist = new NormalDistribution(mean, stdDev);
        return performContinuousTest(
                values,
                dist::density,
                alpha,
                Map.of("mean", mean, "stdDev", stdDev),
                "Нормальное (μ=" + String.format("%.2f", mean) + ", σ=" + String.format("%.2f", stdDev) + ")"
        );
    }

    private static DistributionTestResult testExponential(List<Integer> values, double alpha) {
        double mean = calculateMean(values);
        if (mean <= 0) {
            throw new IllegalArgumentException("Среднее значение должно быть положительным");
        }

        ExponentialDistribution dist = new ExponentialDistribution(1/mean);
        return performContinuousTest(
                values,
                dist::density,
                alpha,
                Map.of("lambda", 1/mean),
                "Экспоненциальное (λ=" + String.format("%.2f", 1/mean) + ")"
        );
    }

    private static DistributionTestResult performDiscreteTest(
            List<Integer> data,
            java.util.function.Function<Integer, Double> probFunction,
            double alpha,
            Map<String, Double> params,
            String distributionInfo) {

        // Группировка данных с учетом минимального количества наблюдений
        Map<Integer, Integer> groupedFreq = groupFrequenciesWithMinCount(data, MIN_EXPECTED_COUNT);
        if (groupedFreq.size() < MIN_CATEGORIES) {
            throw new IllegalArgumentException(
                    "Недостаточно категорий после группировки (требуется минимум " + MIN_CATEGORIES + ")");
        }

        int total = data.size();
        Map<Integer, Double> expectedProb = new LinkedHashMap<>();
        Map<Integer, Double> observedProb = new LinkedHashMap<>();
        List<Integer> sortedKeys = new ArrayList<>(groupedFreq.keySet());
        Collections.sort(sortedKeys);

        // Расчет вероятностей
        for (Integer k : sortedKeys) {
            int observedCount = groupedFreq.get(k);
            observedProb.put(k, (double) observedCount / total);

            double prob = probFunction.apply(k);
            if (prob <= 0) {
                throw new IllegalArgumentException(
                        "Нулевая вероятность для значения " + k + " в " + distributionInfo);
            }
            expectedProb.put(k, prob);
        }

        // Проверка ожидаемых частот
        String warning = null;
        double[] expected = new double[sortedKeys.size()];
        long[] observed = new long[sortedKeys.size()];

        for (int i = 0; i < sortedKeys.size(); i++) {
            Integer k = sortedKeys.get(i);
            observed[i] = groupedFreq.get(k);
            expected[i] = expectedProb.get(k) * total;

            if (expected[i] < MIN_EXPECTED_COUNT) {
                warning = "Внимание: некоторые ожидаемые частоты < " + MIN_EXPECTED_COUNT;
            }
        }

        ChiSquareTestResult testResult = performChiSquareTest(expected, observed, alpha);

        return new DistributionTestResult(
                testResult.chiSquare(),
                testResult.pValue(),
                testResult.criticalValue(),
                testResult.hypothesisAccepted(),
                expectedProb,
                observedProb,
                params,
                warning
        );
    }

    private static DistributionTestResult performContinuousTest(
            List<Integer> data,
            java.util.function.Function<Double, Double> densityFunction,
            double alpha,
            Map<String, Double> params,
            String distributionInfo) {

        double min = data.stream().mapToInt(Integer::intValue).min().orElse(0);
        double max = data.stream().mapToInt(Integer::intValue).max().orElse(1);
        double binSize = (max - min) / HISTOGRAM_BINS;
        int total = data.size();

        // Подсчет наблюдаемых частот
        int[] observedCounts = new int[HISTOGRAM_BINS];
        for (int value : data) {
            int bin = Math.min((int) ((value - min) / binSize), HISTOGRAM_BINS - 1);
            observedCounts[bin]++;
        }

        // Расчет ожидаемых частот
        double[] expected = new double[HISTOGRAM_BINS];
        for (int i = 0; i < HISTOGRAM_BINS; i++) {
            double lower = min + i * binSize;
            double upper = min + (i + 1) * binSize;
            expected[i] = integrate(densityFunction, lower, upper) * total;

            if (expected[i] <= 0) {
                throw new IllegalArgumentException(
                        "Нулевая вероятность в интервале [" + lower + ", " + upper + "] для " + distributionInfo);
            }
        }

        // Конвертация в long[] для хи-квадрат
        long[] observed = new long[HISTOGRAM_BINS];
        for (int i = 0; i < HISTOGRAM_BINS; i++) {
            observed[i] = observedCounts[i];
        }

        // Подготовка результатов для отображения
        Map<Integer, Double> expectedProb = new LinkedHashMap<>();
        Map<Integer, Double> observedProb = new LinkedHashMap<>();

        for (int i = 0; i < HISTOGRAM_BINS; i++) {
            int binCenter = (int) (min + (i + 0.5) * binSize);
            expectedProb.put(binCenter, expected[i] / total);
            observedProb.put(binCenter, (double) observedCounts[i] / total);
        }

        ChiSquareTestResult testResult = performChiSquareTest(expected, observed, alpha);

        return new DistributionTestResult(
                testResult.chiSquare(),
                testResult.pValue(),
                testResult.criticalValue(),
                testResult.hypothesisAccepted(),
                expectedProb,
                observedProb,
                params,
                null
        );
    }

    // Вспомогательные методы
    private record ChiSquareTestResult(
            double chiSquare,
            double pValue,
            double criticalValue,
            boolean hypothesisAccepted
    ) {}

    private static ChiSquareTestResult performChiSquareTest(
            double[] expected,
            long[] observed,
            double alpha) {

        ChiSquareTest test = new ChiSquareTest();
        double chiSquare = test.chiSquare(expected, observed);
        double pValue = test.chiSquareTest(expected, observed);
        int df = expected.length - 1;
        double criticalValue = new ChiSquaredDistribution(df).inverseCumulativeProbability(1 - alpha);

        return new ChiSquareTestResult(
                chiSquare,
                pValue,
                criticalValue,
                pValue > alpha
        );
    }

    private static Map<Integer, Integer> groupFrequenciesWithMinCount(
            List<Integer> data,
            int minCount) {

        Map<Integer, Integer> freq = data.stream()
                .collect(Collectors.groupingBy(k -> k, Collectors.summingInt(k -> 1)));

        // Объединение редких категорий
        List<Integer> rareKeys = freq.entrySet().stream()
                .filter(e -> e.getValue() < minCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (rareKeys.size() > 1) {
            int combinedCount = rareKeys.stream().mapToInt(freq::get).sum();
            rareKeys.forEach(freq::remove);
            if (combinedCount > 0) {
                freq.put(rareKeys.get(0), combinedCount);
            }
        }

        return freq;
    }

    private static double calculateMean(List<Integer> data) {
        return data.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static double calculateStdDev(List<Integer> data, double mean) {
        return Math.sqrt(data.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0));
    }

    private static double calculateBinomialP(
            List<Integer> defectCounts,
            List<Integer> batchSizes) {

        int totalDefects = defectCounts.stream().mapToInt(Integer::intValue).sum();
        int totalItems = batchSizes.stream().mapToInt(Integer::intValue).sum();
        return (double) totalDefects / totalItems;
    }

    private static void validateBinomialInput(
            List<Integer> defectCounts,
            List<Integer> batchSizes) {

        if (defectCounts.size() != batchSizes.size()) {
            throw new IllegalArgumentException(
                    "Количество записей о дефектах должно совпадать с количеством партий");
        }

        for (int i = 0; i < defectCounts.size(); i++) {
            if (defectCounts.get(i) < 0) {
                throw new IllegalArgumentException(
                        "Количество дефектов не может быть отрицательным");
            }
            if (batchSizes.get(i) <= 0) {
                throw new IllegalArgumentException(
                        "Размер партии должен быть положительным");
            }
            if (defectCounts.get(i) > batchSizes.get(i)) {
                throw new IllegalArgumentException(
                        "Количество дефектов не может превышать размер партии");
            }
        }
    }

    private static double integrate(
            java.util.function.Function<Double, Double> f,
            double a,
            double b) {

        int n = 100;
        double h = (b - a) / n;
        double sum = 0.5 * (f.apply(a) + f.apply(b));

        for (int i = 1; i < n; i++) {
            sum += f.apply(a + i * h);
        }

        return sum * h;
    }
}
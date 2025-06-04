package com.defectanalysis.controller;

import com.defectanalysis.model.AnalysisResult;
import com.defectanalysis.model.BatchData;
import com.defectanalysis.service.AnalysisService;
import com.defectanalysis.service.FileImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AnalysisController {

    private final AnalysisService analysisService;
    private FileImportService fileImportService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public String analyzeDefects(
            @RequestParam List<Integer> batchSizes,
            @RequestParam List<Integer> defectCounts,
            @RequestParam String distributionType,
            @RequestParam double significanceLevel,
            Model model) {

        // Создаем объект с входными данными для отображения
        List<BatchDataItem> items = new ArrayList<>();
        int totalBatchSize = 0;
        int totalDefectCount = 0;

        for (int i = 0; i < batchSizes.size(); i++) {
            int bs = batchSizes.get(i);
            int dc = defectCounts.get(i);
            items.add(new BatchDataItem(bs, dc));
            totalBatchSize += bs;
            totalDefectCount += dc;
        }

        BatchDataSummary batchData = new BatchDataSummary(items, totalBatchSize, totalDefectCount);
        model.addAttribute("batchData", batchData);
        model.addAttribute("significanceLevel", significanceLevel);

        // Валидация входных данных
        if (batchSizes == null || defectCounts == null || batchSizes.size() != defectCounts.size()) {
            model.addAttribute("error", "Несоответствие размеров входных данных");
            return "input-formTest";
        }

        if (batchSizes.isEmpty() || defectCounts.isEmpty()) {
            model.addAttribute("error", "Входные данные не могут быть пустыми");
            return "input-formTest";
        }

        // Проверка для биномиального распределения
        if ("BINOMIAL".equals(distributionType)) {
            for (int i = 0; i < batchSizes.size(); i++) {
                if (defectCounts.get(i) > batchSizes.get(i)) {
                    model.addAttribute("error",
                            "Для биномиального распределения количество дефектов не может превышать размер партии");
                    return "input-formTest";
                }
            }
        }

        try {
            AnalysisResult result = analysisService.analyzeDefectData(
                    batchSizes,
                    defectCounts,
                    distributionType,
                    significanceLevel);

            model.addAttribute("result", result);
            model.addAttribute("expectedLabels", result.getExpectedDistribution().keySet());
            model.addAttribute("expectedValues", result.getExpectedDistribution().values());
            model.addAttribute("observedValues", result.getObservedDistribution().values());
            model.addAttribute("significanceLevel", significanceLevel);
            model.addAttribute("distributionType", distributionType);

            return "results";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "input-formTest";
        } catch (Exception e) {
            model.addAttribute("error", "Произошла ошибка при анализе данных");
            return "input-formTest";
        }
    }

    // Вспомогательные классы DTO
    public record BatchDataItem(int batchSize, int defectCount) {}
    public record BatchDataSummary(List<BatchDataItem> items, int totalBatchSize, int totalDefectCount) {}

    @GetMapping("/inputTest")
    public String showAnalysisParams(Model model) {
        // Можно добавить дополнительные параметры, если нужно
        return "input-formTest";
    }

    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType) {

        try {
            List<BatchData> data = fileImportService.importData(file, fileType);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
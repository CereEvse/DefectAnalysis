package com.defectanalysis.controller;

import com.defectanalysis.model.BatchData;
import com.defectanalysis.model.AnalysisResult;
import com.defectanalysis.service.AnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;

@Controller
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    public String analyzeDefects(@ModelAttribute BatchData batchData, Model model) {
        List<Integer> batchSizes = batchData.getBatchSizes();
        List<Integer> defectCounts = batchData.getDefectCounts();

        AnalysisResult result = analysisService.analyzeDefectData(batchSizes, defectCounts);

        model.addAttribute("result", result);
        model.addAttribute("expectedLabels", result.getExpectedDistribution().keySet());
        model.addAttribute("expectedValues", result.getExpectedDistribution().values());
        model.addAttribute("observedValues", result.getObservedDistribution().values());

        return "results";
    }
}
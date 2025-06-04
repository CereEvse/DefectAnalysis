package com.defectanalysis.service;

import com.defectanalysis.model.BatchData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileImportService {

    public List<BatchData> importData(MultipartFile file, String fileType) throws Exception {
        switch (fileType.toLowerCase()) {
            case "excel":
                return importFromExcel(file);
            case "csv":
                return importFromCSV(file);
            case "json":
                return importFromJSON(file);
            default:
                throw new IllegalArgumentException("Unsupported file type");
        }
    }

    private List<BatchData> importFromExcel(MultipartFile file) throws Exception {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        List<BatchData> data = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Пропускаем заголовок

            Cell sizeCell = row.getCell(0);
            Cell defectCell = row.getCell(1);

            if (sizeCell != null && defectCell != null) {
                data.add(new BatchData(
                        (int) sizeCell.getNumericCellValue(),
                        (int) defectCell.getNumericCellValue()
                ));
            }
        }

        return data;
    }

    private List<BatchData> importFromCSV(MultipartFile file) throws Exception {
        List<BatchData> data = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }

            String[] values = line.split(",");
            if (values.length >= 2) {
                data.add(new BatchData(
                        Integer.parseInt(values[0].trim()),
                        Integer.parseInt(values[1].trim())
                ));
            }
        }

        return data;
    }

    private List<BatchData> importFromJSON(MultipartFile file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file.getInputStream(),
                new TypeReference<List<BatchData>>() {});
    }
}

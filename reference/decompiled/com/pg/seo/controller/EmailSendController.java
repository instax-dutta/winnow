/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.apache.poi.ss.usermodel.Cell
 *  org.apache.poi.ss.usermodel.Row
 *  org.apache.poi.ss.usermodel.Row$MissingCellPolicy
 *  org.apache.poi.ss.usermodel.Sheet
 *  org.apache.poi.xssf.usermodel.XSSFWorkbook
 *  org.springframework.http.HttpEntity
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpMethod
 *  org.springframework.http.MediaType
 *  org.springframework.http.ResponseEntity
 *  org.springframework.stereotype.Controller
 *  org.springframework.util.MultiValueMap
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.ResponseBody
 *  org.springframework.web.client.RestTemplate
 *  org.springframework.web.multipart.MultipartFile
 */
package com.pg.seo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class EmailSendController {
    @PostMapping(value={"/sendData"})
    @ResponseBody
    public String sendExcelData(@RequestParam(value="file") MultipartFile file, @RequestParam(value="scriptType") String scriptType) {
        try {
            List<Map<String, String>> excelData = this.extractExcelData(file);
            String uniqueId = UUID.randomUUID().toString();
            HashMap<String, Object> payload = new HashMap<String, Object>();
            payload.put("uniqueId", uniqueId);
            payload.put("scriptType", scriptType);
            payload.put("data", excelData);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity requestEntity = new HttpEntity((Object)jsonPayload, (MultiValueMap)headers);
            ResponseEntity response = restTemplate.exchange("https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec", HttpMethod.POST, requestEntity, String.class, new Object[0]);
            return (String)response.getBody();
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private List<Map<String, String>> extractExcelData(MultipartFile file) throws IOException {
        ArrayList<Map<String, String>> dataList = new ArrayList<Map<String, String>>();
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        ArrayList<String> headers = new ArrayList<String>();
        for (Cell cell : headerRow) {
            headers.add(cell.getStringCellValue());
        }
        for (int i = 1; i <= sheet.getLastRowNum(); ++i) {
            Row row = sheet.getRow(i);
            HashMap<String, String> rowData = new HashMap<String, String>();
            for (int j = 0; j < headers.size(); ++j) {
                Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                rowData.put((String)headers.get(j), cell.toString());
            }
            dataList.add(rowData);
        }
        workbook.close();
        return dataList;
    }
}


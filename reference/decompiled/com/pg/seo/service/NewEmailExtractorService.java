/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.poi.ss.usermodel.Cell
 *  org.apache.poi.ss.usermodel.CellType
 *  org.apache.poi.ss.usermodel.Row
 *  org.apache.poi.xssf.usermodel.XSSFSheet
 *  org.apache.poi.xssf.usermodel.XSSFWorkbook
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Service
 */
package com.pg.seo.service;

import com.pg.seo.entity.EmailExtractor;
import com.pg.seo.entity.EmailExtractorData;
import com.pg.seo.repository.EmailExtractorDataRepository;
import com.pg.seo.repository.EmailExtractorRepository;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NewEmailExtractorService {
    private static final int BATCH_SIZE = 100;
    @Autowired
    @Qualifier(value="commonEmailExecutor")
    private ExecutorService executor;
    @Autowired
    private EmailExtractorRepository emailExtractorRepository;
    @Autowired
    private EmailExtractorDataRepository emailExtractorDataRepository;
    private final ConcurrentMap<String, Set<String>> emailDedupMap = new ConcurrentHashMap<String, Set<String>>();
    private final SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Async
    public void extract(InputStream file, Long loginId) {
        int processedCount = 0;
        int failedCount = 0;
        List<EmailExtractorData> batchList = Collections.synchronizedList(new ArrayList());
        ArrayList<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        EmailExtractor emailExtractor = new EmailExtractor();
        emailExtractor.setLoginId(loginId);
        emailExtractor.setStatus("IN PROGRESS");
        emailExtractor.setStartDate(new Date(System.currentTimeMillis()));
        emailExtractor.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
        emailExtractor.setNoOfDomain(0L);
        emailExtractor = (EmailExtractor)this.emailExtractorRepository.save(emailExtractor);
        Long extractorId = emailExtractor.getEmailExtractorId();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file);){
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator rowIterator = sheet.iterator();
            int rowCount = 0;
            while (rowIterator.hasNext()) {
                Row row = (Row)rowIterator.next();
                if (rowCount++ == 0) continue;
                Future<Boolean> future = this.executor.submit(() -> {
                    try {
                        String domain = this.getDomainValue(row.getCell(0));
                        if (domain == null) {
                            return false;
                        }
                        Document doc = Jsoup.connect((String)("https://" + domain)).timeout(10000).get();
                        String html = doc.html();
                        Matcher matcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(html);
                        boolean hasValidEmail = false;
                        while (matcher.find()) {
                            String email = matcher.group();
                            if (!this.isValidEmail(email, domain) || !this.isUnique(domain, email)) continue;
                            EmailExtractorData data = new EmailExtractorData();
                            data.setEmailExtractorId(extractorId);
                            data.setDomain(domain);
                            data.setEmail(email);
                            data.setStartDate(new Date(System.currentTimeMillis()));
                            data.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
                            batchList.add(data);
                            hasValidEmail = true;
                        }
                        if (batchList.size() >= 100) {
                            List list = batchList;
                            synchronized (list) {
                                this.saveBatch(new ArrayList<EmailExtractorData>(batchList));
                                batchList.clear();
                            }
                        }
                        return hasValidEmail;
                    }
                    catch (Exception e) {
                        return false;
                    }
                });
                futures.add(future);
            }
            for (Future<Boolean> future : futures) {
                try {
                    if (((Boolean)future.get(15L, TimeUnit.SECONDS)).booleanValue()) {
                        ++processedCount;
                        continue;
                    }
                    ++failedCount;
                }
                catch (TimeoutException e) {
                    future.cancel(true);
                    ++failedCount;
                    System.err.println("Timeout: domain task took too long.");
                }
                catch (Exception e) {
                    ++failedCount;
                }
            }
            if (!batchList.isEmpty()) {
                this.saveBatch(batchList);
            }
            EmailExtractor finalStatus = this.emailExtractorRepository.findByEmailExtractorId(extractorId);
            if (processedCount == 0) {
                finalStatus.setStatus("FAILED");
            } else {
                finalStatus.setStatus("SUCCESS");
                finalStatus.setNoOfDomain(Long.valueOf(processedCount));
            }
            this.emailExtractorRepository.save(finalStatus);
            System.out.println("Final status updated: " + finalStatus.getStatus() + " with " + processedCount + " domains");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDomainValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case CellType.STRING -> cell.getStringCellValue().trim();
            case CellType.NUMERIC -> String.valueOf((long)cell.getNumericCellValue());
            default -> null;
        };
    }

    private boolean isValidEmail(String email, String domain) {
        return email.contains(domain.split("\\.")[0]) && !email.matches(".*(\\.png|\\.jpg|\\.gif|\\.jpeg|\\.webp|noreply|example|yourname|google).*");
    }

    private boolean isUnique(String domain, String email) {
        this.emailDedupMap.putIfAbsent(domain, ConcurrentHashMap.newKeySet());
        return ((Set)this.emailDedupMap.get(domain)).add(email);
    }

    private void saveBatch(List<EmailExtractorData> batch) {
        try {
            this.emailExtractorDataRepository.saveAll(batch);
        }
        catch (Exception e) {
            for (EmailExtractorData d : batch) {
                try {
                    this.emailExtractorDataRepository.save(d);
                }
                catch (Exception ex) {
                    System.err.println("Error saving email data: " + d.getEmail());
                }
            }
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  jakarta.servlet.http.HttpSession
 *  org.apache.poi.ss.usermodel.Cell
 *  org.apache.poi.ss.usermodel.Row
 *  org.apache.poi.ss.usermodel.Sheet
 *  org.apache.poi.xssf.usermodel.XSSFWorkbook
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.scheduling.TaskScheduler
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.ResponseBody
 *  org.springframework.web.multipart.MultipartFile
 */
package com.pg.seo.controller;

import com.pg.seo.entity.EmailQueue;
import com.pg.seo.entity.GmailMaster;
import com.pg.seo.model.EmailData;
import com.pg.seo.repository.EmailQueueRepository;
import com.pg.seo.service.GmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class NewEmailController {
    private final GmailService gmailService;
    private final TaskScheduler scheduler;
    private final EmailQueueRepository emailQueueRepository;
    @Autowired
    @Qualifier(value="commonEmailExecutor")
    private ExecutorService executor;

    public NewEmailController(GmailService gmailService, TaskScheduler scheduler, EmailQueueRepository emailQueueRepository) {
        this.gmailService = gmailService;
        this.scheduler = scheduler;
        this.emailQueueRepository = emailQueueRepository;
    }

    @RequestMapping(value={"/drafts"}, method={RequestMethod.GET})
    @ResponseBody
    public Map<String, String> getDraftSubjects(String email) throws Exception {
        return this.gmailService.getAllDraftSubjectsWithIds(email);
    }

    @RequestMapping(value={"/getAllGmail"}, method={RequestMethod.POST})
    @ResponseBody
    public List<GmailMaster> getAllGmail(String email) throws Exception {
        return this.gmailService.getAllGmail();
    }

    @RequestMapping(value={"/send"}, method={RequestMethod.POST})
    @ResponseBody
    public String sendEmails(@RequestParam(value="fromEmail") String fromEmail, @RequestParam(value="template") String template, @RequestParam(value="date") String dateStr, @RequestParam(value="file") MultipartFile file) {
        LocalDateTime scheduledTime = dateStr != null ? LocalDateTime.parse(dateStr) : LocalDateTime.now();
        for (EmailData email : this.parseEmailDataFromExcel(file)) {
            EmailQueue emailQueue = new EmailQueue();
            emailQueue.setFromEmail(fromEmail);
            emailQueue.setToEmail(email.getToEmail());
            emailQueue.setDraftId(template);
            emailQueue.setDomain(email.getDomain());
            emailQueue.setScheduledTime(scheduledTime);
            emailQueue.setStatus("PENDING");
            this.emailQueueRepository.save(emailQueue);
        }
        return "Emails queued for sending.";
    }

    @RequestMapping(value={"/gmailTemplate"}, method={RequestMethod.GET})
    public String gmailTemplate(HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "GmailTemplate";
    }

    @GetMapping(value={"/last-24-hours/live"})
    @ResponseBody
    public Map<String, Object> getSentCountFromGmail(@RequestParam String fromEmail) {
        try {
            Map<String, Object> stats = this.gmailService.getEmailSendStats(fromEmail);
            return stats;
        }
        catch (Exception e) {
            return new HashMap<String, Object>();
        }
    }

    @GetMapping(value={"/getScheduledEmail"})
    @ResponseBody
    public List<EmailQueue> getScheduledEmail(@RequestParam String fromEmail) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1L).minusSeconds(1L);
        return this.emailQueueRepository.findByFromEmailAndScheduledTimeBetweenOrderByScheduledTimeDesc(fromEmail, startOfDay, endOfDay);
    }

    @Scheduled(fixedRate=60000L)
    public void processScheduledEmails() {
        List<EmailQueue> dueEmails = this.emailQueueRepository.findByStatusAndScheduledTimeBefore("PENDING", LocalDateTime.now());
        Map<String, List<EmailQueue>> groupedBySender = dueEmails.stream().collect(Collectors.groupingBy(EmailQueue::getFromEmail));
        ArrayList<Callable<List>> tasks = new ArrayList<Callable<List>>();
        for (Map.Entry<String, List<EmailQueue>> entry : groupedBySender.entrySet()) {
            String fromEmail = entry.getKey();
            List<EmailQueue> emails = entry.getValue();
            tasks.add(() -> {
                ArrayList<EmailQueue> processed = new ArrayList<EmailQueue>();
                for (EmailQueue email : emails) {
                    try {
                        this.gmailService.sendDraftEmailById(fromEmail, email.getToEmail(), email.getDraftId(), email.getDomain());
                        email.setStatus("SENT");
                    }
                    catch (Exception e) {
                        email.setStatus("FAILED");
                        email.setErrorMessage(e.getMessage());
                    }
                    processed.add(email);
                }
                return processed;
            });
        }
        try {
            List futures = this.executor.invokeAll(tasks);
            ArrayList allProcessed = new ArrayList();
            for (Future future : futures) {
                allProcessed.addAll((Collection)future.get());
            }
            this.emailQueueRepository.saveAll(allProcessed);
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public List<EmailData> parseEmailDataFromExcel(MultipartFile file) {
        ArrayList<EmailData> emails = new ArrayList<EmailData>();
        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is);){
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                Cell emailCell = row.getCell(1);
                Cell domainCell = row.getCell(0);
                if (emailCell == null || domainCell == null) continue;
                EmailData emailData = new EmailData();
                emailData.setToEmail(emailCell.getStringCellValue().trim());
                emailData.setDomain(domainCell.getStringCellValue().trim());
                emails.add(emailData);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file", e);
        }
        return emails;
    }
}


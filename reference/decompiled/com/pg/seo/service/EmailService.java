/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.mail.MessagingException
 *  jakarta.mail.internet.MimeMessage
 *  org.apache.poi.ss.usermodel.Row
 *  org.apache.poi.ss.usermodel.Sheet
 *  org.apache.poi.xssf.usermodel.XSSFSheet
 *  org.apache.poi.xssf.usermodel.XSSFWorkbook
 *  org.jsoup.Jsoup
 *  org.jsoup.nodes.Document
 *  org.jsoup.nodes.Element
 *  org.jsoup.select.Elements
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.mail.javamail.JavaMailSenderImpl
 *  org.springframework.mail.javamail.MimeMessageHelper
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Service
 *  org.springframework.web.multipart.MultipartFile
 *  org.thymeleaf.context.Context
 *  org.thymeleaf.context.IContext
 *  org.thymeleaf.spring6.SpringTemplateEngine
 */
package com.pg.seo.service;

import com.pg.seo.entity.ClientGroup;
import com.pg.seo.entity.ClientMaster;
import com.pg.seo.entity.EmailExtractor;
import com.pg.seo.entity.EmailExtractorData;
import com.pg.seo.entity.EmailMaster;
import com.pg.seo.entity.EmailSendJob;
import com.pg.seo.entity.EmailSendStatus;
import com.pg.seo.entity.EmailTemplate;
import com.pg.seo.model.ClientGroupDto;
import com.pg.seo.model.ClientMasterDto;
import com.pg.seo.model.ClientMasterResponse;
import com.pg.seo.model.DashboardDbReport;
import com.pg.seo.model.EmailMasterDto;
import com.pg.seo.model.EmailSendRequest;
import com.pg.seo.model.Mail;
import com.pg.seo.model.PagingRequest;
import com.pg.seo.repository.ClientGroupRepository;
import com.pg.seo.repository.ClientMasterRepository;
import com.pg.seo.repository.EmailExtractorDataRepository;
import com.pg.seo.repository.EmailExtractorRepository;
import com.pg.seo.repository.EmailMasterRepository;
import com.pg.seo.repository.EmailSendJobRepository;
import com.pg.seo.repository.EmailSendStatusRepository;
import com.pg.seo.repository.EmailTemplateRepository;
import com.pg.seo.service.EmailExtractionService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {
    private SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy");
    private SimpleDateFormat sdfSqlDate = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdfRegularDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    @Autowired
    EmailMasterRepository emailMasterRepository;
    @Autowired
    ClientGroupRepository clientGroupRepository;
    @Autowired
    ClientMasterRepository clientMasterRepository;
    @Autowired
    EmailSendJobRepository emailSendJobRepository;
    @Autowired
    EmailTemplateRepository emailTemplateRepository;
    @Autowired
    EmailSendStatusRepository emailSendStatusRepository;
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    EmailExtractorRepository emailExtractorRepository;
    @Value(value="${app.upload.path}")
    public String uploadpath;
    @Autowired
    EmailExtractorDataRepository emailExtractorDataRepository;
    @Autowired
    EmailExtractionService emailExtractionService;

    public String emailSend(EmailSendRequest emailSendRequest, String userName, Long loginId) throws MessagingException {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDate = now.with(TemporalAdjusters.lastDayOfMonth());
        List<EmailSendJob> emailSendJobList = this.emailSendJobRepository.findByLoginIdMonth(loginId, Date.valueOf(startDate), Date.valueOf(lastDate));
        Integer emailSendJobCount = emailSendJobList.stream().map(EmailSendJob::getNoOfEmail).reduce(0, Integer::sum);
        if (emailSendJobCount > 50000) {
            return "Daily Limit Reached ";
        }
        EmailSendJob emailSendJob = new EmailSendJob();
        EmailMaster emailMaster = this.emailMasterRepository.findByEmail(emailSendRequest.getFromEmail());
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.starttls.enable", emailMaster.getEnable());
        props.put("mail.smtp.auth", emailMaster.getAuth());
        props.put("mail.transport.protocol", emailMaster.getProtocol());
        mailSender.setHost(emailMaster.getHost());
        mailSender.setPort(emailMaster.getPort().intValue());
        mailSender.setUsername(emailMaster.getUsername());
        mailSender.setPassword(emailMaster.getPassword());
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, 3, StandardCharsets.UTF_8.name());
        EmailTemplate emailTemplate = this.emailTemplateRepository.findByEmailTemplateId(Long.parseLong(emailSendRequest.getTemplate()));
        if (emailSendRequest.getEmailType().equalsIgnoreCase("G")) {
            List<ClientMaster> clientMasterList = this.clientMasterRepository.findByGroupCodeAndLoginId(emailSendRequest.getGroup(), loginId);
            emailSendJob.setFromEmail(emailSendRequest.getFromEmail());
            emailSendJob.setStatus("In Progress");
            emailSendJob.setType("Group");
            emailSendJob.setNoOfEmail(clientMasterList.size());
            emailSendJob.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
            emailSendJob.setCreatedBy(userName);
            emailSendJob.setDate(new Date(new java.util.Date().getTime()));
            emailSendJob.setLoginId(loginId);
            emailSendJob = (EmailSendJob)this.emailSendJobRepository.save(emailSendJob);
            int noOfSuccess = 0;
            for (ClientMaster clientMaster : clientMasterList) {
                EmailSendStatus emailSendStatus = new EmailSendStatus();
                emailSendStatus.setEmail(clientMaster.getEmail());
                emailSendStatus.setEmailTemplateId(emailTemplate.getEmailTemplateId());
                emailSendStatus.setDate(new Date(new java.util.Date().getTime()));
                emailSendStatus.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
                emailSendStatus.setCreatedBy(userName);
                emailSendStatus.setStatus("S");
                emailSendStatus.setLoginId(loginId);
                Mail mail = new Mail();
                mail.setFrom(emailSendRequest.getFromEmail());
                mail.setMailTo(clientMaster.getEmail());
                mail.setSubject(emailTemplate.getTemplateName());
                HashMap<String, Object> model = new HashMap<String, Object>();
                model.put("name", clientMaster.getName());
                model.put("webLink", clientMaster.getWebLink());
                model.put("content", emailTemplate.getTemplateContent());
                mail.setProps(model);
                String status = this.emailSender(mailSender, message, helper, mail);
                if (status.equalsIgnoreCase("SUCCESS")) {
                    ++noOfSuccess;
                }
                emailSendJob = (EmailSendJob)this.emailSendJobRepository.findById(emailSendJob.getEmailSendJobId()).get();
                emailSendJob.setNoOfSuccess(noOfSuccess);
                this.emailSendJobRepository.save(emailSendJob);
                emailSendStatus.setStatus(status);
                this.emailSendStatusRepository.save(emailSendStatus);
            }
        } else {
            emailSendJob.setFromEmail(emailSendRequest.getFromEmail());
            emailSendJob.setStatus("In Progress");
            emailSendJob.setType("Individual");
            emailSendJob.setNoOfEmail(emailSendRequest.getClient().length);
            emailSendJob.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
            emailSendJob.setCreatedBy(userName);
            emailSendJob.setDate(new Date(new java.util.Date().getTime()));
            emailSendJob.setLoginId(loginId);
            emailSendJob = (EmailSendJob)this.emailSendJobRepository.save(emailSendJob);
            int noOfSuccess = 0;
            for (String email : Arrays.asList(emailSendRequest.getClient())) {
                ClientMaster clientMaster = this.clientMasterRepository.findByClientMasterId(Long.parseLong(email));
                EmailSendStatus emailSendStatus = new EmailSendStatus();
                emailSendStatus.setEmail(clientMaster.getEmail());
                emailSendStatus.setEmailTemplateId(emailTemplate.getEmailTemplateId());
                emailSendStatus.setDate(new Date(new java.util.Date().getTime()));
                emailSendStatus.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
                emailSendStatus.setCreatedBy(userName);
                emailSendStatus.setStatus("S");
                emailSendStatus.setLoginId(loginId);
                Mail mail = new Mail();
                mail.setFrom(emailSendRequest.getFromEmail());
                mail.setMailTo(clientMaster.getEmail());
                mail.setSubject(emailTemplate.getTemplateName());
                HashMap<String, Object> model = new HashMap<String, Object>();
                model.put("name", clientMaster.getName());
                model.put("content", emailTemplate.getTemplateContent());
                model.put("webLink", clientMaster.getWebLink());
                mail.setProps(model);
                String status = this.emailSender(mailSender, message, helper, mail);
                if (status.equalsIgnoreCase("SUCCESS")) {
                    ++noOfSuccess;
                }
                emailSendJob = (EmailSendJob)this.emailSendJobRepository.findById(emailSendJob.getEmailSendJobId()).get();
                emailSendJob.setNoOfSuccess(noOfSuccess);
                this.emailSendJobRepository.save(emailSendJob);
                emailSendStatus.setStatus(status);
                this.emailSendStatusRepository.save(emailSendStatus);
            }
        }
        emailSendJob = (EmailSendJob)this.emailSendJobRepository.findById(emailSendJob.getEmailSendJobId()).get();
        emailSendJob.setStatus("Success");
        this.emailSendJobRepository.save(emailSendJob);
        return "SUCCESS";
    }

    private String emailSender(JavaMailSenderImpl mailSender, MimeMessage message, MimeMessageHelper helper, Mail mail) throws MessagingException {
        try {
            Context context = new Context();
            context.setVariables(mail.getProps());
            String html = this.templateEngine.process("mailtemplate", (IContext)context);
            helper.setTo(mail.getMailTo());
            helper.setText(html, true);
            helper.setSubject(mail.getSubject());
            helper.setFrom(mail.getFrom());
            mailSender.send(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "FAILURE";
        }
        return "SUCCESS";
    }

    public List<ClientGroupDto> getClientGroup(Long loginId) {
        List<ClientGroup> clientGroupList = this.clientGroupRepository.findByLoginId(loginId);
        List<ClientGroupDto> clientGroupDtoList = clientGroupList.stream().map(a -> new ClientGroupDto(a.getGroupId(), a.getGroupCode(), a.getGroupDesc())).collect(Collectors.toList());
        return clientGroupDtoList;
    }

    public ClientMasterResponse getClientMaster(Long loginId) {
        ClientMasterResponse clientMasterResponse = new ClientMasterResponse();
        List<ClientMaster> clientMasterList = this.clientMasterRepository.findByLoginId(loginId);
        List<ClientMasterDto> clientMasterDto = clientMasterList.stream().map(a -> new ClientMasterDto(a.getClientMasterId(), a.getName(), a.getEmail(), a.getWebLink(), a.getClientGroup().getGroupCode())).collect(Collectors.toList());
        clientMasterResponse.setClientMasterDtoList(clientMasterDto);
        return clientMasterResponse;
    }

    public ClientMasterResponse getClientMasterPagination(Long loginId, PagingRequest pagingRequest) {
        ClientMasterResponse clientMasterResponse = new ClientMasterResponse();
        PageRequest paging = PageRequest.of((int)(pagingRequest.getStart() / 100), (int)100);
        Page<ClientMaster> clientMasterPage = this.clientMasterRepository.findAllByLoginId(loginId, (Pageable)paging);
        List<ClientMasterDto> clientMasterDto = clientMasterPage.getContent().stream().map(a -> new ClientMasterDto(a.getClientMasterId(), a.getName(), a.getEmail(), a.getWebLink(), a.getClientGroup().getGroupCode())).collect(Collectors.toList());
        clientMasterResponse.setClientMasterDtoList(clientMasterDto);
        clientMasterResponse.setTotalRecord(clientMasterPage.getTotalElements());
        return clientMasterResponse;
    }

    public List<EmailMaster> getEmailMasters(Long loginId) {
        List<EmailMaster> emailMasterList = this.emailMasterRepository.findByLoginId(loginId);
        return emailMasterList;
    }

    public List<EmailTemplate> getEmailTemplates(Long loginId) {
        List<EmailTemplate> emailTemplateList = this.emailTemplateRepository.findByLoginId(loginId);
        return emailTemplateList;
    }

    public EmailTemplate getEmailTemplate(Long emailTemplateId) {
        EmailTemplate emailTemplate = this.emailTemplateRepository.findByEmailTemplateId(emailTemplateId);
        return emailTemplate;
    }

    public Long removeEmailTemplate(Long emailTemplateId, String userName) {
        EmailTemplate emailTemplate = this.emailTemplateRepository.findByEmailTemplateId(emailTemplateId);
        emailTemplate.setIsActive(0);
        emailTemplate.setModifiedBy(userName);
        emailTemplate.setModifiedDate(this.sdfDateTime.format(new java.util.Date()));
        emailTemplate = (EmailTemplate)this.emailTemplateRepository.save(emailTemplate);
        return emailTemplate.getEmailTemplateId();
    }

    public String saveEmailTemplate(String templateName, String templateContent, String userName, Long loginId) {
        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setTemplateName(templateName);
        emailTemplate.setTemplateContent(templateContent);
        emailTemplate.setLoginId(loginId);
        emailTemplate.setStatus("A");
        emailTemplate.setIsActive(1);
        emailTemplate.setCreatedBy(userName);
        emailTemplate.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
        emailTemplate = (EmailTemplate)this.emailTemplateRepository.save(emailTemplate);
        return "SUCCESS";
    }

    public String addEmailClient(MultipartFile file, String groupCode, String userName, Long loginId) {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream());
            XSSFSheet ws = wb.getSheetAt(0);
            Iterator rowIterator = ws.iterator();
            int count = 0;
            ArrayList<ClientMaster> clientMasterList = new ArrayList<ClientMaster>();
            while (rowIterator.hasNext()) {
                Row row = (Row)rowIterator.next();
                Iterator cellIterator = row.cellIterator();
                if (count >= 1) {
                    ClientMaster clientMaster = new ClientMaster();
                    if (row.getCell(0) != null) {
                        clientMaster.setEmail(row.getCell(1).getStringCellValue());
                        clientMaster.setName(row.getCell(0).getStringCellValue());
                        clientMaster.setWebLink(row.getCell(0).getStringCellValue());
                        clientMaster.setClientGroup(this.clientGroupRepository.findByLoginIdAndGroupCode(loginId, groupCode));
                        clientMaster.setLoginId(loginId);
                        clientMaster.setIsActive(1);
                        clientMaster.setCreatedBy(userName);
                        clientMaster.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
                        clientMasterList.add(clientMaster);
                    }
                }
                ++count;
            }
            this.clientMasterRepository.saveAll(clientMasterList);
            wb.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
        return "SUCCESS";
    }

    public Long createClientGroup(String groupCode, String groupDesc, String userName, Long loginId) {
        ClientGroup group = new ClientGroup();
        group = this.clientGroupRepository.findByGroupCode(groupCode);
        if (group != null) {
            return 0L;
        }
        group = new ClientGroup();
        group.setLoginId(loginId);
        group.setGroupCode(groupCode);
        group.setGroupDesc(groupDesc);
        group.setIsActive(1);
        group.setCreatedBy(userName);
        group.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
        this.clientGroupRepository.save(group);
        return group.getGroupId();
    }

    public Long removeClientGroup(String groupCode, String userName, Long loginId) {
        List<ClientMaster> clientMasterList = this.clientMasterRepository.findByGroupCode(groupCode);
        if (clientMasterList.size() > 0) {
            return 0L;
        }
        ClientGroup group = this.clientGroupRepository.findByLoginIdAndGroupCode(loginId, groupCode);
        group.setIsActive(0);
        group.setModifiedBy(userName);
        group.setModifiedDate(this.sdfDateTime.format(new java.util.Date()));
        this.clientGroupRepository.save(group);
        return group.getGroupId();
    }

    public Integer removeClient(String groupCode, String userName, Long loginId) {
        List<ClientMaster> clientMasterList = this.clientMasterRepository.findByGroupCode(groupCode);
        if (clientMasterList.size() <= 0) {
            return 0;
        }
        clientMasterList = clientMasterList.stream().map(a -> {
            a.setIsActive(0);
            return a;
        }).collect(Collectors.toList());
        this.clientMasterRepository.saveAll(clientMasterList);
        return clientMasterList.size();
    }

    public List<EmailSendJob> getEmailSendJobs(Long loginId) {
        List<EmailSendJob> emailSendJobList = this.emailSendJobRepository.findByDateAndLoginId(new Date(new java.util.Date().getTime()), loginId);
        return emailSendJobList;
    }

    public Long saveEmailMaster(EmailMasterDto emailMasterDto, String userName, Long loginId) {
        EmailMaster emailMaster = new EmailMaster();
        if (emailMasterDto.getEmailMasterId() != null) {
            emailMaster = this.emailMasterRepository.findByEmailMasterId(emailMasterDto.getEmailMasterId());
            emailMaster.setEmail(emailMasterDto.getEmail());
            emailMaster.setUsername(emailMasterDto.getEmail());
            emailMaster.setProtocol(emailMasterDto.getProtocol());
            emailMaster.setHost(emailMasterDto.getHost());
            emailMaster.setAuth(emailMasterDto.getAuth());
            emailMaster.setEnable(emailMasterDto.getEnable());
            emailMaster.setPort(emailMasterDto.getPort());
            emailMaster.setPassword(emailMasterDto.getPassword());
            emailMaster.setLoginId(loginId);
            emailMaster.setModifiedBy(userName);
            emailMaster.setModifiedDate(this.sdfDateTime.format(new java.util.Date()));
        } else {
            emailMaster.setEmail(emailMasterDto.getEmail());
            emailMaster.setUsername(emailMasterDto.getEmail());
            emailMaster.setProtocol(emailMasterDto.getProtocol());
            emailMaster.setHost(emailMasterDto.getHost());
            emailMaster.setAuth(emailMasterDto.getAuth());
            emailMaster.setEnable(emailMasterDto.getEnable());
            emailMaster.setPort(emailMasterDto.getPort());
            emailMaster.setPassword(emailMasterDto.getPassword());
            emailMaster.setLoginId(loginId);
            emailMaster.setCreatedBy(userName);
            emailMaster.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
            emailMaster.setIsActive(1);
        }
        emailMaster = (EmailMaster)this.emailMasterRepository.save(emailMaster);
        return emailMaster.getEmailMasterId();
    }

    public EmailMaster getEmailMaster(Long emailMasterId) {
        EmailMaster emailMaster = this.emailMasterRepository.findByEmailMasterId(emailMasterId);
        return emailMaster;
    }

    public Long rmoveEmailMaster(Long emailMasterId, String userName) {
        EmailMaster emailMaster = this.emailMasterRepository.findByEmailMasterId(emailMasterId);
        emailMaster.setModifiedBy(userName);
        emailMaster.setModifiedDate(this.sdfDateTime.format(new java.util.Date()));
        emailMaster.setIsActive(0);
        emailMaster = (EmailMaster)this.emailMasterRepository.save(emailMaster);
        return emailMaster.getEmailMasterId();
    }

    public DashboardDbReport getDashboardDbReport(Long loginId) {
        DashboardDbReport dashboardDbReport = new DashboardDbReport();
        List<EmailTemplate> emailTemplateList = this.emailTemplateRepository.findByLoginId(loginId);
        List<ClientGroup> clientGroupList = this.clientGroupRepository.findByLoginId(loginId);
        List<ClientMaster> clientMasterList = this.clientMasterRepository.findByLoginId(loginId);
        List<EmailMaster> emailMasterList = this.emailMasterRepository.findByLoginId(loginId);
        dashboardDbReport.setEmailCount(emailMasterList.size());
        dashboardDbReport.setClientCount(clientMasterList.size());
        dashboardDbReport.setGroupCount(clientGroupList.size());
        dashboardDbReport.setTemplateCount(emailTemplateList.size());
        return dashboardDbReport;
    }

    public List<EmailTemplate> getEmailTemplatesForApprove() {
        List emailTemplateList = this.emailTemplateRepository.findAll();
        return emailTemplateList.stream().filter(a -> a.getStatus().equalsIgnoreCase("N")).collect(Collectors.toList());
    }

    public Long approveTemplate(String userName, Long emailTemplateId) {
        EmailTemplate emailTemplate = this.emailTemplateRepository.findByEmailTemplateId(emailTemplateId);
        emailTemplate.setStatus("A");
        emailTemplate.setModifiedBy(userName);
        emailTemplate.setModifiedDate(this.sdfDateTime.format(new java.util.Date()));
        emailTemplate = (EmailTemplate)this.emailTemplateRepository.save(emailTemplate);
        return emailTemplate.getEmailTemplateId();
    }

    @Async
    public void extract(InputStream file, Long loginId) throws ExecutionException, InterruptedException, TimeoutException {
        int count = 0;
        EmailExtractor emailExtractor = new EmailExtractor();
        emailExtractor.setLoginId(loginId);
        emailExtractor.setStatus("IN PROGRESS");
        emailExtractor.setStartDate(new Date(new java.util.Date().getTime()));
        emailExtractor.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
        emailExtractor.setNoOfDomain(0L);
        emailExtractor = (EmailExtractor)this.emailExtractorRepository.save(emailExtractor);
        Long emailExtractorId = emailExtractor.getEmailExtractorId();
        ArrayList resultList1 = new ArrayList();
        ArrayList completableFutures = new ArrayList();
        try {
            XSSFWorkbook wb = new XSSFWorkbook(file);
            XSSFSheet ws = wb.getSheetAt(0);
            for (Row row : ws) {
                if (count >= 1) {
                    CompletableFuture requestCompletableFuture = CompletableFuture.supplyAsync(() -> this.extracted(row, emailExtractorId)).completeOnTimeout(new ConcurrentHashMap(), 360L, TimeUnit.MINUTES);
                    completableFutures.add(requestCompletableFuture);
                }
                ++count;
            }
            wb.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));
        CompletionStage allPageContentsFuture = allFutures.thenApply(v -> completableFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        ((CompletableFuture)allPageContentsFuture).get();
        EmailExtractor emailExtractorUpdate = this.emailExtractorRepository.findByEmailExtractorId(emailExtractorId);
        emailExtractorUpdate.setStatus("SUCCESS");
        this.emailExtractorRepository.save(emailExtractorUpdate);
    }

    public ConcurrentHashMap<String, String> extracted(Row row, Long emailExtractorId) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        try {
            Document doc = Jsoup.connect((String)("https://" + row.getCell(0).getStringCellValue())).get();
            Elements contact = doc.getAllElements();
            for (Element link : contact) {
                Pattern pat = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.]*@[a-zA-Z0-9]+([.][a-zA-Z]+)+");
                String l = link.html();
                Matcher mat = pat.matcher(l);
                while (mat.find()) {
                    String email = mat.group();
                    map.put(mat.group(), row.getCell(0).getStringCellValue());
                    String domain = row.getCell(0).getStringCellValue();
                    boolean status = email.contains(domain.split("\\.")[0]) && !email.endsWith(".png") && !email.endsWith(".jpg") && !email.endsWith(".gif") && !email.endsWith(".jpeg") && !email.endsWith(".JPG") && !email.endsWith(".GIF") && !email.endsWith(".JPEG") && !email.endsWith(".PNG") && !email.endsWith(".webp") && !email.endsWith("domain") && !email.endsWith("email.com") && !email.endsWith(".org") && !email.contains("google") && !email.contains("example") && !email.contains("sentry") && !email.contains("u003") && !email.contains("@email") && !email.contains("@hotmail") && !email.contains("@live") && !email.contains("@me") && !email.contains("yourname") && !email.contains(".php") && !email.contains("noreply") && !email.contains("googleadservices") && !email.contains("dictionary.reference") && !email.contains("mnsi.net") && !email.contains("answers.com") && !email.contains("@company") && !email.contains("@gmail");
                    if (!status) continue;
                    try {
                        EmailExtractorData emailExtractorData = new EmailExtractorData();
                        emailExtractorData.setEmailExtractorId(emailExtractorId);
                        emailExtractorData.setDomain(domain);
                        emailExtractorData.setEmail(email);
                        emailExtractorData.setStartDate(new Date(new java.util.Date().getTime()));
                        emailExtractorData.setCreatedDate(this.sdfDateTime.format(new java.util.Date()));
                        this.emailExtractorDataRepository.save(emailExtractorData);
                    }
                    catch (Exception exception) {}
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return map;
    }

    public String generateExcel(String startDate, String endDate) throws ParseException {
        new Date(this.sdfSqlDate.parse(startDate).getTime());
        List<EmailExtractorData> emailExtractorDatalist = this.emailExtractorDataRepository.findByStartDateBetween(new Date(this.sdfSqlDate.parse(startDate).getTime()), new Date(this.sdfSqlDate.parse(endDate).getTime()));
        List emailExtractorDatalistNew = emailExtractorDatalist.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<EmailExtractorData>(Comparator.comparing(EmailExtractorData::getDomain))), ArrayList::new));
        Object filename = "";
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheetEmail = workbook.createSheet("email-excel-sheet");
        int emailRowCount = 1;
        Row emailRow = sheetEmail.createRow(0);
        emailRow.createCell(0).setCellValue("Domain");
        emailRow.createCell(1).setCellValue("Email");
        for (EmailExtractorData emailExtractData : emailExtractorDatalistNew) {
            Row row = sheetEmail.createRow(emailRowCount);
            row.createCell(0).setCellValue(emailExtractData.getDomain());
            row.createCell(1).setCellValue(emailExtractData.getEmail());
            ++emailRowCount;
        }
        try {
            filename = startDate + "_" + endDate + "_" + emailExtractorDatalistNew.size() + "1.xlsx";
            FileOutputStream out = new FileOutputStream(new File(this.uploadpath + (String)filename));
            workbook.write((OutputStream)out);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return filename;
    }

    public List<EmailExtractor> getEmailExtractorList(Long loginId) {
        List<EmailExtractor> emailExtractorList = this.emailExtractorRepository.findByLoginId(loginId);
        emailExtractorList.sort(Comparator.comparingLong(EmailExtractor::getEmailExtractorId).reversed());
        return emailExtractorList;
    }

    public EmailExtractor getEmailExtractor(Long emailExtractorId, Long loginId) {
        EmailExtractor emailExtractor = this.emailExtractorRepository.findByEmailExtractorIdAndLoginId(emailExtractorId, loginId);
        return emailExtractor;
    }

    public List<Long> getMonthlyReport(int month, Long loginId) {
        ArrayList<Long> monthlyReport = new ArrayList<Long>();
        LocalDate now = LocalDate.now();
        if (month > 0) {
            now = now.withMonth(month);
        }
        LocalDate startDate = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDate = now.with(TemporalAdjusters.lastDayOfMonth());
        List<EmailExtractor> emailExtractorList = this.emailExtractorRepository.findByLoginIdMonth(loginId, Date.valueOf(startDate), Date.valueOf(lastDate));
        Long extractCount = emailExtractorList.stream().map(EmailExtractor::getNoOfDomain).reduce(0L, Long::sum);
        List<EmailSendJob> emailSendJobList = this.emailSendJobRepository.findByLoginIdMonth(loginId, Date.valueOf(startDate), Date.valueOf(lastDate));
        Integer emailSendJobCount = emailSendJobList.stream().map(EmailSendJob::getNoOfEmail).reduce(0, Integer::sum);
        monthlyReport.add(Long.valueOf(emailSendJobCount.intValue()));
        monthlyReport.add(extractCount);
        return monthlyReport;
    }

    public List<Long> getMonthlyEmailReport(int month, Long loginId) {
        ArrayList<Long> monthlyReport = new ArrayList<Long>();
        LocalDate now = LocalDate.now();
        if (month > 0) {
            now = now.withMonth(month);
        }
        LocalDate startDate = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDate = now.with(TemporalAdjusters.lastDayOfMonth());
        List<EmailSendJob> emailSendJobList = this.emailSendJobRepository.findByLoginIdMonth(loginId, Date.valueOf(startDate), Date.valueOf(lastDate));
        Integer emailSendJobCount = emailSendJobList.stream().map(EmailSendJob::getNoOfEmail).reduce(0, Integer::sum);
        Integer emailSendSuccessJobCount = emailSendJobList.stream().map(EmailSendJob::getNoOfSuccess).reduce(0, Integer::sum);
        monthlyReport.add(Long.valueOf(emailSendJobCount.intValue()));
        monthlyReport.add(Long.valueOf(emailSendSuccessJobCount.intValue()));
        monthlyReport.add(Long.valueOf(emailSendJobCount - emailSendSuccessJobCount));
        return monthlyReport;
    }
}


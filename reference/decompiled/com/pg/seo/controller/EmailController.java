/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  jakarta.servlet.http.HttpSession
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.io.ByteArrayResource
 *  org.springframework.core.io.Resource
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.MediaType
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.ResponseEntity$BodyBuilder
 *  org.springframework.security.core.Authentication
 *  org.springframework.stereotype.Controller
 *  org.springframework.ui.Model
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestMethod
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.ResponseBody
 *  org.springframework.web.multipart.MultipartFile
 *  org.springframework.web.servlet.mvc.support.RedirectAttributes
 */
package com.pg.seo.controller;

import com.pg.seo.entity.EmailExtractor;
import com.pg.seo.entity.EmailMaster;
import com.pg.seo.entity.EmailSendJob;
import com.pg.seo.entity.EmailTemplate;
import com.pg.seo.model.BaseResponse;
import com.pg.seo.model.ClientGroupDto;
import com.pg.seo.model.ClientMasterDto;
import com.pg.seo.model.ClientMasterResponse;
import com.pg.seo.model.DashboardDbReport;
import com.pg.seo.model.EmailDto;
import com.pg.seo.model.EmailMasterDto;
import com.pg.seo.model.EmailSendRequest;
import com.pg.seo.model.EmailTemplateRequest;
import com.pg.seo.model.Page;
import com.pg.seo.model.PagingRequest;
import com.pg.seo.service.EmailInboxService;
import com.pg.seo.service.EmailService;
import com.pg.seo.service.NewEmailExtractorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailController {
    @Autowired
    EmailService emailService;
    @Autowired
    EmailInboxService emailInboxService;
    @Autowired
    NewEmailExtractorService emailExtractorService;
    SimpleDateFormat simpDate = new SimpleDateFormat("MM-dd-yyyy");
    SimpleDateFormat sqlDate = new SimpleDateFormat("yyyy-MM-dd");
    @Value(value="${app.upload.path}")
    public String uploadpath;

    @RequestMapping(value={"/email"}, method={RequestMethod.GET})
    public String email(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "EmailSend";
    }

    @RequestMapping(value={"/getClientGroup"}, method={RequestMethod.POST})
    @ResponseBody
    public List<ClientGroupDto> getClientGroup(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getClientGroup(loginId);
    }

    @RequestMapping(value={"/getClientMaster"}, method={RequestMethod.POST})
    @ResponseBody
    public ClientMasterResponse getClientMaster(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getClientMaster(loginId);
    }

    @RequestMapping(value={"/getEmailMaster"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailMaster> getEmailMaster(HttpServletRequest req, HttpSession ses, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getEmailMasters(loginId);
    }

    @RequestMapping(value={"/emailTemplate"}, method={RequestMethod.GET})
    public String emailTemplate(HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "EmailTemplate";
    }

    @RequestMapping(value={"/getEmailTemplate"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailTemplate> getEmailTemplate(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getEmailTemplates(loginId);
    }

    @RequestMapping(value={"/getEmailTemplateApprove"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailTemplate> getEmailTemplateForSend(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getEmailTemplates(loginId).stream().filter(a -> a.getStatus().equalsIgnoreCase("A")).collect(Collectors.toList());
    }

    @RequestMapping(value={"/getEmailTemplateApproveList"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailTemplate> getEmailTemplateApproveList(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        return this.emailService.getEmailTemplatesForApprove();
    }

    @RequestMapping(value={"/getTemplate"}, method={RequestMethod.GET})
    @ResponseBody
    public EmailTemplate getTemplate(@RequestParam(value="emailTemplateId") Long emailTemplateId, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        return this.emailService.getEmailTemplate(emailTemplateId);
    }

    @RequestMapping(value={"/saveEmailTemplate"}, method={RequestMethod.POST})
    @ResponseBody
    public BaseResponse saveEmailTemplate(@RequestBody EmailTemplateRequest emailTemplateRequest, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res, Authentication auth) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        String result = this.emailService.saveEmailTemplate(emailTemplateRequest.getTemplateName(), emailTemplateRequest.getTemplateContent(), auth.getName(), loginId);
        baseResponse.setStatusMessage(result);
        return baseResponse;
    }

    @GetMapping(value={"/removeTemplate"})
    @ResponseBody
    public BaseResponse removeTemplate(@RequestParam(value="emailTemplateId") Long emailTemplateId, HttpServletRequest req, Authentication auth) {
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.removeEmailTemplate(emailTemplateId, auth.getName());
        if (count > 0L) {
            baseResponse.setStatusMessage("SUCCESS");
            baseResponse.setStatusDesc("Template Removed");
            return baseResponse;
        }
        baseResponse.setStatusMessage("FAILURE");
        baseResponse.setStatusDesc("Unable to remove template");
        return baseResponse;
    }

    @RequestMapping(value={"/sendEmail"}, method={RequestMethod.POST})
    @ResponseBody
    public BaseResponse sendEmail(@RequestBody EmailSendRequest emailSendRequest, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res, Authentication auth) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        String result = this.emailService.emailSend(emailSendRequest, auth.getName(), loginId);
        baseResponse.setStatusMessage(result);
        return baseResponse;
    }

    @PostMapping(value={"/emailClientUpload"})
    @ResponseBody
    public BaseResponse emailClientUpload(@RequestParam(value="file") MultipartFile uploadfiles, @RequestParam(value="groupCode") String groupCode, HttpSession ses, HttpServletRequest req, Authentication auth) {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        try {
            String result = this.emailService.addEmailClient(uploadfiles, groupCode, auth.getName(), loginId);
            baseResponse.setStatusMessage(result);
        }
        catch (Exception e) {
            baseResponse.setStatusMessage("FAILUER");
            e.printStackTrace();
        }
        return baseResponse;
    }

    @RequestMapping(value={"/emailClient"}, method={RequestMethod.GET})
    public String emailClient(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "emailClient";
    }

    @RequestMapping(value={"/getClientMasterPagination"}, method={RequestMethod.POST})
    @ResponseBody
    public Page<ClientMasterDto> getClientMasterPagination(@RequestBody PagingRequest pagingRequest, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        ClientMasterResponse clientMasterResponse = this.emailService.getClientMasterPagination(loginId, pagingRequest);
        Page<ClientMasterDto> page = new Page<ClientMasterDto>(clientMasterResponse.getClientMasterDtoList());
        page.setRecordsFiltered(clientMasterResponse.getTotalRecord());
        page.setRecordsTotal(100L);
        page.setDraw(pagingRequest.getDraw());
        return page;
    }

    @RequestMapping(value={"/clientGroup"}, method={RequestMethod.GET})
    public String clientGroup(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "ClientGroup";
    }

    @GetMapping(value={"/createClientGroup"})
    @ResponseBody
    public BaseResponse createClientGroup(@RequestParam(value="groupCode") String groupCode, @RequestParam(value="groupDesc") String groupDesc, HttpServletRequest req, Authentication auth, HttpSession ses) {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.createClientGroup(groupCode, groupDesc, auth.getName(), loginId);
        if (count == 0L) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("Group Code Exist Please Provide New One ");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("Group Created");
        return baseResponse;
    }

    @GetMapping(value={"/removeClientGroup"})
    @ResponseBody
    public BaseResponse removeClientGroup(@RequestParam(value="groupCode") String groupCode, HttpServletRequest req, Authentication auth, HttpSession ses) {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.removeClientGroup(groupCode, auth.getName(), loginId);
        if (count == 0L) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("Please remove all client from this group then you can remove group");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("Group Removed");
        return baseResponse;
    }

    @GetMapping(value={"/removeClient"})
    @ResponseBody
    public BaseResponse removeClient(@RequestParam(value="groupCode") String groupCode, HttpServletRequest req, Authentication auth, HttpSession ses) {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        Integer count = this.emailService.removeClient(groupCode, auth.getName(), loginId);
        if (count == 0) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("No Client Exist in this group");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("Client Removed");
        return baseResponse;
    }

    @GetMapping(value={"/getEmailInbox"})
    @ResponseBody
    public List<EmailDto> getEmailInbox(@RequestParam(value="emailMasterId") Long emailMasterId, @RequestParam(value="fromDate") String fromDate, @RequestParam(value="toDate") String toDate, HttpServletRequest req, Authentication auth) {
        return this.emailInboxService.getEmailInbox(emailMasterId, fromDate, toDate);
    }

    @GetMapping(value={"/emailInbox"})
    public String emailInbox(HttpServletRequest req, Authentication auth) {
        return "EmailInbox";
    }

    @RequestMapping(value={"/getEmailSendJobs"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailSendJob> getEmailSendJobs(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getEmailSendJobs(loginId);
    }

    @GetMapping(value={"/taskReports"})
    @ResponseBody
    public List<Long> taskComments(@RequestParam int month, @RequestParam String filterType, HttpSession ses) {
        ArrayList<Long> response = new ArrayList<Long>();
        response.add(1000L);
        response.add(3000L);
        response.add(4000L);
        response.add(2000L);
        response.add(500L);
        return response;
    }

    @RequestMapping(value={"/emailMaster"}, method={RequestMethod.GET})
    public String emailMaster(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "EmailMaster";
    }

    @RequestMapping(value={"/saveEmail"}, method={RequestMethod.POST})
    @ResponseBody
    public BaseResponse saveEmailMaster(@RequestBody EmailMasterDto emailMasterDto, HttpServletRequest req, Authentication auth, HttpSession ses) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.saveEmailMaster(emailMasterDto, auth.getName(), loginId);
        if (count == 0L) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("FAILURE");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("SUCCESS");
        return baseResponse;
    }

    @GetMapping(value={"/getEmailMasterById"})
    @ResponseBody
    public EmailMaster getEmailMasterById(@RequestParam Long emailMasterId, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        return this.emailService.getEmailMaster(emailMasterId);
    }

    @GetMapping(value={"/removeEmail"})
    @ResponseBody
    public BaseResponse removeEmail(@RequestParam(value="emailMasterId") Long emailMasterId, HttpServletRequest req, Authentication auth) {
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.rmoveEmailMaster(emailMasterId, auth.getName());
        if (count == 0L) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("FAILURE");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("Email Removed");
        return baseResponse;
    }

    @GetMapping(value={"/getDashboardDbReport"})
    @ResponseBody
    public DashboardDbReport getDashboardDbReport(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getDashboardDbReport(loginId);
    }

    @GetMapping(value={"/approveTemplate"})
    @ResponseBody
    public BaseResponse approveTemplate(@RequestParam(value="emailTemplateId") Long emailTemplateId, HttpServletRequest req, Authentication auth) {
        BaseResponse baseResponse = new BaseResponse();
        Long count = this.emailService.approveTemplate(auth.getName(), emailTemplateId);
        if (count == 0L) {
            baseResponse.setStatusMessage("FAILURE");
            baseResponse.setStatusDesc("FAILURE");
            return baseResponse;
        }
        baseResponse.setStatusMessage("SUCCESS");
        baseResponse.setStatusDesc("Template Approved");
        return baseResponse;
    }

    @RequestMapping(value={"/approveTemplates"}, method={RequestMethod.GET})
    public String approveTemplates(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        return "EmailTemplateApprove";
    }

    @RequestMapping(value={"/emailExtractor"}, method={RequestMethod.GET})
    public String emailExtractor(Model model, String error, String logout, HttpServletRequest req, HttpSession ses, HttpServletResponse response) {
        model.addAttribute("fromDate", (Object)this.sqlDate.format(new Date()));
        model.addAttribute("toDate", (Object)this.sqlDate.format(new Date()));
        return "EmailExtractor";
    }

    @PostMapping(value={"/emailExtractor"})
    @ResponseBody
    public BaseResponse emailExtractor(@RequestParam(value="file") MultipartFile uploadfiles, HttpSession ses, HttpServletRequest req, Authentication auth) {
        Long loginId = (Long)ses.getAttribute("loginId");
        BaseResponse baseResponse = new BaseResponse();
        try {
            this.emailExtractorService.extract(uploadfiles.getInputStream(), loginId);
            baseResponse.setStatusMessage("result");
        }
        catch (Exception e) {
            baseResponse.setStatusMessage("FAILUER");
            e.printStackTrace();
        }
        return baseResponse;
    }

    @RequestMapping(value={"/getEmailExtractor"}, method={RequestMethod.POST})
    @ResponseBody
    public List<EmailExtractor> getEmailExtractor(HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getEmailExtractorList(loginId);
    }

    @PostMapping(path={"/downloadFile"})
    public ResponseEntity<Resource> downloadFile(@RequestParam(value="fromDate") String fromDate, @RequestParam(value="toDate") String toDate, HttpSession ses, HttpServletRequest req) throws IOException, IOException {
        Long loginId = (Long)ses.getAttribute("loginId");
        String fileName = "";
        try {
            fileName = this.emailService.generateExcel(fromDate, toDate);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(this.uploadpath + fileName);
        HttpHeaders header = new HttpHeaders();
        header.add("Content-Disposition", "attachment; filename=" + fileName);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");
        Path path = Paths.get(file.getAbsolutePath(), new String[0]);
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        return ((ResponseEntity.BodyBuilder)ResponseEntity.ok().headers(header)).contentLength(file.length()).contentType(MediaType.parseMediaType((String)"application/octet-stream")).body((Object)resource);
    }

    @RequestMapping(value={"/getMonthlyReport"}, method={RequestMethod.GET})
    @ResponseBody
    public List<Long> getMonthlyReport(@RequestParam(value="month") int month, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getMonthlyReport(month, loginId);
    }

    @RequestMapping(value={"/getMonthlyEmailReport"}, method={RequestMethod.GET})
    @ResponseBody
    public List<Long> getMonthlyEmailReport(@RequestParam(value="month") int month, HttpServletRequest req, HttpSession ses, RedirectAttributes redir, HttpServletResponse res) throws Exception {
        Long loginId = (Long)ses.getAttribute("loginId");
        return this.emailService.getMonthlyEmailReport(month, loginId);
    }
}


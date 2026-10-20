package io.github.instaxdutta.winnow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import io.github.instaxdutta.winnow.domain.AccountKind;
import io.github.instaxdutta.winnow.domain.AccountProtocol;
import io.github.instaxdutta.winnow.domain.AppRole;
import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.domain.AuthMethod;
import io.github.instaxdutta.winnow.domain.Campaign;
import io.github.instaxdutta.winnow.domain.CampaignStatus;
import io.github.instaxdutta.winnow.domain.Client;
import io.github.instaxdutta.winnow.domain.ClientGroup;
import io.github.instaxdutta.winnow.domain.EmailTemplate;
import io.github.instaxdutta.winnow.domain.GmailAccount;
import io.github.instaxdutta.winnow.domain.Outbox;
import io.github.instaxdutta.winnow.domain.OutboxStatus;
import io.github.instaxdutta.winnow.domain.SmtpAccount;
import io.github.instaxdutta.winnow.domain.TargetType;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import io.github.instaxdutta.winnow.domain.UserStatus;
import io.github.instaxdutta.winnow.repository.AppRoleRepository;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import io.github.instaxdutta.winnow.repository.CampaignRepository;
import io.github.instaxdutta.winnow.repository.ClientGroupRepository;
import io.github.instaxdutta.winnow.repository.ClientRepository;
import io.github.instaxdutta.winnow.repository.EmailTemplateRepository;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import io.github.instaxdutta.winnow.repository.OutboxRepository;
import io.github.instaxdutta.winnow.repository.SmtpAccountRepository;
import io.github.instaxdutta.winnow.security.AppUserPrincipal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The CSRF protection of every mutating endpoint.
 *
 * <p>The list of the endpoints is not written down here from memory: it is read out of the
 * application itself, and the test that reads it proves that the endpoints this file
 * covers are exactly the mutating endpoints of the application.
 *
 * <p>Every endpoint is asked twice: once without a CSRF token, which must be refused
 * before the controller runs, and once with a token, which must reach the controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CsrfProtectionIT {

    /**
     * The login of the fixture user.
     *
     * <p>A test password is a fixture, not a secret. The hash below is the BCrypt hash
     * of that login at the cost the application uses, so the tests do not pay for the
     * hashing on every fixture.
     */
    private static final String FIXTURE_LOGIN = "test-password-12345";

    /** The BCrypt hash of the fixture login, at cost twelve. */
    private static final String FIXTURE_LOGIN_HASH = "$2a$12$UeN7q.nYCnifiD2h9LVCDuHvKYFsnEniZSTktGLb6VBG29U.dKONm";

    /** The marker of a successful answer of the API. */
    private static final String SUCCESS = "\"status\":\"SUCCESS\"";

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private AppRoleRepository roles;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientGroupRepository clientGroups;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private EmailTemplateRepository templates;

    @Autowired
    private SmtpAccountRepository smtpAccounts;

    @Autowired
    private GmailAccountRepository gmailAccounts;

    @Autowired
    private CampaignRepository campaigns;

    @Autowired
    private OutboxRepository outboxes;

    private String token;
    private AppUserPrincipal principal;

    private ClientGroup group;
    private Client client;
    private EmailTemplate templateForUpdate;
    private EmailTemplate templateForSubmit;
    private EmailTemplate templateForApprove;
    private EmailTemplate templateForReject;
    private EmailTemplate templateForDelete;
    private SmtpAccount smtpForUpdate;
    private SmtpAccount smtpForDelete;
    private SmtpAccount smtpForCampaign;
    private GmailAccount gmailForUpdate;
    private GmailAccount gmailForDelete;
    private Campaign campaignForCancel;
    private Outbox outboxForCancel;

    // ------------------------------------------------------------------ fixtures

    @BeforeEach
    void createFixtures() {
        // The hash of the fixture login has to be the hash of that login, otherwise the
        // password change endpoint would refuse it for the wrong reason.
        assertThat(passwordEncoder.matches(FIXTURE_LOGIN, FIXTURE_LOGIN_HASH)).isTrue();
        token = UUID.randomUUID().toString().substring(0, 8);
        principal = createAdministrator("csrf-admin-" + token);

        group = newGroup("GRP-" + token);
        client = newClient(group, "Client of the CSRF test");
        templateForUpdate = newTemplate("Template to update", TemplateStatus.APPROVED);
        templateForSubmit = newTemplate("Template to submit", TemplateStatus.DRAFT);
        templateForApprove = newTemplate("Template to approve", TemplateStatus.PENDING_APPROVAL);
        templateForReject = newTemplate("Template to reject", TemplateStatus.PENDING_APPROVAL);
        templateForDelete = newTemplate("Template to delete", TemplateStatus.DRAFT);
        smtpForUpdate = newSmtpAccount("smtp-update-" + token + "@example.invalid");
        smtpForDelete = newSmtpAccount("smtp-delete-" + token + "@example.invalid");
        smtpForCampaign = newSmtpAccount("smtp-campaign-" + token + "@example.invalid");
        gmailForUpdate = newGmailAccount("gmail-update-" + token + "@example.invalid");
        gmailForDelete = newGmailAccount("gmail-delete-" + token + "@example.invalid");
        campaignForCancel = newCampaign(templateForUpdate, smtpForCampaign);
        outboxForCancel = newOutbox(smtpForCampaign);
        // The fixtures are written before the test reads anything, so a fixture that the
        // schema cannot hold is a failure of the test and not a silent empty result.
        entityManager.flush();
    }

    private AppUserPrincipal createAdministrator(String username) {
        Instant now = Instant.now();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(FIXTURE_LOGIN_HASH);
        user.setEmail(username + "@example.invalid");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        AppRole admin = roles.findByCode("ROLE_ADMIN").orElseThrow();
        user.getRoles().add(admin);
        users.save(user);
        return new AppUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private ClientGroup newGroup(String code) {
        Instant now = Instant.now();
        ClientGroup group = new ClientGroup();
        group.setOwnerUserId(principal.userId());
        group.setCode(code);
        group.setName("Group " + code);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        return clientGroups.save(group);
    }

    private Client newClient(ClientGroup group, String name) {
        Instant now = Instant.now();
        Client client = new Client();
        client.setOwnerUserId(principal.userId());
        client.setGroup(group);
        client.setName(name);
        client.setEmail("client-" + token + "@example.invalid");
        client.setCreatedAt(now);
        client.setUpdatedAt(now);
        return clients.save(client);
    }

    private EmailTemplate newTemplate(String name, TemplateStatus status) {
        Instant now = Instant.now();
        EmailTemplate template = new EmailTemplate();
        template.setOwnerUserId(principal.userId());
        template.setName(name);
        template.setSubject("Subject of " + name);
        template.setContentHtml("<p>Body of " + name + "</p>");
        template.setStatus(status);
        template.setVersion(1);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return templates.save(template);
    }

    private SmtpAccount newSmtpAccount(String email) {
        Instant now = Instant.now();
        SmtpAccount account = new SmtpAccount();
        account.setOwnerUserId(principal.userId());
        account.setEmail(email);
        account.setProtocol(AccountProtocol.SMTP);
        account.setHost("mail.example.invalid");
        account.setPort(587);
        account.setUsername("sender-" + token);
        account.setAuthMethod(AuthMethod.PLAIN);
        account.setEnabled(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return smtpAccounts.save(account);
    }

    private GmailAccount newGmailAccount(String email) {
        Instant now = Instant.now();
        GmailAccount account = new GmailAccount();
        account.setOwnerUserId(principal.userId());
        account.setEmail(email);
        account.setOauthClientId("oauth-client-" + token);
        account.setScopes("gmail.send");
        account.setEnabled(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return gmailAccounts.save(account);
    }

    private Campaign newCampaign(EmailTemplate template, SmtpAccount account) {
        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.setOwnerUserId(principal.userId());
        campaign.setFromAccountId(account.getId());
        campaign.setFromAccountKind(AccountKind.SMTP);
        campaign.setTargetType(TargetType.INDIVIDUAL);
        campaign.setTemplate(template);
        campaign.setRecipientCount(0);
        campaign.setStatus(CampaignStatus.QUEUED);
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        return campaigns.save(campaign);
    }

    private Outbox newOutbox(SmtpAccount account) {
        Instant now = Instant.now();
        Outbox row = new Outbox();
        row.setOwnerUserId(principal.userId());
        row.setFromAccountId(account.getId());
        row.setToEmail("recipient-" + token + "@example.invalid");
        row.setSubject("Scheduled");
        row.setBodyHtml("<p>Body</p>");
        row.setScheduledAt(now.plus(1, ChronoUnit.HOURS));
        row.setStatus(OutboxStatus.SCHEDULED);
        row.setAttemptCount(0);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return outboxes.save(row);
    }

    /** A spreadsheet with one row. */
    private byte[] spreadsheet(String name) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("rows");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Name of " + name);
            row.createCell(1).setCellValue(name + "-" + token + "@example.invalid");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("The fixture spreadsheet could not be written", e);
        }
    }

    // ---------------------------------------------------------------- the endpoints

    /** One mutating endpoint of the application. */
    private record MutatingEndpoint(
            String method,
            String templatePath,
            String description,
            Supplier<MockHttpServletRequestBuilder> request) {}

    /**
     * Every mutating endpoint of the web layer.
     *
     * <p>The list was read out of the controllers in {@code io.github.instaxdutta.winnow.web}. The test
     * {@link #theMutatingEndpointsOfTheApplicationAreTheOnesThisTestCovers()} proves that
     * the list is complete.
     */
    private List<MutatingEndpoint> mutatingEndpoints() {
        return List.of(
                new MutatingEndpoint(
                        "POST",
                        "/api/campaigns",
                        "creating a campaign",
                        () -> post("/api/campaigns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fromAccountKind\":\"SMTP\",\"fromAccountId\":" + smtpForCampaign.getId()
                                        + ",\"templateId\":" + templateForUpdate.getId()
                                        + ",\"targetType\":\"GROUP\",\"groupCode\":\"" + group.getCode() + "\"}")),
                new MutatingEndpoint(
                        "POST",
                        "/api/campaigns/{id}/cancel",
                        "cancelling a campaign",
                        () -> post("/api/campaigns/" + campaignForCancel.getId() + "/cancel")),
                new MutatingEndpoint(
                        "POST",
                        "/api/clients/import",
                        "importing the contacts of a spreadsheet",
                        () -> multipart("/api/clients/import")
                                .file(new MockMultipartFile(
                                        "file",
                                        "clients.xlsx",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        spreadsheet("imported")))
                                .param("groupId", String.valueOf(group.getId()))),
                new MutatingEndpoint(
                        "DELETE",
                        "/api/clients/{id}",
                        "removing a contact",
                        () -> delete("/api/clients/" + client.getId())),
                new MutatingEndpoint(
                        "POST",
                        "/api/groups",
                        "creating a client group",
                        () -> post("/api/groups")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"NEW-" + token + "\",\"name\":\"A new group\"}")),
                new MutatingEndpoint(
                        "PUT",
                        "/api/groups/{id}",
                        "changing a client group",
                        () -> put("/api/groups/" + group.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + group.getCode() + "\",\"name\":\"A renamed group\"}")),
                new MutatingEndpoint(
                        "DELETE",
                        "/api/groups/{id}",
                        "removing a client group",
                        () -> delete("/api/groups/" + group.getId())),
                new MutatingEndpoint(
                        "POST",
                        "/api/extraction",
                        "starting an extraction run",
                        () -> multipart("/api/extraction")
                                .file(new MockMultipartFile(
                                        "file",
                                        "domains.xlsx",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        spreadsheet("domains")))),
                new MutatingEndpoint(
                        "POST",
                        "/api/accounts/gmail",
                        "creating a Google sending account",
                        () -> post("/api/accounts/gmail")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"gmail-new-" + token
                                        + "@example.invalid\",\"oauthClientId\":\"oauth-client\",\"scopes\":\"gmail.send\","
                                        + "\"enabled\":true}")),
                new MutatingEndpoint(
                        "PUT",
                        "/api/accounts/gmail/{id}",
                        "changing a Google sending account",
                        () -> put("/api/accounts/gmail/" + gmailForUpdate.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + gmailForUpdate.getEmail()
                                        + "\",\"oauthClientId\":\"oauth-client-changed\",\"scopes\":\"gmail.send\","
                                        + "\"enabled\":false}")),
                new MutatingEndpoint(
                        "DELETE",
                        "/api/accounts/gmail/{id}",
                        "removing a Google sending account",
                        () -> delete("/api/accounts/gmail/" + gmailForDelete.getId())),
                new MutatingEndpoint(
                        "POST",
                        "/api/outbox",
                        "scheduling a message",
                        () -> post("/api/outbox")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"fromAccountId\":" + smtpForCampaign.getId()
                                        + ",\"toEmail\":\"recipient-" + token
                                        + "@example.invalid\",\"subject\":\"Subject\",\"bodyHtml\":\"<p>Body</p>\","
                                        + "\"scheduledAt\":\"" + Instant.now().plus(1, ChronoUnit.HOURS) + "\"}")),
                new MutatingEndpoint(
                        "POST",
                        "/api/outbox/{id}/cancel",
                        "cancelling a scheduled message",
                        () -> post("/api/outbox/" + outboxForCancel.getId() + "/cancel")),
                new MutatingEndpoint(
                        "POST",
                        "/api/accounts/smtp",
                        "creating an SMTP sending account",
                        () -> post("/api/accounts/smtp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"smtp-new-" + token
                                        + "@example.invalid\",\"protocol\":\"SMTP\",\"host\":\"mail.example.invalid\","
                                        + "\"port\":587,\"username\":\"sender\",\"authMethod\":\"PLAIN\",\"enabled\":true}")),
                new MutatingEndpoint(
                        "PUT",
                        "/api/accounts/smtp/{id}",
                        "changing an SMTP sending account",
                        () -> put("/api/accounts/smtp/" + smtpForUpdate.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"" + smtpForUpdate.getEmail()
                                        + "\",\"protocol\":\"SMTP\",\"host\":\"mail2.example.invalid\","
                                        + "\"port\":587,\"username\":\"sender\",\"authMethod\":\"PLAIN\",\"enabled\":false}")),
                new MutatingEndpoint(
                        "DELETE",
                        "/api/accounts/smtp/{id}",
                        "removing an SMTP sending account",
                        () -> delete("/api/accounts/smtp/" + smtpForDelete.getId())),
                new MutatingEndpoint(
                        "POST",
                        "/api/templates",
                        "creating an email template",
                        () -> post("/api/templates")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"A new template\",\"subject\":\"Subject\","
                                        + "\"contentHtml\":\"<p>Body</p>\"}")),
                new MutatingEndpoint(
                        "PUT",
                        "/api/templates/{id}",
                        "changing an email template",
                        () -> put("/api/templates/" + templateForUpdate.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"A changed template\",\"subject\":\"Subject\","
                                        + "\"contentHtml\":\"<p>Changed body</p>\"}")),
                new MutatingEndpoint(
                        "DELETE",
                        "/api/templates/{id}",
                        "removing an email template",
                        () -> delete("/api/templates/" + templateForDelete.getId())),
                new MutatingEndpoint(
                        "POST",
                        "/api/templates/{id}/submit",
                        "sending an email template for approval",
                        () -> post("/api/templates/" + templateForSubmit.getId() + "/submit")),
                new MutatingEndpoint(
                        "POST",
                        "/api/templates/{id}/approve",
                        "approving an email template",
                        () -> post("/api/templates/" + templateForApprove.getId() + "/approve")),
                new MutatingEndpoint(
                        "POST",
                        "/api/templates/{id}/reject",
                        "rejecting an email template",
                        () -> post("/api/templates/" + templateForReject.getId() + "/reject")),
                new MutatingEndpoint(
                        "POST",
                        "/api/me/password",
                        "changing the password of the caller",
                        () -> post("/api/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\":\"" + FIXTURE_LOGIN
                                        + "\",\"newPassword\":\"a-new-password-12345\"}")));
    }

    // ------------------------------------------------------------------- the tests

    @TestFactory
    Stream<DynamicTest> everyMutatingEndpointRejectsARequestWithoutACsrfToken() {
        return mutatingEndpoints().stream().map(endpoint -> DynamicTest.dynamicTest(
                endpoint.description() + " refuses a request without a CSRF token",
                () -> {
                    MvcResult result = mockMvc.perform(endpoint.request().get().with(user(principal))).andReturn();
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("the status of %s %s without a CSRF token", endpoint.method(), endpoint.templatePath())
                            .isIn(302, 403);
                    assertThat(result.getResponse().getContentAsString())
                            .as("the body of %s %s without a CSRF token", endpoint.method(), endpoint.templatePath())
                            .doesNotContain(SUCCESS);
                }));
    }

    @TestFactory
    Stream<DynamicTest> everyMutatingEndpointAcceptsARequestWithACsrfToken() {
        return mutatingEndpoints().stream().map(endpoint -> DynamicTest.dynamicTest(
                endpoint.description() + " accepts a request with a CSRF token",
                () -> {
                    MvcResult result = mockMvc
                            .perform(endpoint.request().get().with(user(principal)).with(csrf()))
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    assertThat(status)
                            .as("the status of %s %s with a CSRF token", endpoint.method(), endpoint.templatePath())
                            .isEqualTo(200);
                    assertThat(result.getResponse().getContentAsString())
                            .as("the body of %s %s with a CSRF token", endpoint.method(), endpoint.templatePath())
                            .contains(SUCCESS);
                }));
    }

    @Test
    void anAnonymousRequestWithoutACsrfTokenIsSentToTheLoginPage() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ANON-" + token + "\",\"name\":\"Anonymous\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getRedirectedUrl()).contains("/login");
    }

    @Test
    void theMutatingEndpointsOfTheApplicationAreTheOnesThisTestCovers() {
        Set<String> actual = new LinkedHashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods().entrySet()) {
            Set<RequestMethod> methods =
                    entry.getKey().getMethodsCondition().getMethods();
            for (RequestMethod method : methods) {
                if (method == RequestMethod.GET
                        || method == RequestMethod.HEAD
                        || method == RequestMethod.OPTIONS) {
                    continue;
                }
                for (String pattern : entry.getKey().getPatternValues()) {
                    if (!pattern.isEmpty()) {
                        actual.add(method.name() + " " + pattern);
                    }
                }
            }
        }
        Set<String> expected = new LinkedHashSet<>();
        for (MutatingEndpoint endpoint : mutatingEndpoints()) {
            expected.add(endpoint.method() + " " + endpoint.templatePath());
        }

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void noReadEndpointChangesState() {
        List<String> offenders = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods().entrySet()) {
            boolean isRead = entry.getKey().getMethodsCondition().getMethods().contains(
                    RequestMethod.GET);
            if (!isRead) {
                continue;
            }
            String name = entry.getValue().getMethod().getName().toLowerCase(Locale.ROOT);
            if (name.startsWith("create")
                    || name.startsWith("update")
                    || name.startsWith("delete")
                    || name.startsWith("save")
                    || name.startsWith("cancel")
                    || name.startsWith("approve")
                    || name.startsWith("reject")
                    || name.startsWith("submit")
                    || name.startsWith("schedule")
                    || name.startsWith("import")
                    || name.startsWith("changepassword")) {
                offenders.add(entry.getValue().getMethod().getDeclaringClass().getSimpleName()
                        + "."
                        + entry.getValue().getMethod().getName());
            }
        }

        assertThat(offenders).isEmpty();
    }

    private Map<RequestMappingInfo, HandlerMethod> handlerMethods() {
        // The bean is fetched by name: the actuator contributes a second handler mapping
        // of the same type.
        RequestMappingHandlerMapping mapping =
                context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        return mapping.getHandlerMethods();
    }
}

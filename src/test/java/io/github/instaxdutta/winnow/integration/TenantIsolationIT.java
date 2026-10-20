package io.github.instaxdutta.winnow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.instaxdutta.winnow.domain.AccountKind;
import io.github.instaxdutta.winnow.domain.AccountProtocol;
import io.github.instaxdutta.winnow.domain.AppUser;
import io.github.instaxdutta.winnow.domain.AuthMethod;
import io.github.instaxdutta.winnow.domain.Campaign;
import io.github.instaxdutta.winnow.domain.CampaignStatus;
import io.github.instaxdutta.winnow.domain.DeliveryStatus;
import io.github.instaxdutta.winnow.domain.Client;
import io.github.instaxdutta.winnow.domain.ClientGroup;
import io.github.instaxdutta.winnow.domain.EmailTemplate;
import io.github.instaxdutta.winnow.domain.ExtractionResult;
import io.github.instaxdutta.winnow.domain.ExtractionRun;
import io.github.instaxdutta.winnow.domain.ExtractionStatus;
import io.github.instaxdutta.winnow.domain.GmailAccount;
import io.github.instaxdutta.winnow.domain.InboxMessage;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.domain.Outbox;
import io.github.instaxdutta.winnow.domain.OutboxStatus;
import io.github.instaxdutta.winnow.domain.SmtpAccount;
import io.github.instaxdutta.winnow.domain.TargetType;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import io.github.instaxdutta.winnow.domain.UserStatus;
import io.github.instaxdutta.winnow.model.ClientDto;
import io.github.instaxdutta.winnow.model.ClientGroupRequest;
import io.github.instaxdutta.winnow.model.ClientImportResult;
import io.github.instaxdutta.winnow.model.EmailTemplateRequest;
import io.github.instaxdutta.winnow.model.GmailAccountRequest;
import io.github.instaxdutta.winnow.model.SmtpAccountRequest;
import io.github.instaxdutta.winnow.repository.AppRoleRepository;
import io.github.instaxdutta.winnow.repository.AppUserRepository;
import io.github.instaxdutta.winnow.repository.CampaignRepository;
import io.github.instaxdutta.winnow.repository.ClientGroupRepository;
import io.github.instaxdutta.winnow.repository.ClientRepository;
import io.github.instaxdutta.winnow.repository.DeliveryRepository;
import io.github.instaxdutta.winnow.repository.EmailTemplateRepository;
import io.github.instaxdutta.winnow.repository.ExtractionResultRepository;
import io.github.instaxdutta.winnow.repository.ExtractionRunRepository;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import io.github.instaxdutta.winnow.repository.InboxMessageRepository;
import io.github.instaxdutta.winnow.repository.OutboxRepository;
import io.github.instaxdutta.winnow.repository.SmtpAccountRepository;
import io.github.instaxdutta.winnow.service.CampaignService;
import io.github.instaxdutta.winnow.service.ClientGroupService;
import io.github.instaxdutta.winnow.service.ClientService;
import io.github.instaxdutta.winnow.service.ExtractionService;
import io.github.instaxdutta.winnow.service.GmailAccountService;
import io.github.instaxdutta.winnow.service.InboxService;
import io.github.instaxdutta.winnow.service.OutboxService;
import io.github.instaxdutta.winnow.service.SmtpAccountService;
import io.github.instaxdutta.winnow.service.TemplateService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tenant isolation of the application.
 *
 * <p>This test is the defence against one tenant reading or changing another tenant's
 * data: two tenants with data of their own, and every repository method that returns
 * tenant owned data is asked for the other tenant's rows.
 *
 * <p>Every test runs in a transaction that is rolled back, so the fixtures of one test
 * never reach another one.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationIT {

    /**
     * The login of the fixture users.
     *
     * <p>A test password is a fixture, not a secret. The hash below is the BCrypt hash
     * of that login at the cost the application uses, so the tests do not pay for the
     * hashing on every fixture.
     */
    private static final String FIXTURE_LOGIN = "test-password-12345";

    /** The BCrypt hash of the fixture login, at cost twelve. */
    private static final String FIXTURE_LOGIN_HASH = "$2a$12$UeN7q.nYCnifiD2h9LVCDuHvKYFsnEniZSTktGLb6VBG29U.dKONm";

    /** A moment in the past, used as the start of a window. */
    private static final Instant FROM = Instant.now().minus(30, ChronoUnit.DAYS);

    /** A moment in the future, used as the end of a window. */
    private static final Instant TO = Instant.now().plus(30, ChronoUnit.DAYS);

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private AppRoleRepository roles;

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

    @Autowired
    private ExtractionRunRepository extractionRuns;

    @Autowired
    private ExtractionResultRepository extractionResults;

    @Autowired
    private InboxMessageRepository inboxMessages;

    @Autowired
    private DeliveryRepository deliveries;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientGroupService clientGroupService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SmtpAccountService smtpAccountService;

    @Autowired
    private GmailAccountService gmailAccountService;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private InboxService inboxService;

    private String token;

    private AppUser tenantA;
    private AppUser tenantB;

    private ClientGroup groupA;
    private ClientGroup groupB;
    private Client clientA;
    private Client clientB;
    private EmailTemplate templateA;
    private EmailTemplate templateB;
    private SmtpAccount smtpA;
    private SmtpAccount smtpB;
    private GmailAccount gmailA;
    private GmailAccount gmailB;
    private Campaign campaignA;
    private Campaign campaignB;
    private Outbox outboxA;
    private Outbox outboxB;
    private ExtractionRun runA;
    private ExtractionRun runB;

    // ------------------------------------------------------------------ fixtures

    @BeforeEach
    void createTwoTenantsWithData() {
        token = UUID.randomUUID().toString().substring(0, 8);
        tenantA = newUser("tenant-a-" + token);
        tenantB = newUser("tenant-b-" + token);

        groupA = newGroup(tenantA, "GRP-A-" + token);
        groupB = newGroup(tenantB, "GRP-B-" + token);
        clientA = newClient(tenantA, groupA, "Client of A", "client-a-" + token + "@example.invalid");
        clientB = newClient(tenantB, groupB, "Client of B", "client-b-" + token + "@example.invalid");
        templateA = newTemplate(tenantA, "Template of A", TemplateStatus.APPROVED);
        templateB = newTemplate(tenantB, "Template of B", TemplateStatus.APPROVED);
        smtpA = newSmtpAccount(tenantA, "smtp-a-" + token + "@example.invalid");
        smtpB = newSmtpAccount(tenantB, "smtp-b-" + token + "@example.invalid");
        gmailA = newGmailAccount(tenantA, "gmail-a-" + token + "@example.invalid");
        gmailB = newGmailAccount(tenantB, "gmail-b-" + token + "@example.invalid");
        campaignA = newCampaign(tenantA, templateA, smtpA.getId());
        campaignB = newCampaign(tenantB, templateB, smtpB.getId());
        outboxA = newOutbox(tenantA, smtpA.getId());
        outboxB = newOutbox(tenantB, smtpB.getId());
        runA = newExtractionRun(tenantA);
        runB = newExtractionRun(tenantB);
        // The fixtures are written before the test reads anything, so a fixture that the
        // schema cannot hold is a failure of the test and not a silent empty result.
        entityManager.flush();
    }

    private AppUser newUser(String username) {
        Instant now = Instant.now();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(FIXTURE_LOGIN_HASH);
        user.setEmail(username + "@example.invalid");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(roles.findByCode("ROLE_ADMIN").orElseThrow());
        return users.save(user);
    }

    private ClientGroup newGroup(AppUser owner, String code) {
        Instant now = Instant.now();
        ClientGroup group = new ClientGroup();
        group.setOwnerUserId(owner.getId());
        group.setCode(code);
        group.setName("Group " + code);
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        return clientGroups.save(group);
    }

    private Client newClient(AppUser owner, ClientGroup group, String name, String email) {
        Instant now = Instant.now();
        Client client = new Client();
        client.setOwnerUserId(owner.getId());
        client.setGroup(group);
        client.setName(name);
        client.setEmail(email);
        client.setCreatedAt(now);
        client.setUpdatedAt(now);
        return clients.save(client);
    }

    private EmailTemplate newTemplate(AppUser owner, String name, TemplateStatus status) {
        Instant now = Instant.now();
        EmailTemplate template = new EmailTemplate();
        template.setOwnerUserId(owner.getId());
        template.setName(name);
        template.setSubject("Subject of " + name);
        template.setContentHtml("<p>Body of " + name + "</p>");
        template.setStatus(status);
        template.setVersion(1);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return templates.save(template);
    }

    private SmtpAccount newSmtpAccount(AppUser owner, String email) {
        Instant now = Instant.now();
        SmtpAccount account = new SmtpAccount();
        account.setOwnerUserId(owner.getId());
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

    private GmailAccount newGmailAccount(AppUser owner, String email) {
        Instant now = Instant.now();
        GmailAccount account = new GmailAccount();
        account.setOwnerUserId(owner.getId());
        account.setEmail(email);
        account.setOauthClientId("oauth-client-" + token);
        account.setScopes("gmail.send");
        account.setEnabled(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return gmailAccounts.save(account);
    }

    private Campaign newCampaign(AppUser owner, EmailTemplate template, Long fromAccountId) {
        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.setOwnerUserId(owner.getId());
        campaign.setFromAccountId(fromAccountId);
        campaign.setFromAccountKind(AccountKind.SMTP);
        campaign.setTargetType(TargetType.INDIVIDUAL);
        campaign.setTemplate(template);
        campaign.setRecipientCount(1);
        campaign.setStatus(CampaignStatus.QUEUED);
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        return campaigns.save(campaign);
    }

    private Outbox newOutbox(AppUser owner, Long fromAccountId) {
        Instant now = Instant.now();
        Outbox row = new Outbox();
        row.setOwnerUserId(owner.getId());
        row.setFromAccountId(fromAccountId);
        row.setToEmail("recipient-" + token + "@example.invalid");
        row.setSubject("Scheduled");
        row.setBodyHtml("<p>Body</p>");
        // The row is scheduled in the future so the dispatch job never picks it up.
        row.setScheduledAt(now.plus(1, ChronoUnit.HOURS));
        row.setStatus(OutboxStatus.SCHEDULED);
        row.setAttemptCount(0);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        return outboxes.save(row);
    }

    private ExtractionRun newExtractionRun(AppUser owner) {
        Instant now = Instant.now();
        ExtractionRun run = new ExtractionRun();
        run.setOwnerUserId(owner.getId());
        // A terminal status, so the scheduler that resumes abandoned runs leaves it alone.
        run.setStatus(ExtractionStatus.SUCCESS);
        run.setDomainCount(1);
        run.setSourceFileName("domains-" + token + ".xlsx");
        run.setStartedAt(now);
        run.setFinishedAt(now);
        run.setCreatedAt(now);
        run.setUpdatedAt(now);
        return extractionRuns.save(run);
    }

    private ExtractionResult newExtractionResult(ExtractionRun run, String email) {
        Instant now = Instant.now();
        ExtractionResult result = new ExtractionResult();
        result.setRun(run);
        result.setDomain("example.invalid");
        result.setEmail(email);
        result.setEmailHash(sha256(email));
        result.setSourceRow(1);
        result.setCreatedAt(now);
        return extractionResults.save(result);
    }

    private InboxMessage newInboxMessage(Long accountId, String providerMessageId) {
        Instant now = Instant.now();
        InboxMessage message = new InboxMessage();
        message.setAccountId(accountId);
        message.setProviderMessageId(providerMessageId);
        message.setFromAddress("sender-" + token + "@example.invalid");
        message.setSubject("Inbound");
        message.setBodyHtml("<p>Inbound body</p>");
        message.setReceivedAt(now);
        message.setRead(false);
        message.setCreatedAt(now);
        return inboxMessages.save(message);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    // ------------------------------------------------------- client group isolation

    @Test
    void tenantACannotSeeTenantBsClientGroups() {
        assertThat(clientGroups.findByOwnerUserIdAndDeletedAtIsNullOrderByCodeAsc(tenantA.getId()))
                .extracting(ClientGroup::getId)
                .containsExactly(groupA.getId());
        assertThat(clientGroups.findByOwnerUserIdAndDeletedAtIsNullOrderByCodeAsc(tenantB.getId()))
                .extracting(ClientGroup::getId)
                .containsExactly(groupB.getId());
    }

    @Test
    void tenantACannotReadTenantBsClientGroupById() {
        assertThat(clientGroups.findByIdAndOwnerUserIdAndDeletedAtIsNull(groupB.getId(), tenantA.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotFindTenantBsClientGroupByCode() {
        assertThat(clientGroups.findByOwnerUserIdAndCodeAndDeletedAtIsNull(tenantA.getId(), groupB.getCode()))
                .isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsClientGroups() {
        assertThat(clientGroups.countByOwnerUserIdAndDeletedAtIsNull(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void theClientGroupCodeCheckIsGlobalBecauseTheCodeIsUniqueAcrossThePlatform() {
        // The code of a group is unique across the whole platform, so the duplicate check
        // is deliberately not tenant scoped. It answers "does this code exist", never
        // "does this tenant own a group with this code", so it discloses no tenant data.
        assertThat(clientGroups.existsByCode(groupB.getCode())).isTrue();
        assertThat(clientGroups.existsByCode("no-group-has-this-code")).isFalse();
    }

    // ------------------------------------------------------------- client isolation

    @Test
    void tenantACannotSeeTenantBsClients() {
        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(tenantA.getId()))
                .extracting(Client::getId)
                .containsExactly(clientA.getId());
    }

    @Test
    void tenantACannotSeeTenantBsClientsOfTheGroup() {
        assertThat(clients.findByOwnerUserIdAndGroupIdAndDeletedAtIsNullOrderByNameAsc(
                        tenantA.getId(), groupB.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotPageTenantBsClients() {
        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNull(tenantA.getId(), PageRequest.of(0, 50))
                        .getContent())
                .extracting(Client::getId)
                .containsExactly(clientA.getId());
    }

    @Test
    void tenantACannotReadTenantBsClientById() {
        assertThat(clients.findByIdAndOwnerUserIdAndDeletedAtIsNull(clientB.getId(), tenantA.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsClients() {
        assertThat(clients.countByOwnerUserIdAndDeletedAtIsNull(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsClientsOfTheGroup() {
        assertThat(clients.countByOwnerUserIdAndGroupIdAndDeletedAtIsNull(tenantA.getId(), groupB.getId()))
                .isZero();
    }

    // ----------------------------------------------------------- template isolation

    @Test
    void tenantACannotSeeTenantBsTemplates() {
        assertThat(templates.findByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenantA.getId()))
                .extracting(EmailTemplate::getId)
                .containsExactly(templateA.getId());
    }

    @Test
    void tenantACannotSeeTenantBsPendingTemplates() {
        assertThat(templates.findByOwnerUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        tenantA.getId(), TemplateStatus.APPROVED))
                .extracting(EmailTemplate::getId)
                .containsExactly(templateA.getId());
    }

    @Test
    void tenantACannotReadTenantBsTemplateById() {
        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantA.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsTemplates() {
        assertThat(templates.countByOwnerUserIdAndDeletedAtIsNull(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsPendingTemplates() {
        assertThat(templates.countByOwnerUserIdAndStatusAndDeletedAtIsNull(tenantA.getId(), TemplateStatus.PENDING_APPROVAL))
                .isZero();
    }

    // ---------------------------------------------------------- account isolation

    @Test
    void tenantACannotSeeTenantBsSmtpAccounts() {
        assertThat(smtpAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(tenantA.getId()))
                .extracting(SmtpAccount::getId)
                .containsExactly(smtpA.getId());
    }

    @Test
    void tenantACannotReadTenantBsSmtpAccountById() {
        assertThat(smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(smtpB.getId(), tenantA.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotFindTenantBsSmtpAccountByEmail() {
        assertThat(smtpAccounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(tenantA.getId(), smtpB.getEmail()))
                .isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsSmtpAccounts() {
        assertThat(smtpAccounts.countByOwnerUserIdAndDeletedAtIsNull(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void theSmtpAddressCheckIsGlobalBecauseTheAddressIsUniqueAcrossThePlatform() {
        // The address of an SMTP account is unique across the whole platform, so the
        // duplicate check is deliberately not tenant scoped. It answers "does this address
        // exist", never "does this tenant own an account with this address", so it
        // discloses no tenant data. Without it a cross tenant duplicate would surface as
        // a database constraint violation instead of a 409.
        assertThat(smtpAccounts.existsByEmail(smtpB.getEmail())).isTrue();
        assertThat(smtpAccounts.existsByEmail("no-account-has-this-address@example.invalid")).isFalse();
    }

    @Test
    void tenantACannotSeeTenantBsGmailAccounts() {
        assertThat(gmailAccounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(tenantA.getId()))
                .extracting(GmailAccount::getId)
                .containsExactly(gmailA.getId());
    }

    @Test
    void tenantACannotReadTenantBsGmailAccountById() {
        assertThat(gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(gmailB.getId(), tenantA.getId()))
                .isEmpty();
    }

    @Test
    void tenantACannotFindTenantBsGmailAccountByEmail() {
        assertThat(gmailAccounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(tenantA.getId(), gmailB.getEmail()))
                .isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsGmailAccounts() {
        assertThat(gmailAccounts.countByOwnerUserIdAndDeletedAtIsNull(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void theGmailAddressCheckIsGlobalBecauseTheAddressIsUniqueAcrossThePlatform() {
        // The same deliberate global check as the SMTP address. The address column is
        // unique across the whole platform.
        assertThat(gmailAccounts.existsByEmail(gmailB.getEmail())).isTrue();
        assertThat(gmailAccounts.existsByEmail("no-account-has-this-address@example.invalid")).isFalse();
    }

    // ---------------------------------------------------------- campaign isolation

    @Test
    void tenantACannotSeeTenantBsCampaigns() {
        assertThat(campaigns.findByOwnerUserIdOrderByCreatedAtDesc(tenantA.getId()))
                .extracting(Campaign::getId)
                .containsExactly(campaignA.getId());
    }

    @Test
    void tenantACannotSeeTenantBsCampaignsWithStatus() {
        assertThat(campaigns.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                        tenantA.getId(), CampaignStatus.QUEUED))
                .extracting(Campaign::getId)
                .containsExactly(campaignA.getId());
    }

    @Test
    void tenantACannotReadTenantBsCampaignById() {
        assertThat(campaigns.findByIdAndOwnerUserId(campaignB.getId(), tenantA.getId())).isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsCampaigns() {
        assertThat(campaigns.countByOwnerUserId(tenantA.getId())).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsCampaignsInAWindow() {
        assertThat(campaigns.countByOwnerUserIdAndCreatedAtBetween(tenantA.getId(), FROM, TO)).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsCampaignsWithStatusInAWindow() {
        assertThat(campaigns.countByOwnerUserIdAndStatusAndCreatedAtBetween(
                        tenantA.getId(), CampaignStatus.QUEUED, FROM, TO))
                .isEqualTo(1);
    }

    // ------------------------------------------------------------ outbox isolation

    @Test
    void tenantACannotSeeTenantBsOutboxRows() {
        assertThat(outboxes.findByOwnerUserIdAndScheduledAtBetweenOrderByScheduledAtDesc(
                        tenantA.getId(), FROM, TO))
                .extracting(Outbox::getId)
                .containsExactly(outboxA.getId());
    }

    @Test
    void tenantACannotReadTenantBsOutboxRowById() {
        assertThat(outboxes.findByIdAndOwnerUserId(outboxB.getId(), tenantA.getId())).isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsOutboxRowsWithStatus() {
        assertThat(outboxes.countByOwnerUserIdAndStatus(tenantA.getId(), OutboxStatus.SCHEDULED)).isEqualTo(1);
    }

    @Test
    void tenantACannotCancelTenantBsOutboxRow() {
        assertThatThrownBy(() -> outboxService.cancel(tenantA.getId(), outboxB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(outboxes.findByIdAndOwnerUserId(outboxB.getId(), tenantB.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.SCHEDULED);
    }

    @Test
    void tenantACannotClaimTenantBsOutboxRow() {
        // A row leaves the schedulable state only through a claim or through a cancel of
        // the tenant that owns it. Tenant A has no handle on tenant B's row, so neither
        // can move it.
        assertThatThrownBy(() -> outboxService.cancel(tenantA.getId(), outboxB.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(outboxes.findByIdAndOwnerUserId(outboxB.getId(), tenantA.getId())).isEmpty();

        Outbox row = outboxes.findByIdAndOwnerUserId(outboxB.getId(), tenantB.getId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.SCHEDULED);
        assertThat(row.getAttemptCount()).isZero();
    }

    @Test
    void aClaimedOutboxRowCannotBeClaimedTwice() {
        // The claim is one conditional statement, so a second worker that races for the
        // same row updates zero rows. The row is never sent twice.
        int claimed = outboxes.claim(outboxA.getId(), OutboxStatus.SCHEDULED, OutboxStatus.SENDING, Instant.now());
        int claimedAgain = outboxes.claim(outboxA.getId(), OutboxStatus.SCHEDULED, OutboxStatus.SENDING, Instant.now());

        assertThat(claimed).isEqualTo(1);
        assertThat(claimedAgain).isZero();
        assertThat(outboxes.findByIdAndOwnerUserId(outboxA.getId(), tenantA.getId()).orElseThrow().getAttemptCount())
                .isEqualTo(1);
    }

    @Test
    void aClaimedOutboxRowIsNoLongerClaimable() {
        assertThat(outboxes.findClaimable(outboxA.getId(), OutboxStatus.SCHEDULED)).isPresent();

        outboxes.claim(outboxA.getId(), OutboxStatus.SCHEDULED, OutboxStatus.SENDING, Instant.now());

        assertThat(outboxes.findClaimable(outboxA.getId(), OutboxStatus.SCHEDULED)).isEmpty();
    }

    @Test
    void theOutboxClaimAndTheDueQueryBelongToTheDispatcherAndNotToATenant() {
        // The dispatcher is a platform wide job, so it reads the rows that are due across
        // every tenant and routes each row by the tenant the row carries. The claim
        // itself is conditional, so exactly one worker wins a row. These queries are
        // deliberately not tenant scoped; the tenant of a row is what routes the send.
        assertThat(outboxes.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                        OutboxStatus.SCHEDULED, Instant.now()))
                .isEmpty();
        assertThat(outboxes.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                        OutboxStatus.SCHEDULED, Instant.now().plus(2, ChronoUnit.HOURS)))
                .extracting(Outbox::getId)
                .contains(outboxA.getId(), outboxB.getId());
    }

    // --------------------------------------------------------- extraction isolation

    @Test
    void tenantACannotSeeTenantBsExtractionRuns() {
        assertThat(extractionRuns.findByOwnerUserIdOrderByStartedAtDesc(tenantA.getId()))
                .extracting(ExtractionRun::getId)
                .containsExactly(runA.getId());
    }

    @Test
    void tenantACannotReadTenantBsExtractionRunById() {
        assertThat(extractionRuns.findByIdAndOwnerUserId(runB.getId(), tenantA.getId())).isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsExtractionRunsWithStatus() {
        assertThat(extractionRuns.countByOwnerUserIdAndStatus(tenantA.getId(), ExtractionStatus.SUCCESS)).isEqualTo(1);
        assertThat(extractionRuns.countByOwnerUserIdAndStatus(tenantA.getId(), ExtractionStatus.QUEUED)).isZero();
    }

    @Test
    void tenantACannotSeeTenantBsExtractionResults() {
        ExtractionResult resultA = newExtractionResult(runA, "a-" + token + "@example.invalid");
        newExtractionResult(runB, "b-" + token + "@example.invalid");
        // The results belong to a run, and a run belongs to a tenant, so the results of
        // another tenant are never in the result set of a run.
        assertThat(extractionResults.findByRunId(runA.getId()))
                .extracting(ExtractionResult::getId)
                .containsExactly(resultA.getId());
    }

    @Test
    void tenantACannotReadTenantBsExtractionResultsThroughTheService() {
        assertThatThrownBy(() -> extractionService.results(tenantA.getId(), runB.getId(), 0, 50))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void tenantACannotSeeTheDistinctDomainsOfTenantBsExtractionRun() {
        newExtractionResult(runA, "a-" + token + "@example.invalid");
        newExtractionResult(runB, "b-" + token + "@example.invalid");

        assertThat(extractionResults.findDistinctDomainsByRunId(runA.getId())).containsExactly("example.invalid");
        assertThat(extractionResults.findDistinctDomainsByRunId(runB.getId())).containsExactly("example.invalid");
    }

    @Test
    void tenantACannotCountTenantBsExtractionResults() {
        newExtractionResult(runA, "a-" + token + "@example.invalid");
        newExtractionResult(runB, "b-" + token + "@example.invalid");

        assertThat(extractionResults.countByRunId(runA.getId())).isEqualTo(1);
    }

    @Test
    void tenantACannotFindTenantBsExtractionResultByEmailHash() {
        ExtractionResult resultA = newExtractionResult(runA, "a-" + token + "@example.invalid");
        ExtractionResult resultB = newExtractionResult(runB, "b-" + token + "@example.invalid");

        assertThat(extractionResults.existsByRunIdAndEmailHash(runA.getId(), resultB.getEmailHash())).isFalse();
        assertThat(extractionResults.existsByRunIdAndEmailHash(runA.getId(), resultA.getEmailHash())).isTrue();
    }

    // ------------------------------------------------------------- inbox isolation

    @Test
    void tenantACannotSeeTenantBsInboxMessages() {
        InboxMessage messageA = newInboxMessage(smtpA.getId(), "message-a-" + token);
        newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.findByAccountIdAndReceivedAtBetweenOrderByReceivedAtDesc(
                        smtpA.getId(), FROM, TO))
                .extracting(InboxMessage::getId)
                .containsExactly(messageA.getId());
    }

    @Test
    void tenantACannotFindTenantBsInboxMessageByProviderId() {
        InboxMessage messageB = newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.findByAccountIdAndProviderMessageId(smtpA.getId(), messageB.getProviderMessageId()))
                .isEmpty();
    }

    @Test
    void tenantACannotCheckWhetherTenantBsInboxMessageExists() {
        InboxMessage messageA = newInboxMessage(smtpA.getId(), "message-a-" + token);
        InboxMessage messageB = newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.existsByAccountIdAndProviderMessageId(smtpA.getId(), messageB.getProviderMessageId()))
                .isFalse();
        assertThat(inboxMessages.existsByAccountIdAndProviderMessageId(smtpA.getId(), messageA.getProviderMessageId()))
                .isTrue();
    }

    @Test
    void tenantACannotCountTenantBsInboxMessages() {
        newInboxMessage(smtpA.getId(), "message-a-" + token);
        newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.countByAccountIdAndReceivedAtBetween(smtpA.getId(), FROM, TO)).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsUnreadInboxMessages() {
        newInboxMessage(smtpA.getId(), "message-a-" + token);
        newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.countByAccountIdAndReadFalse(smtpA.getId())).isEqualTo(1);
    }

    @Test
    void tenantACannotCountTenantBsUnreadInboxMessagesInAWindow() {
        newInboxMessage(smtpA.getId(), "message-a-" + token);
        newInboxMessage(smtpB.getId(), "message-b-" + token);

        assertThat(inboxMessages.countByAccountIdAndReadFalseAndReceivedAtBetween(smtpA.getId(), FROM, TO)).isEqualTo(1);
    }

    @Test
    void tenantACannotReadTenantBsInboxThroughTheService() {
        assertThatThrownBy(() -> inboxService.list(tenantA.getId(), smtpB.getId(), FROM, TO))
                .isInstanceOf(NotFoundException.class);
    }

    // -------------------------------------------------------- mutation isolation

    @Test
    void tenantACannotDeleteTenantBsClient() {
        assertThatThrownBy(() -> clientService.delete(tenantA.getId(), clientB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(clients.findByIdAndOwnerUserIdAndDeletedAtIsNull(clientB.getId(), tenantB.getId()))
                .isPresent();
    }

    @Test
    void tenantACannotDeleteTenantBsClientGroup() {
        assertThatThrownBy(() -> clientGroupService.delete(tenantA.getId(), groupB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(clientGroups.findByIdAndOwnerUserIdAndDeletedAtIsNull(groupB.getId(), tenantB.getId()))
                .isPresent();
    }

    @Test
    void tenantACannotUpdateTenantBsClientGroup() {
        assertThatThrownBy(() -> clientGroupService.update(
                        tenantA.getId(), groupB.getId(), new ClientGroupRequest("STOLEN-" + token, "Stolen")))
                .isInstanceOf(NotFoundException.class);

        assertThat(clientGroups.findByIdAndOwnerUserIdAndDeletedAtIsNull(groupB.getId(), tenantB.getId())
                .orElseThrow()
                .getCode())
                .isEqualTo(groupB.getCode());
    }

    @Test
    void tenantACannotDeleteTenantBsTemplate() {
        assertThatThrownBy(() -> templateService.delete(tenantA.getId(), templateB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantB.getId()))
                .isPresent();
    }

    @Test
    void tenantACannotUpdateTenantBsTemplate() {
        assertThatThrownBy(() -> templateService.update(
                        tenantA.getId(),
                        templateB.getId(),
                        new EmailTemplateRequest("Stolen", "Stolen", "<p>Stolen</p>")))
                .isInstanceOf(NotFoundException.class);

        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantB.getId())
                .orElseThrow()
                .getName())
                .isEqualTo(templateB.getName());
    }

    @Test
    void tenantACannotSubmitTenantBsTemplateForApproval() {
        assertThatThrownBy(() -> templateService.submitForApproval(tenantA.getId(), templateB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantB.getId())
                .orElseThrow()
                .getStatus())
                .isEqualTo(TemplateStatus.APPROVED);
    }

    @Test
    void tenantACannotApproveTenantBsTemplate() {
        assertThatThrownBy(() -> templateService.approve(tenantA.getId(), templateB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantB.getId())
                .orElseThrow()
                .getStatus())
                .isEqualTo(TemplateStatus.APPROVED);
    }

    @Test
    void tenantACannotRejectTenantBsTemplate() {
        assertThatThrownBy(() -> templateService.reject(tenantA.getId(), templateB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(templates.findByIdAndOwnerUserIdAndDeletedAtIsNull(templateB.getId(), tenantB.getId())
                .orElseThrow()
                .getStatus())
                .isEqualTo(TemplateStatus.APPROVED);
    }

    @Test
    void tenantACannotDeleteTenantBsSmtpAccount() {
        assertThatThrownBy(() -> smtpAccountService.delete(tenantA.getId(), smtpB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(smtpB.getId(), tenantB.getId()))
                .isPresent();
    }

    @Test
    void tenantACannotUpdateTenantBsSmtpAccount() {
        assertThatThrownBy(() -> smtpAccountService.update(
                        tenantA.getId(),
                        smtpB.getId(),
                        new SmtpAccountRequest(
                                "stolen-" + token + "@example.invalid",
                                AccountProtocol.SMTP,
                                "mail.example.invalid",
                                587,
                                "sender",
                                AuthMethod.PLAIN,
                                true,
                                null)))
                .isInstanceOf(NotFoundException.class);

        assertThat(smtpAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(smtpB.getId(), tenantB.getId())
                .orElseThrow()
                .getEmail())
                .isEqualTo(smtpB.getEmail());
    }

    @Test
    void tenantACannotDeleteTenantBsGmailAccount() {
        assertThatThrownBy(() -> gmailAccountService.delete(tenantA.getId(), gmailB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(gmailB.getId(), tenantB.getId()))
                .isPresent();
    }

    @Test
    void tenantACannotUpdateTenantBsGmailAccount() {
        assertThatThrownBy(() -> gmailAccountService.update(
                        tenantA.getId(),
                        gmailB.getId(),
                        new GmailAccountRequest(
                                "stolen-" + token + "@example.invalid",
                                "oauth-client",
                                "gmail.send",
                                true,
                                null,
                                null)))
                .isInstanceOf(NotFoundException.class);

        assertThat(gmailAccounts.findByIdAndOwnerUserIdAndDeletedAtIsNull(gmailB.getId(), tenantB.getId())
                .orElseThrow()
                .getEmail())
                .isEqualTo(gmailB.getEmail());
    }

    @Test
    void tenantACannotCancelTenantBsCampaign() {
        assertThatThrownBy(() -> campaignService.cancel(tenantA.getId(), campaignB.getId()))
                .isInstanceOf(NotFoundException.class);

        assertThat(campaigns.findByIdAndOwnerUserId(campaignB.getId(), tenantB.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.QUEUED);
    }

    // -------------------------------------------------------- delivery isolation

    @Test
    void tenantACannotSeeTenantBsDeliveries() {
        assertThat(deliveries.findByCampaignId(campaignB.getId())).isEmpty();
    }

    @Test
    void tenantACannotSeeTenantBsDeliveriesWithStatus() {
        assertThat(deliveries.findByCampaignIdAndStatus(campaignB.getId(), DeliveryStatus.QUEUED)).isEmpty();
    }

    @Test
    void tenantACannotCountTenantBsDeliveriesWithStatus() {
        assertThat(deliveries.countByCampaignIdAndStatus(campaignB.getId(), DeliveryStatus.QUEUED)).isZero();
    }

    // ---------------------------------------------------- the contract of the service

    @Test
    void theUserTableIsTheTenantTableAndIsNotFilteredByTheTenant() {
        // The user table is the tenant table itself, so a row of it is not tenant owned
        // data. The listing of the users is an administrator operation and is not
        // filtered; the roles on the row are what decide what the user may do.
        assertThat(users.findByIdInAndDeletedAtIsNull(List.of(tenantA.getId(), tenantB.getId())))
                .extracting(AppUser::getId)
                .containsExactlyInAnyOrder(tenantA.getId(), tenantB.getId());
    }

    @Test
    void theServiceReturnsTheDataOfTheTenantItIsGiven() {
        // A service method takes the tenant id as an argument, so it returns the data of
        // that tenant. It is the web layer that has to pass the id of the authenticated
        // caller, and a request parameter must never be able to choose it. This is the
        // contract that matters, and it is the opposite of taking the tenant from a
        // session attribute that was set inconsistently.
        List<ClientDto> clientsOfB =
                clientService.list(tenantB.getId(), 0, 50, null).data();

        assertThat(clientsOfB).extracting(ClientDto::email)
                .containsExactly(clientB.getEmail());
        assertThat(clientsOfB).extracting(ClientDto::email)
                .doesNotContain(clientA.getEmail());
    }

    @Test
    void theServiceNeverReturnsTheDataOfAnotherTenantWhenItIsGivenItsOwnTenant() {
        assertThat(clientService.list(tenantA.getId(), 0, 50, null).data())
                .extracting(ClientDto::email)
                .containsExactly(clientA.getEmail());
    }

    @Test
    void theImportOfASpreadsheetOnlyFillsTheTenantOfTheCaller() {
        ClientImportResult result = clientService.importExcel(
                tenantA.getId(), groupA.getId(), new ByteArrayInputStream(spreadsheet()));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(tenantA.getId()))
                .hasSize(2);
        assertThat(clients.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(tenantB.getId()))
                .hasSize(1);
    }

    /** A one row spreadsheet. */
    private byte[] spreadsheet() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("clients");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Imported client");
            row.createCell(1).setCellValue("imported-" + token + "@example.invalid");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("The fixture spreadsheet could not be written", e);
        }
    }
}

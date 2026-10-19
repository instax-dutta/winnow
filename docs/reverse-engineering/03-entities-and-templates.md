<task id="ses_eaab33276ffero1Beww9GrlXu8" state="completed">
<task_result>
# SEO Email Sender — Reverse-Engineering Report

**Sources:** `seo-0.1.0/reference/decompiled/com/pg/seo/{entity,model,repository,login,controller,service,cfg}` and `/tmp/seo-re/work/extracted/BOOT-INF/classes/{templates,static}`.

**Global schema inference (applies to all of A1):** No entity in the codebase contains a single `@Column`, `@JoinColumn`-free FK, `@Basic`, `@Enumerated`, or `@Lob` annotation. Every column name and type is therefore derived by Hibernate's implicit naming strategy from the Java field name. Three facts pin the physical schema:
1. Native SQL in repositories uses camelCase columns (`c.groupCode`, `c.loginId`, `d.startDate`) → **physical columns are camelCase, not snake_case** (MySQL column names are case-insensitive but `_` is significant, so `groupCode` ≠ `group_code`).
2. `@Where(clause="isActive =1")` resolves to a physical `isActive` column.
3. `login_role_security` is declared with explicit `@JoinColumn(name="LoginId")` / `("RoleId")` — PascalCase, matching the `Login`/`Role` entity property names, so the physical-naming strategy is the **non-snake-casing (standard/identity) one**.

Also: `hibernate.hbm2ddl.auto=validate` + `MySQLDialect` (`cfg/JpaConfigurationMySql.java`) → **the schema was hand-created and is never generated or migrated**. There is no Flyway/Liquibase, so schema drift is silent until boot-time validation fails.

---

# PART A — Backend Domain Model

## A1. Entities by functional area

### Area 1 — Clients (groups + client master)

#### `ClientGroup` → table `client_group`
| Aspect | Value |
|---|---|
| `@Entity` | yes |
| `@Table(name=...)` | `client_group` |
| `@Id` | `groupId` — `Long`, `@GeneratedValue(strategy=GenerationType.IDENTITY)` |
| Class-level filter | `@Where(clause="isActive =1")` (Hibernate) — **global soft-delete filter** |

Fields (all `private`):
| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `groupId` | `Long` | `groupId` | `BIGINT` PK AI | IDENTITY |
| `groupCode` | `String` | `groupCode` | `VARCHAR(255)` | **Referenced as a FK target by `ClientMaster`**; no unique constraint |
| `groupDesc` | `String` | `groupDesc` | `VARCHAR(255)` | |
| `loginId` | `Long` | `loginId` | `BIGINT` | No FK to `login` |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | primitive → non-nullable |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | holds a username, not a FK |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | **date as string** |
| `modifiedBy` | `String` | `modifiedBy` | `VARCHAR(255)` | |
| `modifiedDate` | `String` | `modifiedDate` | `VARCHAR(255)` | **date as string** |
| `clientMaster` | `List<ClientMaster>` | — | — | `@JsonIgnoreProperties` + `@OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.LAZY, mappedBy="clientGroup")` |

Relationships: one `ClientGroup` → many `ClientMaster`, `CascadeType.ALL` (⚠️ see A5), `LAZY`, owning side is `ClientMaster.clientGroup`.

Getters: `getGroupId, getGroupCode, getGroupDesc, getLoginId, getIsActive, getCreatedBy, getCreatedDate, getModifiedBy, getModifiedDate, getClientMaster`
Setters: `setGroupId, setGroupCode, setGroupDesc, setLoginId, setIsActive, setCreatedBy, setCreatedDate, setModifiedBy, setModifiedDate, setClientMaster`
Also: `equals/hashCode/toString/canEqual` (Lombok-generated) — `equals` compares **every** field including the child collection.

#### `ClientMaster` → table `client_master`
| Aspect | Value |
|---|---|
| `@Entity` | yes |
| `@Table(name=...)` | `client_master` |
| `@Id` | `clientMasterId` — `Long`, IDENTITY |
| Class-level filter | `@Where(clause="isActive =1")` |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `clientMasterId` | `Long` | `clientMasterId` | `BIGINT` PK AI | |
| `name` | `String` | `name` | `VARCHAR(255)` | |
| `email` | `String` | `email` | `VARCHAR(255)` | PII, no unique index |
| `webLink` | `String` | `webLink` | `VARCHAR(255)` | |
| `loginId` | `Long` | `loginId` | `BIGINT` | no FK |
| `clientGroup` | `ClientGroup` | `groupCode` | `VARCHAR(255)` FK | `@ManyToOne(cascade={CascadeType.ALL}, fetch=FetchType.LAZY)` + `@JoinColumn(name="groupCode", referencedColumnName="groupCode")` — **FK to a non-PK, non-unique column** |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | |
| `modifiedBy` | `String` | `modifiedBy` | `VARCHAR(255)` | |
| `modifiedDate` | `String` | `modifiedDate` | `VARCHAR(255)` | |

Getters: `getClientMasterId, getName, getEmail, getWebLink, getLoginId, getClientGroup, getIsActive, getCreatedBy, getCreatedDate, getModifiedBy, getModifiedDate`
Setters: `setClientMasterId, setName, setEmail, setWebLink, setLoginId, setClientGroup, setIsActive, setCreatedBy, setCreatedDate, setModifiedBy, setModifiedDate`

### Area 2 — Templates

#### `EmailTemplate` → table `email_template`
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_template` |
| `@Id` | `emailTemplateId` — `Long`, IDENTITY |
| Class-level filter | `@Where(clause="isActive =1")` |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailTemplateId` | `Long` | `emailTemplateId` | `BIGINT` PK AI | |
| `templateName` | `String` | `templateName` | `VARCHAR(255)` | |
| `templateContent` | `String` | `templateContent` | `VARCHAR(255)` ⚠️ | **Stores full CKEditor HTML — 255 chars is insufficient; no `@Lob`/`@Column(length=…)`** |
| `status` | `String` | `status` | `VARCHAR(255)` | free text (`PENDING`/`APPROVED`) — no enum, no check |
| `loginId` | `Long` | `loginId` | `BIGINT` | no FK |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | |
| `createdBy/createdDate/modifiedBy/modifiedDate` | `String` | same | `VARCHAR(255)` | dates as strings |

Getters: `getEmailTemplateId, getTemplateName, getTemplateContent, getStatus, getLoginId, getIsActive, getCreatedBy, getCreatedDate, getModifiedBy, getModifiedDate`
Setters: mirror, `setEmailTemplateId … setModifiedDate`

### Area 3 — Sending (accounts, queue, jobs, status)

#### `EmailMaster` → table `email_master` (SMTP accounts)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_master` |
| `@Id` | `emailMasterId` — `Long`, IDENTITY |
| Class-level filter | `@Where(clause="isActive =1")` |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailMasterId` | `Long` | `emailMasterId` | `BIGINT` PK AI | |
| `email` | `String` | `email` | `VARCHAR(255)` | no unique constraint |
| `protocol` | `String` | `protocol` | `VARCHAR(255)` | only `"smtp"` used |
| `host` | `String` | `host` | `VARCHAR(255)` | `smtp.gmail.com` / `smtp-mail.outlook.com` / `smtp.hostinger.com` |
| `username` | `String` | `username` | `VARCHAR(255)` | service always sets it = `email` (form never sends it) |
| `port` | `Integer` | `port` | `INT` | only `587` offered in UI |
| `auth` | `String` | `auth` | `VARCHAR(255)` | string `"true"`/`"false"` |
| `enable` | `String` | `enable` | `VARCHAR(255)` | string `"true"`/`"false"` |
| `password` | `String` | `password` | `VARCHAR(255)` | 🔴 **SMTP password, plaintext, no encryption** |
| `loginId` | `Long` | `loginId` | `BIGINT` | |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | |
| `createdBy/createdDate/modifiedBy/modifiedDate` | `String` | same | `VARCHAR(255)` | |

Getters: `getEmailMasterId, getEmail, getProtocol, getHost, getUsername, getPort, getAuth, getEnable, getPassword, getLoginId, getIsActive, getCreatedBy, getCreatedDate, getModifiedBy, getModifiedDate`
Setters: mirror.

#### `GmailMaster` → table `gmail_master` (Google OAuth accounts)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `gmail_master` |
| `@Id` | `gmailMasterId` — `Long`, IDENTITY |
| Class-level filter | `@Where(clause="isActive =1")` |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `gmailMasterId` | `Long` | `gmailMasterId` | `BIGINT` PK AI | |
| `email` | `String` | `email` | `VARCHAR(255)` | Gmail mailbox, no unique constraint |
| `clientId` | `String` | `clientId` | `VARCHAR(255)` | 🔴 OAuth **client ID** (semi-public) |
| `secret` | `String` | `secret` | `VARCHAR(255)` | 🔴 **OAuth client secret, plaintext** |
| `refreshToken` | `String` | `refreshToken` | `VARCHAR(255)` | 🔴 **Long-lived OAuth refresh token, plaintext** — grants permanent mailbox read/send |
| `accessToken` | `String` | `accessToken` | `VARCHAR(255)` | 🔴 **Bearer token, plaintext** |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | |
| `createdBy/createdDate/modifiedBy/modifiedDate` | `String` | same | `VARCHAR(255)` | |

Getters: `getGmailMasterId, getEmail, getClientId, getSecret, getRefreshToken, getAccessToken, getIsActive, getCreatedBy, getCreatedDate, getModifiedBy, getModifiedDate`
Setters: mirror.
Consumed by `GmailService`: `new GoogleCredential.Builder().setClientSecrets(clientId, secret).setRefreshToken(refreshToken)` → the row is the credential store.

#### `EmailQueue` → table `email_queue` (Gmail scheduled sends)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_queue` |
| `@Id` | `emailQueueId` — `Long`, IDENTITY |
| Filters | **none** (no `@Where`, no `isActive`) |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailQueueId` | `Long` | `emailQueueId` | `BIGINT` PK AI | |
| `fromEmail` | `String` | `fromEmail` | `VARCHAR(255)` | no FK to `gmail_master` |
| `toEmail` | `String` | `toEmail` | `VARCHAR(255)` | |
| `draftId` | `String` | `draftId` | `VARCHAR(255)` | references a Gmail draft (external id) |
| `domain` | `String` | `domain` | `VARCHAR(255)` | reused as the "Message" column in the UI |
| `scheduledTime` | `LocalDateTime` | `scheduledTime` | `DATETIME(6)` | ✅ the only proper timestamp in the schema |
| `status` | `String` | `status` | `VARCHAR(255)` | free text |
| `errorMessage` | `String` | `errorMessage` | `VARCHAR(255)` ⚠️ | SMTP error text can exceed 255 → truncation |

**No audit columns at all** (no `createdBy/createdDate/modifiedBy/modifiedDate`).

Getters: `getEmailQueueId, getFromEmail, getToEmail, getDraftId, getDomain, getScheduledTime, getStatus, getErrorMessage`
Setters: mirror.

#### `EmailSendJob` → table `email_send_job`
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_send_job` |
| `@Id` | `emailSendJobId` — `Long`, IDENTITY |
| Filters | none |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailSendJobId` | `Long` | `emailSendJobId` | `BIGINT` PK AI | |
| `fromEmail` | `String` | `fromEmail` | `VARCHAR(255)` | |
| `type` | `String` | `type` | `VARCHAR(255)` | `G` (group) / `I` (individual) |
| `date` | `java.sql.Date` | `date` | `DATE` | ⚠️ **`DATE` type — time-of-day lost**; `date` is also a reserved-ish word |
| `status` | `String` | `status` | `VARCHAR(255)` | |
| `noOfEmail` | `Integer` | `noOfEmail` | `INT` | count |
| `noOfSuccess` | `Integer` | `noOfSuccess` | `INT` | count; `noOfEmail - noOfSuccess` = failures |
| `loginId` | `Long` | `loginId` | `BIGINT` | no FK |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | date as string |

**No `modifiedBy`/`modifiedDate`, no `isActive`.**

Getters: `getEmailSendJobId, getFromEmail, getType, getDate, getStatus, getNoOfEmail, getNoOfSuccess, getLoginId, getCreatedBy, getCreatedDate`
Setters: mirror.

#### `EmailSendStatus` → table `email_send_status`
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_send_status` |
| `@Id` | `emailSendStatusId` — `Long`, IDENTITY |
| Filters | none |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailSendStatusId` | `Long` | `emailSendStatusId` | `BIGINT` PK AI | |
| `email` | `String` | `email` | `VARCHAR(255)` | recipient |
| `date` | `java.sql.Date` | `date` | `DATE` | ⚠️ time lost |
| `emailTemplateId` | `Long` | `emailTemplateId` | `BIGINT` | bare Long, **no FK constraint** |
| `status` | `String` | `status` | `VARCHAR(255)` | |
| `loginId` | `Long` | `loginId` | `BIGINT` | no FK |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | date as string |

**No `modifiedBy`/`modifiedDate`, no `isActive`.** Repository has **zero** custom methods (write-only table).

Getters: `getEmailSendStatusId, getEmail, getDate, getEmailTemplateId, getStatus, getLoginId, getCreatedBy, getCreatedDate`
Setters: mirror.

### Area 4 — Extraction / scraping / inbox

#### `EmailExtractor` → table `email_extractor` (an extraction *run*)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_extractor` |
| `@Id` | `emailExtractorId` — `Long`, IDENTITY |
| Filters | none |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailExtractorId` | `Long` | `emailExtractorId` | `BIGINT` PK AI | |
| `noOfDomain` | `Long` | `noOfDomain` | `BIGINT` | count of domains found (misnomer) |
| `startDate` | `java.sql.Date` | `startDate` | `DATE` | ⚠️ time lost; used for month bucketing |
| `status` | `String` | `status` | `VARCHAR(255)` | `SUCCESS` / `PENDING` / failure |
| `loginId` | `Long` | `loginId` | `BIGINT` | no FK |
| `fileName` | `String` | `fileName` | `VARCHAR(255)` | uploaded source file name |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | date as string |

**No `modifiedBy`/`modifiedDate`, no `isActive`.**

Getters: `getEmailExtractorId, getNoOfDomain, getStartDate, getStatus, getLoginId, getFileName, getCreatedBy, getCreatedDate`
Setters: mirror.

#### `EmailExtractorData` → table `email_extractor_data` (per-row results)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_extractor_data` |
| `@Id` | `emailExtractorDataId` — `Long`, IDENTITY |
| Filters | none |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailExtractorDataId` | `Long` | `emailExtractorDataId` | `BIGINT` PK AI | |
| `emailExtractorId` | `Long` | `emailExtractorId` | `BIGINT` | bare Long → **no FK**, no `@ManyToOne`, no index ⇒ orphan rows |
| `startDate` | `java.sql.Date` | `startDate` | `DATE` | ⚠️ time lost |
| `domain` | `String` | `domain` | `VARCHAR(255)` | |
| `email` | `String` | `email` | `VARCHAR(255)` | extracted address; no unique constraint |
| `createdBy` | `String` | `createdBy` | `VARCHAR(255)` | |
| `createdDate` | `String` | `createdDate` | `VARCHAR(255)` | date as string |

**No `modifiedBy`/`modifiedDate`, no `isActive`.** This is the highest-volume table (one row per scraped email) with the fewest constraints.

Getters: `getEmailExtractorDataId, getEmailExtractorId, getStartDate, getDomain, getEmail, getCreatedBy, getCreatedDate`
Setters: mirror.

#### `EmailInbox` → table `email_inbox`
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `email_inbox` |
| `@Id` | `emailInboxId` — `Long`, IDENTITY |
| Class-level filter | `@Where(clause="isActive =1")` |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `emailInboxId` | `Long` | `emailInboxId` | `BIGINT` PK AI | |
| `emailId` | `String` | `emailId` | `VARCHAR(255)` | external message id, unindexed |
| `from` | `String` | `from` | `VARCHAR(255)` | ⚠️ `from` is a SQL reserved word (quoted) |
| `subject` | `String` | `subject` | `VARCHAR(255)` | |
| `content` | `String` | `content` | `VARCHAR(255)` ⚠️ | **full email body** — 255 chars, no `@Lob` |
| `date` | `java.sql.Date` | `date` | `DATE` | ⚠️ time lost |
| `isActive` | `int` | `isActive` | `INT` NOT NULL | |

**No `loginId`** (inbox is global across users), **no audit columns at all**.

Getters: `getEmailInboxId, getEmailId, getFrom, getSubject, getContent, getDate, getIsActive`
Setters: mirror.

### Area 5 — Users

#### `Login` → table `login` (package `com.pg.seo.login`)
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `login` |
| `@Id` | `LoginId` — `Long`, `@GeneratedValue(strategy=GenerationType.AUTO)` — **annotation is on the getter** (property access) |
| Filters | none |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `LoginId` | `Long` | `LoginId` | `BIGINT` PK | AUTO (sequence/table) |
| `Username` | `String` | `Username` | `VARCHAR(255)` | no unique constraint |
| `Password` | `String` | `Password` | `VARCHAR(255)` | BCrypt hash (`BCryptPasswordEncoder` in `SecurityConfiguration` + `WebMvcConfigruation` + `LoginServiceImpl`) — ⚠️ default BCrypt length is 60, fits, but no `varchar(100)` sizing |
| `EmpId` | `String` | `EmpId` | `VARCHAR(255)` | |
| `IsActive` | `int` | `IsActive` | `INT` NOT NULL | |
| `CreatedBy` | `String` | `CreatedBy` | `VARCHAR(255)` | |
| `CreatedDate` | `String` | `CreatedDate` | `VARCHAR(255)` | date as string |
| `ModifiedBy` | `String` | `ModifiedBy` | `VARCHAR(255)` | |
| `ModifiedDate` | `String` | `ModifiedDate` | `VARCHAR(255)` | date as string |
| `roles` | `Set<Role>` | — | — | `@ManyToMany` + `@JoinTable(name="login_role_security", joinColumns={@JoinColumn(name="LoginId")}, inverseJoinColumns={@JoinColumn(name="RoleId")})` — **no cascade, no fetch specified (default LAZY), no ordering** |

Getters: `getLoginId, getUsername, getPassword, getEmpId, getRoles, getIsActive, getCreatedDate, getCreatedBy, getModifiedBy, getModifiedDate`
Setters: `setLoginId, setUsername, setPassword, setEmpId, setRoles, setIsActive, setCreatedDate, setCreatedBy, setModifiedBy, setModifiedDate`

#### `Role` → table `role_security`
| Aspect | Value |
|---|---|
| `@Entity` / `@Table` | yes / `role_security` |
| `@Id` | `RoleId` — `Long`, `@GeneratedValue(AUTO)` on getter |

| Field | Java type | Inferred column | Inferred SQL type | Notes |
|---|---|---|---|---|
| `RoleId` | `Long` | `RoleId` | `BIGINT` PK | |
| `RoleName` | `String` | `RoleName` | `VARCHAR(255)` | ⚠️ no unique constraint; observed value `"MANG"` (`sec:authorize="hasAnyRole('MANG')"`) |
| `Login` | `Set<Login>` | — | — | `@ManyToMany(mappedBy="roles")` (inverse) |

**No `isActive`, no audit columns.** Roles are hardcoded into the UI string `'MANG'`.

#### Join table `login_role_security`
| Column | Type | Notes |
|---|---|---|
| `LoginId` | `BIGINT` | FK → `login.LoginId` |
| `RoleId` | `BIGINT` | FK → `role_security.RoleId` |

No PK, no index, no timestamp.

---

## A2. Repositories

All are Spring Data JPA `JpaRepository<T, Long>`.

| # | Interface | Extends | Custom methods | `@Query` text |
|---|---|---|---|---|
| 1 | `ClientGroupRepository` | `JpaRepository<ClientGroup, Long>` | `findAll()` · `findByLoginId(Long)` · `findByLoginIdAndGroupCode(Long, String)` · `findByGroupCode(String)` | — |
| 2 | `ClientMasterRepository` | `JpaRepository<ClientMaster, Long>` | `findByLoginId(Long)` · `findByGroupCodeAndLoginId(String, Long)` · `findByGroupCode(String)` · `findByClientMasterId(Long)` · `findAllByLoginId(Long, Pageable)` | `findByGroupCodeAndLoginId`: `select * from client_master c where c.groupCode=:groupCode and c.loginId=:loginId and c.isactive=1` (native) <br> `findByGroupCode`: `select * from client_master c where c.groupCode=:groupCode and c.isactive=1` (native) |
| 3 | `EmailExtractorRepository` | `JpaRepository<EmailExtractor, Long>` | `findByLoginId(Long)` · `findByLoginIdMonth(Long, Date, Date)` · `findByEmailExtractorIdAndLoginId(Long, Long)` · `findByEmailExtractorId(Long)` | `findByLoginIdMonth`: `select * FROM email_extractor d WHERE d.startDate between :startDate and :endDate  and loginId =:loginId` (native) |
| 4 | `EmailExtractorDataRepository` | `JpaRepository<EmailExtractorData, Long>` | `findByEmailExtractorId(Long)` · `findByStartDateBetween(Date, Date)` | — |
| 5 | `EmailInboxRepository` | `JpaRepository<EmailInbox, Long>` | `findByEmailId(String)` · `deleteByEmailIdAndDate(String, Date)` | — |
| 6 | `EmailMasterRepository` | `JpaRepository<EmailMaster, Long>` | `findByEmail(String)` · `findByLoginId(Long)` · `findByEmailMasterId(Long)` | — |
| 7 | `EmailQueueRepository` | `JpaRepository<EmailQueue, Long>` (**no `@Repository` annotation**) | `findByStatusAndScheduledTimeBefore(String, LocalDateTime)` · `findByFromEmailAndScheduledTimeBetweenOrderByScheduledTimeDesc(String, LocalDateTime, LocalDateTime)` | — |
| 8 | `EmailSendJobRepository` | `JpaRepository<EmailSendJob, Long>` | `findByDateAndLoginId(Date, Long)` · `findByLoginIdMonth(Long, Date, Date)` | `findByLoginIdMonth`: `select * FROM email_send_job d WHERE d.date between :startDate and :endDate  and loginId =:loginId` (native) |
| 9 | `EmailSendStatusRepository` | `JpaRepository<EmailSendStatus, Long>` | **none** (marker only) | — |
| 10 | `EmailTemplateRepository` | `JpaRepository<EmailTemplate, Long>` | `findByLoginId(Long)` · `findByEmailTemplateId(Long)` | — |
| 11 | `GmailMasterRepository` | `JpaRepository<GmailMaster, Long>` | `findByEmail(String)` | — |
| 12 | `LoginRepository` (in `com.pg.seo.login`) | `JpaRepository<Login, Long>` | `findByUsername(String)` | — |
| 13 | `RoleRepository` (in `com.pg.seo.login`) | `JpaRepository<Role, Long>` | **none** | — |

Notes:
- Only **4** `@Query` methods in the whole app, all `nativeQuery=true` and all `select *` (no projections, no pagination in SQL).
- Native queries are inconsistent about `isActive` (added manually in `ClientMasterRepository`, absent elsewhere) — a symptom of the `@Where` filter not applying to native SQL.
- `EmailSendStatusRepository` is write-only — the `email_send_status` table is never queried by any code path.

---

## A3. DTOs / models

| Class | Fields (type) | Shape | Duplicates an entity? |
|---|---|---|---|
| `BaseResponse` | `statusCode:String`, `statusMessage:String`, `statusDesc:String` | API envelope. **`statusMessage` is overloaded** — it holds `"SUCCESS"`/`"FAILURE"` on some endpoints and the human message on others (`emailClientUpload` returns `statusMessage` as the error text) | Distinct |
| `ClientGroupDto` | `groupId:Long`, `groupCode:String`, `groupDesc:String` | Read projection | **Partial subset** of `ClientGroup` (drops `loginId`, `isActive`, all audit fields) |
| `ClientMasterDto` | `clientMasterId:Long`, `name:String`, `email:String`, `webLink:String`, `groupCode:String` | Read projection **and** request body for `/saveEmail` (misuse) | **Flattened** variant of `ClientMaster` — replaces the `clientGroup` object with a scalar `groupCode` |
| `ClientMasterResponse` (extends `BaseResponse`) | `clientMasterDtoList:List<ClientMasterDto>`, `totalRecord:Long` | Paged list envelope | Distinct |
| `DashboardDbReport` (extends `BaseResponse`) | `emailCount:Integer`, `groupCount:Integer`, `clientCount:Integer`, `templateCount:Integer` | Dashboard counters | Distinct (aggregate) |
| `EmailData` | `toEmail:String`, `domain:String`, `uniqueId:String` | Per-recipient row parsed out of an uploaded Excel for Gmail sends | Distinct (no entity) |
| `EmailDto` | `emailInboxId:Long`, `from:String`, `subject:String`, `content:String` | Inbox read projection | **Partial subset** of `EmailInbox` (drops `emailId`, `date`, `isActive`) |
| `EmailExtractDto` | `domain:String`, `email:String` | Extracted-address projection | **Partial subset** of `EmailExtractorData` (drops id, extractorId, dates) |
| `EmailMasterDto` | `emailMasterId:Long`, `email:String`, `protocol:String`, `host:String`, `username:String`, `port:Integer`, `auth:String`, `enable:String`, `password:String` | 🔴 **1:1 duplicate of the `EmailMaster` entity, including the plaintext `password`** — and is used as the `@RequestBody` for `/saveEmail` | **Exact duplicate of an entity, secret included** |
| `EmailRequest` | `fromEmail:String`, `draftSubject:String`, `emails:List<EmailData>`, `scheduleTime:LocalDateTime` | Gmail batch-send request | Distinct (composite request) |
| `EmailSendRequest` | `fromEmail:String`, `emailType:String`, `group:String`, `client:String[]`, `template:String` | SMTP send request | Distinct (request) |
| `EmailTemplateRequest` | `templateName:String`, `templateContent:String` | Template save request | Distinct (request) |
| `Mail` | `from:String`, `mailTo:String`, `subject:String`, `attachments:List<Object>`, `props:Map<String,Object>` | Internal SMTP message model | Distinct |
| `Page<T>` | `data:List<T>`, `recordsFiltered:Long`, `recordsTotal:Long`, `draw:int` | DataTables server-side envelope | Distinct |
| `PagingRequest` | `start:int`, `length:int`, `draw:int` | DataTables server-side request | Distinct |

`Page.recordsTotal` is hard-coded to `100L` in `EmailController.getClientMasterPagination` — a bug (total is not the row count).

---

## A4. Proposed modern schema for the rebuild

Target: MySQL 8.0 / InnoDB, `utf8mb4_0900_ai_ci` everywhere, `TIMESTAMP(6)`/`DATETIME(6)` for all times, `BIGINT UNSIGNED` PKs, Flyway migrations, no class-level `@Where` (use explicit soft-delete predicates), real FKs with `ON DELETE` policies, and secrets out of the DB.

### 1. `app_user` (replaces `login`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AUTO_INCREMENT | |
| `username` | `VARCHAR(150)` **NOT NULL UNIQUE** | was `login.Username` |
| `password_hash` | `VARCHAR(100)` NOT NULL | BCrypt (60 chars); Argon2id preferred |
| `employee_id` | `VARCHAR(64)` NULL UNIQUE | was `login.EmpId` |
| `email` | `VARCHAR(254)` NULL | new; for notifications |
| `status` | `ENUM('ACTIVE','DISABLED','LOCKED')` NOT NULL DEFAULT 'ACTIVE' | replaces `IsActive int` |
| `failed_login_count` | `INT UNSIGNED` NOT NULL DEFAULT 0 | new |
| `locked_until` | `DATETIME(6)` NULL | new |
| `last_login_at` | `DATETIME(6)` NULL | new |
| `created_at` / `created_by` | `DATETIME(6)` / `BIGINT UNSIGNED` | audit |
| `updated_at` / `updated_by` | `DATETIME(6)` / `BIGINT UNSIGNED` | audit |
| `deleted_at` | `DATETIME(6)` NULL | soft delete |
| Indexes | `uk_app_user_username (username)`, `uk_app_user_employee_id (employee_id)`, `idx_app_user_deleted (deleted_at)` | |

### 2. `app_role` (replaces `role_security`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK | |
| `code` | `VARCHAR(50)` NOT NULL UNIQUE | e.g. `MANG`, `USER`, `ADMIN` |
| `name` | `VARCHAR(150)` NOT NULL | |
| `created_at`/`updated_at` | `DATETIME(6)` | |
| `deleted_at` | `DATETIME(6)` NULL | |

### 3. `app_user_role`
| Column | Type | Notes |
|---|---|---|
| `user_id` | `BIGINT UNSIGNED` NOT NULL | FK → `app_user(id)` ON DELETE CASCADE |
| `role_id` | `BIGINT UNSIGNED` NOT NULL | FK → `app_role(id)` ON DELETE CASCADE |
| `granted_at` | `DATETIME(6)` NOT NULL | |
| PK | `(user_id, role_id)` | |
| Index | `idx_app_user_role_role (role_id)` | |

### 4. `client_group`
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | was `groupId` |
| `code` | `VARCHAR(32)` **NOT NULL UNIQUE** | was `groupCode` — was non-unique and used as a FK target |
| `name` | `VARCHAR(255)` NOT NULL | was `groupDesc` |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` ON DELETE SET NULL — was a bare `loginId` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | `DATETIME(6)` / `BIGINT UNSIGNED` | replaces `String` dates + string users |
| `deleted_at` | `DATETIME(6)` NULL | replaces `isActive int` + `@Where` |
| Indexes | `uk_client_group_code (code)`, `idx_client_group_owner (owner_user_id)` | |

### 5. `client`
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | was `clientMasterId` |
| `name` | `VARCHAR(255)` NOT NULL | |
| `email` | `VARCHAR(254)` NOT NULL | PII |
| `web_link` | `VARCHAR(2048)` NULL | was `VARCHAR(255)` |
| `group_id` | `BIGINT UNSIGNED` NULL | FK → `client_group(id)` ON DELETE RESTRICT — **replaces the FK to a non-unique `groupCode`** |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | |
| `deleted_at` | `DATETIME(6)` NULL | |
| Indexes | `idx_client_group (group_id)`, `idx_client_owner (owner_user_id)`, `idx_client_email (email)` | **`idx_client_group` fixes the missing FK index** |

### 6. `email_account` (SMTP — replaces `email_master`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `email` | `VARCHAR(254)` NOT NULL **UNIQUE** | was unconstrained |
| `protocol` | `ENUM('SMTP','SMTPS')` NOT NULL DEFAULT 'SMTP' | replaces string |
| `host` | `VARCHAR(255)` NOT NULL | |
| `port` | `INT UNSIGNED` NOT NULL | CHECK 1..65535 |
| `username` | `VARCHAR(254)` NOT NULL | |
| `auth_method` | `ENUM('PLAIN','LOGIN','NONE')` NOT NULL DEFAULT 'PLAIN' | replaces `auth` string |
| `enabled` | `BOOLEAN` NOT NULL DEFAULT TRUE | replaces `enable` string |
| `secret_ref` | `VARCHAR(255)` NULL | 🔴 **pointer to the secret store** — no password in the DB |
| `secret_ciphertext` | `VARBINARY(2048)` NULL | 🔴 AES-256-GCM ciphertext, envelope-encrypted, only if the store is not available |
| `secret_key_version` | `VARCHAR(64)` NULL | KMS key version for rotation |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | |
| `deleted_at` | `DATETIME(6)` NULL | |
| Indexes | `uk_email_account_email (email)`, `idx_email_account_owner (owner_user_id)` | |

### 7. `gmail_account` (replaces `gmail_master`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `email` | `VARCHAR(254)` NOT NULL **UNIQUE** | |
| `oauth_client_id` | `VARCHAR(255)` NOT NULL | semi-public; may stay in DB |
| `secret_ref` | `VARCHAR(255)` NULL | 🔴 pointer to secret store for the **OAuth client secret** |
| `refresh_token_ciphertext` | `VARBINARY(4096)` NULL | 🔴 **the highest-value secret in the system** |
| `access_token_ciphertext` | `VARBINARY(4096)` NULL | 🔴 short-lived bearer token, best kept in memory only |
| `token_expires_at` | `DATETIME(6)` NULL | |
| `scopes` | `VARCHAR(512)` NULL | e.g. `gmail.send,gmail.readonly` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | |
| `deleted_at` | `DATETIME(6)` NULL | |
| Indexes | `uk_gmail_account_email (email)` | |

### 8. `email_template` (replaces `email_template`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `name` | `VARCHAR(255)` NOT NULL | |
| `content_html` | `MEDIUMTEXT` NOT NULL | 🔧 fixes the `VARCHAR(255)` truncation of CKEditor HTML |
| `status` | `ENUM('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','ARCHIVED')` NOT NULL DEFAULT 'DRAFT' | replaces free-text `status` |
| `approved_by` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` — the approve flow currently loses who approved |
| `approved_at` | `DATETIME(6)` NULL | |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` |
| `version` | `INT UNSIGNED` NOT NULL DEFAULT 1 | for audit of template edits |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | |
| `deleted_at` | `DATETIME(6)` NULL | |
| Indexes | `uk_email_template_name_owner (name, owner_user_id)`, `idx_email_template_status (status)`, `idx_email_template_owner (owner_user_id)` | |

### 9. `email_send_job` (replaces `email_send_job`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `from_account_id` | `BIGINT UNSIGNED` NULL | FK → `email_account(id)` — replaces free-text `fromEmail` |
| `target_type` | `ENUM('GROUP','INDIVIDUAL')` NOT NULL | replaces `type` string |
| `template_id` | `BIGINT UNSIGNED` NULL | FK → `email_template(id)` |
| `recipient_count` | `INT UNSIGNED` NOT NULL DEFAULT 0 | `noOfEmail` |
| `success_count` | `INT UNSIGNED` NOT NULL DEFAULT 0 | `noOfSuccess` |
| `failure_count` | `INT UNSIGNED` NOT NULL DEFAULT 0 | generated column or maintained |
| `status` | `ENUM('QUEUED','RUNNING','SUCCESS','PARTIAL','FAILED')` NOT NULL | |
| `started_at` / `finished_at` | `DATETIME(6)` | replaces `java.sql.Date date` |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | adds the missing `updated_*` |
| Indexes | `idx_esj_owner (owner_user_id)`, `idx_esj_from (from_account_id)`, `idx_esj_template (template_id)`, `idx_esj_status_created (status, created_at)` | fixes missing FK indexes |

### 10. `email_delivery` (replaces `email_send_status` — per-recipient)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `job_id` | `BIGINT UNSIGNED` NULL | FK → `email_send_job(id)` ON DELETE CASCADE — replaces bare `emailTemplateId` |
| `recipient_email` | `VARCHAR(254)` NOT NULL | |
| `template_id` | `BIGINT UNSIGNED` NULL | FK → `email_template(id)` |
| `status` | `ENUM('QUEUED','SENT','FAILED','BOUNCED')` NOT NULL | |
| `provider_message_id` | `VARCHAR(255)` NULL | |
| `error_detail` | `TEXT` NULL | |
| `sent_at` | `DATETIME(6)` NULL | replaces `java.sql.Date date` |
| `created_at`/`created_by` | as above | |
| Indexes | `idx_email_delivery_job (job_id)`, `idx_email_delivery_recipient (recipient_email)`, `idx_email_delivery_template (template_id)`, `idx_email_delivery_sent_at (sent_at)` | |

### 11. `email_outbox` (replaces `email_queue`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `from_account_id` | `BIGINT UNSIGNED` NULL | FK → `gmail_account(id)` |
| `to_email` | `VARCHAR(254)` NOT NULL | |
| `draft_id` | `VARCHAR(255)` NULL | external Gmail draft id |
| `subject` | `VARCHAR(998)` NULL | was mis-mapped to `domain` |
| `scheduled_at` | `DATETIME(6)` NOT NULL | was `scheduledTime` (already a proper timestamp) |
| `status` | `ENUM('SCHEDULED','SENDING','SENT','FAILED','CANCELLED')` NOT NULL | |
| `attempt_count` | `INT UNSIGNED` NOT NULL DEFAULT 0 | new |
| `error_message` | `TEXT` NULL | was `VARCHAR(255)` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | adds the entirely missing audit columns |
| Indexes | `idx_outbox_status_sched (status, scheduled_at)`, `idx_outbox_from (from_account_id)`, `idx_outbox_to (to_email)` | the scheduler's hot path |

### 12. `extraction_run` (replaces `email_extractor`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `status` | `ENUM('QUEUED','RUNNING','SUCCESS','FAILED','PARTIAL')` NOT NULL | |
| `domain_count` | `BIGINT UNSIGNED` NOT NULL DEFAULT 0 | `noOfDomain` |
| `source_file_name` | `VARCHAR(512)` NULL | `fileName` |
| `source_file_object_key` | `VARCHAR(1024)` NULL | pointer to S3/MinIO instead of `C:\deployment` |
| `started_at` | `DATETIME(6)` NOT NULL | replaces `java.sql.Date startDate` |
| `finished_at` | `DATETIME(6)` NULL | |
| `error_detail` | `TEXT` NULL | |
| `owner_user_id` | `BIGINT UNSIGNED` NULL | FK → `app_user(id)` |
| `created_at`/`created_by`/`updated_at`/`updated_by` | as above | |
| Indexes | `idx_extraction_run_owner (owner_user_id)`, `idx_extraction_run_started (started_at)` | |

### 13. `extraction_result` (replaces `email_extractor_data`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `run_id` | `BIGINT UNSIGNED` NOT NULL | FK → `extraction_run(id)` ON DELETE CASCADE — **replaces the orphan-prone bare `emailExtractorId`** |
| `domain` | `VARCHAR(255)` NOT NULL | |
| `email` | `VARCHAR(254)` NOT NULL | |
| `email_hash` | `BINARY(32)` NOT NULL | SHA-256 of normalised email for fast dedupe |
| `source_row` | `INT UNSIGNED` NULL | traceable back to the uploaded sheet |
| `created_at` | `DATETIME(6)` NOT NULL | |
| Indexes | `idx_extraction_result_run (run_id)`, `uk_extraction_result_run_email (run_id, email_hash)` | **unique dedupe constraint the original lacks** |

### 14. `email_inbox_message` (replaces `email_inbox`)
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT UNSIGNED` PK AI | |
| `account_id` | `BIGINT UNSIGNED` NULL | FK → `email_account(id)` — the original has **no `loginId` at all** |
| `provider_message_id` | `VARCHAR(255)` NOT NULL | `emailId` |
| `from_address` | `VARCHAR(254)` NOT NULL | `from` (avoid the reserved word) |
| `subject` | `VARCHAR(998)` NULL | |
| `body_html` | `MEDIUMTEXT` NULL | `content` was `VARCHAR(255)` |
| `received_at` | `DATETIME(6)` NOT NULL | replaces `java.sql.Date date` |
| `is_read` | `BOOLEAN` NOT NULL DEFAULT FALSE | new |
| `created_at` | `DATETIME(6)` NOT NULL | |
| `deleted_at` | `DATETIME(6)` NULL | |
| Indexes | `uk_inbox_account_msg (account_id, provider_message_id)`, `idx_inbox_received (received_at)`, `idx_inbox_from (from_address)` | |

### Secret handling (explicit)

| Original column / location | Secret? | Where it should live | Encryption at rest |
|---|---|---|---|
| `email_master.password` | 🔴 Yes — SMTP password, **plaintext**, rendered in a `type="text"` HTML input | **AWS Secrets Manager** (or Vault / GCP Secret Manager), referenced by `email_account.secret_ref` | KMS CMK envelope encryption: generate a per-secret DEK, AES-256-GCM with a random 96-bit IV, store ciphertext + IV + tag + KMS key ARN/version. Rotate on write. Never log, never return to the client. |
| `gmail_master.secret` | 🔴 Yes — Google OAuth **client secret** | Secret manager | Same as above |
| `gmail_master.refreshToken` | 🔴 **Highest value in the system** — permanent mailbox read/send | Secret manager, ideally in a separate credential vault with a short TTL lease | Same; plus treat as revocable — store `token_id` and re-derive via a refresh broker that holds the secret |
| `gmail_master.accessToken` | 🔴 Yes — bearer token | **Do not persist**; hold in memory / Redis with a short TTL | Redis at-rest encryption (KMS) + TLS in transit; or not stored at all (re-derive from refresh token) |
| `gmail_master.clientId` | 🟡 Semi-public identifier | DB is acceptable | TLS only |
| `login.Password` | 🟡 Password **hash**, not a secret | DB is the correct place | BCrypt/Argon2id (already BCrypt) — but move to `password_hash VARCHAR(100)` and add per-user salt handling + pepper in secret manager |
| `application.properties` → `app.datasource.mysql.password=elexyt2024` | 🔴 Yes — **DB root password committed in the jar** | Secret manager, injected as `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` env var | KMS-encrypted at rest; rotate immediately, the value is already public |
| `application.properties` → `wordpress.api.token` | 🔴 Yes — appears to be a real WP API token | Secret manager | same |
| `application.properties` → `license` (JWT) | 🟡 Licence blob, may embed a signing key | Secret manager or signed licence file | same |
| `application.properties` → `mac` | 🟡 Device fingerprint, used for licence binding | Config, not a DB column | n/a |
| `client.email` / `email_inbox.from` / `email_extractor_data.email` | 🟡 PII, not secrets | DB, but classify + encrypt at rest via **TDE (RDS/EBS encryption)** and add a retention policy | Volume encryption + column-level encryption only if PII classification demands it |

---

## A5. Schema problems in the original

### 5.1 Date/time modelling
1. **Date columns stored as `VARCHAR`**: `createdDate`, `modifiedDate` (all 6 "master" tables), `CreatedDate`, `ModifiedDate` (`login`). Values are written by `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")` into a `String` column. Consequences: lexicographic ordering happens to work only because the format is fixed-width, but the column is untyped, unindexable for ranges, locale/timezone-ambiguous, and MySQL cannot compute on it.
2. **`java.sql.Date` for business timestamps**: `email_extractor.startDate`, `email_extractor_data.startDate`, `email_send_job.date`, `email_send_status.date`, `email_inbox.date` → `DATE` columns that **discard the time of day**. A send job started at 23:59 and finished at 00:01 are indistinguishable; `email_send_status` cannot order events within a day.
3. **Month bucketing via `between` on a `DATE`**: `findByLoginIdMonth(loginId, Date.valueOf(firstDay), Date.valueOf(lastDay))` relies on inclusive-exclusive semantics; a `DATE` column stores the last day as `YYYY-MM-DD 00:00:00`, so any row on the last day with a time component is excluded. With `DATE` storage it works only by accident.
4. `email_queue.scheduledTime` is the **only** correct timestamp (`LocalDateTime` → `DATETIME`). It is also the only table with no `createdBy/createdDate` at all.

### 5.2 Missing indexes on FK / join columns
Every one of these is a real FK candidate with **no index**:
- `client_master.groupCode` → `client_group.groupCode`
- `client_group.loginId`, `client_master.loginId`
- `email_master.loginId`, `email_template.loginId`, `email_extractor.loginId`, `email_send_job.loginId`, `email_send_status.loginId`
- `email_extractor_data.emailExtractorId` (no FK at all)
- `email_send_status.emailTemplateId` (no FK at all)
- `email_queue.fromEmail` (→ `gmail_master.email`)
- `email_inbox.emailId`
- `login_role_security.LoginId` and `.RoleId` (join table with no PK and no index)

Consequence: `findByLoginId(...)` on every list screen is a **full table scan**; the `getEmailInbox` / `getEmailSendJobs` endpoints load unbounded result sets with no `LIMIT`.

### 5.3 No unique constraints anywhere
- Not a single `@Column(unique=true)`, `@UniqueConstraint`, or `UNIQUE` in the schema source.
- `login.Username` — duplicate usernames are possible; `findByUsername` then returns `NonUniqueResultException` at login.
- `email_master.email` — two SMTP accounts for the same address; `findByEmail` breaks.
- `gmail_master.email` — same; `getAllGmail` would show duplicates and the OAuth credential becomes ambiguous.
- `client_group.groupCode` — **this is the FK target for `client_master`**, so duplicate group codes make the join ambiguous.
- `client_master.email` — the same client can be uploaded twice, and `email_extractor_data.email` has no dedupe constraint across runs.
- `role_security.RoleName` — duplicate roles possible.
- `email_template.templateName` — no per-owner uniqueness.
- `email_extractor_data (emailExtractorId, email)` — no uniqueness, so re-running an extraction duplicates rows.

### 5.4 Charset / collation mixing
No DDL was supplied with the artefacts (no `.sql` file exists anywhere under `/tmp/seo-re` or the decompiled tree), so the exact per-table charsets **cannot be read directly**. What the artefacts do prove:
- `application.properties`: `app.datasource.mysql.url=jdbc:mysql://localhost:3306/emailsender` — **no `characterEncoding=utf8&connectionCollation=…&useSSL=…` parameters**, so the connection inherits the server default. If the server default is `latin1` (MySQL ≤5.7 default) any `utf8` payload in `templateContent` / `content` / `errorMessage` is silently mangled on write.
- `driver-class-name=com.mysql.jdbc.Driver` — the **deprecated MySQL Connector/J 5.x** driver, not `com.mysql.cj.jdbc.Driver`.
- The schema is hand-created (`hibernate.hbm2ddl.auto=validate`, no migration tool), and hand-created schemas in phpMyAdmin/DBeaver default to whatever the GUI picks — historically `latin1` for older dumps and `utf8mb3` for MySQL 5.7-era dumps. `utf8mb3` (the 3-byte alias of `utf8mb4`) cannot store 4-byte characters (emoji in subject lines, some CJK extensions, rare names) and silently truncates or errors.
- `spring.sql.init.continue-on-error=true` masks any failed `data.sql`/`schema.sql` at boot, so charset/index errors during init are invisible.

**Verification needed before the rebuild** (one command, must be run against the live DB):
```sql
SELECT TABLE_NAME, TABLE_COLLATION FROM information_schema.TABLES  WHERE TABLE_SCHEMA='emailsender';
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='emailsender' AND CHARACTER_SET_NAME IS NOT NULL;
```
Target: every table and every text column `utf8mb4_0900_ai_ci` (or `utf8mb4_unicode_520_ci` on MySQL 5.7), database-level `utf8mb4`, and `SET NAMES utf8mb4` on every connection.

### 5.5 Missing audit columns
| Table | `createdBy` | `createdDate` | `modifiedBy` | `modifiedDate` |
|---|---|---|---|---|
| `client_group` | ✅ | ✅ | ✅ | ✅ |
| `client_master` | ✅ | ✅ | ✅ | ✅ |
| `email_master` | ✅ | ✅ | ✅ | ✅ |
| `email_template` | ✅ | ✅ | ✅ | ✅ |
| `gmail_master` | ✅ | ✅ | ✅ | ✅ |
| `email_extractor` | ✅ | ✅ | ❌ | ❌ |
| `email_extractor_data` | ✅ | ✅ | ❌ | ❌ |
| `email_send_job` | ✅ | ✅ | ❌ | ❌ |
| `email_send_status` | ✅ | ✅ | ❌ | ❌ |
| `email_inbox` | ❌ | ❌ | ❌ | ❌ |
| `email_queue` | ❌ | ❌ | ❌ | ❌ |
| `login` | ✅ | ✅ | ✅ | ✅ |
| `role_security` | ❌ | ❌ | ❌ | ❌ |
| `login_role_security` | ❌ | ❌ | ❌ | ❌ |

Additionally, **no row-level `version`/optimistic-lock column** anywhere, so concurrent edits (e.g. two admins editing a template) silently last-write-win.

### 5.6 Missing soft delete
| Table | Soft delete? | Notes |
|---|---|---|
| `client_group` | ✅ `isActive` + `@Where` | |
| `client_master` | ✅ `isActive` + `@Where` | |
| `email_master` | ✅ `isActive` + `@Where` | |
| `email_template` | ✅ `isActive` + `@Where` | |
| `gmail_master` | ✅ `isActive` + `@Where` | |
| `email_inbox` | ✅ `isActive` + `@Where` | |
| `email_extractor` | ❌ | hard delete; run history lost |
| `email_extractor_data` | ❌ | hard delete; highest-volume table, worst case |
| `email_send_job` | ❌ | hard delete; send history lost |
| `email_send_status` | ❌ | hard delete |
| `email_queue` | ❌ | hard delete; scheduled sends vanish |
| `login` | ✅ `IsActive` (no `@Where` filter) | |
| `role_security` | ❌ | |

The `isActive` flag itself is an `int` rather than a `BOOLEAN` and is combined with a Hibernate `@Where` class filter. That filter is **silently bypassed by every native query** (all 4 `@Query(nativeQuery=true)` methods) and by `deleteByEmailIdAndDate`, so "soft-deleted" rows are visible and hard-deletable depending on which code path you take.

### 5.7 Relationship / cascade defects
1. **`client_master.groupCode` → `client_group.groupCode`** — a FK to a **non-primary, non-unique** column. Hibernate 6 rejects `referencedColumnName` pointing at a non-unique column; today it works only because the DB has no FK constraint at all. Any group-code rename orphans clients silently.
2. **`CascadeType.ALL` on `@ManyToOne clientGroup`** — removing one client cascades a delete of the entire `ClientGroup` **and every other client in it**. `ClientGroup.html` has two trash buttons (red = delete group, yellow = "remove clients") and the underlying service `removeClient` does a bulk soft-delete, but the *entity* cascade is the nuclear option.
3. **`CascadeType.ALL` on `@OneToMany clientMaster`** — persisting a `ClientGroup` cascades inserts/updates to the entire client collection; `equals`/`hashCode` include that collection, so adding a client to a loaded group mutates the group's hash.
4. **`@JsonIgnoreProperties` misapplied** — it is a Jackson annotation placed on JPA fields (`ClientGroup.clientMaster`, `ClientGroup.setClientMaster`). It has **no effect on serialization** and, used without the `ignoreUnknown` attribute, does nothing at all. The intent was `@JsonIgnoreProperties(ignoreUnknown = true)` at class level. As written, `ClientGroup` is serialized with its full `clientMaster` list, and `ClientMaster` is serialized with its `clientGroup` back-reference → **infinite recursion risk** on any endpoint that returns the entity directly (e.g. `/getEmailMaster` returns `List<EmailMaster>` raw).
5. **Bare `Long` FKs** (`email_extractor_data.emailExtractorId`, `email_send_status.emailTemplateId`, every `loginId`): no `@ManyToOne`, therefore **no FK constraint**, therefore orphan rows and no referential integrity. Every `loginId` in 6 tables is an unenforced dangling reference.

### 5.8 Type modelling defects
- `isActive` as `int` (0/1) instead of `BOOLEAN`.
- `auth` and `enable` as `String` holding `"true"`/`"false"` instead of `BOOLEAN`.
- `status` as free-text `String` in **5 tables** (`email_template`, `email_extractor`, `email_send_job`, `email_send_status`, `email_queue`) with no enum and no check constraint — no referential integrity on state transitions.
- `templateContent` and `content` as `VARCHAR(255)` storing **HTML documents** → truncation of CKEditor output and of email bodies. Needs `TEXT`/`MEDIUMTEXT` or `@Lob`.
- `errorMessage` as `VARCHAR(255)` for SMTP stack traces → truncation.
- `webLink` as `VARCHAR(255)` → too short for long URLs.
- `email_master.username` is always overwritten with the email address (`EmailService.saveEmailMaster`), so the column is redundant; the UI form has **no username field** at all.
- Column named `from` (`email_inbox`) is a SQL keyword and requires quoting everywhere.
- Column named `date` (`email_send_job`, `email_send_status`, `email_inbox`) is a reserved-ish type name.
- `Integer`/`Long` counters (`noOfEmail`, `noOfSuccess`, `noOfDomain`) are nullable with no `NOT NULL` and no `CHECK (success <= total)`.

### 5.9 Configuration / operational defects
- `hibernate.show_sql=true` in production → full SQL logging.
- `hibernate.hbm2ddl.auto=validate` with **no Flyway/Liquibase** → the only schema-drift detector is a boot-time crash.
- `app.upload.path=C:\deployment` — a **Windows absolute path** hardcoded in the jar; breaks on Linux and is not a configured volume.
- `spring.servlet.multipart.max-file-size=128MB` — very large, and combined with a 600 s AJAX timeout and `location.reload()` in the UI, a single upload can exhaust the disk.
- `com.mysql.jdbc.Driver` — deprecated driver; no `serverTimezone`, `useSSL`, or `characterEncoding` in the JDBC URL.
- Hikari pool left at defaults (no `maximumPoolSize`, `connectionTimeout`).
- `PasswordEncoder` is declared **twice** — `SecurityConfiguration.passwordencoder()` and `WebMvcConfigruation.passwordencoder()` — both `BCryptPasswordEncoder`, both `@Bean`. One will silently shadow the other.
- `LoginDetailsServiceImpl` does `new User(login.getUsername(), login.getPassword(), grantedAuthorities)` with **no `{noop}` prefix**, so a BCrypt hash is used correctly — but there is no password-change flow, no lockout, no expiry, and no `passwordEncoder.upgradeEncoding` rehash on login.
- `SecurityConfiguration` has `maximumSessions(1)` but the session-creation policy is `IF_REQUIRED`, so the "max 1" is effectively advisory; and there is no `sessionManagement().invalidSessionUrl` redirect wired to the `/sessionExpired` template that exists.

### 5.10 Summary counts
| Problem class | Count |
|---|---|
| Tables with zero unique constraints | 13 of 13 |
| Tables with zero indexes on FK columns | 11 of 13 |
| Date/time columns stored as `VARCHAR` | 12+ |
| Date/time columns with time-of-day discarded (`java.sql.Date`) | 5 |
| Tables missing `modifiedBy`/`modifiedDate` | 8 |
| Tables with no audit columns at all | 4 (`email_inbox`, `email_queue`, `role_security`, `login_role_security`) |
| Tables with no soft delete | 6 |
| Columns holding plaintext secrets | 4 (`email_master.password`, `gmail_master.secret`, `gmail_master.refreshToken`, `gmail_master.accessToken`) |
| Bare `Long` pseudo-FKs with no constraint | 12+ |

---

# PART B — Frontend (Thymeleaf + static assets)

## B1. Template inventory

| File | Purpose | Page sections / forms / modals |
|---|---|---|
| `login.html` | Authentication | 2-column card: left = logo/`Mail.jpg` (carousel **commented out**), right = login form (`username`, `password`, CSRF, Login button) + `alert alert-danger` on `param.error`; inline `<style>` block ~290 lines. `layout:decorator="layout"` but **no `layout.html` exists** in the template dir → the decorator layout is missing (the `Header :: head` fragment is pulled in separately) |
| `Header.html` | Shared head + 2 navbars (Thymeleaf fragments `head`, `navbar`, `navbar1`) | `head` = all CSS/JS + global `#myTable` DataTable init (dead) + inline navbar style override; `navbar` = dark top bar (brand, "Dash Board", Log Out button + hidden `logoutForm` POST `/logout` with CSRF); `navbar1` = white second nav bar with 9 app links (Send, Template, Client, Client Group, Inbox, Email, Approve Template, Email Extractor) |
| `NewDashBoard.html` | Main dashboard (`/`, `/welcome` → `LoginController.welcome` returns `"NewDashBoard"`) | 4 KPI cards (Email / Group / Client / Template) + 2 Chart.js cards with a 12-month `<select>` each (Monthly Email Status Report = pie; Monthly Email Send Report = doughnut). Inline `card-counter` CSS |
| `Dashboard.html` | **Orphan** legacy dashboard — no controller returns `"Dashboard"` | 3 hover-effect tiles (Email → `/email`, Template → `/email` **wrong**, Clients → `emailClient`) + a large commented-out block (OCR, Merge) |
| `ClientGroup.html` | Manage client groups | Card header "Create New Group" button → `#addModal` (Bootstrap modal) with form `#formFile` (`groupCode`, `groupDesc`); DataTable `#clientGroup` (Group Name, Group Code, Action) with per-row red "remove group" / yellow "remove clients" trash buttons; `clientGroup()` loader; `swal` confirmations |
| `emailClient.html` | Bulk-upload clients | Card header "Upload Client" button → `#addModal` with form `#formFile` (file input, `#groupCode` select); DataTable `#emailClientData` (Name, Email, WebLink, Group) in **server-side mode**; `.loader` overlay + `sending.gif` |
| `EmailExtractor.html` | Upload a file of web-links → extract emails; download results | Card header: "Upload WebLinks" button → `#addModal` (`#formFile`, file input) **and** a separate `<form th:action="@{/downloadFile}">` with `fromDate` + `toDate` `<input type="date">` and an orange download button; DataTable `#emailExtractorData` (Date, Status rendered as a Bootstrap progress bar) |
| `EmailInbox.html` | Read inbox for a chosen account | `#emailInboxForm` with `#fromEmail` select + a search button; a commented-out From/To date range; DataTable `#emailInbox` (From, Subject, Action) with a red "See Message" button; `#previewModal` (modal-xl, 1350px) containing `#previewData` |
| `EmailMaster.html` | Manage SMTP accounts | Card header "Add Email" → `#addModal` (`#formFile`) with 7 fields (email/protocol/host/port/password/auth/enable); DataTable `#emailMaster` (Email, Protocol, Host, Port, Action) with an edit (yellow) and delete (red) button; `swal` confirms |
| `EmailTemplate.html` | Manage email templates | Card header "Add Template" → `#addModal` with `TemplateName` text input + `Templatecontent` textarea (CKEditor 5 classic); DataTable `#template` (Template Name, Preview, Action) with eye + trash buttons; `#previewModal` with `#previewData` |
| `EmailTemplateApprove.html` | Manager approval queue for templates | Card "Approve Template" + DataTable `#template` (Template Name, Preview, Action) with eye + check buttons; `#previewModal`. **Contains a full copy of the dead `emailTemplate()`/CKEditor block from `EmailTemplate.html`** even though the form does not exist here |
| `EmailSend.html` | Send emails via SMTP (the main "Send" page) | Form `#emailSendForm` (no action): `fromEmail` select, `emailType` select (Group/individual), `group` select, `client` multi-select, `template` select, orange "Send" button; right-hand live preview card (`#previewData`); DataTable `#emailSendJob` (From Email, Type, NoOfEmail, NoOfSuccess, Status-as-progress-bar) |
| `GmailTemplate.html` | Send emails via Gmail OAuth (the other "Send" page) | Form `#emailSendForm` (multipart): `fromEmail` select (live-search), `template` select (populated from Gmail drafts), `date` `datetime-local`, `file` input, green SUBMIT; right-hand "📬 Email Quota Report" card (From Email, Emails Sent Last 24h, Daily Quota Limit, Remaining Quota); DataTable `#emailSendJob` (From Email, ToEmail, **Message**, ScheduledTime, Status, ErrorMessage) |
| `mailtemplate.html` | **Email body template**, not a page | Minimal standalone HTML (no Bootstrap, no nav) — inline-styled "Hi Team" greeting + `th:utext="${content}"` + `th:text="${webLink}"`; a large **commented-out** table-based HTML email layout referencing `${name}`, `${username}`, `${password}`, `@{${loginPath}}` — **this is a credential-leak template from a different product (Red Pluto Analytics HR)** |

**Referenced but non-existent templates:** `layout.html` (used by every `layout:decorator="layout"`), `MainDashBoard.html` (`LoginController.MainDashBoard` → `"MainDashBoard"`), `SessionExp.html` (`/sessionExpired` → `"SessionExp"`), `accessdenied.html` (`/accessdenied` → `"accessdenied"`). All four will throw a template-resolution error at runtime.

## B2. Navigation / IA

```
[login.html]  POST /login  (Spring Security form login; success → /welcome)

[/welcome | /]  → NewDashBoard.html
   │
   ├── Header::navbar (top bar, always on every page)
   │     ├── "Dash Board"  → welcome
   │     └── "Log Out"     → POST /logout (hidden form, CSRF)
   │
   └── Header::navbar1 (app nav, 8 links, present on every page except login)
         ├── Send               → /gmailTemplate      (GmailTemplate.html)  [fa fa-home]
         ├── Template           → emailTemplate       (EmailTemplate.html)  [fa fa-clock-o]
         ├── Client             → emailClient         (emailClient.html)    [fa fa-calendar-o]
         ├── Client Group       → clientGroup         (ClientGroup.html)    [fa fa-calendar-o]
         ├── Inbox              → emailInbox          (EmailInbox.html)     [fa fa-calendar-o]
         ├── Email              → /emailMaster        (EmailMaster.html)    [fa fa-home]
         ├── Approve Template   → /approveTemplates   (EmailTemplateApprove.html)
         │                        [sec:authorize="hasAnyRole('MANG')"]     [fa fa-home]
         └── Email Extractor    → /emailExtractor     (EmailExtractor.html) [fa fa-home]

Dead / not in nav:
  /email                 → EmailSend.html        (the SMTP send page — no nav entry!)
  Dashboard.html         → orphaned, no controller
  /searchFile            → commented out in navbar
  /changePasswordView    → commented out in navbar
  /taskDashboard, /taskAssignDashboard, /createTask, /taskReport → commented-out dropdown
```

**IA observations**
- The primary nav is a flat 8-item bar; there is **no grouping or hierarchy** — Send, Template, Client, Client Group, Inbox, Email, Approve Template, Email Extractor are all siblings.
- The two "Send" flows (SMTP `/email` and Gmail `/gmailTemplate`) are **not unified** and only the Gmail one is in the nav. `/email` is reachable only from the orphaned `Dashboard.html` tile.
- The icon set is semantically wrong: "Send" and "Email" and "Approve Template" and "Email Extractor" all use `fa fa-home`; "Template" uses `fa fa-clock-o`; "Client", "Client Group" and "Inbox" all use `fa fa-calendar-o`. The icons carry no information.
- `Dashboard.html` maps both the "Email" and "Template" tiles to `/email`.
- `sec:authorize="hasAnyRole('MANG')"` gates the Approve link in the UI only; the controller's `/approveTemplates` page and `/approveTemplate` action are **not method-secured** — the `sec` namespace points at `thymeleaf-extras-springsecurity3` (Spring Security 3 era) which is unlikely to be on the classpath of a Spring Boot 3 / Jakarta app at all, so the attribute would be silently dropped.

## B3. Every AJAX / fetch call

All calls are **jQuery `$.ajax`**. There is no `fetch`, no `axios`, no XHR. CSRF is sent as the `X-CSRF-TOKEN` header, read from `<meta name="_csrf">`. `contentType: 'application/json; charset=utf-8'` is set on most calls even when the body is a plain query string (the server binds `@RequestParam`).

| # | Template | Method | Endpoint | Payload | Response → use |
|---|---|---|---|---|---|
| 1 | NewDashBoard | GET | `getDashboardDbReport` | – | `DashboardDbReport` → fills `#email`, `#group`, `#client`, `#template` counters |
| 2 | NewDashBoard | GET | `getMonthlyReport` | `month`, `filterType='STATUS'` | `List<Long>` `[emailsSent, emailsExtracted]` → pie chart `statusChart` |
| 3 | NewDashBoard | GET | `getMonthlyEmailReport` | `month`, `filterType='PRIORITY'` | `List<Long>` `[sent, success, failure]` → doughnut `priorityChart` |
| 4 | ClientGroup | POST | `getClientGroup` | – | `List<ClientGroupDto>` → DataTable rows + action buttons |
| 5 | ClientGroup | GET | `createClientGroup` | `groupCode`, `groupDesc` | `BaseResponse` → swal success/failure |
| 6 | ClientGroup | GET | `removeClientGroup` | `groupCode` | `BaseResponse` |
| 7 | ClientGroup | GET | `removeClient` | `groupCode` | `BaseResponse` |
| 8 | emailClient | POST | `getClientMasterPagination` | DataTables server-side body `{draw, start, length, order, columns[]}` | `Page<ClientMasterDto>` `{data[], recordsFiltered, recordsTotal, draw}` → DataTable |
| 9 | emailClient | POST | `getClientGroup` | – | `List<ClientGroupDto>` → `#groupCode` select options |
| 10 | emailClient | POST | `emailClientUpload` | multipart: `file`, `groupCode` | `BaseResponse` → swal |
| 11 | EmailExtractor | POST | `getEmailExtractor` | – | `List<EmailExtractor>` → DataTable (startDate + status progress bar) |
| 12 | EmailExtractor | POST | `emailExtractor` | multipart: `file` | `BaseResponse` — **no success handler; the page reloads immediately** |
| 13 | EmailExtractor | form POST | `downloadFile` | `fromDate`, `toDate` | `ResponseEntity<Resource>` — a **file download** via `target="_blank"`, not AJAX |
| 14 | EmailInbox | POST | `getEmailMaster` | – | `List<EmailMaster>` → `#fromEmail` select (value = `emailMasterId`) |
| 15 | EmailInbox | GET | `getEmailInbox` | `emailMasterId`, `fromDate`, `toDate` | `List<EmailDto>` → DataTable (from, subject, action). `fromDate`/`toDate` are **always `undefined`** (their inputs are commented out) |
| 16 | EmailMaster | POST | `getEmailMaster` | – | `List<EmailMaster>` → DataTable |
| 17 | EmailMaster | POST | `saveEmail` | JSON `{email, protocol, host, username, port, auth, enable, password}` | `BaseResponse` → swal. ⚠️ `username` is always `undefined` (no field in the form) |
| 18 | EmailMaster | GET | `removeEmail` | `emailMasterId` | `BaseResponse` |
| 19 | EmailMaster | GET | `getEmailMasterById` | `emailMasterId` | `EmailMaster` — **the UI calls a non-existent `updateEmail()` function instead** |
| 20 | EmailSend | POST | `getEmailMaster` | – | `List<EmailMaster>` → `#fromEmail` select (value = email) |
| 21 | EmailSend | POST | `getClientMaster` | – | `ClientMasterResponse` `{clientMasterDtoList[]}` → `#client` select |
| 22 | EmailSend | POST | `getClientGroup` | – | `List<ClientGroupDto>` → `#group` select |
| 23 | EmailSend | POST | `getEmailTemplateApprove` | – | `List<EmailTemplate>` → `#template` select (approved only) |
| 24 | EmailSend | GET | `getTemplate` | `emailTemplateId` | `EmailTemplate` → `#previewData` live preview |
| 25 | EmailSend | POST | `sendEmail` | JSON `{fromEmail, emailType, group, client, template}` | `BaseResponse` — **the page calls `location.reload()` immediately, so the response is never seen** |
| 26 | EmailSend | POST | `getEmailSendJobs` | – | `List<EmailSendJob>` → DataTable |
| 27 | EmailTemplate | POST | `getEmailTemplate` | – | `List<EmailTemplate>` → DataTable |
| 28 | EmailTemplate | GET | `getTemplate` | `emailTemplateId` | `EmailTemplate` → `#previewData` in `#previewModal` |
| 29 | EmailTemplate | GET | `removeTemplate` | `emailTemplateId` | `BaseResponse` |
| 30 | EmailTemplate | POST | `saveEmailTemplate` | JSON `{templateName, templateContent}` | `BaseResponse` |
| 31 | EmailTemplateApprove | POST | `getEmailTemplateApproveList` | – | `List<EmailTemplate>` → DataTable |
| 32 | EmailTemplateApprove | GET | `getTemplate` | `emailTemplateId` | `EmailTemplate` → preview modal |
| 33 | EmailTemplateApprove | GET | `approveTemplate` | `emailTemplateId` | `BaseResponse` |
| 34 | EmailTemplateApprove | POST | `saveEmailTemplate` | JSON `{templateName, templateContent}` | `BaseResponse` — **dead code, no form on this page** |
| 35 | GmailTemplate | POST | `getAllGmail` | – | `List<GmailMaster>` → `#fromEmail` select |
| 36 | GmailTemplate | GET | `drafts` | `email` | `Map<String,String>` (draftId → subject) → `#template` select |
| 37 | GmailTemplate | GET | `/last-24-hours/live` | `fromEmail` | `{fromEmail, emailsSentInLast24Hours, limitEstimate, remainingQuotaEstimate}` → quota card |
| 38 | GmailTemplate | GET | `getScheduledEmail` | `fromEmail` | `List<EmailQueue>` → DataTable (fromEmail, toEmail, domain, scheduledTime, status, errorMessage) |
| 39 | GmailTemplate | POST | `send` | multipart: `fromEmail`, `template`, `date`, `file` | plain-text response → raw `alert()` |

**Notes on the AJAX layer**
- Every `$.ajax` has an **empty `error:` handler** (`function(data) {}`) → all failures are silent (no toast, no console output in most cases). The only exception is `emailClient.html`'s upload, which `console.log`s the response.
- 15 of 39 calls are `GET` for **state-changing operations** (`createClientGroup`, `removeClientGroup`, `removeClient`, `removeEmail`, `removeTemplate`, `approveTemplate`) → CSRF-protected only by the custom header, and cached/prefetchable/bookmarkable. `GET` with side effects is the wrong verb; it also means a link-prefetch or a browser retry can delete data.
- 20 calls use `contentType: 'application/json; charset=utf-8'` with a `data: {k: v}` object, which jQuery serialises as `k=v` form-urlencoded — the declared content type is a lie. It works only because the server binds `@RequestParam`.
- `EmailSend.html` and `GmailTemplate.html` use `location.reload()` immediately after firing the request, aborting the in-flight XHR in most browsers.
- No request deduplication, no loading state on the Send/Upload buttons (the `.loader` overlay is only used on `emailClient.html` and is `display:none` by default).

## B4. Form fields and validation

### `login.html` — `POST /login`
| Field | Type | Required | Pattern / other | Notes |
|---|---|---|---|---|
| `username` | text | ✅ `required="required"` | none (placeholder "Enter a valid username" is decorative) | |
| `password` | password | ✅ `required="required"` | none | |
| `_csrf` | hidden | — | `${_csrf.parameterName}` / `${_csrf.token}` | |

No client-side validation beyond `required`. No autocomplete attributes, no `minlength`.

### `Header.html` — `POST /logout`
`logoutForm` (hidden): `_csrf` hidden + hidden submit. Triggered by `href="javascript: document.logoutForm.submit()"`.

### `ClientGroup.html` — `form#formFile` → `GET createClientGroup`
| Field | Type | Required | Validation |
|---|---|---|---|
| `groupCode` | text | ❌ **no `required`** (label has `class="required"` — CSS only) | `maxlength="3"` only |
| `groupDesc` | text | ❌ **no `required`** | `maxlength="255"` |

`onsubmit="createGroup(event)"` → `e.preventDefault()` then AJAX. **Zero client-side validation**; empty group code and empty description are both accepted.

### `emailClient.html` — `form#formFile` → multipart `POST emailClientUpload`
| Field | Type | Required | Validation |
|---|---|---|---|
| `file` | file | ❌ **no `required`** despite the `.required` label | none — no `accept` attribute, no size check; the file is parsed as a spreadsheet server-side |
| `groupCode` | select | ✅ `required` | populated from `getClientGroup` |

No file-type / file-size / row-count validation client-side.

### `EmailExtractor.html`
`form#formFile` → multipart `POST emailExtractor`:
| Field | Type | Required | Validation |
|---|---|---|---|
| `file` | file | ❌ | none |

`form th:action="@{/downloadFile}"`:
| Field | Type | Required | Validation |
|---|---|---|---|
| `fromDate` | date | ✅ `required="required"` | browser date format only |
| `toDate` | date | ✅ `required="required"` | **no `fromDate <= toDate` check** — a reversed range silently returns nothing |

### `EmailInbox.html` — `form#emailInboxForm` (method=post, **no action**)
| Field | Type | Required | Validation |
|---|---|---|---|
| `fromEmail` | select | ✅ `required` | populated from `getEmailMaster` |
| `fromDate` | date | — | **commented out in the HTML** but still read by JS → `undefined` is sent |
| `toDate` | date | — | same |

The form has **no `action`**; the search button is `type="button"` with `onclick="emailSearch()"`.

### `EmailMaster.html` — `form#formFile` → `POST saveEmail`
| Field | Type | Required | Validation | Notes |
|---|---|---|---|---|
| `email` | **email** | ✅ `required` | HTML5 email format | ⚠️ `name="eamil"` — **typo**, but the JS reads by `#id` so it works |
| `protocol` | select | ✅ `required` | options: only `smtp` | |
| `host` | select | ✅ `required` | `smtp.gmail.com` / `smtp-mail.outlook.com` / `smtp.hostinger.com` | hardcoded, not a free-text host |
| `port` | select | ✅ `required` | only `587` | 465/25 impossible |
| `password` | **text** | ✅ `required` | none | 🔴 **`type="text"` — the SMTP password is displayed in cleartext in the browser** |
| `auth` | select | ✅ `required` | `true` only | |
| `enable` | select | ✅ `required` | `true` only | |

**No `username` field** — the JS reads `$("#username").val()` → `undefined`. No "show/hide password" toggle, no password strength, no re-type confirmation.

### `EmailSend.html` — `form#emailSendForm` (method=post, **no action**)
| Field | Type | Required | Validation |
|---|---|---|---|
| `fromEmail` | select | ✅ `required` | from `getEmailMaster` |
| `emailType` | select | ❌ (marked `*` in the label only) | `G` / `I` |
| `group` | select | ❌ | toggled visible/disabled by `emailType` |
| `client` | select `multiple` | ❌ | toggled by `emailType`; **no minimum-selection check** |
| `template` | select | ❌ | from `getEmailTemplateApprove` (approved only) |

The Send button is `type="button"` → HTML5 `required` **never fires**; `emailType`/`template` can be empty and the request still goes out.

### `EmailTemplate.html` — `form action=""` (no method, no `onsubmit`)
| Field | Type | Required | Validation |
|---|---|---|---|
| `TemplateName` | text | ❌ **no `name` attribute** | none — an empty template name is accepted |
| `Templatecontent` | textarea + CKEditor 5 | ❌ | none — no length limit, no HTML sanitisation, no required-subject check |

Submit is `type="button" onclick="emailTemplate()"`; the Cancel button is `type="reset" onclick="window.location.reload()"`.
The `form action=""` with no method means a native submit would **GET the current page** — but the submit button is not a submit button, so it never fires.

### `EmailTemplateApprove.html`
No form. The `emailTemplate()` function and the CKEditor bootstrap are **copy-paste dead code**.

### `GmailTemplate.html` — `form#emailSendForm` (multipart, `onsubmit="emailSend(event)"`)
| Field | Type | Required | Validation |
|---|---|---|---|
| `fromEmail` | select | ✅ `required` | `data-live-search="true"`, from `getAllGmail` |
| `template` | select | ❌ | from `drafts` (Gmail drafts, not `email_template`) |
| `date` | datetime-local | ❌ | no min/past constraint — a past date is accepted |
| `file` | file | ❌ | recipient list; no `accept`/size check |

The submit button **is** `type="submit"` here, so native validation applies — but only to `fromEmail`.

### Validation helpers present but unused
`/assets/js/validate.js` ("Nirmalay Labs Validation javascript File on 14January2019") is loaded on every page and exports `blankValidation(fieldId, fieldType, message)` (+ variants) using `swal`. **It is never invoked in any template.**

## B5. Third-party libraries and versions

| Library | Version | Source | Loaded on |
|---|---|---|---|
| jQuery | **3.6.0** | local `/assets/js/jquery-3.6.0.min.js` | all pages |
| jQuery Migrate | **3.0.0** | CDN `code.jquery.com` | all pages |
| jQuery UI | **1.11.2** (+ `smoothness` theme) | CDN `ajax.googleapis.com` over **plain HTTP** | all pages |
| Bootstrap | **4.0.0** | local `/assets/bootstrap/` (css + js + glyphicons fonts) | all pages |
| Popper.js | **1.x** (2017, Federico Zivolo) | local `popper.min.js` | all pages |
| Bootstrap-Select | **1.13.1** | local | all pages |
| Font Awesome | **4.7.0** | local `assets/font-awesome-4.7.0/` | all pages |
| DataTables | **1.10.22** (`jquery.dataTables.js`) **AND 1.10.25** (`datatables.min.js`) | local | both on all pages — **two different DataTables versions loaded simultaneously** |
| DataTables CSS | 1.10.25 (`datatables.min.css`) + `jquery.dataTables.css` | local | all pages |
| Chart.js | **2.9.4** | CDN `cdnjs.cloudflare.com` | `NewDashBoard.html` only |
| CKEditor 5 | **36.0.0** (`classic` build) | CDN `cdn.ckeditor.com` | 8 templates (loaded on `ClientGroup`, `emailClient`, `EmailExtractor`, `EmailInbox`, `EmailMaster`, `EmailSend`, `EmailTemplate`, `EmailTemplateApprove`, `GmailTemplate`) — most don't use it |
| SweetAlert2 | **7.33.1** | local `sweetalert2.all.min.js` | all pages |
| SweetAlert (legacy) | **1.1.3** | CDN `cdnjs.cloudflare.com` | all pages — **dead since 2017, superseded by SweetAlert2 which is also loaded** |
| Moment.js | **2.29.2** | CDN | all pages — **never used** |
| PDF.js | **0.8.49** (2016) | local `pdf.js` | all pages — **known vulnerable (CVE-2018-4990 arbitrary JS execution via crafted PDF)** |
| bootstrap-pdf-viewer | unversioned | local | all pages |
| jQuery ScannerDetection | unversioned | local | all pages — **never used** |
| SlimSelect | unversioned | local `slimselect.min.js` | **not referenced by any template** — dead file |
| GSAP TweenMax | unversioned | `assets/dashboard/js/` | **not referenced** — dead file |
| `site-examples.css` | Gridism + **YUI 3.14.0** (2013) | local, 70 KB | all pages — dead CSS |
| `css/css/style.css` | "BizPage" (BootstrapMade) | local | **not referenced** — dead file |
| `assets/dashboard/css/reset.min.css`, `dashboard/font/font.css` | – | local | **not referenced** |

**Not loaded but present:** `assets/sample.doc`, `assets/sample.pdf`.

**CDN dependency risk:** every CDN asset is a runtime internet dependency with no SRI attribute on the jQuery UI / jQuery Migrate / SweetAlert / Moment / Chart.js includes. `Header.html` comments out an SRI-protected `scannerdetection` include, so the project knows the pattern but does not apply it.

## B6. UX gaps and broken links

### Hard breakage (JS errors / dead handlers)
| # | Issue | Location |
|---|---|---|
| 1 | **`updateEmail()` is called but never defined** — the yellow edit button on every SMTP account row throws `ReferenceError: updateEmail is not defined`. The corresponding `getEmailMasterById` endpoint exists in the controller but is never reachable. | `EmailMaster.html:314` |
| 2 | **`ClassicEditor.create(document.querySelector('#Templatecontent'))` on a page with no `#Templatecontent`** → `ClassicEditor.create(null)` throws on every page load of the approval page. | `EmailTemplateApprove.html:311-318` |
| 3 | **`location.reload()` immediately after `$.ajax`** — aborts the in-flight request. Affects "Send" (`EmailSend.html:535`) and the extractor upload (`EmailExtractor.html:346`). The user never sees success/failure and the table reloads from stale state. | `EmailSend.html`, `EmailExtractor.html` |
| 4 | **The "Send" button is `type="button"`, not `submit`** → HTML5 `required` on `fromEmail` is never enforced, and `emailType`/`template` can be empty. | `EmailSend.html:186` |
| 5 | **`#username` does not exist in the SMTP form** → `data['username'] = undefined` is sent; the server then overwrites `username` with the email anyway. Silent data loss. | `EmailMaster.html:423` |
| 6 | **`fromDate`/`toDate` inputs are commented out but the JS still sends them** → `undefined` query params on every inbox search. | `EmailInbox.html:69-82, 285-286` |
| 7 | **4 templates referenced by controllers do not exist**: `layout.html`, `MainDashBoard.html`, `SessionExp.html`, `accessdenied.html` → `ThymeleafException` on `/`, `MainDashBoard.htm`, `/sessionExpired`, `/accessdenied`. | `LoginController` |
| 8 | **`layout:decorator="layout"` on every page with no `layout.html`** → template resolution error (or silent no-op depending on the Thymeleaf layout dialect in use). | all templates except `Header.html`, `mailtemplate.html` |
| 9 | **Invalid HTML: stray `</li>`** (no matching `<li>`) in `Header.html:125`, `NewDashBoard.html:150`, `EmailSend.html:222`; **unclosed `</div>`** in `GmailTemplate.html:157-158`; **`<meta>` outside `<head>`** in `NewDashBoard.html:14`. | multiple |
| 10 | **HTML-comment syntax inside JavaScript** (`<!--  "#F1F487",-->` in `NewDashBoard.html:359-360`; `<!--var status=…-->` blocks in `GmailTemplate.html:441-447`) — parses only because sloppy-mode scripts allow `<!--` as a line comment. Any move to ES modules, `"use strict"`, or a bundler turns these into syntax errors. | 2 templates |
| 11 | **`#myTable` does not exist** → the global DataTable init in `Header.html:51` is dead code, and every real table's last column remains sortable (it should not be). | `Header.html` |
| 12 | **Dead `href="#"` placeholders** — `Header.html:144` (commented dropdown), `login.html:325,338` (commented carousel "BUY NOW"), `mailtemplate.html:42` (commented logo). | 3 files |
| 13 | **Dead links to other products**: `href="UserCoursePage.htm"` ×3 and `href="/speechToText"`, `href="/merger"` in `Dashboard.html`; `href="taskDashboard"`, `taskAssignDashboard`, `createTask`, `taskReport`, `searchFile`, `changePasswordView` in `Header.html` (all commented out but still present). | |
| 14 | **`Dashboard.html` is orphaned** — no controller returns `"Dashboard"`, and its "Template" tile points at `/email` instead of `/emailTemplate`. | `Dashboard.html:157` |
| 15 | **The `/email` (SMTP Send) page has no nav entry** — it is only reachable from the orphaned `Dashboard.html`. | |
| 16 | **`href="javascript: document.logoutForm.submit()"`** — legacy `javascript:` URL, blocked by some CSPs, and the target form is `th:hidden` so some browsers will not submit a hidden form. | `Header.html:94` |
| 17 | **Content injection via string-concatenated `onclick` handlers** — `EmailInbox.html:305` builds `onclick='preview("` + email body + `")'`; any quote or newline in the subject/body breaks the handler and injects markup. `EmailTemplate.html:236-237` and `ClientGroup.html:219` build the same pattern (lower risk, numeric/3-char IDs). | 3 files |
| 18 | **Inbox preview renders `JSON.stringify(content)`** — the email body is shown JSON-escaped, not as HTML. Users see `\"<div>…\"` instead of the message. | `EmailInbox.html:334` |
| 19 | **`window.location.reload()` used as the cancel button** in `EmailTemplate.html:144` — reloads the page instead of just closing the modal. | |
| 20 | **Raw `alert()` in `GmailTemplate.html:388,393`** while every other page uses SweetAlert2 — inconsistent, and the message includes an emoji (fails on some enterprise browsers/GBK locales). | |
| 21 | **`GmailTemplate.html` "Message" column shows `response[i].domain`** — mislabelled; it is the domain, not the message. | `GmailTemplate.html:453` |
| 22 | **Two DataTables versions (1.10.22 + 1.10.25) and two DataTables CSS files loaded together** → the second overwrites the first's behaviour; column-order/ordering bugs are likely. | `Header.html` |
| 23 | **Both `bootstrap.css` AND `bootstrap.min.css` are linked** (≈230 KB), plus `site-examples.css` (70 KB) and two DataTables stylesheets — ~350 KB of CSS on every page, of which the majority is unused. | `Header.html:18-22` |
| 24 | **All CDN assets over plain HTTP** (`ajax.googleapis.com` for jQuery UI) → mixed-content blocked under HTTPS; jQuery UI 1.11.2 (2013) is EOL with known XSS issues. | `Header.html:37-38` |
| 25 | **CKEditor 5 36.0.0 loads on 9 pages but is used on 1**; **PDF.js 0.8.49** (2016, CVE-2018-4990) and **bootstrap-pdf-viewer** load on every page but are used on none. | `Header.html` |
| 26 | **No error feedback on any AJAX failure** — 38 of 39 `$.ajax` calls have an empty `error:` handler. | all templates |
| 27 | **No empty-state, no pagination controls, no row count, no search box** on any DataTable except the server-side one on `emailClient.html` (which has `searching:false`). | all list pages |
| 28 | **Accessibility:** icon-only buttons rely on `title` attributes; `ClientGroup.html` distinguishes "delete group" (red trash) from "remove clients" (yellow trash) by **colour alone**; the modal `#previewData` is a `<span>` used as a content container; `<th>` elements have no `scope`; no `<label for>` on any input except the visual `label.required`; no visible focus states (login CSS explicitly kills `box-shadow` on focus and sets `outline-width: 0`). | all templates |
| 29 | **`login.html` still ships an active `$(document).ready` that initialises a carousel (`#myCarousel`) that is commented out** → `$('#myCarousel').carousel(...)` on an empty jQuery set is a no-op, but the ~4 KB of minified carousel CSS remains inline. | `login.html:396` |
| 30 | **`mailtemplate.html` contains `${username}` and `${password}` placeholders** in a commented-out block of a *credential-email* template from a different product ("Red Pluto Analytics HR"). If uncommented, it would mail plaintext credentials. | `mailtemplate.html:87` |
| 31 | **No visible "create/manage user" or "change password" flow** (`changePasswordView` is commented out of the nav and no such controller endpoint exists), and the only role is the hardcoded string `'MANG'`. | |
| 32 | **`approveTemplate` and `removeTemplate` have no reject/undo**, and `EmailTemplateApprove.html` shows the *same* "Sent Successfully." swal text used by the delete flows. | |

## B7. Visual style

**Framework:** Bootstrap **4.0.0** (the very first 4.x release, Dec 2018) — grid + components. Font Awesome 4.7.0 for icons. **No other component framework.**

**Theme:** light. The base is Bootstrap's default light theme; the only dark element is the top navbar.

**Palette**
| Token | Value | Where |
|---|---|---|
| Navbar (top bar) | `#484848` charcoal | `Header.html` inline `<style>` overrides `styles.css`'s gradient |
| Navbar hover / active link | `#fbc531` amber | `Header.html` inline |
| App nav (second bar) | Bootstrap default white | `navbar1` |
| Table header | `#A0CFEC` light blue | inline `style="background-color:#A0CFEC"` on every `<thead>` |
| Primary button | `#92c755` green (overrides Bootstrap `#007bff`) | `styles.css` `.btn-primary` |
| Warning button | `#e7b63a` amber | `styles.css` |
| Accent orange | `#EB5406` | inline on the Send / download buttons |
| Accent blue | `#488AC7` | every modal title and form label |
| Accent teal | `#65A5BA` | `Dashboard.html` tile captions |
| Dashboard body bg | `#E4EEF4` | `NewDashBoard.html` inline `style` |
| Login body bg | `#B0BEC5` blue-grey | `login.html` inline CSS |
| Login button | `#1A237E` indigo | `login.html` `.btn-blue` |
| Login footer gradient | `#3A6073` → `#16222A` | `login.html` |
| Card shadow | `2px 2px 10px #DADADA` | `.card-counter` in `NewDashBoard.html` |

**Density: deliberately cramped.** `styles.css` overrides Bootstrap's grid gutters for *every* breakpoint to `padding-top/bottom: 2.5px` (Bootstrap default 15px), sets `.row { max-width: 99% !important; margin-left: 5px; margin-top: 8px; }`, `.bd-navbar { min-height: 1rem; }`, and `.content { margin-left: -129.5px; margin-right: -129.5px; }` (negative margins pulling content under the fixed navbars). Forms use `padding: 10px 12px` with `font-size: 14px` and `letter-spacing: 1px` (login page only). Table rows are the default Bootstrap `table-striped` density.

**Typography:** the default Bootstrap 4 system stack (`-apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial`). `Open Sans` / `Montserrat` are referenced by the **unused** `css/css/style.css` and `styles.css` but no webfont is loaded → they never apply.

**Layout:** a fixed 2-navbar stack (dark top bar + white app bar), then a full-width `.row` → `.col-sm-12` → `.card` → `.card-header` / `.card-body` → a nested `.card` → `.table-responsive-sm` → `.table-bordered .table-striped .table-hover`. Every list page repeats this exact 4-level nesting. Modals are Bootstrap `.modal` with `modal-dialog` / `modal-lg` / `modal-xl` (max-width forced to 1350px via inline `!important`).

**Progress bars as status indicators:** `EmailExtractor.html`, `EmailSend.html` and `GmailTemplate.html` render status as a Bootstrap `.progress` bar — 75 % striped for in-progress, 100 % `bg-success` for `SUCCESS`/`Success`. `GmailTemplate.html` had the same block **commented out** and falls back to raw text, so the two send pages look inconsistent.

**No dark mode. No CSS variables, no theming layer, no design tokens.** Every colour is hardcoded either in `styles.css` or in an inline `style=` attribute inside the template — 30+ inline style blocks across the 14 templates, and the `.loader` and `.filelabel` CSS blocks are **copy-pasted verbatim into 5 and 4 templates respectively**.

**Asset hygiene:** 4 unused JS files (`slimselect.min.js`, `TweenMax.min.js`, `validate.js`, `jquery.scannerdetection.min.js`), 2 unused CSS files (`site-examples.css` 70 KB, `css/css/style.css`), ~20 unused images (`hrms*.jpg`, `hrms_bgcolor.jpg`, `Logo.png`, `logohr.png`, `newLogo.jpg`, `mylogo.jpg`, `scanner.png`, `merger.png`, `sms.png`, `tracking.png`, `Store.png`, `user.jpg`, `barcode.png`, `spinner.gif`, `infinity-loop.gif`, `Listening.gif`, `loading.gif` 1.3 MB), and `sample.doc`/`sample.pdf`.

---

**Not checked / known gaps:** no DDL was available in either input, so the exact per-table/per-column charsets, collations, lengths and existing indexes could not be read — the charset findings in A5.4 are inferred from the JDBC URL, the driver class, and the absence of a migration tool, and must be confirmed with the `information_schema` query given there. I did not decompile the 20+ classes under `BOOT-INF/classes/com/pg/seo/` (the decompiled tree is the same jar, so nothing was missed there), and I did not analyse `scraper/service/AmazonScraperService.java` or the bundled Puppeteer/Node app in `BOOT-INF/classes/puppeteer/` beyond confirming they are present — the Puppeteer side (Amazon product scraping) appears to be dead code, since no entity, repository, or template references it.

**Risks to carry into the rebuild:** (1) the physical schema is camelCase while every new tool defaults to snake_case — a migration must rename or the native queries must be rewritten; (2) `CascadeType.ALL` on `ClientMaster.clientGroup` can delete a whole client group in one call; (3) `gmail_master.refreshToken` and `email_master.password` are plaintext and the DB root password (`elexyt2024`) is baked into the shipped jar — all three need rotation, not just migration; (4) `email_extractor_data` is the highest-volume table with no FK, no index, no unique constraint and no soft delete, so it will be the first thing to break under load.
</task_result>
</task>
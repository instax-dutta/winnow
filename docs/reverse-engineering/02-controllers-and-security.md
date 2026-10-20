<task id="ses_eaab342e7ffe7hS4LFlis2pQom" state="completed">
<task_result>
# Decompiled Analysis — `com.pg.seo` (seo-0.1.0 reference)

Source: `/Users/saiduttaabhishekdash/seo-0.1.0/reference/decompiled/com/pg/seo/` (CFR 0.152, Spring Boot 3.3.0, JDK 17).
Supporting facts from `/Users/saiduttaabhishekdash/seo-0.1.0/SECRETS.txt` (harvest of `application.properties` + `emailsend21.sql` + shipped templates) are marked **[CFG]**.

---

## 0. De-obfuscated `SecurityConfiguration.filterChain` (the master key for everything else)

`cfg/SecurityConfiguration.java:62` (CFR flattened the builder chain; reconstructed):

```java
http.authorizeHttpRequests()
    .requestMatchers("/resources/**", "/webjars/**", "/assets/**", "/static/assets/**").permitAll()
    .requestMatchers("/").hasRole("USER")                       // -> authority "ROLE_USER"
    .requestMatchers("/static/assets/css/**").permitAll()
    .requestMatchers("/static/assets/js/**").permitAll()
    .requestMatchers("/resources/**").permitAll()                // duplicate
    .requestMatchers("/api/v1/getBarcode4j").permitAll()         // NO SUCH ENDPOINT
    .requestMatchers("/word").permitAll()                        // NO SUCH ENDPOINT
    .anyRequest().authenticated()
    .and()
    .formLogin().loginPage("/login")
        .defaultSuccessUrl("/welcome")                           // overridden by successHandler
        .failureUrl("/login?error")
        .permitAll()
        .usernameParameter("username")
        .passwordParameter("password")
        .successHandler(successHandler)                          // LoginSuccessHandler
    .and()
    .logout().logoutSuccessUrl("/login?logout")                  // default LogoutFilter; CustomLogoutHandler NOT wired
    .and()
    .exceptionHandling().accessDeniedPage("/accessdenied")
    .and()
    .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        .invalidSessionUrl("/login")
        .maximumSessions(1);
```

**Security rule legend used in the tables below**
| Rule | Applies to |
|---|---|
| `permitAll` | `GET /login`, `POST /login` (form-login processing URL, `loginPage().permitAll()`), `/resources/**`, `/webjars/**`, `/assets/**`, `/static/assets/**`, `/static/assets/css/**`, `/static/assets/js/**`, `/api/v1/getBarcode4j` (no controller), `/word` (no controller) |
| `hasRole("USER")` | `GET /` **only** → requires authority `ROLE_USER` |
| `authenticated` | every other endpoint (48 − 11 above) |
| CSRF | **enabled** (Spring default; no `.csrf()` call anywhere in the codebase) |
| Headers | **no** `headers()` config → Spring Security 6 defaults apply: `Cache-Control`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `HSTS` (https only), `X-XSS-Protection: 0` |

---

## 1. COMPLETE ENDPOINT TABLE (48 mappings, 3 controllers + 1 login controller)

### 1.1 `com.pg.seo.login.LoginController` (5 mappings, 6 paths)

| Method | Path | Request params | Return | Template | Security rule |
|---|---|---|---|---|---|
| GET | `/login` | `error`, `logout` (query, both optional) | `String` | `login` | `permitAll` |
| GET | `/`, `/welcome` | – | `String` | `NewDashBoard` | `/` → `hasRole("USER")`; `/welcome` → `authenticated` |
| GET | `MainDashBoard.htm` (no leading `/`) | – | `String` | `MainDashBoard` | `authenticated` |
| GET | `/sessionExpired`, `/invalidSession` | – | `String` | `SessionExp` | `authenticated` |
| GET | `/accessdenied` | – | `String` | `accessdenied` | `authenticated` (but reachable anonymously via FORWARD dispatch from `ExceptionTranslationFilter`, see §6.12) |

`login` handler body:
```java
if (error != null)  model.addAttribute("error", "Your username and password is invalid.");
if (logout != null) model.addAttribute("message", "You have been logged out successfully.");
return "login";
```
Both are **presence**-based (`?error` with an empty value still shows the error).

### 1.2 `com.pg.seo.controller.EmailController` (36 mappings)

| Method | Path | Params / Body | Return | Template | Security rule |
|---|---|---|---|---|---|
| GET | `/email` | `error`,`logout` (unused) | `String` | `EmailSend` | `authenticated` |
| POST | `/getClientGroup` | session `loginId` | `List<ClientGroupDto>` (JSON) | – | `authenticated` |
| POST | `/getClientMaster` | session `loginId` | `ClientMasterResponse` (JSON) | – | `authenticated` |
| POST | `/getEmailMaster` | session `loginId` | `List<EmailMaster>` (JSON, **includes `password`**) | – | `authenticated` |
| GET | `/emailTemplate` | – | `String` | `EmailTemplate` | `authenticated` |
| POST | `/getEmailTemplate` | session `loginId` | `List<EmailTemplate>` | – | `authenticated` |
| POST | `/getEmailTemplateApprove` | session `loginId` | `List<EmailTemplate>` filtered `status.equalsIgnoreCase("A")` | – | `authenticated` |
| POST | `/getEmailTemplateApproveList` | – | `List<EmailTemplate>` — **`findAll()` over ALL tenants**, filtered `status="N"` | – | `authenticated` |
| GET | `/getTemplate` | `emailTemplateId` (Long, req) | `EmailTemplate` (JSON) | – | `authenticated` — **not tenant scoped** |
| POST | `/saveEmailTemplate` | JSON `EmailTemplateRequest{templateName, templateContent}` | `BaseResponse` | – | `authenticated` |
| GET | `/removeTemplate` | `emailTemplateId` (Long, req) | `BaseResponse` | – | `authenticated` — **state change over GET** |
| POST | `/sendEmail` | JSON `EmailSendRequest{fromEmail, emailType, group, client[], template}` | `BaseResponse` | – | `authenticated` |
| POST | `/emailClientUpload` | multipart `file`, `groupCode` | `BaseResponse` | – | `authenticated` |
| GET | `/emailClient` | – | `String` | `emailClient` | `authenticated` |
| POST | `/getClientMasterPagination` | JSON `PagingRequest{start,length,draw}` | `Page<ClientMasterDto>` | – | `authenticated` |
| GET | `/clientGroup` | – | `String` | `ClientGroup` | `authenticated` |
| GET | `/createClientGroup` | `groupCode`, `groupDesc` (req) | `BaseResponse` | – | `authenticated` — **state change over GET** |
| GET | `/removeClientGroup` | `groupCode` (req) | `BaseResponse` | – | `authenticated` — **state change over GET** |
| GET | `/removeClient` | `groupCode` (req) | `BaseResponse` | – | `authenticated` — **state change over GET** |
| GET | `/getEmailInbox` | `emailMasterId`, `fromDate`, `toDate` (req) | `List<EmailDto>` (JSON) | – | `authenticated` — **not tenant scoped; `fromDate`/`toDate` ignored** |
| GET | `/emailInbox` | – | `String` | `EmailInbox` | `authenticated` |
| POST | `/getEmailSendJobs` | session `loginId` | `List<EmailSendJob>` | – | `authenticated` |
| GET | `/taskReports` | `month` (int, req), `filterType` (String, req) | `List<Long>` = **hard-coded** `[1000,3000,4000,2000,500]` | – | `authenticated` — stub |
| GET | `/emailMaster` | – | `String` | `EmailMaster` | `authenticated` |
| POST | `/saveEmail` | JSON `EmailMasterDto{emailMasterId,email,protocol,host,username,port,auth,enable,password}` | `BaseResponse` | – | `authenticated` |
| GET | `/getEmailMasterById` | `emailMasterId` (Long, req) | `EmailMaster` (JSON, **includes plaintext SMTP `password`**) | – | `authenticated` — **not tenant scoped** |
| GET | `/removeEmail` | `emailMasterId` (Long, req) | `BaseResponse` | – | `authenticated` — **state change over GET** |
| GET | `/getDashboardDbReport` | session `loginId` | `DashboardDbReport{emailCount,groupCount,clientCount,templateCount}` | – | `authenticated` |
| GET | `/approveTemplate` | `emailTemplateId` (Long, req) | `BaseResponse` | – | `authenticated` — **state change over GET, not tenant scoped** |
| GET | `/approveTemplates` | – | `String` | `EmailTemplateApprove` | `authenticated` |
| GET | `/emailExtractor` | – | `String` + model `fromDate`,`toDate` (both = today, `yyyy-MM-dd`) | `EmailExtractor` | `authenticated` |
| POST | `/emailExtractor` | multipart `file` | `BaseResponse` (status `"result"` / `"FAILUER"`) | – | `authenticated` |
| POST | `/getEmailExtractor` | session `loginId` | `List<EmailExtractor>` (desc by id) | – | `authenticated` |
| POST | `/downloadFile` | `fromDate`, `toDate` (req) | `ResponseEntity<Resource>` (`application/octet-stream`) | – | `authenticated` — **ignores `loginId`** |
| GET | `/getMonthlyReport` | `month` (int, req) | `List<Long>` `[emailSendJobCount, extractCount]` | – | `authenticated` |
| GET | `/getMonthlyEmailReport` | `month` (int, req) | `List<Long>` `[sent, success, failed]` | – | `authenticated` |

### 1.3 `com.pg.seo.controller.NewEmailController` (6 mappings)

| Method | Path | Params / Body | Return | Template | Security rule |
|---|---|---|---|---|---|
| GET | `/drafts` | `email` (String, **not `@RequestParam`-annotated → binds by name**) | `Map<String,String>` draftId→subject | – | `authenticated` — **no tenant check** |
| POST | `/getAllGmail` | `email` (unused) | `List<GmailMaster>` — **`findAll()`, includes `clientId`,`secret`,`refreshToken`,`accessToken`** | – | `authenticated` |
| POST | `/send` | `fromEmail`, `template`, `date` (ISO `LocalDateTime`), multipart `file` | `String` `"Emails queued for sending."` | – | `authenticated` — **no tenant check** |
| GET | `/gmailTemplate` | – | `String` | `GmailTemplate` | `authenticated` |
| GET | `/last-24-hours/live` | `fromEmail` | `Map<String,Object>` `{fromEmail, emailsSentInLast24Hours, limitEstimate, remainingQuotaEstimate}` | – | `authenticated` |
| GET | `/getScheduledEmail` | `fromEmail` | `List<EmailQueue>` (today only, desc by scheduledTime) | – | `authenticated` — **no tenant check** |
| — | *(no HTTP mapping)* | — | `@Scheduled(fixedRate=60000) processScheduledEmails()` | — | internal, every 60 s |

### 1.4 `com.pg.seo.controller.EmailSendController` (1 mapping)

| Method | Path | Params / Body | Return | Template | Security rule |
|---|---|---|---|---|---|
| POST | `/sendData` | multipart `file`, `scriptType` | `String` (raw response body of the Google Apps Script call, `text/plain`) | – | `authenticated` |

---

## 2. AUTHENTICATION FLOW (exact)

### 2.1 `UserDetailsService` — `login/LoginDetailsServiceImpl.java`

```java
@Service
public class LoginDetailsServiceImpl implements UserDetailsService {
    @Autowired private Environment env;
    @Autowired private LoginRepository loginRepository;

    @Transactional(readOnly=false)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
            byte[] mac = networkInterface.getHardwareAddress();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < mac.length; ++i) {
                stringBuilder.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
            }
            System.out.print("MAC address  : " + stringBuilder.toString());
            if (!Objects.requireNonNull(this.env.getProperty("mac")).equalsIgnoreCase(stringBuilder.toString())) {
                throw new UsernameNotFoundException("License expired license renewal contact administrator");
            }
            DecodedJWT jwt = JWT.decode((String)Objects.requireNonNull(this.env.getProperty("license")));
            if (jwt.getExpiresAt().before(new Date())) {
                throw new UsernameNotFoundException("License expired license renewal contact administrator");
            }
        }
        catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) { ... }
        catch (ExpiredJwtException | UnknownHostException ex) { ... }
        catch (SocketException e) { throw new RuntimeException(e); }

        Login login = this.loginRepository.findByUsername(username);
        if (login != null && login.getIsActive() == 1) {
            HashSet<SimpleGrantedAuthority> grantedAuthorities = new HashSet<>();
            for (Role role : login.getRoles())
                grantedAuthorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            return new User(login.getUsername(), login.getPassword(), grantedAuthorities);
        }
        throw new UsernameNotFoundException("username not found");
    }
}
```

Order of operations, per login attempt:
1. **MAC check first** (see §4) → mismatch ⇒ `UsernameNotFoundException`.
2. **Licence JWT decode** (see §4) → expired ⇒ `UsernameNotFoundException`.
3. **DB lookup** `loginRepository.findByUsername(username)` → derived query, parametrised (no SQL injection).
4. **Active check**: `login.getIsActive() == 1` (column `login.IsActive`, `int`). Inactive ⇒ `UsernameNotFoundException("username not found")`.
5. **Authorities**: `new SimpleGrantedAuthority(role.getRoleName())` — the raw DB string, e.g. `ROLE_ADMIN`. Note: `SimpleGrantedAuthority` does **not** add a `ROLE_` prefix, and the DB value already contains `ROLE_`.
6. Returns `org.springframework.security.core.userdetails.User(username, password, authorities)` where `password` is the **bcrypt hash** from the DB.

All failures surface to the user as the same message: `UsernameNotFoundException` → `BadCredentialsException` → `failureUrl("/login?error")` → template shows *"Your username and password is invalid."* The MAC/licence error text is never shown (it is swallowed by the exception translator).

### 2.2 bcrypt

- Bean: `cfg/WebMvcConfigruation.java:30-33`
  ```java
  @Bean
  public BCryptPasswordEncoder passwordencoder() { return new BCryptPasswordEncoder(); }   // default strength 10
  ```
- `cfg/SecurityConfiguration.java:66-68` has a **second, non-bean** copy (missing `@Bean` → dead):
  ```java
  public PasswordEncoder passwordencoder() { return new BCryptPasswordEncoder(); }
  ```
- `login/LoginServiceImpl.java:42` (the only encoder call in the app):
  ```java
  login.setPassword(this.bCryptPasswordEncoder.encode((CharSequence)login.getPassword()));
  ```
  → strength 10. **[CFG]** The seeded hashes are `$2y$12$…` / `$2a$12$…` (strength **12**), and two accounts share the *same* hash - the hash of the first operator account also
  authenticates the administrator account (both values are in `SECRETS.txt`). Spring's `BCryptPasswordEncoder.matches()` accepts `$2a$/$2b$/$2y$` and any cost, so the 12-cost hashes still verify; only *new* users get cost 10.
- `DaoAuthenticationProvider` picks up the single `BCryptPasswordEncoder` bean; the `PasswordEncoder` interface bean does not exist (the `SecurityConfiguration` method is not annotated), but the concrete-type bean satisfies the provider.

### 2.3 Session policy / max sessions / fixation

- `SessionCreationPolicy.IF_REQUIRED` — a session is created only when needed (always, for form login).
- `maximumSessions(1)` — **one concurrent session per user**; the new login wins, the old session is expired (`maxSessionsPreventsLogin` default `false`). The expired session is redirected to `/login?error` (no `expiredUrl` configured).
- ⚠️ **No `SessionRegistry` bean and no `HttpSessionEventPublisher` bean exist anywhere** (grep confirms zero hits). Spring creates a private `SessionRegistryImpl`, but without `HttpSessionEventPublisher` expired/destroyed sessions are never removed from the registry → the registry grows and users get spuriously "logged out" (max-session exceeded) over time. This is a rebuild must-fix.
- **Session fixation: mitigated by default.** `sessionFixation()` is never overridden → Spring's default `migrateSession` (calls `HttpServletRequest.changeSessionId()`) applies at login.
- `invalidSessionUrl("/login")` — a request bearing an unknown `JSESSIONID` is redirected to `/login`.
- `exceptionHandling().accessDeniedPage("/accessdenied")` — a forward to `/accessdenied` (a GET, exempt from CSRF).

### 2.4 CSRF configuration

- **CSRF is NOT disabled.** There is no `.csrf(...)` call in any file (grep over all 67 classes: zero hits for `csrf`/`Csrf`). Spring Security's default applies: `CookieCsrfTokenRepository` → `XSRF-TOKEN` cookie (readable by JS) + `X-CSRF-TOKEN` request header, plus the `_csrf` request parameter for forms.
- Consequence for the rebuild: **every** `@ResponseBody` POST (`/saveEmailTemplate`, `/sendEmail`, `/saveEmail`, `/getClientMasterPagination`, `/getClientGroup`, …), the multipart POSTs (`/emailClientUpload`, `/emailExtractor`, `/sendData`, `/send`), and the `POST /login` form submission will return **403** unless the client sends the token. The shipped `login.html` must contain the `_csrf` hidden field (templates are not in the reference tree — verify against the JAR).
- **6 endpoints perform state changes over GET** → these are *exempt* from CSRF protection (Spring only checks non-safe methods) and are therefore trivially CSRF-able via `<img src=...>`: `/removeTemplate`, `/createClientGroup`, `/removeClientGroup`, `/removeClient`, `/removeEmail`, `/approveTemplate`.

### 2.5 Logout behaviour

- Configured: `.logout().logoutSuccessUrl("/login?logout")`. Default `LogoutFilter` → invalidates the HTTP session, clears the `SecurityContext`, clears remember-me, redirects to `/login?logout`; the controller then sets `model.addAttribute("message", "You have been logged out successfully.")`.
- **Logout is POST-only by default** in Spring Security 6 (`AntPathRequestMatcher("/logout","POST")`) and CSRF-protected → the UI must POST a form with the CSRF token.
- `login/CustomLogoutHandler.java` exists and is registered as a bean but is **never wired** into `.logout()`:
  ```java
  public class CustomLogoutHandler implements LogoutHandler {
      public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
          HttpSession ses = request.getSession();     // does nothing at all
      }
  }
  ```
  `SecurityConfiguration.java:70-73`:
  ```java
  @Bean
  public LogoutHandler logoutSuccessHandler() { return new CustomLogoutHandler(); }
  ```
  → **dead bean**. Also `login/LogoutController.java` is an **empty class with no annotations** → contributes zero endpoints.
- The `loginId` / `Username` session attributes are **not** explicitly invalidated on logout; they die with the invalidated session.

### 2.6 Success handler — `login/LoginSuccessHandler.java`

```java
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Autowired LoginServiceImpl loginService;          // unused
    @Autowired LoginRepository loginRepository;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        HttpSession ses = request.getSession();
        Login login = new Login();
        login.setUsername(request.getParameter("username"));
        login.setPassword(request.getParameter("password"));   // raw password onto a transient entity (dead)
        Object token = null;                                     // dead
        try {
            login = this.loginRepository.findByUsername(request.getParameter("username"));
            ses.setAttribute("loginId", (Object)login.getLoginId());
            ses.setAttribute("Username", (Object)request.getParameter("username"));
        } catch (Exception e) { e.printStackTrace(); }
        response.sendRedirect("/welcome");
    }
}
```

- **Redirect target: `/welcome`** (hard-coded, `302`). `defaultSuccessUrl("/welcome")` is redundant/ignored because a custom `successHandler` is set.
- **Session attributes set: exactly two** — `loginId` (`Long`, the tenant key used by *every* controller) and `Username` (`String`).
- It is **not** `SavedRequestAwareAuthenticationSuccessHandler` → the user is always dumped on `/welcome`, never back on the URL they originally requested.
- The username is taken from the **raw request parameter**, not `authentication.getName()`. Same value in practice (both take the first `username` form field), but the session identity is not derived from the `Authentication` object.
- If `findByUsername` returns null → NPE → caught → `printStackTrace()` → the session has **no `loginId`** → every tenant-scoped query in the app silently returns empty/NULL-scoped data. Authentication itself has already succeeded at this point.

---

## 3. AUTHORIZATION MODEL

### 3.1 Roles that exist

`[CFG]` `role_security` seed: `(1,'ROLE_ADMIN'), (2,'ROLE_EMP'), (3,'ROLE_MANG'), (4,'ROLE_HR')` — table `role_security`, entity `login/Role.java` (`@Table(name="role_security")`, `@ManyToMany(mappedBy="roles")`).

`login/Login.java`:
```java
@Entity @Table(name="login")
public class Login {
    @Id @GeneratedValue(strategy=GenerationType.AUTO) public Long getLoginId() { ... }
    private String Username, Password, EmpId; private int IsActive;
    private String CreatedBy, CreatedDate, ModifiedBy, ModifiedDate;
    @ManyToMany
    @JoinTable(name="login_role_security",
               joinColumns=@JoinColumn(name="LoginId"),
               inverseJoinColumns=@JoinColumn(name="RoleId"))
    public Set<Role> getRoles() { ... }
}
```
Join table: `login_role_security(LoginId, RoleId)`.

⚠️ **The grant is `new SimpleGrantedAuthority(role.getRoleName())`** — the DB string verbatim. So the granted authorities are `ROLE_ADMIN`, `ROLE_EMP`, `ROLE_MANG`, `ROLE_HR`.

### 3.2 Which endpoints require which role

**Exactly one endpoint in the entire application has a role requirement:**

```java
.requestMatchers("/").hasRole("USER")     // needs authority "ROLE_USER"
```

- `hasRole("USER")` → `hasAuthority("ROLE_USER")`.
- **No seeded role is `ROLE_USER`.** Therefore `GET /` returns 403 → `/accessdenied` for **every** seeded user. The app is still usable because `LoginController` maps `"/", "/welcome"` to the *same* method, and the success handler redirects to `/welcome`, which is only `authenticated`. (If you seed a role literally named `USER`, `hasRole` yields `ROLE_USER` and `/` opens up.)
- `ROLE_ADMIN`, `ROLE_EMP`, `ROLE_MANG`, `ROLE_HR` are **never referenced by any matcher or annotation** — they are pure data.

### 3.3 Every role check performed in code (exhaustive)

| # | Location | Check | Effect |
|---|---|---|---|
| 1 | `cfg/SecurityConfiguration.java:62` | `.requestMatchers("/").hasRole("USER")` | the only HTTP-level role gate in the app |
| 2 | `login/LoginDetailsServiceImpl.java:88` | `new SimpleGrantedAuthority(role.getRoleName())` | *grants* authorities; no check |
| 3 | `login/SecurityServiceImpl.java:51-55` | `usernamePasswordAuthenticationToken.isAuthenticated()` | inside `autologin()` — **never called** (dead) |
| 4 | `login/LoginServiceImpl.java:43-45` | `roleRepository.findAll().get(0)` | new user gets the **first** role (seed order ⇒ `ROLE_ADMIN`) — **never called** (dead) |

There is **no** `@PreAuthorize`, `@Secured`, `@RolesAllowed`, `hasRole(...)` in any controller, and **no in-code role check** anywhere. The entire authorization model is: *"is the user logged in?"* plus a manual `loginId` tenant filter that is inconsistently applied.

### 3.4 Tenant isolation (the de-facto authorization) — coverage matrix

| Endpoint | Tenant-scoped by `loginId`? |
|---|---|
| `/getClientGroup`, `/getClientMaster`, `/getEmailMaster`, `/getEmailTemplate`, `/getEmailTemplateApprove`, `/getEmailMasterPagination`, `/getEmailSendJobs`, `/getDashboardDbReport`, `/getEmailExtractor`, `/getMonthlyReport`, `/getMonthlyEmailReport` | ✅ yes |
| `/getTemplate` (`emailTemplateId`) | ❌ `findByEmailTemplateId(id)` — global |
| `/approveTemplate` (`emailTemplateId`) | ❌ global |
| `/getEmailMasterById` (`emailMasterId`) | ❌ global — **returns another tenant's SMTP password** |
| `/getEmailInbox` (`emailMasterId`) | ❌ global |
| `/getEmailTemplateApproveList` | ❌ `findAll()` — **all tenants** |
| `/downloadFile` | ❌ `emailExtractorDataRepository.findByStartDateBetween(...)` — **all tenants' scraped emails** |
| `/removeClient` (`groupCode`) | ❌ `clientMasterRepository.findByGroupCode(groupCode)` — **global write** |
| `/createClientGroup` (`groupCode`) | uniqueness checked globally (`findByGroupCode`) |
| `/removeClientGroup` (`groupCode`) | ✅ `findByLoginIdAndGroupCode` (but NPE if absent) |
| `/sendEmail` | ❌ `fromEmail` → `emailMasterRepository.findByEmail(...)` global; `template` → global; `client[]` → `findByClientMasterId` global |
| `/drafts`, `/getAllGmail`, `/send`, `/last-24-hours/live`, `/getScheduledEmail` | ❌ **no tenant concept at all** (`EmailQueue`/`GmailMaster` have no `loginId` column) |

---

## 4. LICENCE ENFORCEMENT (exact)

### 4.1 MAC address read + format

```java
InetAddress address = InetAddress.getLocalHost();
NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
byte[] mac = networkInterface.getHardwareAddress();
StringBuilder stringBuilder = new StringBuilder();
for (int i = 0; i < mac.length; ++i) {
    stringBuilder.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
}
```
- Interface selected = the one bound to `InetAddress.getLocalHost()` (usually the default-route NIC). **Not** `getHardwareAddress()` over all NICs.
- Format = **uppercase hex, dash-separated, no trailing dash** → `[redacted - see SECRETS.txt]`.
- `System.out.print("MAC address  : " + stringBuilder.toString());` → the machine's MAC is **written to stdout on every login attempt**.

### 4.2 Comparison

```java
if (!Objects.requireNonNull(this.env.getProperty("mac")).equalsIgnoreCase(stringBuilder.toString())) {
    throw new UsernameNotFoundException("License expired license renewal contact administrator");
}
```
- Config key: `mac` (`application.properties`). **[CFG]** value `[redacted - see SECRETS.txt]`.
- Comparison is `String.equalsIgnoreCase` over the **whole string** → case-insensitive, but a different NIC order / a different NIC / a VM's virtual MAC all fail.
- `Objects.requireNonNull` → NPE if `mac` is absent from config (not caught → 500).

### 4.3 JWT decode + expiry

```java
DecodedJWT jwt = JWT.decode((String)Objects.requireNonNull(this.env.getProperty("license")));
if (jwt.getExpiresAt().before(new Date())) {
    throw new UsernameNotFoundException("License expired license renewal contact administrator");
}
```
- Library: **auth0 `com.auth0.jwt.JWT`** → `decode()` = **decode only, signature NEVER verified, no algorithm/secret supplied**. The licence is trivially forgeable by anyone who can edit `application.properties` (or `JWT.decode("...")` on a self-signed token).
- Config key: `license`. **[CFG]** value:
  `[redacted - see SECRETS.txt]`
  → payload `{"Role":"Admin","Issuer":"Issuer","Username":"JavaAnUse","exp":1758171187,"iat":1755492787}`.
- **The shipped licence expired at `1758171187` = 2025-09-18T04:53:07Z.** Today is 2026-10-19 ⇒ `getExpiresAt().before(now)` is **true** ⇒ *the reference build cannot authenticate anyone, on any machine, right now.* You must reissue a licence (or drop the check) when rebuilding.
- `getExpiresAt()` returns `null` for a token without `exp` → **NPE** (not caught) → 500. A forged licence therefore needs an `exp` claim.
- The `catch` clauses `MalformedJwtException | UnsupportedJwtException` (io.jsonwebtoken) and `ExpiredJwtException` (io.jsonwebtoken) are **dead** — auth0's `JWT.decode` throws `JWTDecodeException`/`IllegalArgumentException`. A malformed licence ⇒ uncaught `JWTDecodeException` ⇒ **500**, not a clean licence error.
- `UnknownHostException` is caught in the same multi-catch as `ExpiredJwtException` (i.e. `InetAddress.getLocalHost()` failure) ⇒ licence error message.
- `SocketException` ⇒ `throw new RuntimeException(e)` ⇒ 500.

### 4.4 Behaviour on mismatch

Both the MAC mismatch and the licence expiry raise `UsernameNotFoundException` with the identical message `"License expired license renewal contact administrator"`. Spring Security converts this to `BadCredentialsException` → the user is redirected to `/login?error` and sees *"Your username and password is invalid."* **The machine is locked out and the operator gets no diagnostic** other than the stdout line `MAC address  : XX-XX-…`. Because the check lives inside `loadUserByUsername`, it fires on **every** authentication attempt (including programmatic ones).

---

## 5. FILE UPLOAD / FILE DOWNLOAD ENDPOINTS

### 5.1 Uploads (4)

| Endpoint | Controller | Handling |
|---|---|---|
| `POST /emailClientUpload` | `EmailController:187` | `emailService.addEmailClient(uploadfiles, groupCode, auth.getName(), loginId)` |
| `POST /emailExtractor` | `EmailController:379` | `emailExtractorService.extract(uploadfiles.getInputStream(), loginId)` (`NewEmailExtractorService`, `@Async`) |
| `POST /sendData` | `EmailSendController:52` | `extractExcelData(file)` → POI `XSSFWorkbook` → JSON → **posted to an external URL** |
| `POST /send` | `NewEmailController:89` | `parseEmailDataFromExcel(file)` → `XSSFWorkbook` → rows → `EmailQueue` rows saved |

**Where files are written:** **nowhere on disk.** No `MultipartFile.transferTo(...)`, no `FileOutputStream` of the upload. All four are parsed in memory via `XSSFWorkbook(file.getInputStream())`.

**Path traversal:** *not applicable to uploads* (no filename is ever used to build a path).

**File type validation:** **none.** No content-type check, no extension check, no magic-byte check. `XSSFWorkbook` is constructed directly on the raw stream, so a non-XLSX file throws a POI exception (caught → `FAILUER` / `""`).
- Zip-bomb / decompression-bomb risk: `XSSFWorkbook` on a 128 MB request is unbounded (`ZipSecureFile` not configured anywhere).
- **[CFG]** `spring.servlet.multipart.max-file-size=128MB`, `spring.servlet.multipart.max-request-size=128MB` — no per-endpoint cap.

**Other upload-path issues**
- `/emailClientUpload` → `EmailService.addEmailClient` (line 325): reads `row.getCell(0)` for **name**, `row.getCell(1)` for **email**, and sets `webLink = row.getCell(0)` (**same cell as name — a bug**). Guard is only `row.getCell(0) != null`, so a row with a null column 1 ⇒ NPE ⇒ caught by the outer `catch (Exception ex) { printStackTrace(); return ""; }` ⇒ the controller stores **`""`** as the status message (neither "SUCCESS" nor "FAILURE"). `getStringCellValue()` on a numeric cell throws `IllegalStateException`.
- `/emailExtractor` → the controller returns `"result"` unconditionally on success and `"FAILUER"` (typo) on failure, but the service swallows all exceptions internally ⇒ the failure branch is effectively unreachable, and the caller never learns the real outcome (the `EmailExtractor` row status is `IN PROGRESS` → `SUCCESS`/`FAILED`).
- `/sendData` → the file contents are forwarded to `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec` (**literal placeholder** — the endpoint is non-functional as shipped) via `new RestTemplate()` (a fresh instance, ignoring the `restTemplate` bean), with **no timeout** and no SSRF/allow-list control.
- `/send` → `parseEmailDataFromExcel` expects **column 0 = domain, column 1 = email** (opposite order to `/emailClientUpload`!). Non-string cells ⇒ `IllegalStateException` ⇒ wrapped as `RuntimeException("Failed to parse Excel file")` ⇒ 500.

### 5.2 Download (1) — `POST /downloadFile`

`EmailController:402-421`:
```java
@PostMapping(path={"/downloadFile"})
public ResponseEntity<Resource> downloadFile(@RequestParam("fromDate") String fromDate,
        @RequestParam("toDate") String toDate, HttpSession ses, HttpServletRequest req) throws IOException {
    Long loginId = (Long)ses.getAttribute("loginId");     // <-- read and NEVER used
    String fileName = "";
    try { fileName = this.emailService.generateExcel(fromDate, toDate); }
    catch (Exception e) { e.printStackTrace(); }
    File file = new File(this.uploadpath + fileName);
    HttpHeaders header = new HttpHeaders();
    header.add("Content-Disposition", "attachment; filename=" + fileName);
    header.add("Cache-Control", "no-cache, no-store, must-revalidate");
    header.add("Pragma", "no-cache");
    header.add("Expires", "0");
    Path path = Paths.get(file.getAbsolutePath(), new String[0]);
    ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
    return ResponseEntity.ok().headers(header)
        .contentLength(file.length())
        .contentType(MediaType.parseMediaType("application/octet-stream"))
        .body(resource);
}
```

The generator, `EmailService.generateExcel` (line 553):
```java
new Date(this.sdfSqlDate.parse(startDate).getTime());          // sdfSqlDate = "yyyy-MM-dd", LENIENT
List<EmailExtractorData> l = emailExtractorDataRepository.findByStartDateBetween(from, to);   // NOT tenant-scoped
...
filename = startDate + "_" + endDate + "_" + emailExtractorDatalistNew.size() + "1.xlsx";
FileOutputStream out = new FileOutputStream(new File(this.uploadpath + (String)filename));
workbook.write(out);
```
- **Write location**: `app.upload.path` **[CFG]** = `C:\deployment` (Windows path, no trailing separator) → files land at `C:\deployment2024-01-01_2024-01-31_35831.xlsx`. Concatenation is bare string concatenation (no `File.separator`), so the property **must** end with a separator.
- **Path traversal**: `filename` embeds the **raw** `fromDate`/`toDate` strings. `SimpleDateFormat.parse()` parses only the *leading* `yyyy-MM-dd` and **ignores trailing text**, so `fromDate=2024-01-01/../../evil` parses successfully and yields the filename `2024-01-01/../../evil_2024-01-01_10.xlsx` → an attacker-controlled relative path under the upload dir. Practical impact is limited because the *intermediate* directory (`C:\deployment2024-01-01`) does not exist → the write throws `FileNotFoundException` (caught, `printStackTrace`) and the subsequent `Files.readAllBytes` throws ⇒ 500. It is nonetheless an **unsanitised path primitive** and must not be reproduced.
- **Response header injection**: `"attachment; filename=" + fileName` — the filename is **not quoted and not sanitised**, and it contains attacker-controlled bytes ⇒ a CR/LF in `fromDate` lands in a response header value. On Tomcat 8.5+ `MimeHeaders` rejects illegal header characters (⇒ 500 rather than response splitting); on lax containers it is a live response-splitting vector. The filename is also never quoted, so filenames with spaces/commas break client parsing.
- **Cross-tenant data leak**: `findByStartDateBetween` is **not filtered by `loginId`** ⇒ the exported `.xlsx` contains **every tenant's** scraped email addresses. The `loginId` is fetched and discarded.
- **Memory**: the whole file is read into a `ByteArrayResource` (`Files.readAllBytes`) — no streaming.
- No `Content-Disposition` filename sanitisation, no `X-Content-Type-Options` on this response (the header defaults do apply: `nosniff`).

### 5.3 Other file-ish surfaces
- `EmailService.extracted` / `NewEmailExtractorService.extract` fetch **`https://` + <cell value>** from the uploaded Excel via `Jsoup.connect(...)` → **SSRF** to any internal HTTPS host (e.g. `https://127.0.0.1:8083/`, `https://169.254.169.254/`-style metadata over HTTPS). `NewEmailExtractorService` sets `timeout(10000)`; the (dead) `EmailService.extracted` sets **no timeout** and would hang a pool thread.
- `NewEmailExtractorService.isUnique` uses a per-JVM `ConcurrentHashMap` (`emailDedupMap`) that is **never cleared** → unbounded memory growth + cross-run dedup.
- `EmailInboxService.fetchEmailInbox` (`@Scheduled(cron="0 0 11 * * *")`) writes inbox rows to the DB — it is the only other "file" sink (IMAP → DB). `deleteByEmailIdAndDate(email, new Date())` deletes rows whose `date` equals *now* to the millisecond ⇒ never matches ⇒ the inbox table grows without bound; and `emailInboxList` is never cleared between accounts ⇒ earlier accounts' rows are re-saved (duplicates).

---

## 6. SECURITY WEAKNESSES (file + exact code)

### 6.1 CSRF disabled anywhere? → **No — but CSRF is broken in practice**
- No `.csrf().disable()` anywhere. CSRF is at Spring's default (enabled).
- **The real problem is the opposite**: 6 state-changing endpoints are **`@GetMapping`** ⇒ CSRF-exempt *and* trivially triggerable cross-site:
  ```java
  @GetMapping("/removeTemplate")     // EmailController:162
  @GetMapping("/createClientGroup")  // EmailController:225
  @GetMapping("/removeClientGroup")  // EmailController:241
  @GetMapping("/removeClient")       // EmailController:257
  @GetMapping("/removeEmail")        // EmailController:330
  @GetMapping("/approveTemplate")    // EmailController:352
  ```
  → `<img src="https://host/removeClient?groupCode=X">` deletes a client group; `<img src=".../approveTemplate?emailTemplateId=N">` approves a template.
- Conversely, every legitimate JSON/multipart POST will 403 without `X-CSRF-TOKEN` — confirm the shipped `login.html` and the JS layer actually send it.

### 6.2 Secrets logged / printed
| Location | Code | Secret |
|---|---|---|
| `login/LoginDetailsServiceImpl.java:65` | `System.out.print("MAC address  : " + stringBuilder.toString());` | machine MAC, on every login |
| `login/LoginDetailsServiceImpl.java:75` | `ex.printStackTrace();` | stack trace incl. the licence-token decode path |
| `login/LoginSuccessHandler.java:47` | `e.printStackTrace();` | on session-attribute failure |
| `controller/EmailController.java:198, 390, 410` | `e.printStackTrace();` | upload/extract/download failures (paths, row contents) |
| `controller/NewEmailController.java:164` | `e.printStackTrace();` | scheduler failures |
| `service/EmailService.java:258, 355, 509, 577` | `ex.printStackTrace();` | SMTP/POI errors |
| `service/NewEmailExtractorService.java:150, 187` | `System.out.println(...)` / `System.err.println("Error saving email data: " + d.getEmail())` | scraped email addresses to stderr |
| `service/GmailService.java:136` | `System.out.println("Email sent to " + toEmail);` | recipient addresses |
| `service/EmailInboxService.java:82` | `System.out.println("messages.length---" + messages.length);` | – |
| `login/LoginController.java:33, 60` | `System.out.println("Inside Login")`, `System.out.println("accessdenied")` | – |
- `LoginSuccessHandler` also puts the **raw submitted password** into a `Login` entity: `login.setPassword(request.getParameter("password"));` (dead, but it materialises the plaintext in the heap).
- `cfg/JpaConfigurationMySql.java:80` → `hibernate.show_sql = "true"` ⇒ **all SQL (including parameters bound to `login`/`email_master` inserts) is logged to stdout**. Combined with `saveEmail` storing SMTP passwords, the SMTP credentials are printed by Hibernate.

### 6.3 SQL built by string concatenation? → **No**
All queries are either derived queries or **named-parameter native queries**:
```java
@Query(nativeQuery=true, value="select * FROM email_extractor d WHERE d.startDate between :startDate and :endDate and loginId =:loginId")
@Query(nativeQuery=true, value="select * FROM email_send_job d WHERE d.date between :startDate and :endDate and loginId =:loginId")
@Query(nativeQuery=true, value="select * from client_master c where c.groupCode=:groupCode and c.loginId=:loginId and c.isactive=1")
@Query(nativeQuery=true, value="select * from client_master c where c.groupCode=:groupCode and c.isactive=1")
```
No string concatenation into SQL anywhere. **However** the native queries use `select *` and `c.groupCode` (camelCase) against a MySQL schema — they only work because MySQL column-name matching is case-insensitive; they will break on PostgreSQL/H2. Also `findByGroupCode` (the `groupCode`-only variant) is the one used for a **cross-tenant write** (`removeClient`).

### 6.4 Missing authorization checks (the dominant issue)
Every endpoint below is reachable by **any authenticated user** and acts on **another tenant's data** (no `loginId` in the lookup):
```java
// EmailController:146  – any template
public EmailTemplate getTemplate(@RequestParam("emailTemplateId") Long emailTemplateId, ...) {
    return this.emailService.getEmailTemplate(emailTemplateId);        // findByEmailTemplateId(id) — global
}
// EmailController:324  – any SMTP account, password included
public EmailMaster getEmailMasterById(@RequestParam Long emailMasterId, ...) {
    return this.emailService.getEmailMaster(emailMasterId);           // findByEmailMasterId(id) — global
}
// EmailController:273  – any mailbox
public List<EmailDto> getEmailInbox(@RequestParam("emailMasterId") Long emailMasterId, ...) { ... }
// EmailController:352  – approve ANY tenant's template
public BaseResponse approveTemplate(@RequestParam("emailTemplateId") Long emailTemplateId, ...) {
    Long count = this.emailService.approveTemplate(auth.getName(), emailTemplateId);
}
// EmailController:140  – ALL tenants' pending templates
public List<EmailTemplate> getEmailTemplateApproveList(...) {
    return this.emailService.getEmailTemplatesForApprove();          // emailTemplateRepository.findAll()
// EmailController:241/257 – cross-tenant DELETE
public BaseResponse removeClient(@RequestParam("groupCode") String groupCode, ...) {
    Integer count = this.emailService.removeClient(groupCode, auth.getName(), loginId);
    // -> clientMasterRepository.findByGroupCode(groupCode)   // NO loginId  -> soft-deletes other tenants' clients
```
And the whole `NewEmailController` surface (`/drafts`, `/getAllGmail`, `/send`, `/last-24-hours/live`, `/getScheduledEmail`) has **no tenant scoping at all**.

### 6.5 Credential / secret disclosure to any logged-in user
```java
// NewEmailController:83  – returns EVERY GmailMaster row as JSON
@RequestMapping(value={"/getAllGmail"}, method={RequestMethod.POST})
@ResponseBody
public List<GmailMaster> getAllGmail(String email) throws Exception {
    return this.gmailService.getAllGmail();      // gmailMasterRepository.findAll()
}
```
`GmailMaster` fields: `gmailMasterId, email, clientId, secret, refreshToken, accessToken, isActive, createdBy, createdDate, modifiedBy, modifiedDate` — **no `@JsonIgnore` anywhere** ⇒ the response contains live Google OAuth **client secrets, refresh tokens and access tokens** for every configured mailbox. **[CFG]** the harvested values include `[redacted - see SECRETS.txt]` and `[redacted - see SECRETS.txt]`.

Same class of leak:
- `POST /getEmailMaster` and `GET /getEmailMasterById` → `EmailMaster` (has `private String password;`, no `@JsonIgnore`) → **plaintext SMTP app passwords** in the JSON body. **[CFG]** 41 SMTP app passwords live in `email_master`.
- `POST /downloadFile` → the xlsx contains all tenants' scraped emails.

### 6.6 Licence forgery / licence is not a security control
- `JWT.decode(...)` (auth0) **never verifies the signature**. Anyone who can change `license=` in `application.properties` (or `JWT.decode("…")` a self-made token) unlocks the app.
- The JWT is **shipped in the repository** **[CFG]**, and it expired **2025-09-18**.
- The MAC lock is a *licensing* control, not a security control, and the MAC is printed to stdout.

### 6.7 Plaintext password storage
- `email_master.password` — SMTP app passwords stored **unencrypted** (`EmailService.saveEmailMaster` → `emailMaster.setPassword(emailMasterDto.getPassword())`).
- `gmail_master.secret` / `refreshToken` / `accessToken` — unencrypted OAuth material.
- `LoginSuccessHandler` transiently holds the raw login password.

### 6.8 Open redirect? → **None found**
All redirects are hard-coded: `response.sendRedirect("/welcome")` (`LoginSuccessHandler:49`), `.defaultSuccessUrl("/welcome")`, `.failureUrl("/login?error")`, `.logoutSuccessUrl("/login?logout")`, `.invalidSessionUrl("/login")`, `.accessDeniedPage("/accessdenied")`. No redirect target is derived from a request parameter. The only request-derived value used in a header is the file name (§6.9).

### 6.9 XSS / HTML injection surfaces
- **Response header injection** (see §5.2): `header.add("Content-Disposition", "attachment; filename=" + fileName);` with `fileName` built from raw request parameters ⇒ CR/LF injection attempt.
- **Stored HTML in outbound email**: `EmailService.emailSender` renders the `mailtemplate` Thymeleaf template with `content = emailTemplate.getTemplateContent()` — i.e. **operator-supplied HTML** is injected into the mail. If `mailtemplate` uses `th:utext="${content}"` (the only sensible way to send HTML mail) it is **unescaped** ⇒ stored XSS in the recipient's mail client; if it uses `th:text` the HTML is escaped and the mail body is broken. The template is not in the reference tree — pull it from the JAR and check.
- **Stored HTML in Gmail drafts**: `GmailService.sendDraftEmailById` does `html.replace("{{Domain}}", domainValue)` on the draft's `text/html` part, where `domainValue` comes from the uploaded Excel ⇒ HTML injection into the sent Gmail message.
- **IMAP content into the UI**: `EmailInboxService.getTextFromMessage` stores raw `text/html` (`result = (String)message.getContent()`) into `EmailInbox.content` and returns it via `GET /getEmailInbox` as `EmailDto.content`. If the template renders it with `th:utext` ⇒ stored XSS from **any inbound e-mail**. (The multipart branch does sanitise via `Jsoup.parse(html).text()`, but the single-part `text/html` branch does not.)
- **Scraped content**: `EmailService.extracted` stores regex-extracted emails and `NewEmailExtractorService` stores scraped strings; `EmailTemplate.templateName` is used as the **email Subject** (`mail.setSubject(emailTemplate.getTemplateName())`) ⇒ subject-header injection attempts (no CRLF filtering).

### 6.10 Session fixation → **mitigated** (Spring default `migrateSession`). Not disabled anywhere.

### 6.11 Hardcoded credentials in Java → **none** (verified by the harvest: "No credentials hardcoded in Java"). The hard-coded items are:
- `controller/EmailSendController.java:68` — `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec` (a placeholder URL, not a credential; the endpoint is non-functional).
- `cfg/PuppeteerRunner.java:18` — `new ProcessBuilder("npm.cmd", "install")` and `new ProcessBuilder("node", "server.js")` in `src/main/resources/puppeteer` (Windows-only `npm.cmd`; the class is not a bean and is never referenced ⇒ dead).
- `scraper/service/AmazonScraperService.java:25` — `http://localhost:3000/scrape?url=` (plaintext local HTTP; class is not a bean, no caller ⇒ dead).
- **[CFG]** the real secrets live in `application.properties`: MySQL root password `[redacted - see SECRETS.txt]`, `license` JWT, `mac`, plus a WordPress REST token; and in the DB seed: 41 SMTP app passwords, 2 Google OAuth clients (id/secret/refresh/access token), 3 bcrypt hashes (two identical).

### 6.12 Other findings worth carrying into the rebuild
1. **403 on `GET /`** for every seeded role (`hasRole("USER")` vs `ROLE_ADMIN|ROLE_EMP|ROLE_MANG|ROLE_HR`) — see §3.2.
2. **No `SessionRegistry` / `HttpSessionEventPublisher`** ⇒ `maximumSessions(1)` registry leak ⇒ spurious logouts.
3. **`/accessdenied` is reachable anonymously** — `ExceptionTranslationFilter` forwards (`DispatcherType.FORWARD`), and Spring Security's `FilterChainProxy` is registered for `REQUEST`/`ASYNC`/`ERROR` only, so the forwarded request bypasses the filter chain.
4. **Denied/redirected users get no `loginId`** ⇒ `ses.getAttribute("loginId")` is `null` ⇒ all tenant queries run with a `null` tenant key (`findByLoginId(null)` ⇒ empty list, not an error) ⇒ silent "empty dashboard" instead of an error.
5. **Error handling swallows everything**: 12 `printStackTrace()` sites; `addEmailClient` returns `""` on failure; `emailClientUpload` then reports `""`; `emailExtractor` reports the literal `"FAILUER"` (typo) or `"result"`.
6. **NPE-prone paths** (uncaught ⇒ 500): `EmailService.removeClientGroup` (`findByLoginIdAndGroupCode` ⇒ null), `getEmailTemplateApprove` (`getStatus()` null), `sendEmail` (`emailTemplate` null ⇒ NPE at `emailTemplate.getEmailTemplateId()`), `getMonthlyReport` (`now.withMonth(month)` throws `DateTimeException` for month > 12), `getEmailMasterById` (null entity), `LoginDetailsServiceImpl` (null `mac` byte[], null `jwt.getExpiresAt()`).
7. **`getEmailSendJobs` is effectively always empty**: `emailSendJobRepository.findByDateAndLoginId(new Date(new java.util.Date().getTime()), loginId)` compares a `java.sql.Date` column to an *exact millisecond* ⇒ no rows match.
8. **Daily limit is actually monthly**: `emailSend` counts `emailSendJobRepository.findByLoginIdMonth(loginId, firstDayOfMonth, lastDayOfMonth)` and returns `"Daily Limit Reached "` when `> 50000` ⇒ the cap is per calendar month, mislabelled.
9. **Pagination is broken**: `PageRequest.of((int)(pagingRequest.getStart() / 100), 100)` hard-codes page size 100 and ignores `pagingRequest.getLength()`; `page.setRecordsTotal(100L)` is a hard-coded literal; `recordsFiltered` = the real total.
10. **SSRF** via the scraped-domain Excel column (`Jsoup.connect("https://" + cell)`), plus an external POST of user data to Google Apps Script from `/sendData`.
11. **`@Scheduled` inside a `@Controller`** (`NewEmailController.processScheduledEmails`, `fixedRate=60000`) — a controller method doubling as a scheduler; it processes **all tenants'** queued mail with no per-tenant quota, and a failure in one sender aborts the `invokeAll` batch (`ExecutionException` ⇒ the remaining groups are lost, `e.printStackTrace()`).
12. **Raw `LocalDateTime.parse(dateStr)`** on `/send` ⇒ a non-ISO value ⇒ `DateTimeParseException` ⇒ 500 (and the `dateStr != null` guard is dead: `@RequestParam` is required by default ⇒ a missing value is a 400).
13. **`SimpleDateFormat` as a shared mutable field** — `EmailController.simpDate`/`sqlDate`, `EmailService.sdfDateTime`/`sdfDate`/`sdfSqlDate`/`sdfRegularDateTime` are **non-thread-safe instance fields on singletons** ⇒ date corruption under concurrent requests. `EmailController.simpDate` is entirely unused.

---

## 7. DEAD CODE / UNUSED ENDPOINTS / REFERENCES TO NON-EXISTENT FEATURES

### 7.1 Dead classes / members
| Item | File | Why dead |
|---|---|---|
| `LogoutController` | `login/LogoutController.java` | **empty class, no annotations** → zero endpoints |
| `SecurityService` / `SecurityServiceImpl` | `login/SecurityService*.java` | **no caller anywhere** (`autologin`, `findLoggedInUsername`) |
| `LoginServiceImpl` | `login/LoginServiceImpl.java` | autowired in `LoginController` and `LoginSuccessHandler` but **never used** → `save()` (the only user-creation path) is unreachable; `getHeaders(String)` is private & unused |
| `LoginServiceImpl.save()` | line 41 | would grant `roleRepository.findAll().get(0)` (⇒ `ROLE_ADMIN`) to every new user — a live privilege-escalation landmine if ever wired up |
| `SecurityConfiguration.passwordencoder()` | line 66 | **missing `@Bean`** → not registered; the live encoder is `WebMvcConfigruation.passwordencoder()` |
| `SecurityConfiguration.logoutSuccessHandler()` | line 71 | `@Bean LogoutHandler` **never wired** into `.logout()`; `CustomLogoutHandler.logout()` is a no-op anyway |
| `WebMvcConfigruation.taskExecutor()` | line 35 | **missing `@Bean`** → not registered |
| `WebMvcConfigruation.restTemplate()` | line 46 | bean exists, but `EmailSendController` does `new RestTemplate()` inline |
| `GmailServiceFactory` | `cfg/GmailServiceFactory.java` | `@Service` but **never injected** (`GmailService` builds its own credentials) |
| `EmailExtractionService` | `service/EmailExtractionService.java` | `@Service`, autowired into `EmailService` but **never called** (`EmailService` has its own `extracted()`) |
| `EmailService.extract(...)` / `EmailService.extracted(...)` | `service/EmailService.java:483,519` | superseded by `NewEmailExtractorService`; the controller injects `NewEmailExtractorService` |
| `EmailService.getEmailExtractor(id, loginId)` | line 588 | the only properly tenant-scoped extractor lookup — **never called** |
| `PuppeteerRunner` | `cfg/PuppeteerRunner.java` | not a `@Component`/`@Service`, no caller |
| `AmazonScraperService` + `Product` | `scraper/**` | not a bean, no controller, no caller |
| `EmailController.simpDate` | line 90 | unused `SimpleDateFormat` |
| `LoginSuccessHandler`'s `Login login = new Login(); login.setUsername(...); login.setPassword(...);` and `Object token = null;` | lines 37-40 | overwritten / never used |
| `CustomLogoutHandler`'s `HttpSession ses = request.getSession();` | line 22 | result unused |

### 7.2 Security-config matchers pointing at non-existent endpoints
```java
.requestMatchers("/api/v1/getBarcode4j").permitAll()   // no controller, no route anywhere
.requestMatchers("/word").permitAll()                   // no controller, no route anywhere
```
Both are `permitAll` holes for URLs that (currently) 404. **[CFG]** the config file also carries a *WordPress REST token*, suggesting a `/word` integration existed in another build.

### 7.3 Endpoints that reference features that do not exist
| Endpoint / code | Reference | Status |
|---|---|---|
| `POST /sendData` | `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec` | **literal `YOUR_SCRIPT_ID` placeholder** ⇒ the endpoint always errors (returns `"Error: " + e.getMessage()`) |
| `GET /taskReports` | returns `[1000,3000,4000,2000,500]` | hard-coded stub, ignores `month`/`filterType` |
| `GET /getEmailTemplateApproveList` | filters `status == "N"` | **nothing in the codebase ever sets `status="N"`** (`saveEmailTemplate` sets `"A"`, `approveTemplate` sets `"A"`) ⇒ the approval queue is always empty; `GET /approveTemplates` renders an empty page |
| `GET /getEmailInbox` | `fromDate`, `toDate` | parameters accepted and **completely ignored** by `EmailInboxService.getEmailInbox` |
| `GET /taskReports` / `getMonthlyReport` | "Daily Limit Reached" message | limit is monthly, not daily |
| `EmailController` `/getEmailSendJobs` | `findByDateAndLoginId(now, loginId)` | effectively always returns `[]` |
| `LoginService` (`save`, `findByUsername`) | user provisioning | no endpoint calls them ⇒ **no user can be created through the app** (users only exist via the SQL seed) |
| `SecurityServiceImpl.autologin` | programmatic login | no caller |

### 7.4 Behavioural quirks to preserve-or-fix (rebuild checklist)
1. `GET /` = 403 for all seeded roles; use `/welcome` (same handler) as the real entry point.
2. Session attributes after login: exactly `loginId` (Long) and `Username` (String) — every controller depends on `loginId` as the tenant key.
3. Login always lands on `/welcome` (no saved-request support).
4. Logout is `POST /logout` + CSRF token → redirect `/login?logout`.
5. Failed login → `/login?error` → *"Your username and password is invalid."* (licence/MAC failures are indistinguishable from bad credentials).
6. `app.upload.path` = `C:\deployment`, used by bare string concatenation for the xlsx download; `spring.servlet.multipart.*` = 128MB.
7. `server.port` = 8083 **[CFG]**; DB = `jdbc:mysql://localhost:3306/emailsender`, user `root` **[CFG]**.
8. The licence JWT in the repo **expired 2025-09-18** ⇒ reissue before you can log in; the MAC is `[redacted - see SECRETS.txt]` **[CFG]**.

---

**Not checked / risks:** the Thymeleaf templates (`login.html`, `mailtemplate`, `NewDashBoard`, …) and the `puppeteer/*.js` assets are **not** in the reference tree — the CSRF-token presence in `login.html` and whether `mailtemplate` uses `th:utext` (stored XSS in outbound mail) could not be confirmed from Java alone; pull them from `seo-0.0.1-SNAPSHOT.jar` to close that gap. The `emailsend21.sql` seed was read only via the summary in `SECRETS.txt`, not the file itself.
</task_result>
</task>
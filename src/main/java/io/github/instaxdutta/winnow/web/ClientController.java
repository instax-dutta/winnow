package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.domain.ValidationException;
import io.github.instaxdutta.winnow.model.ClientDto;
import io.github.instaxdutta.winnow.model.ClientImportResult;
import io.github.instaxdutta.winnow.model.PagedResponse;
import io.github.instaxdutta.winnow.security.AuthPrincipalResolver;
import io.github.instaxdutta.winnow.service.ClientService;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The client contacts.
 *
 * <p>The tenant comes from the authenticated principal. It is never a request parameter.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clients;
    private final AuthPrincipalResolver principals;

    public ClientController(ClientService clients, AuthPrincipalResolver principals) {
        this.clients = clients;
        this.principals = principals;
    }

    /**
     * One page of the contacts of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<PagedResponse<ClientDto>> list(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "50") int length,
            @RequestParam(required = false) String search) {
        Long ownerUserId = principals.currentUserId();
        return ApiResponse.ok(clients.list(ownerUserId, start, length, search));
    }

    /**
     * Imports the contacts of a spreadsheet.
     */
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<ClientImportResult> importClients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupId", required = false) Long groupId) {
        UploadGuard.check(file);
        Long ownerUserId = principals.currentUserId();
        try (InputStream in = file.getInputStream()) {
            return ApiResponse.ok(clients.importExcel(ownerUserId, groupId, in));
        } catch (IOException e) {
            throw new ValidationException("The uploaded file could not be read");
        }
    }

    /**
     * Removes one contact.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        clients.delete(principals.currentUserId(), id);
        return ApiResponse.ok("The client was removed", null);
    }
}

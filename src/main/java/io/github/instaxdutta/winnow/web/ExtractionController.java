package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.domain.ValidationException;
import io.github.instaxdutta.winnow.model.ExtractionResultDto;
import io.github.instaxdutta.winnow.model.ExtractionRunDto;
import io.github.instaxdutta.winnow.model.PagedResponse;
import io.github.instaxdutta.winnow.security.AuthPrincipalResolver;
import io.github.instaxdutta.winnow.service.ExtractionService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The extraction runs.
 *
 * <p>The upload is a spreadsheet of domains. It is checked at the edge and parsed in
 * memory. The name of the file is kept as metadata and is never turned into a path.
 */
@RestController
@RequestMapping("/api/extraction")
public class ExtractionController {

    private final ExtractionService extraction;
    private final AuthPrincipalResolver principals;

    public ExtractionController(ExtractionService extraction, AuthPrincipalResolver principals) {
        this.extraction = extraction;
        this.principals = principals;
    }

    /**
     * The runs of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<ExtractionRunDto>> list() {
        return ApiResponse.ok(extraction.list(principals.currentUserId()));
    }

    /**
     * Starts one run over the domains of an uploaded spreadsheet.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<ExtractionRunDto> start(@RequestParam("file") MultipartFile file) {
        UploadGuard.check(file);
        Long ownerUserId = principals.currentUserId();
        try (InputStream in = file.getInputStream()) {
            return ApiResponse.ok(extraction.start(ownerUserId, file.getOriginalFilename(), in));
        } catch (IOException e) {
            throw new ValidationException("The uploaded file could not be read");
        }
    }

    /**
     * One run.
     */
    @GetMapping("/{id}")
    @PreAuthorize(Roles.READ)
    public ApiResponse<ExtractionRunDto> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(extraction.get(principals.currentUserId(), id));
    }

    /**
     * One page of the addresses of a run.
     */
    @GetMapping("/{id}/results")
    @PreAuthorize(Roles.READ)
    public ApiResponse<PagedResponse<ExtractionResultDto>> results(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "50") int length) {
        return ApiResponse.ok(extraction.results(principals.currentUserId(), id, start, length));
    }
}

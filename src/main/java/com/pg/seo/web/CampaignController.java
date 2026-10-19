package com.pg.seo.web;

import com.pg.seo.model.CampaignDto;
import com.pg.seo.model.CampaignRequest;
import com.pg.seo.model.DeliveryDto;
import com.pg.seo.model.PagedResponse;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.CampaignService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The send campaigns.
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaigns;
    private final AuthPrincipalResolver principals;

    public CampaignController(CampaignService campaigns, AuthPrincipalResolver principals) {
        this.campaigns = campaigns;
        this.principals = principals;
    }

    /**
     * Creates one campaign.
     */
    @PostMapping
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<CampaignDto> create(@Valid @RequestBody CampaignRequest request) {
        return ApiResponse.ok(campaigns.create(principals.currentUserId(), request));
    }

    /**
     * One page of the campaigns of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<PagedResponse<CampaignDto>> list(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "50") int length) {
        return ApiResponse.ok(campaigns.list(principals.currentUserId(), start, length));
    }

    /**
     * One campaign.
     */
    @GetMapping("/{id}")
    @PreAuthorize(Roles.READ)
    public ApiResponse<CampaignDto> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(campaigns.get(principals.currentUserId(), id));
    }

    /**
     * The deliveries of one campaign.
     */
    @GetMapping("/{id}/deliveries")
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<DeliveryDto>> deliveries(@PathVariable("id") Long id) {
        return ApiResponse.ok(campaigns.deliveries(principals.currentUserId(), id));
    }

    /**
     * Cancels one campaign.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<CampaignDto> cancel(@PathVariable("id") Long id) {
        Long ownerUserId = principals.currentUserId();
        campaigns.cancel(ownerUserId, id);
        return ApiResponse.ok(campaigns.get(ownerUserId, id));
    }
}

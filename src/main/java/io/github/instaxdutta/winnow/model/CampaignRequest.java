package io.github.instaxdutta.winnow.model;

import io.github.instaxdutta.winnow.domain.AccountKind;
import io.github.instaxdutta.winnow.domain.TargetType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CampaignRequest(
        @NotNull AccountKind fromAccountKind,
        @NotNull Long fromAccountId,
        @NotNull Long templateId,
        @NotNull TargetType targetType,
        /* Used when targetType is GROUP. */
        String groupCode,
        /* Used when targetType is INDIVIDUAL. */
        List<Long> clientIds) {}

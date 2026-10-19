/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

import com.pg.seo.model.BaseResponse;
import com.pg.seo.model.ClientMasterDto;
import java.util.List;

public class ClientMasterResponse
extends BaseResponse {
    private List<ClientMasterDto> clientMasterDtoList;
    private Long totalRecord;

    public ClientMasterResponse(List<ClientMasterDto> clientMasterDtoList, Long totalRecord) {
        this.clientMasterDtoList = clientMasterDtoList;
        this.totalRecord = totalRecord;
    }

    public List<ClientMasterDto> getClientMasterDtoList() {
        return this.clientMasterDtoList;
    }

    public Long getTotalRecord() {
        return this.totalRecord;
    }

    public void setClientMasterDtoList(List<ClientMasterDto> clientMasterDtoList) {
        this.clientMasterDtoList = clientMasterDtoList;
    }

    public void setTotalRecord(Long totalRecord) {
        this.totalRecord = totalRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ClientMasterResponse)) {
            return false;
        }
        ClientMasterResponse other = (ClientMasterResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$totalRecord = this.getTotalRecord();
        Long other$totalRecord = other.getTotalRecord();
        if (this$totalRecord == null ? other$totalRecord != null : !((Object)this$totalRecord).equals(other$totalRecord)) {
            return false;
        }
        List<ClientMasterDto> this$clientMasterDtoList = this.getClientMasterDtoList();
        List<ClientMasterDto> other$clientMasterDtoList = other.getClientMasterDtoList();
        return !(this$clientMasterDtoList == null ? other$clientMasterDtoList != null : !((Object)this$clientMasterDtoList).equals(other$clientMasterDtoList));
    }

    @Override
    protected boolean canEqual(Object other) {
        return other instanceof ClientMasterResponse;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $totalRecord = this.getTotalRecord();
        result = result * 59 + ($totalRecord == null ? 43 : ((Object)$totalRecord).hashCode());
        List<ClientMasterDto> $clientMasterDtoList = this.getClientMasterDtoList();
        result = result * 59 + ($clientMasterDtoList == null ? 43 : ((Object)$clientMasterDtoList).hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ClientMasterResponse(clientMasterDtoList=" + this.getClientMasterDtoList() + ", totalRecord=" + this.getTotalRecord() + ")";
    }

    public ClientMasterResponse() {
    }
}


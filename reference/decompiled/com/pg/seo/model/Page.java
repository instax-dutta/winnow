/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

import java.util.List;

public class Page<T> {
    private List<T> data;
    private Long recordsFiltered;
    private Long recordsTotal;
    private int draw;

    public Page(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return this.data;
    }

    public Long getRecordsFiltered() {
        return this.recordsFiltered;
    }

    public Long getRecordsTotal() {
        return this.recordsTotal;
    }

    public int getDraw() {
        return this.draw;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public void setRecordsFiltered(Long recordsFiltered) {
        this.recordsFiltered = recordsFiltered;
    }

    public void setRecordsTotal(Long recordsTotal) {
        this.recordsTotal = recordsTotal;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public Page() {
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.pg.seo.model;

public class PagingRequest {
    private int start;
    private int length;
    private int draw;

    public void setStart(int start) {
        this.start = start;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setDraw(int draw) {
        this.draw = draw;
    }

    public int getStart() {
        return this.start;
    }

    public int getLength() {
        return this.length;
    }

    public int getDraw() {
        return this.draw;
    }
}


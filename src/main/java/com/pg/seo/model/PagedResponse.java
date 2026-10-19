package com.pg.seo.model;

import java.util.List;

/**
 * A page of results.
 *
 * <p>{@code recordsTotal} is the real number of rows the filter matches, not a constant.
 * The original hard-coded it to 100, so the pager was always wrong.
 */
public record PagedResponse<T>(
        List<T> data,
        long recordsTotal,
        long recordsFiltered,
        int draw,
        int start,
        int length) {

    public static <T> PagedResponse<T> of(List<T> data, long total, long filtered, int draw, int start, int length) {
        return new PagedResponse<>(List.copyOf(data), total, filtered, draw, start, length);
    }
}

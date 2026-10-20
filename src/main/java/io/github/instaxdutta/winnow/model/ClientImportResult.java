package io.github.instaxdutta.winnow.model;

import java.util.List;

public record ClientImportResult(int imported, int skipped, List<String> errors) {

    public static ClientImportResult of(int imported, int skipped, List<String> errors) {
        return new ClientImportResult(imported, skipped, List.copyOf(errors));
    }
}

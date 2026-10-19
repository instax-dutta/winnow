package com.pg.seo.web;

import com.pg.seo.domain.ValidationException;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * The checks an uploaded file has to pass before it is parsed.
 *
 * <p>The type and the size are checked at the edge, the content is parsed in memory and
 * the name of the file is never turned into a path. The original wrote its exports to a
 * path that was built from the date strings of the request, which was a path traversal
 * primitive.
 */
final class UploadGuard {

    /** The largest upload the application accepts. */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/octet-stream",
            "application/zip");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls");

    private UploadGuard() {}

    /**
     * Checks one upload.
     *
     * @throws ValidationException when the file is empty, too large or not a spreadsheet
     */
    static void check(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("The uploaded file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ValidationException("The uploaded file is larger than 10 MB");
        }
        String extension = extension(file.getOriginalFilename());
        String type = file.getContentType();
        boolean typeAllowed = type != null && ALLOWED_TYPES.contains(type.toLowerCase(Locale.ROOT));
        if (!typeAllowed || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Only an Excel spreadsheet (.xlsx or .xls) can be uploaded");
        }
    }

    private static String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

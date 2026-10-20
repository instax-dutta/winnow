package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.ExtractionResult;
import io.github.instaxdutta.winnow.domain.ExtractionRun;
import io.github.instaxdutta.winnow.domain.ExtractionStatus;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.domain.ValidationException;
import io.github.instaxdutta.winnow.extraction.ExtractionSink;
import io.github.instaxdutta.winnow.extraction.ExtractionWorker;
import io.github.instaxdutta.winnow.model.ExtractionResultDto;
import io.github.instaxdutta.winnow.model.ExtractionRunDto;
import io.github.instaxdutta.winnow.model.PagedResponse;
import io.github.instaxdutta.winnow.repository.ExtractionResultRepository;
import io.github.instaxdutta.winnow.repository.ExtractionRunRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extraction runs and their results.
 *
 * <p>A run starts with a list of domains read from column A of an uploaded spreadsheet. The
 * domains are handed to the crawler one at a time. A domain that fails never fails the
 * run: it is counted and the run ends as partial.
 */
@Service
public class ExtractionService {

    /** The size of one insert batch. */
    private static final int RESULT_BATCH_SIZE = 200;

    /** The largest number of domains one run accepts. */
    private static final int MAX_DOMAINS = 5000;

    /** The largest page a caller may ask for. */
    private static final int MAX_PAGE_SIZE = 500;

    private static final Pattern DOMAIN = Pattern.compile(
            "^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$");

    private final ExtractionRunRepository runs;
    private final ExtractionResultRepository extractionResults;
    private final ObjectProvider<ExtractionWorker> workers;

    public ExtractionService(
            ExtractionRunRepository runs,
            ExtractionResultRepository extractionResults,
            ObjectProvider<ExtractionWorker> workers) {
        this.runs = runs;
        this.extractionResults = extractionResults;
        this.workers = workers;
    }

    /**
     * Starts a run over the domains in {@code excel}.
     *
     * <p>The run is created in the state {@code QUEUED} and the returned view is the run as
     * it was created. The crawl then runs inline, so the stored run usually has already
     * reached its final state when this method returns; {@link #get} reports the current
     * state.
     *
     * @param fileName the name of the uploaded file, kept as metadata only
     */
    // Not transactional on purpose: the crawl is an outbound call and it must not hold a
    // database transaction open.
    public ExtractionRunDto start(Long ownerUserId, String fileName, InputStream excel) {
        List<DomainRow> domains = readDomains(excel);
        Instant now = Instant.now();
        ExtractionRun run = new ExtractionRun();
        run.setOwnerUserId(ownerUserId);
        run.setStatus(ExtractionStatus.QUEUED);
        run.setDomainCount(domains.size());
        run.setSourceFileName(safeFileName(fileName));
        run.setStartedAt(now);
        run.setCreatedAt(now);
        run.setCreatedBy(ownerUserId);
        run.setUpdatedAt(now);
        run.setUpdatedBy(ownerUserId);
        runs.save(run);

        ExtractionRunDto started = toDto(run);
        crawl(run, domains);
        return started;
    }

    @Transactional(readOnly = true)
    public ExtractionRunDto get(Long ownerUserId, Long id) {
        return toDto(require(ownerUserId, id));
    }

    @Transactional(readOnly = true)
    public List<ExtractionRunDto> list(Long ownerUserId) {
        return runs.findByOwnerUserIdOrderByStartedAtDesc(ownerUserId).stream()
                .map(ExtractionService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ExtractionResultDto> results(Long ownerUserId, Long runId, int start, int length) {
        require(ownerUserId, runId);
        int pageSize = Math.min(Math.max(length, 1), MAX_PAGE_SIZE);
        int offset = Math.max(start, 0);
        List<ExtractionResult> all = extractionResults.findByRunId(runId);
        List<ExtractionResult> page = all.subList(
                Math.min(offset, all.size()), Math.min(offset + pageSize, all.size()));
        return PagedResponse.of(
                page.stream().map(ExtractionService::toDto).toList(),
                all.size(),
                all.size(),
                1,
                offset,
                pageSize);
    }

    /**
     * Closes a run that was left behind.
     *
     * <p>A run always ends in {@code SUCCESS}, {@code PARTIAL} or {@code FAILED}. It is
     * never left in {@code QUEUED} or {@code RUNNING}, so the operator always sees what
     * happened to it.
     *
     * @param run    the run to close
     * @param status the terminal status
     * @param detail why it ended that way, may be null
     */
    @Transactional
    public void closeRun(ExtractionRun run, ExtractionStatus status, String detail) {
        Instant finished = Instant.now();
        run.setStatus(status);
        run.setErrorDetail(detail);
        run.setFinishedAt(finished);
        run.setUpdatedAt(finished);
        runs.save(run);
    }

    /**
     * Hands the domains of a run to the extraction worker.
     *
     * <p>The worker owns the fan-out, the deduplication and the batching. The service owns
     * the run row and the sink that writes the results back. There is exactly one
     * implementation of the crawl, in the extraction package.
     */
    private void crawl(ExtractionRun run, List<DomainRow> domains) {
        ExtractionWorker worker = workers.getIfAvailable();
        if (worker == null) {
            run.setStatus(ExtractionStatus.FAILED);
            run.setErrorDetail("No extraction worker is registered");
            run.setFinishedAt(Instant.now());
            run.setUpdatedAt(Instant.now());
            runs.save(run);
            return;
        }
        Instant started = Instant.now();
        run.setStatus(ExtractionStatus.RUNNING);
        run.setStartedAt(started);
        run.setUpdatedAt(started);
        runs.save(run);

        List<ExtractionWorker.DomainEntry> entries = new ArrayList<>(domains.size());
        for (DomainRow row : domains) {
            entries.add(new ExtractionWorker.DomainEntry(row.domain(), row.rowNumber()));
        }
        worker.run(run.getId(), entries, sinkFor(run));
    }

    /**
     * The sink that writes the addresses of a run and closes it.
     *
     * <p>The sink deduplicates on the address hash, so a repeated address inside one run is
     * stored once. The unique index on (run_id, email_hash) is the backstop.
     *
     * @param run the run the addresses belong to
     * @return the sink, safe to call from several worker threads at once
     */
    public ExtractionSink sinkFor(ExtractionRun run) {
        return new RunSink(run);
    }

    /** Writes the addresses of a run and closes it. */
    private final class RunSink implements ExtractionSink {

        private final ExtractionRun run;
        private final Set<ByteBuffer> stored = ConcurrentHashMap.newKeySet();

        RunSink(ExtractionRun run) {
            this.run = run;
            for (ExtractionResult result : extractionResults.findByRunId(run.getId())) {
                stored.add(ByteBuffer.wrap(result.getEmailHash()));
            }
        }

        @Override
        public void storeAddresses(List<ExtractionWorker.FoundAddress> batch) {
            List<ExtractionResult> rows = new ArrayList<>(batch.size());
            for (ExtractionWorker.FoundAddress address : batch) {
                if (!stored.add(ByteBuffer.wrap(address.emailHash()))) {
                    continue;
                }
                ExtractionResult row = new ExtractionResult();
                row.setRun(run);
                row.setDomain(address.domain());
                row.setEmail(address.email());
                row.setEmailHash(address.emailHash());
                row.setSourceRow(address.sourceRow());
                row.setCreatedAt(Instant.now());
                rows.add(row);
            }
            if (!rows.isEmpty()) {
                extractionResults.saveAll(rows);
            }
        }

        @Override
        public void complete(ExtractionStatus status, String errorDetail) {
            Instant finished = Instant.now();
            run.setStatus(status);
            run.setErrorDetail(errorDetail);
            run.setFinishedAt(finished);
            run.setUpdatedAt(finished);
            runs.save(run);
        }
    }

    private List<DomainRow> readDomains(InputStream excel) {
        List<DomainRow> domains = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(excel)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return domains;
            }
            for (Row row : sheet) {
                if (domains.size() >= MAX_DOMAINS) {
                    break;
                }
                String value = normaliseDomain(cellText(row.getCell(0)));
                if (value == null) {
                    continue;
                }
                domains.add(new DomainRow(row.getRowNum() + 1, value));
            }
        } catch (IOException | RuntimeException e) {
            throw new ValidationException("The uploaded file could not be read as a spreadsheet");
        }
        return domains;
    }

    private ExtractionRun require(Long ownerUserId, Long id) {
        return runs
                .findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Extraction run", id));
    }

    static String normaliseDomain(String value) {
        if (value == null) {
            return null;
        }
        String domain = value.trim().toLowerCase(Locale.ROOT);
        if (domain.isEmpty()) {
            return null;
        }
        int scheme = domain.indexOf("://");
        if (scheme >= 0) {
            domain = domain.substring(scheme + 3);
        }
        int slash = domain.indexOf('/');
        if (slash >= 0) {
            domain = domain.substring(0, slash);
        }
        int colon = domain.indexOf(':');
        if (colon >= 0) {
            domain = domain.substring(0, colon);
        }
        while (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (domain.isEmpty() || !DOMAIN.matcher(domain).matches()) {
            return null;
        }
        return domain;
    }

    private static String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String name = fileName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            return null;
        }
        return name.length() > 512 ? name.substring(0, 512) : name;
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> Long.toString((long) cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private static ExtractionRunDto toDto(ExtractionRun run) {
        return new ExtractionRunDto(
                run.getId(),
                run.getStatus(),
                run.getDomainCount(),
                run.getSourceFileName(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorDetail());
    }

    private static ExtractionResultDto toDto(ExtractionResult result) {
        return new ExtractionResultDto(
                result.getId(), result.getDomain(), result.getEmail(), result.getSourceRow());
    }

    /** One domain of the uploaded list, with the row it came from. */
    private record DomainRow(int rowNumber, String domain) {}
}

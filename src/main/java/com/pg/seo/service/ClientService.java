package com.pg.seo.service;

import com.pg.seo.domain.Client;
import com.pg.seo.domain.ClientGroup;
import com.pg.seo.domain.NotFoundException;
import com.pg.seo.domain.TargetType;
import com.pg.seo.domain.ValidationException;
import com.pg.seo.model.ClientDto;
import com.pg.seo.model.ClientImportResult;
import com.pg.seo.model.PagedResponse;
import com.pg.seo.repository.ClientGroupRepository;
import com.pg.seo.repository.ClientRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client contacts and the Excel import.
 *
 * <p>The import reads column A for the name and column B for the address. A row with an
 * unusable address is skipped and reported, and the rows that survive are written in
 * batches. Nothing is ever written to a file, and the uploaded file name is never turned
 * into a path.
 */
@Service
public class ClientService {

    /** The size of one insert batch. */
    private static final int IMPORT_BATCH_SIZE = 200;

    /** The largest page a caller may ask for. */
    private static final int MAX_PAGE_SIZE = 500;

    /** The largest number of rows one import reads. */
    private static final int MAX_IMPORT_ROWS = 50000;

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ClientRepository clients;
    private final ClientGroupRepository groups;

    public ClientService(ClientRepository clients, ClientGroupRepository groups) {
        this.clients = clients;
        this.groups = groups;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClientDto> list(Long ownerUserId, int start, int length, String search) {
        int pageSize = Math.min(Math.max(length, 1), MAX_PAGE_SIZE);
        int offset = Math.max(start, 0);
        long total = clients.countByOwnerUserIdAndDeletedAtIsNull(ownerUserId);
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        if (needle.isEmpty()) {
            Page<Client> page = clients.findByOwnerUserIdAndDeletedAtIsNull(
                    ownerUserId, PageRequest.of(offset / pageSize, pageSize));
            return PagedResponse.of(
                    page.getContent().stream().map(ClientService::toDto).toList(),
                    total,
                    page.getTotalElements(),
                    1,
                    offset,
                    pageSize);
        }

        List<Client> matched = clients.findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(ownerUserId).stream()
                .filter(client -> matches(client, needle))
                .toList();
        List<Client> page = matched.subList(
                Math.min(offset, matched.size()), Math.min(offset + pageSize, matched.size()));
        return PagedResponse.of(
                page.stream().map(ClientService::toDto).toList(), total, matched.size(), 1, offset, pageSize);
    }

    @Transactional
    public ClientImportResult importExcel(Long ownerUserId, Long groupId, InputStream excel) {
        ClientGroup group = groupId == null ? null : requireGroup(ownerUserId, groupId);
        List<String> errors = new ArrayList<>();
        List<ImportedRow> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skipped = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(excel)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return ClientImportResult.of(0, 0, List.of("the workbook has no sheet"));
            }
            for (Row row : sheet) {
                if (rows.size() >= MAX_IMPORT_ROWS) {
                    errors.add("the file has more than " + MAX_IMPORT_ROWS + " usable rows, the rest was ignored");
                    break;
                }
                int rowNumber = row.getRowNum() + 1;
                String name = cellText(row.getCell(0));
                String email = cellText(row.getCell(1)).trim();

                if (name.isEmpty() && email.isEmpty()) {
                    continue;
                }
                if (!isValidEmail(email)) {
                    skipped++;
                    errors.add("row " + rowNumber + ": '" + email + "' is not a valid email address");
                    continue;
                }
                String key = email.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    skipped++;
                    errors.add("row " + rowNumber + ": '" + email + "' is already in this file");
                    continue;
                }
                rows.add(new ImportedRow(name.isEmpty() ? email.substring(0, email.indexOf('@')) : name, email));
            }
        } catch (IOException | RuntimeException e) {
            throw new ValidationException("The uploaded file could not be read as a spreadsheet");
        }

        int imported = 0;
        Instant now = Instant.now();
        List<Client> batch = new ArrayList<>(IMPORT_BATCH_SIZE);
        for (ImportedRow row : rows) {
            Client client = new Client();
            client.setOwnerUserId(ownerUserId);
            client.setName(row.name());
            client.setEmail(row.email());
            client.setGroup(group);
            client.setCreatedAt(now);
            client.setCreatedBy(ownerUserId);
            client.setUpdatedAt(now);
            client.setUpdatedBy(ownerUserId);
            batch.add(client);
            if (batch.size() >= IMPORT_BATCH_SIZE) {
                clients.saveAll(batch);
                imported += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            clients.saveAll(batch);
            imported += batch.size();
        }
        return ClientImportResult.of(imported, skipped, errors);
    }

    @Transactional
    public void delete(Long ownerUserId, Long id) {
        Client client = clients
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Client", id));
        Instant now = Instant.now();
        client.setDeletedAt(now);
        client.setUpdatedAt(now);
        client.setUpdatedBy(ownerUserId);
        clients.save(client);
    }

    /**
     * The recipients of a campaign.
     *
     * <p>A group is resolved inside the tenant. An individual id that belongs to another
     * tenant is skipped, never returned and never reported.
     */
    @Transactional(readOnly = true)
    public List<ClientDto> resolveTargets(
            Long ownerUserId, TargetType targetType, String groupCode, List<Long> clientIds) {
        if (targetType == TargetType.GROUP) {
            if (groupCode == null || groupCode.isBlank()) {
                return List.of();
            }
            ClientGroup group = groups
                    .findByOwnerUserIdAndCodeAndDeletedAtIsNull(ownerUserId, groupCode.trim())
                    .orElseThrow(() -> NotFoundException.of("Client group", groupCode));
            return clients
                    .findByOwnerUserIdAndGroupIdAndDeletedAtIsNullOrderByNameAsc(ownerUserId, group.getId())
                    .stream()
                    .map(ClientService::toDto)
                    .toList();
        }
        if (clientIds == null || clientIds.isEmpty()) {
            return List.of();
        }
        List<ClientDto> targets = new ArrayList<>();
        for (Long clientId : clientIds) {
            if (clientId == null) {
                continue;
            }
            clients.findByIdAndOwnerUserIdAndDeletedAtIsNull(clientId, ownerUserId)
                    .map(ClientService::toDto)
                    .ifPresent(targets::add);
        }
        return targets;
    }

    private ClientGroup requireGroup(Long ownerUserId, Long groupId) {
        return groups
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(groupId, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Client group", groupId));
    }

    private static boolean matches(Client client, String needle) {
        String name = client.getName() == null ? "" : client.getName().toLowerCase(Locale.ROOT);
        String email = client.getEmail() == null ? "" : client.getEmail().toLowerCase(Locale.ROOT);
        return name.contains(needle) || email.contains(needle);
    }

    static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                yield value == Math.rint(value)
                        ? Long.toString((long) value)
                        : Double.toString(value);
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /** One usable row of the uploaded file. */
    private record ImportedRow(String name, String email) {}

    static ClientDto toDto(Client client) {
        ClientGroup group = client.getGroup();
        return new ClientDto(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getWebLink(),
                group == null ? null : group.getId(),
                group == null ? null : group.getCode());
    }
}

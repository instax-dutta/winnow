/**
 * extraction.js - the extraction run list.
 *
 * The runs come from GET /api/extraction/runs. A run is started by posting
 * a spreadsheet to the same path, and the addresses a run found are read
 * from /api/extraction/runs/{id}/results.
 */

import { ENDPOINTS, PAGE_SIZE, asList, get, resource, upload } from './api.js';
import {
  actionButton,
  closeDialog,
  createListView,
  escapeHtml,
  formatDateTime,
  openDialog,
  resetForm,
  setDialogError,
  statusBadge,
  toast,
} from './ui.js';

const dialog = document.querySelector('[data-role="dialog"]');
const form = dialog ? dialog.querySelector('[data-role="form"]') : null;
const submitButton = dialog ? dialog.querySelector('[data-role="submit"]') : null;
const resultsDialog = document.querySelector('[data-role="results-dialog"]');
const resultRows = resultsDialog ? resultsDialog.querySelector('[data-role="result-rows"]') : null;
const resultEmpty = resultsDialog ? resultsDialog.querySelector('[data-role="result-empty"]') : null;
const resultTable = resultsDialog ? resultsDialog.querySelector('table') : null;
const statusFilter = document.querySelector('[data-role="status-filter"]');
const rows = document.querySelector('[data-role="rows"]');

const view = createListView({
  renderRow: (run) => `
    <tr>
      <td class="cell-strong">${escapeHtml(run.sourceFileName || 'Run ' + run.id)}</td>
      <td>${escapeHtml(run.domainCount)}</td>
      <td>${statusBadge(run.status)}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(run.startedAt))}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(run.finishedAt))}</td>
      <td>${escapeHtml(run.errorDetail || '')}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('results', run.id, 'eye', 'Show the addresses this run found')}
        </span>
      </td>
    </tr>`,
  match: (run, query) =>
    [run.sourceFileName, run.status]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (run) => {
    const status = statusFilter ? statusFilter.value : '';
    return !status || String(run.status) === status;
  },
});

function openEditor() {
  resetForm(form, dialog);
  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = 'New extraction run';
  if (submitButton) submitButton.textContent = 'Start run';
  openDialog(dialog);
}

async function load() {
  try {
    view.setRows(asList(await get(ENDPOINTS.extractionRuns)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function showResults(run) {
  if (!resultsDialog) return;
  if (resultRows) resultRows.innerHTML = '';
  if (resultEmpty) resultEmpty.hidden = true;
  if (resultTable) resultTable.hidden = false;

  const title = resultsDialog.querySelector('[data-role="title"]');
  if (title) title.textContent = `Addresses found by ${run.sourceFileName || 'run ' + run.id}`;

  openDialog(resultsDialog);

  try {
    const results = asList(
      await get(resource(ENDPOINTS.extractionRuns, run.id) + '/results', { start: 0, length: PAGE_SIZE }),
    );
    if (resultRows) {
      resultRows.innerHTML = results
        .map(
          (result) => `
          <tr>
            <td>${escapeHtml(result.domain)}</td>
            <td>${escapeHtml(result.email)}</td>
            <td>${escapeHtml(result.sourceRow)}</td>
          </tr>`,
        )
        .join('');
    }
    const found = results.length > 0;
    if (resultTable) resultTable.hidden = !found;
    if (resultEmpty) resultEmpty.hidden = found;
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="results"]');
    if (!button) return;
    const run = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (run) showResults(run);
  });
}

document.querySelectorAll('.page-actions [data-action="create"], .empty-actions [data-action="create"]').forEach((button) => {
  button.addEventListener('click', openEditor);
});

if (statusFilter) {
  statusFilter.addEventListener('change', () => view.pager.refresh());
}

if (form) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setDialogError(dialog, '');

    const file = form.file.files && form.file.files[0];
    if (!file) {
      setDialogError(dialog, 'Choose a spreadsheet to read.');
      return;
    }

    const data = new FormData();
    data.append('file', file);

    if (submitButton) submitButton.disabled = true;
    try {
      await upload(ENDPOINTS.extractionRuns, data);
      closeDialog(dialog);
      toast('Extraction run started.', 'success');
      await load();
    } catch (error) {
      setDialogError(dialog, error.message);
    } finally {
      if (submitButton) submitButton.disabled = false;
    }
  });
}

if (dialog) {
  dialog.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(dialog));
  });
}

if (resultsDialog) {
  resultsDialog.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(resultsDialog));
  });
}

load();

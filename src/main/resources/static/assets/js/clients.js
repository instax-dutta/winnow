/**
 * clients.js - the client list.
 *
 * The list comes from GET /api/clients. Clients are added by importing a
 * spreadsheet and removed with a DELETE on the same resource.
 */

import { ENDPOINTS, PAGE_SIZE, asList, get, remove, resource, upload } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  externalLink,
  fillSelect,
  openDialog,
  resetForm,
  setDialogError,
  toast,
} from './ui.js';

const dialog = document.querySelector('[data-role="dialog"]');
const form = dialog ? dialog.querySelector('[data-role="form"]') : null;
const submitButton = dialog ? dialog.querySelector('[data-role="submit"]') : null;
const groupSelect = dialog ? dialog.querySelector('[data-role="group-select"]') : null;
const groupFilter = document.querySelector('[data-role="group-filter"]');
const rows = document.querySelector('[data-role="rows"]');

const view = createListView({
  renderRow: (client) => `
    <tr>
      <td class="cell-strong">${escapeHtml(client.name)}</td>
      <td>${escapeHtml(client.email)}</td>
      <td>${externalLink(client.webLink, client.webLink)}</td>
      <td>${escapeHtml(client.groupCode || '')}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('delete', client.id, 'trash', 'Delete client')}
        </span>
      </td>
    </tr>`,
  match: (client, query) =>
    [client.name, client.email, client.webLink, client.groupCode]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (client) => {
    const groupId = groupFilter ? groupFilter.value : '';
    return !groupId || String(client.groupId) === groupId;
  },
});

function openImport() {
  resetForm(form, dialog);
  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = 'Import clients';
  if (submitButton) submitButton.textContent = 'Import clients';
  openDialog(dialog);
}

async function loadGroups() {
  try {
    const groups = asList(await get(ENDPOINTS.clientGroups));
    fillSelect(groupSelect, groups, { placeholder: 'No group' });
    fillSelect(groupFilter, groups, { placeholder: 'All groups' });
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function load() {
  try {
    const page = await get(ENDPOINTS.clients, { start: 0, length: PAGE_SIZE });
    view.setRows(asList(page));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function deleteClient(client) {
  const confirmed = await confirmDialog({
    title: 'Delete client',
    message: `Delete ${client.name}? This cannot be undone.`,
    confirmLabel: 'Delete',
  });
  if (!confirmed) return;

  try {
    await remove(resource(ENDPOINTS.clients, client.id));
    toast('Client deleted.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="delete"]');
    if (!button) return;
    const client = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (client) deleteClient(client);
  });
}

document.querySelectorAll('.page-actions [data-action="import"], .empty-actions [data-action="import"]').forEach((button) => {
  button.addEventListener('click', openImport);
});

if (groupFilter) {
  groupFilter.addEventListener('change', () => view.pager.refresh());
}

if (form) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setDialogError(dialog, '');

    const file = form.file.files && form.file.files[0];
    if (!file) {
      setDialogError(dialog, 'Choose a spreadsheet to import.');
      return;
    }

    const data = new FormData();
    data.append('file', file);
    if (groupSelect && groupSelect.value) data.append('groupId', groupSelect.value);

    if (submitButton) submitButton.disabled = true;
    try {
      const result = await upload(ENDPOINTS.clientImport, data);
      const imported = result && typeof result.imported === 'number' ? result.imported : 0;
      const skipped = result && typeof result.skipped === 'number' ? result.skipped : 0;
      closeDialog(dialog);
      toast(
        skipped ? `Imported ${imported} clients, skipped ${skipped}.` : `Imported ${imported} clients.`,
        'success',
      );
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

loadGroups();
load();

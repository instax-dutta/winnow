/**
 * client-groups.js - the client group list.
 *
 * The list comes from GET /api/client-groups. A group is created and
 * changed with a ClientGroupRequest: a short code and a name.
 */

import { ENDPOINTS, asList, get, post, put, remove, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  formValues,
  openDialog,
  resetForm,
  setDialogError,
  toast,
} from './ui.js';

const dialog = document.querySelector('[data-role="dialog"]');
const form = dialog ? dialog.querySelector('[data-role="form"]') : null;
const submitButton = dialog ? dialog.querySelector('[data-role="submit"]') : null;
const rows = document.querySelector('[data-role="rows"]');

let editing = null;

const view = createListView({
  renderRow: (group) => `
    <tr>
      <td class="cell-strong mono">${escapeHtml(group.code)}</td>
      <td>${escapeHtml(group.name)}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('edit', group.id, 'pencil', 'Edit group')}
          ${actionButton('delete', group.id, 'trash', 'Delete group')}
        </span>
      </td>
    </tr>`,
  match: (group, query) =>
    [group.code, group.name]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
});

function openEditor(group) {
  editing = group || null;
  resetForm(form, dialog);

  if (group) {
    form.code.value = group.code || '';
    form.name.value = group.name || '';
  }

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = group ? 'Edit group' : 'Add group';
  if (submitButton) submitButton.textContent = group ? 'Save changes' : 'Save group';

  openDialog(dialog);
}

async function load() {
  try {
    view.setRows(asList(await get(ENDPOINTS.clientGroups)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function deleteGroup(group) {
  const confirmed = await confirmDialog({
    title: 'Delete group',
    message: `Delete the group ${group.code}? Clients in it are not deleted.`,
    confirmLabel: 'Delete',
  });
  if (!confirmed) return;

  try {
    await remove(resource(ENDPOINTS.clientGroups, group.id));
    toast('Group deleted.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const group = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (!group) return;
    if (button.dataset.action === 'edit') openEditor(group);
    if (button.dataset.action === 'delete') deleteGroup(group);
  });
}

document.querySelectorAll('.page-actions [data-action="create"], .empty-actions [data-action="create"]').forEach((button) => {
  button.addEventListener('click', () => openEditor(null));
});

if (form) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setDialogError(dialog, '');

    const values = formValues(form);
    const code = (values.code || '').trim();
    const name = (values.name || '').trim();

    if (!code || !name) {
      setDialogError(dialog, 'A code and a name are required.');
      return;
    }

    const payload = { code, name };

    if (submitButton) submitButton.disabled = true;
    try {
      if (editing) {
        await put(resource(ENDPOINTS.clientGroups, editing.id), payload);
        toast('Group updated.', 'success');
      } else {
        await post(ENDPOINTS.clientGroups, payload);
        toast('Group added.', 'success');
      }
      closeDialog(dialog);
      editing = null;
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

load();

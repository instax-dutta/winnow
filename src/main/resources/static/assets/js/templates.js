/**
 * templates.js - the email template list.
 *
 * The list comes from GET /api/templates. A template is created and changed
 * with an EmailTemplateRequest: a name, a subject and the body.
 */

import { ENDPOINTS, asList, get, post, put, remove, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  formatDateTime,
  formValues,
  openDialog,
  resetForm,
  setDialogError,
  statusBadge,
  toast,
} from './ui.js';

const dialog = document.querySelector('[data-role="dialog"]');
const form = dialog ? dialog.querySelector('[data-role="form"]') : null;
const submitButton = dialog ? dialog.querySelector('[data-role="submit"]') : null;
const preview = document.querySelector('[data-role="preview-dialog"]');
const previewBody = preview ? preview.querySelector('[data-role="preview-body"]') : null;
const previewSubject = preview ? preview.querySelector('[data-role="preview-subject"]') : null;
const previewTitle = preview ? preview.querySelector('[data-role="title"]') : null;
const statusFilter = document.querySelector('[data-role="status-filter"]');
const rows = document.querySelector('[data-role="rows"]');

let editing = null;

const view = createListView({
  renderRow: (template) => `
    <tr>
      <td class="cell-strong">${escapeHtml(template.name)}</td>
      <td>${escapeHtml(template.subject)}</td>
      <td>${statusBadge(template.status)}</td>
      <td>${escapeHtml(template.version)}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(template.updatedAt))}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('preview', template.id, 'eye', 'Preview template')}
          ${template.status === 'DRAFT' ? actionButton('submit', template.id, 'send', 'Send for approval') : ''}
          ${actionButton('edit', template.id, 'pencil', 'Edit template')}
          ${actionButton('delete', template.id, 'trash', 'Delete template')}
        </span>
      </td>
    </tr>`,
  match: (template, query) =>
    [template.name, template.subject]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (template) => {
    const status = statusFilter ? statusFilter.value : '';
    return !status || String(template.status) === status;
  },
});

function openEditor(template) {
  editing = template || null;
  resetForm(form, dialog);

  if (template) {
    form.name.value = template.name || '';
    form.subject.value = template.subject || '';
    form.contentHtml.value = template.contentHtml || '';
  }

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = template ? 'Edit template' : 'New template';
  if (submitButton) submitButton.textContent = template ? 'Save changes' : 'Save template';

  openDialog(dialog);
}

function openPreview(template) {
  if (!preview) return;
  if (previewTitle) previewTitle.textContent = template.name || 'Template preview';
  if (previewSubject) previewSubject.textContent = template.subject || '';
  if (previewBody) {
    // The body is the one field whose contract is "trusted HTML": it is
    // sanitised when the template is saved, so it is rendered as markup.
    previewBody.innerHTML = template.contentHtml || '';
  }
  openDialog(preview);
}

async function load() {
  try {
    view.setRows(asList(await get(ENDPOINTS.templates)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function deleteTemplate(template) {
  const confirmed = await confirmDialog({
    title: 'Delete template',
    message: `Delete the template ${template.name}? This cannot be undone.`,
    confirmLabel: 'Delete',
  });
  if (!confirmed) return;

  try {
    await remove(resource(ENDPOINTS.templates, template.id));
    toast('Template deleted.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function submitForApproval(template) {
  const confirmed = await confirmDialog({
    title: 'Send for approval',
    message: `Send ${template.name} for approval? A campaign can only use an approved template.`,
    confirmLabel: 'Send',
    kind: 'primary',
  });
  if (!confirmed) return;

  try {
    await post(resource(ENDPOINTS.templates, template.id) + '/submit');
    toast('Template sent for approval.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const template = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (!template) return;
    if (button.dataset.action === 'edit') openEditor(template);
    if (button.dataset.action === 'preview') openPreview(template);
    if (button.dataset.action === 'submit') submitForApproval(template);
    if (button.dataset.action === 'delete') deleteTemplate(template);
  });
}

document.querySelectorAll('.page-actions [data-action="create"], .empty-actions [data-action="create"]').forEach((button) => {
  button.addEventListener('click', () => openEditor(null));
});

if (statusFilter) {
  statusFilter.addEventListener('change', () => view.pager.refresh());
}

if (form) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setDialogError(dialog, '');

    const values = formValues(form);
    const name = (values.name || '').trim();
    const subject = (values.subject || '').trim();
    const contentHtml = values.contentHtml || '';

    if (!name || !subject || !contentHtml.trim()) {
      setDialogError(dialog, 'A name, a subject and a body are required.');
      return;
    }

    const payload = { name, subject, contentHtml };

    if (submitButton) submitButton.disabled = true;
    try {
      if (editing) {
        await put(resource(ENDPOINTS.templates, editing.id), payload);
        toast('Template updated.', 'success');
      } else {
        await post(ENDPOINTS.templates, payload);
        toast('Template saved and sent for approval.', 'success');
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

if (preview) {
  preview.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(preview));
  });
}

load();

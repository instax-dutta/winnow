/**
 * template-approvals.js - the approval queue.
 *
 * The queue comes from GET /api/templates/pending. A template is approved
 * with POST /api/templates/{id}/approve and rejected with the same shape
 * and a different verb.
 */

import { ENDPOINTS, asList, get, post, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  formatDateTime,
  openDialog,
  statusBadge,
  toast,
} from './ui.js';

const preview = document.querySelector('[data-role="preview-dialog"]');
const previewBody = preview ? preview.querySelector('[data-role="preview-body"]') : null;
const previewSubject = preview ? preview.querySelector('[data-role="preview-subject"]') : null;
const previewTitle = preview ? preview.querySelector('[data-role="title"]') : null;
const rows = document.querySelector('[data-role="rows"]');

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
          ${actionButton('approve', template.id, 'check', 'Approve template')}
          ${actionButton('reject', template.id, 'close', 'Reject template')}
        </span>
      </td>
    </tr>`,
  match: (template, query) =>
    [template.name, template.subject]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
});

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
    view.setRows(asList(await get(ENDPOINTS.pendingTemplates)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function decide(template, verb, label) {
  const confirmed = await confirmDialog({
    title: `${label} template`,
    message: `${label} the template ${template.name}?`,
    confirmLabel: label,
    kind: verb === 'approve' ? 'primary' : 'danger',
  });
  if (!confirmed) return;

  try {
    await post(resource(ENDPOINTS.templates, template.id) + `/${verb}`);
    toast(`Template ${verb === 'approve' ? 'approved' : 'rejected'}.`, 'success');
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
    if (button.dataset.action === 'preview') openPreview(template);
    if (button.dataset.action === 'approve') decide(template, 'approve', 'Approve');
    if (button.dataset.action === 'reject') decide(template, 'reject', 'Reject');
  });
}

if (preview) {
  preview.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(preview));
  });
}

load();

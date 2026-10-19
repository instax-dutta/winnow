/**
 * outbox.js - the scheduled message list.
 *
 * The list comes from GET /api/outbox. A message is scheduled with an
 * OutboxRequest: the sending mailbox, the recipient, and the time it
 * should go out.
 */

import { ENDPOINTS, asList, get, post, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  fillSelect,
  formValues,
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
const accountSelect = dialog ? dialog.querySelector('#outbox-account') : null;
const statusFilter = document.querySelector('[data-role="status-filter"]');
const rows = document.querySelector('[data-role="rows"]');

let accounts = [];

const view = createListView({
  renderRow: (message) => `
    <tr>
      <td class="cell-strong">${escapeHtml(message.toEmail)}</td>
      <td>${escapeHtml(message.subject)}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(message.scheduledAt))}</td>
      <td>${statusBadge(message.status)}</td>
      <td>${escapeHtml(message.attemptCount)}</td>
      <td>${escapeHtml(message.errorMessage || '')}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${message.status === 'SCHEDULED'
            ? actionButton('cancel', message.id, 'close', 'Cancel this message')
            : ''}
        </span>
      </td>
    </tr>`,
  match: (message, query) =>
    [message.toEmail, message.subject, message.errorMessage]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (message) => {
    const status = statusFilter ? statusFilter.value : '';
    return !status || String(message.status) === status;
  },
});

function openEditor() {
  resetForm(form, dialog);

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = 'Schedule a message';
  if (submitButton) submitButton.textContent = 'Schedule message';

  openDialog(dialog);
}

async function load() {
  try {
    const list = await get(ENDPOINTS.outbox, {
      from: '1970-01-01T00:00:00Z',
      to: new Date().toISOString(),
    });
    view.setRows(asList(list));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function cancelMessage(message) {
  const confirmed = await confirmDialog({
    title: 'Cancel message',
    message: `Cancel the message to ${message.toEmail}? It will not be sent.`,
    confirmLabel: 'Cancel message',
  });
  if (!confirmed) return;

  try {
    await post(resource(ENDPOINTS.outbox, message.id) + '/cancel');
    toast('Message cancelled.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function loadAccounts() {
  try {
    const [smtp, gmail] = await Promise.all([get(ENDPOINTS.smtpAccounts), get(ENDPOINTS.gmailAccounts)]);
    accounts = [
      ...asList(smtp).map((account) => ({ id: account.id, email: `${account.email} (SMTP)` })),
      ...asList(gmail).map((account) => ({ id: account.id, email: `${account.email} (Gmail)` })),
    ];
    fillSelect(accountSelect, accounts, { placeholder: 'Choose a mailbox' });
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="cancel"]');
    if (!button) return;
    const message = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (message) cancelMessage(message);
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

    const values = formValues(form);
    const fromAccountId = values.fromAccountId ? Number(values.fromAccountId) : null;
    const toEmail = (values.toEmail || '').trim();
    const scheduledAt = values.scheduledAt || '';

    if (!fromAccountId || !toEmail || !scheduledAt) {
      setDialogError(dialog, 'A mailbox, a recipient and a send time are required.');
      return;
    }

    const when = new Date(scheduledAt);
    if (Number.isNaN(when.getTime())) {
      setDialogError(dialog, 'The send time is not a valid date and time.');
      return;
    }

    const payload = {
      fromAccountId,
      toEmail,
      subject: (values.subject || '').trim() || null,
      bodyHtml: values.bodyHtml || null,
      scheduledAt: when.toISOString(),
    };

    if (submitButton) submitButton.disabled = true;
    try {
      await post(ENDPOINTS.outbox, payload);
      closeDialog(dialog);
      toast('Message scheduled.', 'success');
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

loadAccounts();
load();

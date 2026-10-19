/**
 * smtp-accounts.js - the SMTP account list.
 *
 * The list comes from GET /api/smtp-accounts. The secret is write only:
 * it is sent when the account is saved and it is never read back, so the
 * form only ever shows whether a secret is stored.
 */

import { ENDPOINTS, asList, get, post, put, remove, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  credentialBadge,
  enabledBadge,
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
const secretHint = dialog ? dialog.querySelector('[data-role="secret-hint"]') : null;
const rows = document.querySelector('[data-role="rows"]');

let editing = null;

const view = createListView({
  renderRow: (account) => `
    <tr>
      <td class="cell-strong">${escapeHtml(account.email)}</td>
      <td>${escapeHtml(account.protocol)}</td>
      <td>${escapeHtml(account.host)}</td>
      <td>${escapeHtml(account.port)}</td>
      <td>${escapeHtml(account.authMethod)}</td>
      <td>${credentialBadge(account.hasCredential)}</td>
      <td>${enabledBadge(account.enabled)}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('edit', account.id, 'pencil', 'Edit account')}
          ${actionButton('delete', account.id, 'trash', 'Delete account')}
        </span>
      </td>
    </tr>`,
  match: (account, query) =>
    [account.email, account.host, account.username]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
});

function openEditor(account) {
  editing = account || null;
  resetForm(form, dialog);

  if (account) {
    form.email.value = account.email || '';
    form.protocol.value = account.protocol || 'SMTP';
    form.host.value = account.host || '';
    form.port.value = account.port || 587;
    form.username.value = account.username || '';
    form.enabled.checked = account.enabled !== false;
  } else {
    form.port.value = 587;
    form.enabled.checked = true;
  }
  form.secret.value = '';

  if (secretHint) {
    secretHint.textContent = account && account.hasCredential
      ? 'A secret is stored. Leave this empty to keep it unchanged.'
      : 'No secret is stored yet.';
  }

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = account ? 'Edit SMTP account' : 'Add SMTP account';
  if (submitButton) submitButton.textContent = account ? 'Save changes' : 'Save account';

  openDialog(dialog);
}

async function load() {
  try {
    view.setRows(asList(await get(ENDPOINTS.smtpAccounts)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function deleteAccount(account) {
  const confirmed = await confirmDialog({
    title: 'Delete SMTP account',
    message: `Delete the account ${account.email}? This cannot be undone.`,
    confirmLabel: 'Delete',
  });
  if (!confirmed) return;

  try {
    await remove(resource(ENDPOINTS.smtpAccounts, account.id));
    toast('Account deleted.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const account = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (!account) return;
    if (button.dataset.action === 'edit') openEditor(account);
    if (button.dataset.action === 'delete') deleteAccount(account);
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
    const email = (values.email || '').trim();
    const host = (values.host || '').trim();
    const username = (values.username || '').trim();
    const port = Number(values.port);

    if (!email || !host || !username) {
      setDialogError(dialog, 'An address, a host and a username are required.');
      return;
    }
    if (!Number.isInteger(port) || port < 1 || port > 65535) {
      setDialogError(dialog, 'The port has to be between 1 and 65535.');
      return;
    }

    const payload = {
      email,
      protocol: values.protocol || 'SMTP',
      host,
      port,
      username,
      authMethod: values.authMethod || 'PLAIN',
      enabled: values.enabled === 'on' || values.enabled === 'true',
      secret: values.secret ? values.secret : null,
    };

    if (submitButton) submitButton.disabled = true;
    try {
      if (editing) {
        await put(resource(ENDPOINTS.smtpAccounts, editing.id), payload);
        toast('Account updated.', 'success');
      } else {
        await post(ENDPOINTS.smtpAccounts, payload);
        toast('Account added.', 'success');
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

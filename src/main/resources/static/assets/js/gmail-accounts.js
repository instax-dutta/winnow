/**
 * gmail-accounts.js - the Gmail account list.
 *
 * The list comes from GET /api/gmail-accounts. The client secret and the
 * refresh token are write only: they go out with a save and they are never
 * read back, so the form only ever shows whether a credential is stored.
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
const secretHints = dialog ? dialog.querySelectorAll('[data-role="secret-hint"]') : [];
const rows = document.querySelector('[data-role="rows"]');

let editing = null;

const view = createListView({
  renderRow: (account) => `
    <tr>
      <td class="cell-strong">${escapeHtml(account.email)}</td>
      <td class="mono">${escapeHtml(account.oauthClientId)}</td>
      <td>${escapeHtml(account.scopes)}</td>
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
    [account.email, account.oauthClientId, account.scopes]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
});

function openEditor(account) {
  editing = account || null;
  resetForm(form, dialog);

  if (account) {
    form.email.value = account.email || '';
    form.oauthClientId.value = account.oauthClientId || '';
    form.scopes.value = account.scopes || '';
    form.enabled.checked = account.enabled !== false;
  } else {
    form.enabled.checked = true;
  }
  form.clientSecret.value = '';
  form.refreshToken.value = '';

  secretHints.forEach((hint, index) => {
    hint.textContent = account && account.hasCredential
      ? `A credential is stored. Leave this empty to keep it unchanged.`
      : 'No credential is stored yet.';
    void index;
  });

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = account ? 'Edit Gmail account' : 'Add Gmail account';
  if (submitButton) submitButton.textContent = account ? 'Save changes' : 'Save account';

  openDialog(dialog);
}

async function load() {
  try {
    view.setRows(asList(await get(ENDPOINTS.gmailAccounts)));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function deleteAccount(account) {
  const confirmed = await confirmDialog({
    title: 'Delete Gmail account',
    message: `Delete the account ${account.email}? This cannot be undone.`,
    confirmLabel: 'Delete',
  });
  if (!confirmed) return;

  try {
    await remove(resource(ENDPOINTS.gmailAccounts, account.id));
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
    const oauthClientId = (values.oauthClientId || '').trim();

    if (!email || !oauthClientId) {
      setDialogError(dialog, 'An address and an OAuth client id are required.');
      return;
    }

    const payload = {
      email,
      oauthClientId,
      scopes: (values.scopes || '').trim() || null,
      enabled: values.enabled === 'on' || values.enabled === 'true',
      clientSecret: values.clientSecret ? values.clientSecret : null,
      refreshToken: values.refreshToken ? values.refreshToken : null,
    };

    if (submitButton) submitButton.disabled = true;
    try {
      if (editing) {
        await put(resource(ENDPOINTS.gmailAccounts, editing.id), payload);
        toast('Account updated.', 'success');
      } else {
        await post(ENDPOINTS.gmailAccounts, payload);
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

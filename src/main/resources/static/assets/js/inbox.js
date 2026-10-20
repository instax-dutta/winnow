/**
 * inbox.js - the inbound message list.
 *
 * The messages come from GET /api/inbox, which always needs the mailbox to
 * read. The mailbox is picked in the toolbar, and the list is reloaded when
 * the pick changes.
 */

import { ENDPOINTS, asList, get } from './api.js';
import {
  actionButton,
  badge,
  closeDialog,
  createListView,
  escapeHtml,
  fillSelect,
  formatDateTime,
  openDialog,
  toast,
} from './ui.js';

const preview = document.querySelector('[data-role="preview-dialog"]');
const previewBody = preview ? preview.querySelector('[data-role="preview-body"]') : null;
const previewFrom = preview ? preview.querySelector('[data-role="preview-from"]') : null;
const previewReceived = preview ? preview.querySelector('[data-role="preview-received"]') : null;
const previewTitle = preview ? preview.querySelector('[data-role="title"]') : null;
const accountSelect = document.querySelector('[data-role="account"]');
const stateFilter = document.querySelector('[data-role="state-filter"]');
const emptyTitle = document.querySelector('[data-role="empty-title"]');
const emptyText = document.querySelector('[data-role="empty-text"]');
const rows = document.querySelector('[data-role="rows"]');

let current = null;

const view = createListView({
  renderRow: (message) => `
    <tr>
      <td class="cell-strong">${escapeHtml(message.fromAddress)}</td>
      <td>${escapeHtml(message.subject)}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(message.receivedAt))}</td>
      <td>${message.read ? badge('Read', 'neutral') : badge('Unread', 'accent')}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('preview', message.id, 'eye', 'Read the message')}
        </span>
      </td>
    </tr>`,
  match: (message, query) =>
    [message.fromAddress, message.subject]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (message) => {
    const state = stateFilter ? stateFilter.value : '';
    if (!state) return true;
    return state === 'read' ? Boolean(message.read) : !message.read;
  },
});

function setEmptyState(title, text) {
  if (emptyTitle) emptyTitle.textContent = title;
  if (emptyText) emptyText.textContent = text;
}

async function load() {
  const accountId = accountSelect ? accountSelect.value : '';
  if (!accountId) {
    view.setRows([]);
    setEmptyState(
      'Choose a mailbox',
      'Replies are collected from the mailboxes you send from. Pick one to read it.',
    );
    return;
  }

  try {
    const list = await get(ENDPOINTS.inbox, {
      accountId,
      from: '1970-01-01T00:00:00Z',
      to: new Date().toISOString(),
    });
    view.setRows(asList(list));
    setEmptyState('No messages yet', 'This mailbox has not received any replies.');
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function loadAccounts() {
  try {
    const accounts = asList(await get(ENDPOINTS.smtpAccounts));
    fillSelect(
      accountSelect,
      accounts.map((account) => ({ id: account.id, email: account.email })),
      { placeholder: 'Choose a mailbox' },
    );
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

function openPreview(message) {
  current = message;
  if (previewTitle) previewTitle.textContent = message.subject || 'Message';
  if (previewFrom) previewFrom.textContent = message.fromAddress || '';
  if (previewReceived) previewReceived.textContent = formatDateTime(message.receivedAt);
  if (previewBody) {
    // The body is the one field whose contract is "trusted HTML": it is
    // sanitised when the message is stored, so it is rendered as markup.
    previewBody.innerHTML = message.bodyHtml || '';
  }
  openDialog(preview);
}

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action="preview"]');
    if (!button) return;
    const message = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (message) openPreview(message);
  });
}

if (preview) {
  preview.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(preview));
  });
}

if (accountSelect) {
  accountSelect.addEventListener('change', () => load());
}

if (stateFilter) {
  stateFilter.addEventListener('change', () => view.pager.refresh());
}

loadAccounts();

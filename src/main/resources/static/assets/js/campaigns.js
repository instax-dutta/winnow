/**
 * campaigns.js - the campaign list and the form that starts one.
 *
 * The list comes from GET /api/campaigns. A campaign is started with a
 * CampaignRequest: the sending mailbox, the template, and either a client
 * group or a hand picked set of clients.
 */

import { ENDPOINTS, PAGE_SIZE, asList, get, post, resource } from './api.js';
import {
  actionButton,
  closeDialog,
  confirmDialog,
  createListView,
  escapeHtml,
  fillSelect,
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
const statusFilter = document.querySelector('[data-role="status-filter"]');
const accountKind = dialog ? dialog.querySelector('#campaign-account-kind') : null;
const accountSelect = dialog ? dialog.querySelector('#campaign-account') : null;
const templateSelect = dialog ? dialog.querySelector('#campaign-template') : null;
const targetSelect = dialog ? dialog.querySelector('#campaign-target') : null;
const groupSelect = dialog ? dialog.querySelector('#campaign-group') : null;
const clientSelect = dialog ? dialog.querySelector('#campaign-clients') : null;
const groupField = dialog ? dialog.querySelector('[data-role="group-field"]') : null;
const clientField = dialog ? dialog.querySelector('[data-role="client-field"]') : null;
const deliveriesDialog = document.querySelector('[data-role="deliveries-dialog"]');
const deliveryRows = deliveriesDialog ? deliveriesDialog.querySelector('[data-role="delivery-rows"]') : null;
const deliveryEmpty = deliveriesDialog ? deliveriesDialog.querySelector('[data-role="delivery-empty"]') : null;
const deliveryTable = deliveriesDialog ? deliveriesDialog.querySelector('table') : null;
const rows = document.querySelector('[data-role="rows"]');

let accounts = { SMTP: [], GMAIL: [] };
let groups = [];
let clients = [];

const view = createListView({
  renderRow: (campaign) => `
    <tr>
      <td class="cell-strong">${escapeHtml(campaign.fromEmail)}</td>
      <td>${escapeHtml(campaign.templateName)}</td>
      <td>${escapeHtml(campaign.targetType)}</td>
      <td>${escapeHtml(campaign.recipientCount)}</td>
      <td>${escapeHtml(campaign.successCount)}</td>
      <td>${escapeHtml(campaign.failureCount)}</td>
      <td>${statusBadge(campaign.status)}</td>
      <td class="nowrap">${escapeHtml(formatDateTime(campaign.createdAt))}</td>
      <td class="col-actions">
        <span class="row-actions">
          ${actionButton('deliveries', campaign.id, 'eye', 'Show the recipients of this campaign')}
          ${campaign.status === 'QUEUED' || campaign.status === 'RUNNING'
            ? actionButton('cancel', campaign.id, 'close', 'Cancel this campaign')
            : ''}
        </span>
      </td>
    </tr>`,
  match: (campaign, query) =>
    [campaign.fromEmail, campaign.templateName, campaign.targetType]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query)),
  filter: (campaign) => {
    const status = statusFilter ? statusFilter.value : '';
    return !status || String(campaign.status) === status;
  },
});

function fillAccounts() {
  const kind = accountKind ? accountKind.value : 'SMTP';
  fillSelect(
    accountSelect,
    accounts[kind] || [],
    { placeholder: 'Choose a mailbox', value: 'id', label: 'email' },
  );
}

function fillTemplates(templates) {
  const approved = templates.filter((template) => String(template.status).toUpperCase() === 'APPROVED');
  fillSelect(templateSelect, approved, {
    placeholder: 'Choose an approved template',
    value: 'id',
    label: 'name',
  });
}

function toggleTargetFields() {
  const individual = targetSelect && targetSelect.value === 'INDIVIDUAL';
  if (groupField) groupField.hidden = Boolean(individual);
  if (clientField) clientField.hidden = !individual;
}

function openEditor() {
  resetForm(form, dialog);
  if (accountKind) accountKind.value = 'SMTP';
  fillAccounts();
  toggleTargetFields();

  const title = dialog.querySelector('[data-role="title"]');
  if (title) title.textContent = 'New campaign';
  if (submitButton) submitButton.textContent = 'Start campaign';

  openDialog(dialog);
}

async function load() {
  try {
    const page = await get(ENDPOINTS.campaigns, { start: 0, length: PAGE_SIZE });
    view.setRows(asList(page));
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function loadChoices() {
  try {
    const [smtp, gmail, templates, clientGroups, clientList] = await Promise.all([
      get(ENDPOINTS.smtpAccounts),
      get(ENDPOINTS.gmailAccounts),
      get(ENDPOINTS.templates),
      get(ENDPOINTS.clientGroups),
      get(ENDPOINTS.clients, { start: 0, length: PAGE_SIZE }),
    ]);

    accounts = { SMTP: asList(smtp), GMAIL: asList(gmail) };
    groups = asList(clientGroups);
    clients = asList(clientList);

    fillAccounts();
    fillTemplates(asList(templates));
    fillSelect(groupSelect, groups, { placeholder: 'Choose a group', value: 'code', label: 'name' });

    if (clientSelect) {
      clientSelect.innerHTML = '';
      clients.forEach((client) => {
        const option = document.createElement('option');
        option.value = String(client.id);
        option.textContent = `${client.name} - ${client.email}`;
        clientSelect.append(option);
      });
    }
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function showDeliveries(campaign) {
  if (!deliveriesDialog) return;
  if (deliveryRows) deliveryRows.innerHTML = '';
  if (deliveryEmpty) deliveryEmpty.hidden = true;
  if (deliveryTable) deliveryTable.hidden = false;

  const title = deliveriesDialog.querySelector('[data-role="title"]');
  if (title) title.textContent = `Recipients of ${campaign.templateName || 'campaign ' + campaign.id}`;

  openDialog(deliveriesDialog);

  try {
    const deliveries = asList(await get(resource(ENDPOINTS.campaigns, campaign.id) + '/deliveries'));
    if (deliveryRows) {
      deliveryRows.innerHTML = deliveries
        .map(
          (delivery) => `
          <tr>
            <td>${escapeHtml(delivery.recipientEmail)}</td>
            <td>${statusBadge(delivery.status)}</td>
            <td class="nowrap">${escapeHtml(formatDateTime(delivery.sentAt))}</td>
            <td>${escapeHtml(delivery.errorDetail || delivery.providerMessageId || '')}</td>
          </tr>`,
        )
        .join('');
    }
    const found = deliveries.length > 0;
    if (deliveryTable) deliveryTable.hidden = !found;
    if (deliveryEmpty) deliveryEmpty.hidden = found;
  } catch (error) {
    toast(error.message, 'danger');
  }
}

async function cancelCampaign(campaign) {
  const confirmed = await confirmDialog({
    title: 'Cancel campaign',
    message: `Cancel the campaign on ${campaign.templateName || 'campaign ' + campaign.id}? The messages already sent are not recalled.`,
    confirmLabel: 'Cancel campaign',
  });
  if (!confirmed) return;

  try {
    await post(resource(ENDPOINTS.campaigns, campaign.id) + '/cancel');
    toast('Campaign cancelled.', 'success');
    await load();
  } catch (error) {
    toast(error.message, 'danger');
  }
}

if (accountKind) accountKind.addEventListener('change', fillAccounts);
if (targetSelect) targetSelect.addEventListener('change', toggleTargetFields);

if (rows) {
  rows.addEventListener('click', (event) => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const campaign = view.rows.find((row) => String(row.id) === String(button.dataset.id));
    if (!campaign) return;
    if (button.dataset.action === 'deliveries') showDeliveries(campaign);
    if (button.dataset.action === 'cancel') cancelCampaign(campaign);
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
    const kind = values.fromAccountKind || 'SMTP';
    const accountId = values.fromAccountId ? Number(values.fromAccountId) : null;
    const templateId = values.templateId ? Number(values.templateId) : null;
    const targetType = values.targetType || 'GROUP';

    if (!accountId || !templateId) {
      setDialogError(dialog, 'Choose a mailbox and an approved template.');
      return;
    }

    const payload = {
      fromAccountKind: kind,
      fromAccountId: accountId,
      templateId,
      targetType,
      groupCode: targetType === 'GROUP' ? values.groupCode || null : null,
      clientIds:
        targetType === 'INDIVIDUAL'
          ? (Array.isArray(values.clientIds) ? values.clientIds : values.clientIds ? [values.clientIds] : []).map(Number)
          : null,
    };

    if (targetType === 'GROUP' && !payload.groupCode) {
      setDialogError(dialog, 'Choose the group the campaign targets.');
      return;
    }
    if (targetType === 'INDIVIDUAL' && (!payload.clientIds || payload.clientIds.length === 0)) {
      setDialogError(dialog, 'Choose at least one client.');
      return;
    }

    if (submitButton) submitButton.disabled = true;
    try {
      await post(ENDPOINTS.campaigns, payload);
      closeDialog(dialog);
      toast('Campaign started.', 'success');
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

if (deliveriesDialog) {
  deliveriesDialog.querySelectorAll('[data-action="close"]').forEach((button) => {
    button.addEventListener('click', () => closeDialog(deliveriesDialog));
  });
}

loadChoices();
load();

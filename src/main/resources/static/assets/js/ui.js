/**
 * ui.js - the shared presentation kit.
 *
 * Everything the screens share: escaping, badges, toasts, dialogs, the
 * client side pager and the list plumbing. The screens themselves only
 * describe what their rows look like.
 */

/* ------------------------------------------------------------------ *
 * Text and markup
 * ------------------------------------------------------------------ */

/** Escape a value for use in HTML. Everything a person typed goes through this. */
export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** An icon from the sprite the layout renders. */
export function icon(name) {
  return `<svg class="nav-icon" aria-hidden="true"><use href="#i-${escapeHtml(name)}"></use></svg>`;
}

const BADGE_KINDS = ['ok', 'warn', 'danger', 'info', 'accent', 'neutral'];

/** A badge with a word in it. Status is never carried by colour alone. */
export function badge(text, kind = 'neutral') {
  const safeKind = BADGE_KINDS.includes(kind) ? kind : 'neutral';
  return `<span class="badge badge-${safeKind}">${escapeHtml(text)}</span>`;
}

/**
 * How each status value reads and how it is coloured.
 * The word is always present, the colour only repeats it.
 */
const STATUS = {
  // TemplateStatus
  DRAFT: { label: 'Draft', kind: 'neutral' },
  PENDING_APPROVAL: { label: 'Pending approval', kind: 'warn' },
  APPROVED: { label: 'Approved', kind: 'ok' },
  REJECTED: { label: 'Rejected', kind: 'danger' },
  ARCHIVED: { label: 'Archived', kind: 'neutral' },
  // CampaignStatus, OutboxStatus, ExtractionStatus
  QUEUED: { label: 'Queued', kind: 'info' },
  SCHEDULED: { label: 'Scheduled', kind: 'info' },
  RUNNING: { label: 'Running', kind: 'info' },
  SENDING: { label: 'Sending', kind: 'info' },
  SUCCESS: { label: 'Success', kind: 'ok' },
  SENT: { label: 'Sent', kind: 'ok' },
  PARTIAL: { label: 'Partial', kind: 'warn' },
  FAILED: { label: 'Failed', kind: 'danger' },
  CANCELLED: { label: 'Cancelled', kind: 'neutral' },
  // DeliveryStatus
  BOUNCED: { label: 'Bounced', kind: 'danger' },
};

/** A badge for one status value. */
export function statusBadge(value) {
  const key = String(value === null || value === undefined ? '' : value).toUpperCase();
  const known = STATUS[key];
  if (known) return badge(known.label, known.kind);
  const fallback = key ? key.charAt(0) + key.slice(1).toLowerCase().replace(/_/g, ' ') : 'Unknown';
  return badge(fallback, 'neutral');
}

/** A yes or no badge for a boolean. */
export function enabledBadge(enabled) {
  return enabled ? badge('Enabled', 'ok') : badge('Disabled', 'neutral');
}

/** A yes or no badge for a stored secret. The secret itself is never shown. */
export function credentialBadge(hasCredential) {
  return hasCredential ? badge('Configured', 'ok') : badge('Not configured', 'warn');
}

/** A short local date and time. */
export function formatDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** A link cell that opens in a new tab without leaking the referrer. */
export function externalLink(href, label) {
  if (!href) return '';
  return `<a href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${escapeHtml(label || href)}</a>`;
}

/* ------------------------------------------------------------------ *
 * Buttons
 * ------------------------------------------------------------------ */

/** An icon button for a row action. */
export function actionButton(action, id, iconName, label) {
  return (
    `<button type="button" class="btn btn-ghost btn-icon" data-action="${escapeHtml(action)}"` +
    ` data-id="${escapeHtml(id)}" title="${escapeHtml(label)}" aria-label="${escapeHtml(label)}">` +
    `${icon(iconName)}</button>`
  );
}

/* ------------------------------------------------------------------ *
 * Toasts
 * ------------------------------------------------------------------ */

/** Show a message. The text is set as text, never as markup. */
export function toast(message, kind = 'info') {
  const host = document.getElementById('toast-host');
  if (!host) return;

  const box = document.createElement('div');
  box.className = `toast toast-${kind}`;
  box.setAttribute('role', kind === 'danger' ? 'alert' : 'status');

  const text = document.createElement('p');
  text.className = 'toast-text';
  text.textContent = message;

  const close = document.createElement('button');
  close.type = 'button';
  close.className = 'toast-close';
  close.setAttribute('aria-label', 'Dismiss');
  close.textContent = '×';
  close.addEventListener('click', () => box.remove());

  box.append(text, close);
  host.append(box);

  const life = kind === 'danger' ? 10000 : 5000;
  window.setTimeout(() => box.remove(), life);
}

/* ------------------------------------------------------------------ *
 * Dialogs
 * ------------------------------------------------------------------ */

/** Open a dialog and put the cursor in the first field. */
export function openDialog(dialog) {
  if (!dialog || dialog.open) return;
  setDialogError(dialog, '');
  if (typeof dialog.showModal === 'function') dialog.showModal();
  else dialog.setAttribute('open', '');
  const first = dialog.querySelector('input:not([type="hidden"]), select, textarea');
  if (first) first.focus();
}

/** Close a dialog. */
export function closeDialog(dialog) {
  if (!dialog) return;
  if (typeof dialog.close === 'function' && dialog.open) dialog.close();
  else dialog.removeAttribute('open');
}

/** Show or clear the error box inside a dialog. */
export function setDialogError(dialog, message) {
  if (!dialog) return;
  const box = dialog.querySelector('[data-role="error"]');
  if (!box) return;
  box.textContent = message || '';
  box.hidden = !message;
}

/**
 * Ask the person to confirm.
 *
 * @returns {Promise<boolean>} true when the action was confirmed.
 */
export function confirmDialog(options = {}) {
  const {
    title = 'Are you sure?',
    message = '',
    confirmLabel = 'Confirm',
    kind = 'danger',
  } = options;

  return new Promise((resolve) => {
    const existing = document.getElementById('confirm-dialog');
    if (existing) existing.remove();

    const dialog = document.createElement('dialog');
    dialog.id = 'confirm-dialog';
    dialog.className = 'modal';

    const head = document.createElement('div');
    head.className = 'modal-head';
    const heading = document.createElement('h2');
    heading.className = 'modal-title';
    heading.textContent = title;
    head.append(heading);

    const body = document.createElement('div');
    body.className = 'modal-body';
    const text = document.createElement('p');
    text.className = 'muted';
    text.textContent = message;
    body.append(text);

    const foot = document.createElement('div');
    foot.className = 'modal-foot';
    const cancel = document.createElement('button');
    cancel.type = 'button';
    cancel.className = 'btn btn-secondary';
    cancel.textContent = 'Cancel';
    const confirm = document.createElement('button');
    confirm.type = 'button';
    confirm.className = kind === 'danger' ? 'btn btn-danger' : 'btn btn-primary';
    confirm.textContent = confirmLabel;
    foot.append(cancel, confirm);

    dialog.append(head, body, foot);
    document.body.append(dialog);

    const finish = (answer) => {
      closeDialog(dialog);
      dialog.remove();
      resolve(answer);
    };

    cancel.addEventListener('click', () => finish(false));
    confirm.addEventListener('click', () => finish(true));
    dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      finish(false);
    });
    dialog.addEventListener('click', (event) => {
      if (event.target === dialog) finish(false);
    });

    openDialog(dialog);
    confirm.focus();
  });
}

/* ------------------------------------------------------------------ *
 * Forms
 * ------------------------------------------------------------------ */

/** Read a form into a plain object. A repeated name becomes an array. */
export function formValues(form) {
  const values = {};
  if (!form) return values;
  const data = new FormData(form);
  for (const [key, value] of data.entries()) {
    if (Object.prototype.hasOwnProperty.call(values, key)) {
      if (Array.isArray(values[key])) values[key].push(value);
      else values[key] = [values[key], value];
    } else {
      values[key] = value;
    }
  }
  return values;
}

/** Reset a form and clear its error box. */
export function resetForm(form, dialog) {
  if (form) form.reset();
  setDialogError(dialog, '');
}

/* ------------------------------------------------------------------ *
 * Pager
 * ------------------------------------------------------------------ */

/**
 * The client side pager.
 *
 * The screens fetch the whole list once and page it in the browser, so the
 * pager owns the page number, the page size and the search term.
 */
export function createPager(root, options = {}) {
  const { search = null, match = null, onChange = null } = options;
  const state = { page: 0, size: 25, offset: 0, total: 0, pages: 1, rows: [] };

  if (!root) {
    return {
      state,
      setRows(rows) {
        state.rows = Array.isArray(rows) ? rows : [];
        state.total = state.rows.length;
        state.pages = 1;
      },
      refresh() {},
    };
  }

  const info = root.querySelector('[data-role="info"]');
  const pageLabel = root.querySelector('[data-role="page"]');
  const sizeSelect = root.querySelector('[data-role="size"]');
  const first = root.querySelector('[data-role="first"]');
  const prev = root.querySelector('[data-role="prev"]');
  const next = root.querySelector('[data-role="next"]');
  const last = root.querySelector('[data-role="last"]');

  if (sizeSelect) {
    state.size = Number(sizeSelect.value) || state.size;
    sizeSelect.addEventListener('change', () => {
      state.size = Number(sizeSelect.value) || state.size;
      state.page = 0;
      emit();
    });
  }

  if (search) {
    let timer = null;
    search.addEventListener('input', () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        state.page = 0;
        emit();
      }, 150);
    });
  }

  if (first) first.addEventListener('click', () => go(0));
  if (prev) prev.addEventListener('click', () => go(state.page - 1));
  if (next) next.addEventListener('click', () => go(state.page + 1));
  if (last) last.addEventListener('click', () => go(state.pages - 1));

  function go(page) {
    const target = Math.min(Math.max(page, 0), state.pages - 1);
    if (target === state.page) return;
    state.page = target;
    emit();
  }

  function visible() {
    const query = search ? search.value.trim().toLowerCase() : '';
    if (!query || typeof match !== 'function') return state.rows;
    return state.rows.filter((row) => match(row, query));
  }

  function emit() {
    const rows = visible();
    state.total = rows.length;
    state.pages = Math.max(1, Math.ceil(state.total / state.size));
    if (state.page > state.pages - 1) state.page = state.pages - 1;
    state.offset = state.page * state.size;
    const slice = rows.slice(state.offset, state.offset + state.size);

    if (info) {
      info.textContent =
        state.total === 0
          ? 'No rows'
          : `${state.offset + 1}-${Math.min(state.offset + state.size, state.total)} of ${state.total}`;
    }
    if (pageLabel) pageLabel.textContent = `Page ${state.page + 1} of ${state.pages}`;
    if (first) first.disabled = state.page === 0;
    if (prev) prev.disabled = state.page === 0;
    if (next) next.disabled = state.page >= state.pages - 1;
    if (last) last.disabled = state.page >= state.pages - 1;

    if (onChange) onChange(slice, state);
  }

  return {
    state,
    setRows(rows) {
      state.rows = Array.isArray(rows) ? rows : [];
      emit();
    },
    refresh: emit,
  };
}

/* ------------------------------------------------------------------ *
 * List screen plumbing
 * ------------------------------------------------------------------ */

/**
 * The shared parts of a list screen: the table, the empty state, the
 * toolbar and the pager.
 *
 * @param {object} options
 *   renderRow - turns one record into the markup of one row.
 *   match     - decides whether a record matches the search term.
 *   filter    - decides whether a record passes the toolbar filters.
 */
export function createListView(options = {}) {
  const { renderRow, match = null, filter = null } = options;

  const body = document.querySelector('[data-role="rows"]');
  const table = document.querySelector('[data-role="table"]');
  const empty = document.querySelector('[data-role="empty"]');
  const emptyTitle = empty ? empty.querySelector('.empty-title') : null;
  const emptyText = empty ? empty.querySelector('.empty-text') : null;
  const summary = document.querySelector('[data-role="summary"]');
  const search = document.querySelector('[data-role="search"]');
  const pagerRoot = document.querySelector('.pager');

  const idleTitle = emptyTitle ? emptyTitle.textContent : '';
  const idleText = emptyText ? emptyText.textContent : '';

  let rows = [];

  function render(slice, state) {
    if (body) body.innerHTML = slice.map(renderRow).join('');

    const total = state ? state.total : slice.length;
    const loaded = state ? state.rows.length : rows.length;
    const hasRows = total > 0;

    if (table) table.hidden = !hasRows;
    if (empty) {
      empty.hidden = hasRows;
      if (emptyTitle) {
        emptyTitle.textContent = loaded > 0 ? 'No rows match' : idleTitle;
      }
      if (emptyText) {
        emptyText.textContent =
          loaded > 0 ? 'Change the search or the filters to see the rows again.' : idleText;
      }
    }
    if (summary) {
      summary.textContent = loaded ? `${loaded} ${loaded === 1 ? 'row' : 'rows'}` : '';
    }
  }

  const pager = createPager(pagerRoot, {
    search,
    match: (row, query) => {
      if (filter && !filter(row)) return false;
      return match ? match(row, query) : true;
    },
    onChange: render,
  });

  return {
    pager,
    get rows() {
      return rows;
    },
    setRows(next) {
      rows = Array.isArray(next) ? next : [];
      pager.setRows(rows);
    },
    render,
  };
}

/** Fill a select with options, keeping the current choice when it survives. */
export function fillSelect(select, options, { placeholder = null, value = 'id', label = 'name' } = {}) {
  if (!select) return;
  const current = select.value;
  select.innerHTML = '';

  if (placeholder !== null) {
    const option = document.createElement('option');
    option.value = '';
    option.textContent = placeholder;
    select.append(option);
  }

  options.forEach((item) => {
    const option = document.createElement('option');
    option.value = String(item[value]);
    option.textContent = item[label];
    select.append(option);
  });

  if (current && [...select.options].some((option) => option.value === current)) {
    select.value = current;
  }
}

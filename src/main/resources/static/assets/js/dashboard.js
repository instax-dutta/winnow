/**
 * dashboard.js - the counters on the landing page.
 *
 * The counters are rendered by the server from DashboardDto, so the page
 * already shows the right numbers. This module refreshes them from
 * GET /api/dashboard so the page stays honest when it is left open, and it
 * animates the numbers once.
 */

import { ENDPOINTS, get } from './api.js';

const counters = document.querySelectorAll('[data-counter]');
const refresh = document.getElementById('refresh-dashboard');
const updated = document.getElementById('dashboard-updated');

const still = window.matchMedia('(prefers-reduced-motion: reduce)');

/** The counter fields of DashboardDto, in the order the page shows them. */
const FIELDS = [
  'clients',
  'clientGroups',
  'templates',
  'pendingTemplates',
  'smtpAccounts',
  'gmailAccounts',
  'campaigns',
  'emailsSent',
  'emailsFailed',
  'extractionRuns',
  'inboxMessages',
];

function animate(element, target) {
  if (!Number.isFinite(target) || target <= 0 || still.matches) {
    element.textContent = String(target);
    return;
  }

  const duration = 420;
  const start = performance.now();

  const step = (now) => {
    const progress = Math.min(1, (now - start) / duration);
    const eased = 1 - Math.pow(1 - progress, 3);
    element.textContent = String(Math.round(target * eased));
    if (progress < 1) window.requestAnimationFrame(step);
    else element.textContent = String(target);
  };

  window.requestAnimationFrame(step);
}

function stamp() {
  if (!updated) return;
  const now = new Date();
  updated.textContent = `Counters read at ${now.toLocaleTimeString()}`;
  updated.hidden = false;
}

async function reload() {
  try {
    const summary = await get(ENDPOINTS.dashboard);
    if (!summary) return;
    FIELDS.forEach((field, index) => {
      const element = counters[index];
      if (!element) return;
      const value = Number(summary[field]);
      animate(element, Number.isFinite(value) ? value : 0);
    });
    stamp();
  } catch (error) {
    // The server rendered numbers are still on the page, so a failed refresh
    // is not worth a toast.
    stamp();
  }
}

if (refresh) {
  refresh.addEventListener('click', () => {
    refresh.disabled = true;
    reload().finally(() => {
      refresh.disabled = false;
    });
  });
}

stamp();
reload();

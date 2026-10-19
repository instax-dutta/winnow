/**
 * api.js - the shared fetch wrapper.
 *
 * Every screen talks to the server through this module so that the CSRF
 * token, the response envelope and the error handling are done in one place.
 *
 * Contract
 *   - The token comes from <meta name="_csrf"> in the page head.
 *   - Every mutating request carries it in the X-CSRF-TOKEN header.
 *   - The server answers with the ApiResponse envelope {status, message, data}.
 *     A status other than SUCCESS is an error and rejects with the message.
 *   - A transport failure rejects with a message a person can act on.
 *   - The token is never written to the console and never returned to a caller.
 */

/** The meta tag the token is read from. */
const CSRF_SELECTOR = 'meta[name="_csrf"]';

/** Methods that change state, and therefore need the token. */
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

/**
 * The JSON endpoints the screens talk to.
 *
 * This is the only place the front end names a server path, so a change of
 * route on the server is a change in one object.
 */
export const ENDPOINTS = {
  dashboard: '/api/dashboard',
  clients: '/api/clients',
  clientImport: '/api/clients/import',
  clientGroups: '/api/groups',
  templates: '/api/templates',
  pendingTemplates: '/api/templates/pending',
  smtpAccounts: '/api/accounts/smtp',
  gmailAccounts: '/api/accounts/gmail',
  campaigns: '/api/campaigns',
  outbox: '/api/outbox',
  extractionRuns: '/api/extraction',
  inbox: '/api/inbox',
};

/** How many rows a screen asks for before it pages them in the browser. */
export const PAGE_SIZE = 200;


/** An error that carries a message meant for a person, not a stack trace. */
export class ApiError extends Error {
  constructor(message, options = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = options.status === undefined ? 0 : options.status;
  }
}

/** The CSRF token, or an empty string when the page has none. */
function csrfToken() {
  const meta = document.querySelector(CSRF_SELECTOR);
  return meta ? meta.getAttribute('content') || '' : '';
}

function buildUrl(url, query) {
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    params.append(key, String(value));
  });
  const search = params.toString();
  return search ? `${url}?${search}` : url;
}

async function readPayload(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function isEnvelope(payload) {
  return (
    payload !== null &&
    typeof payload === 'object' &&
    !Array.isArray(payload) &&
    Object.prototype.hasOwnProperty.call(payload, 'status')
  );
}

/**
 * Call the server.
 *
 * @param {string} method  HTTP method.
 * @param {string} url     Path, relative to the application root.
 * @param {object} options body, query, formData or signal.
 * @returns {Promise<any>} the data carried by the envelope, or null.
 */
export async function request(method, url, options = {}) {
  const verb = String(method || 'GET').toUpperCase();
  const { body, query, formData, signal } = options;
  const hasBody = formData !== undefined || body !== undefined;

  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (MUTATING_METHODS.has(verb)) {
    const token = csrfToken();
    if (token) headers['X-CSRF-TOKEN'] = token;
  }

  const init = { method: verb, headers, credentials: 'same-origin' };
  if (formData !== undefined) init.body = formData;
  else if (body !== undefined) init.body = JSON.stringify(body);
  if (signal) init.signal = signal;

  let response;
  try {
    response = await fetch(buildUrl(url, query), init);
  } catch (error) {
    if (error && error.name === 'AbortError') throw error;
    throw new ApiError('Could not reach the server. Check your connection and try again.');
  }

  const payload = await readPayload(response);

  if (!response.ok) {
    const message =
      isEnvelope(payload) && payload.message
        ? String(payload.message)
        : `The server rejected the request (HTTP ${response.status}).`;
    throw new ApiError(message, { status: response.status });
  }

  if (isEnvelope(payload)) {
    if (String(payload.status).toUpperCase() !== 'SUCCESS') {
      throw new ApiError(
        payload.message ? String(payload.message) : 'The server rejected the request.',
        { status: response.status },
      );
    }
    return payload.data === undefined ? null : payload.data;
  }

  return payload;
}

export const get = (url, query) => request('GET', url, { query });
export const post = (url, body) => request('POST', url, { body });
export const put = (url, body) => request('PUT', url, { body });
export const remove = (url) => request('DELETE', url);
export const upload = (url, formData) => request('POST', url, { formData });

/** Build the path of one record. */
export function resource(base, id) {
  return `${base}/${encodeURIComponent(id)}`;
}

/**
 * Normalise a list response.
 *
 * A screen is happy with a plain array, and equally happy with a page of
 * rows, so both shapes are accepted.
 */
export function asList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.data)) return data.data;
  if (data && Array.isArray(data.content)) return data.content;
  if (data && Array.isArray(data.rows)) return data.rows;
  return [];
}

/** The number of rows a paged response says there are in total. */
export function totalOf(data) {
  if (!data || typeof data !== 'object' || Array.isArray(data)) return 0;
  if (typeof data.recordsTotal === 'number') return data.recordsTotal;
  if (typeof data.recordsFiltered === 'number') return data.recordsFiltered;
  return asList(data).length;
}

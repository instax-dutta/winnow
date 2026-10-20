/**
 * login.js - the sign in form.
 *
 * The form is a plain POST to /login. This module only helps the person
 * filling it in: it checks that both fields carry something, it offers a
 * show and hide toggle for the password, and it stops the button being
 * pressed twice.
 */

const form = document.getElementById('login-form');
const username = document.getElementById('username');
const password = document.getElementById('password');
const toggle = document.getElementById('toggle-password');
const submit = document.getElementById('login-submit');
const formError = document.querySelector('[data-role="form-error"]');

function showError(message) {
  if (!formError) return;
  formError.textContent = message;
  formError.hidden = !message;
}

if (toggle && password) {
  toggle.addEventListener('click', () => {
    const shown = password.type === 'text';
    password.type = shown ? 'password' : 'text';
    toggle.setAttribute('aria-pressed', String(!shown));
    toggle.textContent = shown ? 'Show' : 'Hide';
    password.focus();
  });
}

if (form) {
  form.addEventListener('submit', (event) => {
    showError('');

    const missing = [];
    if (!username || !username.value.trim()) missing.push('a username');
    if (!password || !password.value) missing.push('a password');

    if (missing.length) {
      event.preventDefault();
      showError(`Enter ${missing.join(' and ')} to sign in.`);
      if (username && !username.value.trim()) username.focus();
      else if (password) password.focus();
      return;
    }

    if (submit) {
      submit.disabled = true;
      submit.textContent = 'Signing in';
    }
  });
}

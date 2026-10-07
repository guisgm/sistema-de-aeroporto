import { testDatabase } from './helpers/postgres.mjs';
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';

import { createApp } from '../server/app.mjs';

let db, server, url, cookie, csrf;
before(async () => {
  db = await testDatabase();
  server = createApp(db).listen(0, '127.0.0.1');
  await new Promise((resolve) => server.once('listening', resolve));
  url = `http://127.0.0.1:${server.address().port}`;
});
after(async () => {
  await new Promise((resolve) => server.close(resolve));
  await db.close();
});
test('unauthenticated requests are rejected', async () => {
  const res = await fetch(`${url}/api/state`);
  assert.equal(res.status, 401);
});
test('invalid login returns a clear error', async () => {
  const res = await fetch(`${url}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'unknown@example.com', password: 'bad' }),
  });
  assert.equal(res.status, 401);
});
test('login sets a protected cookie, and state excludes password hashes', async () => {
  const response = await fetch(`${url}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@aerohub.local', password: 'AeroHub@2026!' }),
  });
  assert.equal(response.status, 200);
  cookie = response.headers.get('set-cookie').split(';')[0];
  assert.ok(response.headers.get('set-cookie').includes('HttpOnly'));
  assert.ok(response.headers.get('set-cookie').includes('SameSite=Strict'));
  const login = await response.json();
  csrf = login.csrf;
  assert.equal(login.user.role, 'admin');
  assert.equal(login.user.password, undefined);
  const state = await fetch(`${url}/api/state`, { headers: { Cookie: cookie } });
  assert.equal(state.status, 200);
  assert.equal(state.headers.get('cache-control'), 'no-store');
  assert.equal((await state.json()).users, undefined);
});
test('mutations require a CSRF token and reject foreign origins', async () => {
  const headers = { 'Content-Type': 'application/json', Cookie: cookie };
  let res = await fetch(`${url}/api/terminals`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ name: 'Novo Terminal', kind: 'Misto' }),
  });
  assert.equal(res.status, 403);
  res = await fetch(`${url}/api/terminals`, {
    method: 'POST',
    headers: { ...headers, 'X-CSRF-Token': csrf, Origin: 'https://untrusted.example' },
    body: JSON.stringify({ name: 'Novo Terminal', kind: 'Misto' }),
  });
  assert.equal(res.status, 403);
  res = await fetch(`${url}/api/terminals`, {
    method: 'POST',
    headers: { ...headers, 'X-CSRF-Token': csrf },
    body: JSON.stringify({ name: 'Novo Terminal', kind: 'Misto' }),
  });
  assert.equal(res.status, 201);
});
test('validation errors and duplicate codes are returned without database details', async () => {
  const headers = { 'Content-Type': 'application/json', Cookie: cookie, 'X-CSRF-Token': csrf };
  let res = await fetch(`${url}/api/gates`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ code: 'bad', terminalId: 't1', status: 'available' }),
  });
  assert.equal(res.status, 422);
  assert.ok((await res.json()).fields.code);
  res = await fetch(`${url}/api/gates`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ code: 'A01', terminalId: 't1', status: 'available' }),
  });
  assert.equal(res.status, 409);
  assert.ok(!(await res.json()).message.includes('23505'));
});
test('export uses a downloadable CSV and logout invalidates session', async () => {
  const exported = await fetch(`${url}/api/export/flights`, { headers: { Cookie: cookie } });
  assert.equal(exported.status, 200);
  assert.ok(exported.headers.get('content-disposition').includes('attachment'));
  const out = await fetch(`${url}/api/logout`, {
    method: 'POST',
    headers: { Cookie: cookie, 'X-CSRF-Token': csrf },
  });
  assert.equal(out.status, 200);
  assert.equal((await fetch(`${url}/api/state`, { headers: { Cookie: cookie } })).status, 401);
});

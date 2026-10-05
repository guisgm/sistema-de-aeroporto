let csrf = '';
export function setCsrf(value) {
  csrf = value;
}
export async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    credentials: 'same-origin',
    ...options,
    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf, ...options.headers },
    ...(options.body ? { body: JSON.stringify(options.body) } : {}),
  });
  const data = await response.json();
  if (!response.ok) {
    const error = new Error(data.message || 'Não foi possível concluir a operação.');
    error.status = response.status;
    error.fields = data.fields;
    throw error;
  }
  return data;
}
export async function download(table, date) {
  const response = await fetch(`/api/export/${table}?date=${date}`, { credentials: 'same-origin' });
  if (!response.ok) throw new Error((await response.json()).message);
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = `aerohub-${table}-${date}.csv`;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

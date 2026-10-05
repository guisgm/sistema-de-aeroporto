export const statusLabels = {
  scheduled: 'Programado',
  boarding: 'Embarcando',
  delayed: 'Atrasado',
  cancelled: 'Cancelado',
  landed: 'Pousado',
  maintenance: 'Em manutenção',
  available: 'Disponível',
  unavailable: 'Indisponível',
  blocked: 'Bloqueado',
  confirmed: 'Confirmada',
  checked_in: 'Check-in realizado',
};
export const roles = { admin: 'Administrador', operator: 'Operador', attendant: 'Atendente' };
export const permissions = {
  admin: [
    'flights',
    'airlines',
    'aircraft',
    'terminals',
    'gates',
    'passengers',
    'reservations',
    'checkin',
  ],
  operator: ['flights', 'aircraft', 'gates'],
  attendant: ['passengers', 'reservations', 'checkin'],
};
export const can = (user, entity) => permissions[user?.role]?.includes(entity);
export const time = (value) =>
  value
    ? new Intl.DateTimeFormat('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Sao_Paulo',
      }).format(new Date(value))
    : '—';
export const dateLabel = (value) =>
  new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(`${value}T12:00:00-03:00`));
export const dateTime = (value) =>
  value
    ? new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'America/Sao_Paulo',
      }).format(new Date(value))
    : '—';
export const localDay = (value) =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date(value));
export const inputTime = (value) =>
  value ? new Date(new Date(value).getTime() - 3 * 3600000).toISOString().slice(0, 16) : '';
export const fromInputTime = (value) =>
  value ? new Date(`${value}:00-03:00`).toISOString() : null;
export const initials = (name) =>
  name
    .split(' ')
    .slice(0, 2)
    .map((v) => v[0])
    .join('');
export const normalize = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
export const activeFlight = (f) => !['cancelled', 'landed', 'maintenance'].includes(f.status);
export const seatName = (index) => `${Math.floor(index / 6) + 1}${'ABCDEF'[index % 6]}`;
export const effective = (f) => new Date(f.actual || f.scheduled).getTime();
export function indexState(state) {
  const result = {};
  for (const type of ['airlines', 'aircraft', 'gates', 'terminals', 'passengers', 'flights'])
    result[type] = Object.fromEntries(state[type].map((row) => [row.id, row]));
  return result;
}
export function operationalAlerts(state, date) {
  const flights = state.flights.filter((f) => localDay(f.scheduled) === date);
  return [
    ...flights
      .filter((f) => f.status === 'delayed')
      .map((f) => ({
        id: f.id,
        tone: 'warning',
        title: `${f.number} · ${f.destination}`,
        detail: `Nova previsão ${time(f.actual)}. ${f.notes}`,
        entity: 'flights',
        row: f,
      })),
    ...flights
      .filter((f) => f.status === 'cancelled')
      .map((f) => ({
        id: f.id,
        tone: 'danger',
        title: `${f.number} cancelado`,
        detail: f.notes || 'Comunique os passageiros e revise as reservas.',
        entity: 'flights',
        row: f,
      })),
    ...flights
      .filter((f) => f.status === 'maintenance')
      .map((f) => ({
        id: f.id,
        tone: 'info',
        title: `${f.number} em manutenção`,
        detail: f.notes || 'Voo suspenso para manutenção.',
        entity: 'flights',
        row: f,
      })),
    ...state.aircraft
      .filter((a) => a.status === 'maintenance')
      .map((a) => ({
        id: a.id,
        tone: 'info',
        title: `${a.registration} em manutenção`,
        detail: a.notes || 'Aeronave fora da escala operacional.',
        entity: 'aircraft',
        row: a,
      })),
    ...state.gates
      .filter((g) => g.status === 'blocked')
      .map((g) => ({
        id: g.id,
        tone: 'info',
        title: `Portão ${g.code} bloqueado`,
        detail: 'Portão indisponível para novas alocações.',
        entity: 'gates',
        row: g,
      })),
  ];
}

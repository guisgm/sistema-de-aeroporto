import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { openDatabase, today, localTime } from '../server/database.mjs';
import {
  saveEntity,
  createReservation,
  updateReservation,
  csvExport,
  readState,
  gateOverlap,
} from '../server/operations.mjs';
import { validCpf, schemas } from '../server/validation.mjs';

let db;
const admin = { id: 'admin', role: 'admin' },
  operator = { id: 'operator', role: 'operator' },
  attendant = { id: 'attendant', role: 'attendant' };
beforeEach(() => {
  db = openDatabase(':memory:');
});
afterEach(() => db.close());
const nextDay = () => {
  const d = new Date(`${today()}T12:00:00-03:00`);
  d.setUTCDate(d.getUTCDate() + 1);
  return d.toISOString().slice(0, 10);
};
function resources() {
  const gate = saveEntity(db, admin, 'gates', {
    code: 'A99',
    terminalId: 't1',
    status: 'available',
  });
  const plane = saveEntity(db, admin, 'aircraft', {
    registration: 'PR-TST',
    model: 'Airbus A220',
    capacity: 18,
    airlineId: 'latam',
    status: 'available',
    maintenanceDate: today(),
    notes: '',
  });
  return { gate, plane };
}
function fixture(overrides = {}) {
  const { gate, plane } = resources();
  const values = {
    number: 'LA 9999',
    airlineId: 'latam',
    aircraftId: plane.id,
    origin: 'Sao Paulo',
    destination: 'Recife',
    originCode: 'GRU',
    destinationCode: 'REC',
    type: 'departure',
    scheduled: localTime(nextDay(), '12:00'),
    actual: null,
    duration: 90,
    gateId: gate.id,
    status: 'scheduled',
    notes: '',
    ...overrides,
  };
  const flight = saveEntity(db, admin, 'flights', values);
  return { flight, gate, plane, values };
}
test('seed preserves foreign keys and starts with representative operational data', () => {
  const state = readState(db);
  assert.equal(state.airlines.length, 4);
  assert.equal(state.gates.length, 12);
  assert.equal(state.flights.length, 144);
  assert.equal(db.prepare('PRAGMA foreign_key_check').all().length, 0);
  for (const flight of state.flights) {
    const aircraft = state.aircraft.find((a) => a.id === flight.aircraftId);
    assert.equal(aircraft.airlineId, flight.airlineId);
  }
  const active = state.flights.filter(
    (f) => !['cancelled', 'landed', 'maintenance'].includes(f.status),
  );
  for (let i = 0; i < active.length; i++)
    for (let j = i + 1; j < active.length; j++) {
      const a = active[i],
        b = active[j];
      if (a.gateId === b.gateId)
        assert.equal(gateOverlap(a, b), false, `gate conflict ${a.number}/${b.number}`);
      if (a.aircraftId === b.aircraftId) {
        const x = Date.parse(a.actual || a.scheduled),
          y = Date.parse(b.actual || b.scheduled);
        assert.equal(
          x < y + (b.duration + 60) * 60000 && y < x + (a.duration + 60) * 60000,
          false,
          `aircraft conflict ${a.number}/${b.number}`,
        );
      }
    }
});
test('rejects simultaneous gate occupancy and rolls back rejected changes', () => {
  const { flight, plane } = fixture();
  const otherPlane = saveEntity(db, admin, 'aircraft', { ...plane, registration: 'PR-TSA' });
  const before = db.prepare('SELECT COUNT(*) AS count FROM audit').get().count;
  assert.throws(
    () =>
      saveEntity(db, admin, 'flights', {
        ...flight,
        number: 'LA 9998',
        aircraftId: otherPlane.id,
        scheduled: localTime(nextDay(), '12:30'),
      }),
    /Conflito de portao/,
  );
  assert.equal(db.prepare('SELECT COUNT(*) AS count FROM audit').get().count, before);
});
test('real-time delays are used for gate conflict checks', () => {
  const { flight, plane } = fixture();
  const otherPlane = saveEntity(db, admin, 'aircraft', { ...plane, registration: 'PR-TSA' });
  const second = saveEntity(db, admin, 'flights', {
    ...flight,
    number: 'LA 9998',
    aircraftId: otherPlane.id,
    scheduled: localTime(nextDay(), '14:00'),
  });
  assert.throws(
    () =>
      saveEntity(
        db,
        admin,
        'flights',
        { ...flight, status: 'delayed', actual: localTime(nextDay(), '13:00') },
        flight.id,
      ),
    /Conflito de portao/,
  );
  assert.equal(
    db.prepare('SELECT status FROM flights WHERE id=?').get(flight.id).status,
    'scheduled',
  );
  assert.equal(second.status, 'scheduled');
});
test('gate relocation resolves conflicts and aircraft turnaround is enforced', () => {
  const { flight } = fixture();
  const gate = saveEntity(db, admin, 'gates', {
    code: 'A98',
    terminalId: 't1',
    status: 'available',
  });
  assert.throws(
    () =>
      saveEntity(db, admin, 'flights', {
        ...flight,
        number: 'LA 9998',
        gateId: gate.id,
        scheduled: localTime(nextDay(), '13:30'),
      }),
    /Aeronave alocada/,
  );
  const moved = saveEntity(db, admin, 'flights', { ...flight, gateId: gate.id }, flight.id);
  assert.equal(moved.gateId, gate.id);
});
test('blocked gates and maintenance aircraft cannot be assigned', () => {
  const { values, flight } = fixture();
  assert.throws(
    () => saveEntity(db, admin, 'flights', { ...values, number: 'LA 9920', gateId: 'g24' }),
    /Portao bloqueado/,
  );
  assert.throws(
    () =>
      saveEntity(db, admin, 'flights', {
        ...values,
        number: 'G3 9920',
        airlineId: 'gol',
        aircraftId: 'gol-4',
      }),
    /Aeronave indisponivel/,
  );
  assert.throws(
    () => saveEntity(db, admin, 'flights', { ...flight, airlineId: 'azul' }, flight.id),
    /pertencer a companhia/,
  );
});
test('maintenance and gate blocking require active assignments to be cleared', () => {
  const { plane, gate } = fixture();
  assert.throws(
    () => saveEntity(db, operator, 'aircraft', { ...plane, status: 'maintenance' }, plane.id),
    /Realocar ou cancelar/,
  );
  assert.throws(
    () => saveEntity(db, operator, 'gates', { ...gate, status: 'blocked' }, gate.id),
    /Realocar os voos/,
  );
});
test('prevents lost updates with optimistic version checks', () => {
  const { flight } = fixture();
  const changed = saveEntity(
    db,
    operator,
    'flights',
    { ...flight, notes: 'Revisao operacional' },
    flight.id,
  );
  assert.equal(changed.version, 2);
  assert.throws(
    () =>
      saveEntity(db, operator, 'flights', { ...flight, notes: 'Atualizacao antiga' }, flight.id),
    /outro usuario/,
  );
});
test('different timestamp offsets are stored in canonical UTC for availability checks', () => {
  const { flight } = fixture();
  const scheduled = flight.scheduled.replace('.000Z', '+00:00');
  const updated = saveEntity(db, operator, 'flights', { ...flight, scheduled }, flight.id);
  assert.equal(updated.scheduled, flight.scheduled);
  assert.equal(schemas.flights.parse({ ...flight, scheduled }).scheduled, flight.scheduled);
});
test('server applies role authorization independently of the interface', () => {
  const { flight } = fixture();
  assert.throws(
    () => saveEntity(db, attendant, 'flights', flight, flight.id),
    (error) => error.status === 403,
  );
  assert.throws(
    () =>
      createReservation(db, operator, {
        passengerId: 'passenger-0',
        flightId: flight.id,
        seat: '1A',
      }),
    (error) => error.status === 403,
  );
  assert.throws(
    () => saveEntity(db, operator, 'airlines', {}),
    (error) => error.status === 403,
  );
});
test('operator responses mask personal data and audit never copies sensitive changes', () => {
  const original = db.prepare('SELECT * FROM passengers LIMIT 1').get();
  saveEntity(db, admin, 'passengers', { ...original, email: 'pessoal@example.com' }, original.id);
  const state = readState(db, operator);
  const passenger = state.passengers.find((p) => p.id === original.id);
  assert.equal(passenger.email, 'Acesso restrito');
  assert.equal(passenger.birthDate, '');
  assert.equal(passenger.document, `****${original.document.slice(-4)}`);
  assert.ok(!state.audit.some((a) => a.detail.includes('pessoal@example.com')));
});
test('validates CPF checksums, impossible dates and required fields', () => {
  assert.equal(validCpf('52998224725'), true);
  assert.equal(validCpf('11111111111'), false);
  assert.equal(validCpf('52998224724'), false);
  const p = {
    name: 'Teste Passageiro',
    documentType: 'cpf',
    document: '529.982.247-25',
    birthDate: '2000-02-29',
    email: 'teste@example.com',
    phone: '(11) 99999-9999',
    nationality: 'Brasileira',
  };
  assert.equal(saveEntity(db, attendant, 'passengers', p).document, '52998224725');
  assert.throws(
    () => saveEntity(db, attendant, 'passengers', { ...p, document: '52998224724' }),
    /Documento invalido/,
  );
  assert.equal(schemas.passengers.safeParse({ ...p, birthDate: '2026-02-31' }).success, false);
  assert.equal(schemas.passengers.safeParse({ ...p, birthDate: '2026-99-99' }).success, false);
  assert.equal(schemas.passengers.safeParse({ ...p, email: 'invalid' }).success, false);
});
test('booking, seat exclusivity, check-in, cancellation and audit form a complete flow', () => {
  const { flight } = fixture();
  const r = createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  assert.equal(r.status, 'confirmed');
  assert.match(r.locator, /^[A-F0-9]{6}$/);
  assert.throws(
    () =>
      createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1A',
      }),
    /assento ja esta reservado/,
  );
  assert.throws(
    () =>
      createReservation(db, attendant, {
        passengerId: 'passenger-0',
        flightId: flight.id,
        seat: '1B',
      }),
    /ja possui reserva/,
  );
  assert.throws(
    () =>
      createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '4A',
      }),
    /capacidade/,
  );
  const checked = updateReservation(db, attendant, r.id, 'checkin', r.version);
  assert.equal(checked.status, 'checked_in');
  assert.ok(checked.checkedAt);
  assert.throws(
    () => updateReservation(db, attendant, r.id, 'checkin', checked.version),
    /ja foi realizado/,
  );
  const canceled = updateReservation(db, attendant, r.id, 'cancel', checked.version);
  assert.equal(canceled.status, 'cancelled');
  assert.equal(
    createReservation(db, attendant, {
      passengerId: 'passenger-1',
      flightId: flight.id,
      seat: '1A',
    }).status,
    'confirmed',
  );
  assert.equal(
    db.prepare('SELECT COUNT(*) AS count FROM audit WHERE entityId=?').get(r.id).count,
    3,
  );
});
test('flight cancellation closes reservations and prevents reopening', () => {
  const { flight } = fixture();
  const r = createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  const canceled = saveEntity(
    db,
    operator,
    'flights',
    { ...flight, status: 'cancelled', notes: 'Cancelamento por condicoes meteorologicas' },
    flight.id,
  );
  assert.equal(
    db.prepare('SELECT status FROM reservations WHERE id=?').get(r.id).status,
    'cancelled',
  );
  assert.throws(
    () => saveEntity(db, operator, 'flights', { ...canceled, status: 'scheduled' }, flight.id),
    /nao podem ser alterados/,
  );
  assert.throws(
    () =>
      createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1B',
      }),
    /indisponivel/,
  );
});
test('aircraft capacity cannot remove reserved seats', () => {
  const { flight, plane } = fixture();
  createReservation(db, attendant, { passengerId: 'passenger-0', flightId: flight.id, seat: '3F' });
  assert.throws(
    () => saveEntity(db, operator, 'aircraft', { ...plane, capacity: 6 }, plane.id),
    /excluiria assentos/,
  );
});
test('check-in window and arrival-only restrictions are enforced', () => {
  const { flight } = fixture();
  const reservation = createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  db.prepare('UPDATE flights SET scheduled=? WHERE id=?').run(
    new Date(Date.now() + 72 * 3600000).toISOString(),
    flight.id,
  );
  assert.throws(
    () => updateReservation(db, attendant, reservation.id, 'checkin', reservation.version),
    /48 horas/,
  );
  db.prepare("UPDATE flights SET type='arrival' WHERE id=?").run(flight.id);
  assert.throws(
    () =>
      createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1B',
      }),
    /apenas para partidas/,
  );
});
test('CSV exports neutralize spreadsheet formulas and omit identity documents', () => {
  const p = db.prepare('SELECT * FROM passengers LIMIT 1').get();
  saveEntity(db, admin, 'passengers', { ...p, name: '=SUM(A1:A2)' }, p.id);
  const csv = csvExport(db, admin, 'passengers');
  assert.ok(csv.includes("'=SUM(A1:A2)"));
  assert.ok(!csv.includes(p.document));
  assert.throws(
    () => csvExport(db, operator, 'passengers'),
    (error) => error.status === 403,
  );
  assert.ok(csvExport(db, admin, 'flights', today()).includes('companhia'));
});

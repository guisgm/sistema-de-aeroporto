import { testDatabase } from './helpers/postgres.mjs';
import { test, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { openDatabase, transaction, today, localTime } from '../server/database.mjs';
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
beforeEach(async () => {
  db = await testDatabase();
});
afterEach(async () => await db.close());
const nextDay = () => {
  const d = new Date(`${today()}T12:00:00-03:00`);
  d.setUTCDate(d.getUTCDate() + 1);
  return d.toISOString().slice(0, 10);
};
async function resources() {
  const gate = await saveEntity(db, admin, 'gates', {
    code: 'A99',
    terminalId: 't1',
    status: 'available',
  });
  const plane = await saveEntity(db, admin, 'aircraft', {
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
async function fixture(overrides = {}) {
  const { gate, plane } = await resources();
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
  const flight = await saveEntity(db, admin, 'flights', values);
  return { flight, gate, plane, values };
}
test('seed preserves foreign keys and starts with representative operational data', async () => {
  const state = await readState(db);
  assert.equal(state.airlines.length, 4);
  assert.equal(state.gates.length, 12);
  assert.equal(state.flights.length, 144);
  assert.equal(
    (
      await db.one(
        'SELECT COUNT(*) AS count FROM reservations r LEFT JOIN flights f ON f.id=r.flightId WHERE f.id IS NULL',
      )
    ).count,
    0,
  );
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
test('rejects simultaneous gate occupancy and rolls back rejected changes', async () => {
  const { flight, plane } = await fixture();
  const otherPlane = await saveEntity(db, admin, 'aircraft', { ...plane, registration: 'PR-TSA' });
  const before = (await db.one('SELECT COUNT(*) AS count FROM audit', [])).count;
  await assert.rejects(
    async () =>
      await saveEntity(db, admin, 'flights', {
        ...flight,
        number: 'LA 9998',
        aircraftId: otherPlane.id,
        scheduled: localTime(nextDay(), '12:30'),
      }),
    /Conflito de portao/,
  );
  assert.equal((await db.one('SELECT COUNT(*) AS count FROM audit', [])).count, before);
});
test('real-time delays are used for gate conflict checks', async () => {
  const { flight, plane } = await fixture();
  const otherPlane = await saveEntity(db, admin, 'aircraft', { ...plane, registration: 'PR-TSA' });
  const second = await saveEntity(db, admin, 'flights', {
    ...flight,
    number: 'LA 9998',
    aircraftId: otherPlane.id,
    scheduled: localTime(nextDay(), '14:00'),
  });
  await assert.rejects(
    async () =>
      await saveEntity(
        db,
        admin,
        'flights',
        { ...flight, status: 'delayed', actual: localTime(nextDay(), '13:00') },
        flight.id,
      ),
    /Conflito de portao/,
  );
  assert.equal(
    (await db.one('SELECT status FROM flights WHERE id=?', [flight.id])).status,
    'scheduled',
  );
  assert.equal(second.status, 'scheduled');
});
test('gate relocation resolves conflicts and aircraft turnaround is enforced', async () => {
  const { flight } = await fixture();
  const gate = await saveEntity(db, admin, 'gates', {
    code: 'A98',
    terminalId: 't1',
    status: 'available',
  });
  await assert.rejects(
    async () =>
      await saveEntity(db, admin, 'flights', {
        ...flight,
        number: 'LA 9998',
        gateId: gate.id,
        scheduled: localTime(nextDay(), '13:30'),
      }),
    /Aeronave alocada/,
  );
  const moved = await saveEntity(db, admin, 'flights', { ...flight, gateId: gate.id }, flight.id);
  assert.equal(moved.gateId, gate.id);
});
test('blocked gates and maintenance aircraft cannot be assigned', async () => {
  const { values, flight } = await fixture();
  await assert.rejects(
    async () =>
      await saveEntity(db, admin, 'flights', { ...values, number: 'LA 9920', gateId: 'g24' }),
    /Portao bloqueado/,
  );
  await assert.rejects(
    async () =>
      await saveEntity(db, admin, 'flights', {
        ...values,
        number: 'G3 9920',
        airlineId: 'gol',
        aircraftId: 'gol-4',
      }),
    /Aeronave indisponivel/,
  );
  await assert.rejects(
    async () => await saveEntity(db, admin, 'flights', { ...flight, airlineId: 'azul' }, flight.id),
    /pertencer a companhia/,
  );
});
test('maintenance and gate blocking require active assignments to be cleared', async () => {
  const { plane, gate } = await fixture();
  await assert.rejects(
    async () =>
      await saveEntity(db, operator, 'aircraft', { ...plane, status: 'maintenance' }, plane.id),
    /Realocar ou cancelar/,
  );
  await assert.rejects(
    async () => await saveEntity(db, operator, 'gates', { ...gate, status: 'blocked' }, gate.id),
    /Realocar os voos/,
  );
});
test('prevents lost updates with optimistic version checks', async () => {
  const { flight } = await fixture();
  const changed = await saveEntity(
    db,
    operator,
    'flights',
    { ...flight, notes: 'Revisao operacional' },
    flight.id,
  );
  assert.equal(changed.version, 2);
  await assert.rejects(
    async () =>
      await saveEntity(
        db,
        operator,
        'flights',
        { ...flight, notes: 'Atualizacao antiga' },
        flight.id,
      ),
    /outro usuario/,
  );
});
test('different timestamp offsets are stored in canonical UTC for availability checks', async () => {
  const { flight } = await fixture();
  const scheduled = flight.scheduled.replace('.000Z', '+00:00');
  const updated = await saveEntity(db, operator, 'flights', { ...flight, scheduled }, flight.id);
  assert.equal(updated.scheduled, flight.scheduled);
  assert.equal(schemas.flights.parse({ ...flight, scheduled }).scheduled, flight.scheduled);
});
test('server applies role authorization independently of the interface', async () => {
  const { flight } = await fixture();
  await assert.rejects(
    async () => await saveEntity(db, attendant, 'flights', flight, flight.id),
    (error) => error.status === 403,
  );
  await assert.rejects(
    async () =>
      await createReservation(db, operator, {
        passengerId: 'passenger-0',
        flightId: flight.id,
        seat: '1A',
      }),
    (error) => error.status === 403,
  );
  await assert.rejects(
    async () => await saveEntity(db, operator, 'airlines', {}),
    (error) => error.status === 403,
  );
});
test('operator responses mask personal data and audit never copies sensitive changes', async () => {
  const original = await db.one('SELECT * FROM passengers LIMIT 1', []);
  await saveEntity(
    db,
    admin,
    'passengers',
    { ...original, email: 'pessoal@example.com' },
    original.id,
  );
  const state = await readState(db, operator);
  const passenger = state.passengers.find((p) => p.id === original.id);
  assert.equal(passenger.email, 'Acesso restrito');
  assert.equal(passenger.birthDate, '');
  assert.equal(passenger.document, `****${original.document.slice(-4)}`);
  assert.ok(!state.audit.some((a) => a.detail.includes('pessoal@example.com')));
});
test('validates CPF checksums, impossible dates and required fields', async () => {
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
  assert.equal((await saveEntity(db, attendant, 'passengers', p)).document, '52998224725');
  await assert.rejects(
    async () => await saveEntity(db, attendant, 'passengers', { ...p, document: '52998224724' }),
    /Documento invalido/,
  );
  assert.equal(schemas.passengers.safeParse({ ...p, birthDate: '2026-02-31' }).success, false);
  assert.equal(schemas.passengers.safeParse({ ...p, birthDate: '2026-99-99' }).success, false);
  assert.equal(schemas.passengers.safeParse({ ...p, email: 'invalid' }).success, false);
});
test('booking, seat exclusivity, check-in, cancellation and audit form a complete flow', async () => {
  const { flight } = await fixture();
  const r = await createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  assert.equal(r.status, 'confirmed');
  assert.match(r.locator, /^[A-F0-9]{6}$/);
  await assert.rejects(
    async () =>
      await createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1A',
      }),
    /assento ja esta reservado/,
  );
  await assert.rejects(
    async () =>
      await createReservation(db, attendant, {
        passengerId: 'passenger-0',
        flightId: flight.id,
        seat: '1B',
      }),
    /ja possui reserva/,
  );
  await assert.rejects(
    async () =>
      await createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '4A',
      }),
    /capacidade/,
  );
  const checked = await updateReservation(db, attendant, r.id, 'checkin', r.version);
  assert.equal(checked.status, 'checked_in');
  assert.ok(checked.checkedAt);
  await assert.rejects(
    async () => await updateReservation(db, attendant, r.id, 'checkin', checked.version),
    /ja foi realizado/,
  );
  const canceled = await updateReservation(db, attendant, r.id, 'cancel', checked.version);
  assert.equal(canceled.status, 'cancelled');
  assert.equal(
    (
      await createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1A',
      })
    ).status,
    'confirmed',
  );
  assert.equal(
    (await db.one('SELECT COUNT(*) AS count FROM audit WHERE entityId=?', [r.id])).count,
    3,
  );
});
test('flight cancellation closes reservations and prevents reopening', async () => {
  const { flight } = await fixture();
  const r = await createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  const canceled = await saveEntity(
    db,
    operator,
    'flights',
    { ...flight, status: 'cancelled', notes: 'Cancelamento por condicoes meteorologicas' },
    flight.id,
  );
  assert.equal(
    (await db.one('SELECT status FROM reservations WHERE id=?', [r.id])).status,
    'cancelled',
  );
  await assert.rejects(
    async () =>
      await saveEntity(db, operator, 'flights', { ...canceled, status: 'scheduled' }, flight.id),
    /nao podem ser alterados/,
  );
  await assert.rejects(
    async () =>
      await createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1B',
      }),
    /indisponivel/,
  );
});
test('aircraft capacity cannot remove reserved seats', async () => {
  const { flight, plane } = await fixture();
  await createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '3F',
  });
  await assert.rejects(
    async () => await saveEntity(db, operator, 'aircraft', { ...plane, capacity: 6 }, plane.id),
    /excluiria assentos/,
  );
});
test('check-in window and arrival-only restrictions are enforced', async () => {
  const { flight } = await fixture();
  const reservation = await createReservation(db, attendant, {
    passengerId: 'passenger-0',
    flightId: flight.id,
    seat: '1A',
  });
  await db.execute('UPDATE flights SET scheduled=? WHERE id=?', [
    new Date(Date.now() + 72 * 3600000).toISOString(),
    flight.id,
  ]);
  await assert.rejects(
    async () =>
      await updateReservation(db, attendant, reservation.id, 'checkin', reservation.version),
    /48 horas/,
  );
  await db.execute("UPDATE flights SET type='arrival' WHERE id=?", [flight.id]);
  await assert.rejects(
    async () =>
      await createReservation(db, attendant, {
        passengerId: 'passenger-1',
        flightId: flight.id,
        seat: '1B',
      }),
    /apenas para partidas/,
  );
});
test('CSV exports neutralize spreadsheet formulas and omit identity documents', async () => {
  const p = await db.one('SELECT * FROM passengers LIMIT 1', []);
  await saveEntity(db, admin, 'passengers', { ...p, name: '=SUM(A1:A2)' }, p.id);
  const csv = await csvExport(db, admin, 'passengers');
  assert.ok(csv.includes("'=SUM(A1:A2)"));
  assert.ok(!csv.includes(p.document));
  await assert.rejects(
    async () => await csvExport(db, operator, 'passengers'),
    (error) => error.status === 403,
  );
  assert.ok((await csvExport(db, admin, 'flights', today())).includes('companhia'));
});

test('PostgreSQL preserves state across connections without repeating demo data', async () => {
  const second = await openDatabase({
    connectionString:
      process.env.AEROHUB_TEST_DATABASE_URL ||
      'postgresql://aeroporto_teste@127.0.0.1:55439/sistema_aeroporto',
    schema: db.schema,
    seed: true,
  });
  try {
    const { flight } = await fixture();
    assert.equal(
      (await second.one('SELECT number FROM flights WHERE id=?', [flight.id])).number,
      flight.number,
    );
    assert.equal((await second.one('SELECT COUNT(*) AS count FROM users')).count, 3);
    assert.equal((await second.one('SELECT COUNT(*) AS count FROM flights')).count, 145);
  } finally {
    await second.close();
  }
});

test('concurrent PostgreSQL reservations sell one seat only and leave no orphan audit', async () => {
  const { flight } = await fixture();
  const result = await Promise.allSettled(
    [0, 1].map((i) =>
      createReservation(db, attendant, {
        passengerId: `passenger-${i}`,
        flightId: flight.id,
        seat: '1A',
      }),
    ),
  );
  assert.equal(result.filter((r) => r.status === 'fulfilled').length, 1);
  assert.equal(result.filter((r) => r.status === 'rejected').length, 1);
  assert.equal(
    (await db.one('SELECT COUNT(*) AS count FROM reservations WHERE flightId=?', [flight.id]))
      .count,
    1,
  );
  assert.equal(
    (await db.one("SELECT COUNT(*) AS count FROM audit WHERE action='Reserva'")).count,
    1,
  );
});

test('concurrent PostgreSQL updates reject a stale version', async () => {
  const { flight } = await fixture();
  const result = await Promise.allSettled(
    ['Primeira revisao', 'Segunda revisao'].map((notes) =>
      saveEntity(db, operator, 'flights', { ...flight, notes }, flight.id),
    ),
  );
  assert.equal(result.filter((r) => r.status === 'fulfilled').length, 1);
  assert.equal(result.filter((r) => r.status === 'rejected').length, 1);
  assert.equal((await db.one('SELECT version FROM flights WHERE id=?', [flight.id])).version, 2);
});

test('PostgreSQL constraints roll back all statements and release the transaction client', async () => {
  await assert.rejects(
    () =>
      transaction(db, async () => {
        await db.execute(
          "INSERT INTO terminals(id,name,kind) VALUES ('rollback','Rollback','Misto')",
        );
        await db.execute(
          "INSERT INTO gates(id,code,terminalId,status) VALUES ('invalid','D99','missing','available')",
        );
      }),
    (error) => error.code === '23503',
  );
  assert.equal(await db.one("SELECT id FROM terminals WHERE id='rollback'"), undefined);
  assert.equal((await db.one('SELECT COUNT(*) AS count FROM gates')).count, 12);
});

test('flight CSV filters the Sao Paulo date across the UTC midnight boundary', async () => {
  const { flight } = await fixture({ number: 'LA 9876', scheduled: localTime(nextDay(), '23:30') });
  assert.ok((await csvExport(db, admin, 'flights', nextDay())).includes(flight.number));
  const following = new Date(`${nextDay()}T12:00:00-03:00`);
  following.setUTCDate(following.getUTCDate() + 1);
  assert.ok(
    !(await csvExport(db, admin, 'flights', following.toISOString().slice(0, 10))).includes(
      flight.number,
    ),
  );
});

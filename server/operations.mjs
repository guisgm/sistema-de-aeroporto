import { randomUUID, randomBytes } from 'node:crypto';
import { schemas, validCpf } from './validation.mjs';
import { ensure } from './errors.mjs';
import { transaction, today } from './database.mjs';

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
const tables = ['airlines', 'terminals', 'gates', 'aircraft', 'flights', 'passengers'];
const active = "status NOT IN ('cancelled','landed','maintenance')";
const ms = (value) => new Date(value).getTime();
export const flightTime = (flight) => ms(flight.actual || flight.scheduled);
export const gateOverlap = (a, b) => Math.abs(flightTime(a) - flightTime(b)) < 90 * 60000;
const labels = {
  airlines: 'Companhia',
  terminals: 'Terminal',
  gates: 'Portao',
  aircraft: 'Aeronave',
  flights: 'Voo',
  passengers: 'Passageiro',
};

export function authorize(user, capability) {
  ensure(
    permissions[user.role]?.includes(capability),
    'Seu perfil nao tem permissao para esta operacao.',
    403,
  );
}
export function audit(db, user, action, entity, entityId, detail) {
  db.prepare('INSERT INTO audit VALUES (?,?,?,?,?,?,?)').run(
    randomUUID(),
    new Date().toISOString(),
    user.id,
    action,
    entity,
    entityId,
    detail,
  );
}
export function record(db, table, id) {
  const row = db.prepare(`SELECT * FROM ${table} WHERE id=?`).get(id);
  ensure(row, 'Registro nao encontrado.', 404);
  return row;
}

export function readState(db, user) {
  const state = {};
  for (const table of [...tables, 'reservations'])
    state[table] = db.prepare(`SELECT * FROM ${table}`).all();
  state.audit = db
    .prepare(
      'SELECT a.*, u.name AS userName FROM audit a JOIN users u ON u.id=a.userId ORDER BY a.createdAt DESC LIMIT 300',
    )
    .all();
  state.today = today();
  state.now = new Date().toISOString();
  if (user?.role === 'operator') {
    state.passengers = state.passengers.map((passenger) => ({
      ...passenger,
      document: `****${passenger.document.slice(-4)}`,
      birthDate: '',
      email: 'Acesso restrito',
      phone: 'Acesso restrito',
    }));
  }
  return state;
}

function validateBusiness(db, table, item, existing) {
  if (table === 'passengers') {
    ensure(
      item.documentType === 'cpf'
        ? validCpf(item.document)
        : /^[A-Z]{2}\d{6,9}$/.test(item.document),
      'Documento invalido. Verifique o CPF ou o passaporte (duas letras e 6 a 9 numeros).',
      422,
    );
    ensure(
      item.birthDate <= today() && Number(item.birthDate.slice(0, 4)) >= 1900,
      'Data de nascimento invalida.',
      422,
    );
  }
  if (table === 'gates') {
    record(db, 'terminals', item.terminalId);
    if (existing && (item.status === 'blocked' || existing.terminalId !== item.terminalId)) {
      const assigned = db
        .prepare(
          `SELECT id FROM flights WHERE gateId=? AND ${active} AND COALESCE(actual,scheduled)>=?`,
        )
        .get(existing.id, new Date(Date.now() - 30 * 60000).toISOString());
      ensure(
        !assigned,
        'Realocar os voos ativos antes de bloquear ou mudar o terminal deste portao.',
      );
    }
  }
  if (table === 'aircraft') {
    record(db, 'airlines', item.airlineId);
    if (existing) {
      const assigned = db
        .prepare(
          `SELECT id FROM flights WHERE aircraftId=? AND ${active} AND COALESCE(actual,scheduled)>=?`,
        )
        .get(existing.id, new Date(Date.now() - 30 * 60000).toISOString());
      ensure(
        !(assigned && (item.status !== 'available' || item.airlineId !== existing.airlineId)),
        'Realocar ou cancelar os voos ativos antes de indisponibilizar esta aeronave.',
      );
      const reservations = db
        .prepare(
          "SELECT r.seat FROM reservations r JOIN flights f ON f.id=r.flightId WHERE f.aircraftId=? AND r.status!='cancelled' AND f.status NOT IN ('cancelled','landed')",
        )
        .all(existing.id);
      ensure(
        reservations.every((r) => seatIndex(r.seat) < item.capacity),
        'A capacidade informada excluiria assentos ja reservados.',
      );
    }
  }
  if (table === 'flights') {
    const aircraft = record(db, 'aircraft', item.aircraftId),
      gate = record(db, 'gates', item.gateId),
      airline = record(db, 'airlines', item.airlineId);
    ensure(
      aircraft.airlineId === airline.id,
      'A aeronave deve pertencer a companhia selecionada.',
      422,
    );
    ensure(
      item.originCode !== item.destinationCode &&
        item.origin.toLowerCase() !== item.destination.toLowerCase(),
      'Origem e destino devem ser diferentes.',
      422,
    );
    ensure(
      (item.type === 'departure' ? item.originCode : item.destinationCode) === 'GRU',
      'Esta base opera voos com chegada ou partida em GRU.',
      422,
    );
    ensure(
      !item.actual || ms(item.actual) >= ms(item.scheduled),
      'O horario real nao pode ser anterior ao previsto.',
      422,
    );
    ensure(
      item.status !== 'delayed' || (item.actual && ms(item.actual) > ms(item.scheduled)),
      'Informe a nova previsao para um voo atrasado.',
      422,
    );
    ensure(
      item.status !== 'landed' || item.actual,
      'Informe o horario real de um voo pousado.',
      422,
    );
    ensure(
      !['cancelled', 'maintenance'].includes(item.status) || item.notes.length >= 5,
      'Informe o motivo desta alteracao.',
      422,
    );
    if (existing) {
      ensure(
        existing.status !== 'cancelled' && existing.status !== 'landed',
        'Voos encerrados ou cancelados nao podem ser alterados.',
      );
      const booked = db
        .prepare("SELECT seat FROM reservations WHERE flightId=? AND status!='cancelled'")
        .all(existing.id);
      ensure(
        booked.every((r) => seatIndex(r.seat) < aircraft.capacity),
        'A nova aeronave nao comporta os assentos reservados.',
      );
      ensure(
        !booked.length || item.type === existing.type,
        'Voos com reservas nao podem mudar entre chegada e partida.',
      );
    }
    if (!['cancelled', 'landed', 'maintenance'].includes(item.status)) {
      ensure(
        aircraft.status === 'available',
        'Aeronave indisponivel ou em manutencao. Escolha outra aeronave.',
      );
      ensure(gate.status === 'available', 'Portao bloqueado. Selecione um portao disponivel.');
      const sameGate = db
        .prepare(`SELECT * FROM flights WHERE gateId=? AND id!=? AND ${active}`)
        .all(item.gateId, existing?.id || '');
      const conflict = sameGate.find((f) => gateOverlap(f, item));
      ensure(
        !conflict,
        `Conflito de portao: ${conflict?.number} ja ocupa este portao. Reserve um intervalo de 90 minutos entre voos.`,
      );
      const sameAircraft = db
        .prepare(`SELECT * FROM flights WHERE aircraftId=? AND id!=? AND ${active}`)
        .all(item.aircraftId, existing?.id || '');
      const clash = sameAircraft.find(
        (f) =>
          flightTime(item) < flightTime(f) + (f.duration + 60) * 60000 &&
          flightTime(f) < flightTime(item) + (item.duration + 60) * 60000,
      );
      ensure(
        !clash,
        `Aeronave alocada ao voo ${clash?.number}. Respeite o tempo de voo e 60 minutos de preparacao.`,
      );
    }
  }
}

export function saveEntity(db, user, table, raw, id) {
  ensure(tables.includes(table), 'Operacao inexistente.', 404);
  authorize(user, table);
  const item = schemas[table].parse(raw);
  return transaction(db, () => {
    const existing = id ? record(db, table, id) : null;
    if (existing)
      ensure(
        item.version === existing.version,
        'Este registro foi alterado por outro usuario. Atualize a pagina e tente novamente.',
      );
    validateBusiness(db, table, item, existing);
    const { version, ...values } = item;
    const fields = Object.keys(values);
    const entityId = id || randomUUID();
    if (existing)
      db.prepare(
        `UPDATE ${table} SET ${fields.map((f) => `${f}=?`).join(',')},version=version+1 WHERE id=?`,
      ).run(...Object.values(values), id);
    else
      db.prepare(
        `INSERT INTO ${table}(id,${fields.join(',')}) VALUES (${['?', ...fields.map(() => '?')].join(',')})`,
      ).run(entityId, ...Object.values(values));
    if (table === 'flights' && item.status === 'cancelled') {
      db.prepare(
        "UPDATE reservations SET status='cancelled',version=version+1 WHERE flightId=? AND status!='cancelled'",
      ).run(entityId);
    }
    const identifier = item.number || item.name || item.code || item.registration;
    const sensitive = new Set(['document', 'birthDate', 'email', 'phone']);
    const changed = existing
      ? fields
          .filter((k) => existing[k] !== values[k])
          .map((k) =>
            sensitive.has(k)
              ? `${k}: atualizado`
              : `${k}: ${existing[k] ?? '-'} -> ${values[k] ?? '-'}`,
          )
          .join('; ')
      : 'Novo registro';
    audit(
      db,
      user,
      existing ? 'Alteracao' : 'Cadastro',
      table,
      entityId,
      `${labels[table]} ${identifier}. ${changed}`,
    );
    return record(db, table, entityId);
  });
}

export function seatIndex(seat) {
  const match = /^([1-9]\d?)([A-F])$/.exec(seat);
  return match ? (Number(match[1]) - 1) * 6 + 'ABCDEF'.indexOf(match[2]) : Infinity;
}

export function createReservation(db, user, raw) {
  authorize(user, 'reservations');
  const item = schemas.reservations.parse(raw);
  return transaction(db, () => {
    record(db, 'passengers', item.passengerId);
    const flight = record(db, 'flights', item.flightId),
      aircraft = record(db, 'aircraft', flight.aircraftId);
    ensure(flight.type === 'departure', 'Reservas disponiveis apenas para partidas de GRU.');
    ensure(
      ['scheduled', 'delayed', 'boarding'].includes(flight.status),
      'Voo indisponivel para reservas.',
    );
    ensure(flightTime(flight) > Date.now(), 'O horario de partida deste voo ja passou.');
    ensure(aircraft.status === 'available', 'A aeronave deste voo esta indisponivel.');
    ensure(
      seatIndex(item.seat) < aircraft.capacity,
      'Assento fora da capacidade da aeronave.',
      422,
    );
    ensure(
      !db
        .prepare("SELECT id FROM reservations WHERE flightId=? AND seat=? AND status!='cancelled'")
        .get(item.flightId, item.seat),
      'Este assento ja esta reservado. Escolha outro.',
    );
    ensure(
      !db
        .prepare(
          "SELECT id FROM reservations WHERE flightId=? AND passengerId=? AND status!='cancelled'",
        )
        .get(item.flightId, item.passengerId),
      'O passageiro ja possui reserva neste voo.',
    );
    const id = randomUUID(),
      locator = randomBytes(5).toString('hex').slice(0, 6).toUpperCase();
    db.prepare(
      'INSERT INTO reservations(id,locator,passengerId,flightId,seat,status,createdAt) VALUES (?,?,?,?,?,?,?)',
    ).run(
      id,
      locator,
      item.passengerId,
      item.flightId,
      item.seat,
      'confirmed',
      new Date().toISOString(),
    );
    audit(
      db,
      user,
      'Reserva',
      'reservations',
      id,
      `Reserva ${locator}: ${flight.number}, assento ${item.seat}.`,
    );
    return record(db, 'reservations', id);
  });
}

export function updateReservation(db, user, id, action, version) {
  authorize(user, action === 'checkin' ? 'checkin' : 'reservations');
  return transaction(db, () => {
    const reservation = record(db, 'reservations', id),
      flight = record(db, 'flights', reservation.flightId);
    ensure(
      version === reservation.version,
      'Reserva alterada por outro usuario. Atualize a pagina.',
    );
    ensure(reservation.status !== 'cancelled', 'Esta reserva esta cancelada.');
    if (action === 'checkin') {
      ensure(reservation.status === 'confirmed', 'O check-in ja foi realizado.');
      ensure(
        ['scheduled', 'boarding', 'delayed'].includes(flight.status),
        'Este voo nao permite check-in.',
      );
      ensure(flight.type === 'departure', 'Check-in disponivel apenas para partidas.');
      const time = flightTime(flight) - Date.now();
      ensure(
        time > 0 && time <= 48 * 60 * 60 * 1000,
        'Check-in disponivel nas 48 horas anteriores a partida.',
      );
      ensure(
        record(db, 'aircraft', flight.aircraftId).status === 'available',
        'Aeronave indisponivel para check-in.',
      );
      db.prepare(
        "UPDATE reservations SET status='checked_in',checkedAt=?,version=version+1 WHERE id=?",
      ).run(new Date().toISOString(), id);
      audit(
        db,
        user,
        'Check-in',
        'reservations',
        id,
        `Check-in realizado: ${reservation.locator}, ${flight.number}, assento ${reservation.seat}.`,
      );
    } else {
      ensure(
        flight.status !== 'landed' && flightTime(flight) > Date.now(),
        'Nao e possivel cancelar uma reserva de voo encerrado.',
      );
      db.prepare("UPDATE reservations SET status='cancelled',version=version+1 WHERE id=?").run(id);
      audit(
        db,
        user,
        'Cancelamento',
        'reservations',
        id,
        `Reserva ${reservation.locator} cancelada.`,
      );
    }
    return record(db, 'reservations', id);
  });
}

export function csvExport(db, user, table, day) {
  ensure(
    ['flights', 'passengers', 'airlines', 'audit'].includes(table),
    'Relatorio inexistente.',
    404,
  );
  if (table === 'passengers') authorize(user, 'passengers');
  let rows;
  if (table === 'flights')
    rows = db
      .prepare(
        "SELECT f.number AS voo,a.name AS companhia,f.origin AS origem,f.destination AS destino,f.scheduled AS previsto,f.actual AS real,g.code AS portao,f.status AS status FROM flights f JOIN airlines a ON a.id=f.airlineId JOIN gates g ON g.id=f.gateId WHERE date(f.scheduled,'-3 hours')=? ORDER BY f.scheduled",
      )
      .all(day || today());
  else if (table === 'passengers')
    rows = db
      .prepare(
        'SELECT name AS nome,email,phone AS telefone,nationality AS nacionalidade FROM passengers ORDER BY name',
      )
      .all();
  else if (table === 'airlines')
    rows = db
      .prepare(
        'SELECT name AS companhia,code AS codigo,country AS pais,contact AS contato FROM airlines ORDER BY name',
      )
      .all();
  else
    rows = db
      .prepare(
        'SELECT a.createdAt AS data,u.name AS usuario,a.action AS acao,a.entity AS entidade,a.detail AS detalhe FROM audit a JOIN users u ON u.id=a.userId ORDER BY a.createdAt DESC',
      )
      .all();
  const quote = (value) =>
    `"${String(value ?? '')
      .replace(/^[=+@-]/, "'$&")
      .replace(/"/g, '""')}"`;
  const columns = rows.length ? Object.keys(rows[0]) : ['Sem registros'];
  return (
    '\uFEFF' +
    [
      columns.map(quote).join(';'),
      ...rows.map((row) => columns.map((key) => quote(row[key])).join(';')),
    ].join('\r\n')
  );
}

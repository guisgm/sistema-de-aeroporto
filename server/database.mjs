import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { randomBytes, scryptSync } from 'node:crypto';

export const today = () =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date());
export const localTime = (day, hour) => new Date(`${day}T${hour}:00-03:00`).toISOString();

export function passwordHash(password, salt = randomBytes(16).toString('hex')) {
  return `${salt}:${scryptSync(password, salt, 64).toString('hex')}`;
}

export function openDatabase(path = 'data/aerohub.sqlite') {
  if (path !== ':memory:') mkdirSync(dirname(path), { recursive: true });
  const db = new DatabaseSync(path);
  db.exec(`
    PRAGMA foreign_keys = ON;
    PRAGMA journal_mode = WAL;
    PRAGMA busy_timeout = 5000;
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL,
      role TEXT NOT NULL CHECK(role IN ('admin','operator','attendant')), password TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS airlines (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, code TEXT NOT NULL UNIQUE, color TEXT NOT NULL,
      country TEXT NOT NULL, contact TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS terminals (
      id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, kind TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS gates (
      id TEXT PRIMARY KEY, code TEXT UNIQUE NOT NULL, terminalId TEXT NOT NULL REFERENCES terminals(id),
      status TEXT NOT NULL CHECK(status IN ('available','blocked')), version INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS aircraft (
      id TEXT PRIMARY KEY, registration TEXT UNIQUE NOT NULL, model TEXT NOT NULL, capacity INTEGER NOT NULL CHECK(capacity BETWEEN 6 AND 600),
      airlineId TEXT NOT NULL REFERENCES airlines(id), status TEXT NOT NULL CHECK(status IN ('available','maintenance','unavailable')),
      maintenanceDate TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', version INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS flights (
      id TEXT PRIMARY KEY, number TEXT NOT NULL, airlineId TEXT NOT NULL REFERENCES airlines(id),
      aircraftId TEXT NOT NULL REFERENCES aircraft(id), origin TEXT NOT NULL, destination TEXT NOT NULL,
      originCode TEXT NOT NULL, destinationCode TEXT NOT NULL, type TEXT NOT NULL CHECK(type IN ('departure','arrival')),
      scheduled TEXT NOT NULL, actual TEXT, duration INTEGER NOT NULL CHECK(duration BETWEEN 20 AND 1440),
      gateId TEXT NOT NULL REFERENCES gates(id), status TEXT NOT NULL CHECK(status IN ('scheduled','boarding','delayed','cancelled','landed','maintenance')),
      notes TEXT NOT NULL DEFAULT '', version INTEGER NOT NULL DEFAULT 1, UNIQUE(number,scheduled)
    );
    CREATE TABLE IF NOT EXISTS passengers (
      id TEXT PRIMARY KEY, name TEXT NOT NULL, documentType TEXT NOT NULL CHECK(documentType IN ('cpf','passport')),
      document TEXT UNIQUE NOT NULL, birthDate TEXT NOT NULL, email TEXT NOT NULL, phone TEXT NOT NULL,
      nationality TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS reservations (
      id TEXT PRIMARY KEY, locator TEXT UNIQUE NOT NULL, passengerId TEXT NOT NULL REFERENCES passengers(id),
      flightId TEXT NOT NULL REFERENCES flights(id), seat TEXT NOT NULL,
      status TEXT NOT NULL CHECK(status IN ('confirmed','checked_in','cancelled')), checkedAt TEXT,
      createdAt TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1
    );
    CREATE UNIQUE INDEX IF NOT EXISTS seat_occupied ON reservations(flightId,seat) WHERE status != 'cancelled';
    CREATE UNIQUE INDEX IF NOT EXISTS passenger_booked ON reservations(flightId,passengerId) WHERE status != 'cancelled';
    CREATE TABLE IF NOT EXISTS audit (
      id TEXT PRIMARY KEY, createdAt TEXT NOT NULL, userId TEXT NOT NULL REFERENCES users(id),
      action TEXT NOT NULL, entity TEXT NOT NULL, entityId TEXT NOT NULL, detail TEXT NOT NULL
    );
    CREATE INDEX IF NOT EXISTS flight_date ON flights(scheduled);
    CREATE INDEX IF NOT EXISTS flight_gate ON flights(gateId,scheduled);
    CREATE INDEX IF NOT EXISTS audit_date ON audit(createdAt);
  `);
  if (!db.prepare('SELECT id FROM users LIMIT 1').get()) seed(db);
  return db;
}

export function transaction(db, action) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const result = action();
    db.exec('COMMIT');
    return result;
  } catch (error) {
    db.exec('ROLLBACK');
    throw error;
  }
}

function seed(db) {
  transaction(db, () => {
    const users = [
      ['admin', 'Marina Costa', 'admin@aerohub.local', 'admin'],
      ['operator', 'Rafael Lima', 'operador@aerohub.local', 'operator'],
      ['attendant', 'Camila Santos', 'atendente@aerohub.local', 'attendant'],
    ];
    for (const u of users)
      db.prepare('INSERT INTO users VALUES (?,?,?,?,?)').run(...u, passwordHash('AeroHub@2026!'));
    const airlines = [
      ['latam', 'LATAM Airlines', 'LA', '#7d3456', 'Brasil', 'operacoes@latam.example'],
      ['gol', 'GOL Linhas Aereas', 'G3', '#de7134', 'Brasil', 'operacoes@gol.example'],
      ['azul', 'Azul Linhas Aereas', 'AD', '#247ba5', 'Brasil', 'operacoes@azul.example'],
      ['tap', 'TAP Air Portugal', 'TP', '#577e40', 'Portugal', 'operacoes@tap.example'],
    ];
    for (const a of airlines)
      db.prepare(
        'INSERT INTO airlines(id,name,code,color,country,contact) VALUES (?,?,?,?,?,?)',
      ).run(...a);
    for (let t = 1; t <= 3; t++) {
      db.prepare('INSERT INTO terminals(id,name,kind) VALUES (?,?,?)').run(
        `t${t}`,
        `Terminal ${t}`,
        t === 3 ? 'Internacional' : 'Domestico',
      );
      for (let g = 1; g <= 4; g++)
        db.prepare('INSERT INTO gates(id,code,terminalId,status) VALUES (?,?,?,?)').run(
          `g${t}${g}`,
          `${['A', 'B', 'C'][t - 1]}${String(g).padStart(2, '0')}`,
          `t${t}`,
          t === 2 && g === 4 ? 'blocked' : 'available',
        );
    }
    for (const [airline] of airlines) {
      for (let n = 0; n < 5; n++) {
        db.prepare(
          'INSERT INTO aircraft(id,registration,model,capacity,airlineId,status,maintenanceDate,notes) VALUES (?,?,?,?,?,?,?,?)',
        ).run(
          `${airline}-${n}`,
          `PR-${{ latam: 'LA', gol: 'GO', azul: 'AZ', tap: 'TA' }[airline]}${n}`,
          n === 4 ? 'Airbus A330-900' : n % 2 ? 'Boeing 737-800' : 'Airbus A320neo',
          n === 4 ? 298 : 180,
          airline,
          n === 4 && airline === 'gol' ? 'maintenance' : 'available',
          today(),
          n === 4 && airline === 'gol' ? 'Inspecao preventiva de motores' : '',
        );
      }
    }
    const cities = [
      ['Recife', 'REC'],
      ['Rio de Janeiro', 'GIG'],
      ['Brasilia', 'BSB'],
      ['Salvador', 'SSA'],
      ['Porto Alegre', 'POA'],
      ['Belo Horizonte', 'CNF'],
      ['Fortaleza', 'FOR'],
      ['Lisboa', 'LIS'],
    ];
    const availableGates = db
      .prepare("SELECT id FROM gates WHERE status='available' ORDER BY id")
      .all();
    const hours = ['06:15', '08:40', '11:05', '13:30', '15:55', '18:20', '20:45', '23:10'];
    for (let d = -6; d <= 1; d++) {
      const day = new Date(`${today()}T12:00:00-03:00`);
      day.setUTCDate(day.getUTCDate() + d);
      const date = day.toISOString().slice(0, 10);
      const count = d === 0 ? 32 : 16;
      for (let i = 0; i < count; i++) {
        const airline = airlines[i % 4][0];
        const [city, code] = cities[i % cities.length];
        const type = i % 5 === 1 || i % 5 === 3 ? 'arrival' : 'departure';
        const scheduled = localTime(
          date,
          hours[Math.floor(i / availableGates.length) * 2 + (i % 2)],
        );
        const isDelayed = (d === 0 && [5, 17, 27].includes(i)) || (d < 0 && i % 8 === 0);
        const actual = isDelayed
          ? new Date(new Date(scheduled).getTime() + (d === 0 ? 35 : 15) * 60000).toISOString()
          : null;
        const remaining = new Date(actual || scheduled).getTime() - Date.now();
        const status =
          d < 0
            ? i % 13 === 0
              ? 'cancelled'
              : 'landed'
            : d > 0
              ? 'scheduled'
              : i === 9
                ? 'cancelled'
                : remaining <= 0
                  ? 'landed'
                  : isDelayed
                    ? 'delayed'
                    : type === 'departure' && remaining < 90 * 60000
                      ? 'boarding'
                      : 'scheduled';
        // Each aircraft rotates once per time block; gates keep a 90-minute occupancy interval.
        const aircraftIndex = Math.floor(i / 4) % 4;
        const id = `flight-${d}-${i}`;
        db.prepare(
          'INSERT INTO flights(id,number,airlineId,aircraftId,origin,destination,originCode,destinationCode,type,scheduled,actual,duration,gateId,status,notes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
        ).run(
          id,
          `${airlines[i % 4][2]} ${1200 + i * 37}`,
          airline,
          `${airline}-${aircraftIndex}`,
          type === 'departure' ? 'Sao Paulo' : city,
          type === 'departure' ? city : 'Sao Paulo',
          type === 'departure' ? 'GRU' : code,
          type === 'departure' ? code : 'GRU',
          type,
          scheduled,
          actual || (status === 'landed' ? scheduled : null),
          90,
          availableGates[i % availableGates.length].id,
          status,
          status === 'delayed'
            ? 'Ajuste da malha aerea. Previsao atualizada em 35 minutos.'
            : status === 'cancelled'
              ? 'Cancelamento operacional da companhia.'
              : '',
        );
      }
    }
    const names = [
      'Ana Beatriz Oliveira',
      'Pedro Henrique Santos',
      'Juliana Ferreira',
      'Lucas Almeida',
      'Mariana Souza',
      'Gabriel Rodrigues',
      'Fernanda Lima',
      'Rafael Costa',
      'Camila Ribeiro',
      'Bruno Martins',
      'Isabela Araujo',
      'Thiago Pereira',
      'Leticia Carvalho',
      'Gustavo Mendes',
      'Carolina Rocha',
      'Diego Nascimento',
      'Amanda Barbosa',
      'Felipe Dias',
      'Renata Moreira',
      'Vinicius Cardoso',
      'Patricia Castro',
      'Andre Teixeira',
      'Larissa Gomes',
      'Eduardo Fernandes',
    ];
    for (let i = 0; i < names.length; i++) {
      const passengerId = `passenger-${i}`;
      db.prepare(
        'INSERT INTO passengers(id,name,documentType,document,birthDate,email,phone,nationality) VALUES (?,?,?,?,?,?,?,?)',
      ).run(
        passengerId,
        names[i],
        'passport',
        `BR${String(102340 + i)}`,
        '1990-06-15',
        `passageiro${i + 1}@example.com`,
        '11999990000',
        'Brasileira',
      );
      const flightIndex = [25, 27, 29, 9, 24, 30, 22][i % 7];
      const cancelled = flightIndex === 9;
      db.prepare(
        'INSERT INTO reservations(id,locator,passengerId,flightId,seat,status,checkedAt,createdAt) VALUES (?,?,?,?,?,?,?,?)',
      ).run(
        `reservation-${i}`,
        `GRU${String(i + 1).padStart(3, '0')}`,
        passengerId,
        `flight-0-${flightIndex}`,
        `${Math.floor(i / 7) + 1}${'ABCDEF'[i % 6]}`,
        cancelled ? 'cancelled' : i % 3 === 0 ? 'checked_in' : 'confirmed',
        i % 3 === 0 && !cancelled ? new Date().toISOString() : null,
        new Date().toISOString(),
      );
    }
    db.prepare('INSERT INTO audit VALUES (?,?,?,?,?,?,?)').run(
      'initial',
      new Date().toISOString(),
      'admin',
      'Inicializacao',
      'system',
      'seed',
      'Base de simulacao criada com dados ficticios.',
    );
  });
}

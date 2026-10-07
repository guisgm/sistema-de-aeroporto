-- Schema web PostgreSQL; search_path definido pela conexao.
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

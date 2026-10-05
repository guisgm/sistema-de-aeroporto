import { openDatabase, transaction, today } from '../server/database.mjs';
import { audit } from '../server/operations.mjs';

const db = openDatabase(process.env.DB_PATH || 'data/aerohub.sqlite');
try {
  const flights = db
    .prepare("SELECT * FROM flights WHERE id LIKE 'flight-0-%' AND version=1")
    .all();
  const count = transaction(db, () => {
    let changed = 0;
    for (const flight of flights) {
      const day = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(
        new Date(flight.scheduled),
      );
      if (day !== today() || ['cancelled', 'maintenance'].includes(flight.status)) continue;
      const remaining = Date.parse(flight.actual || flight.scheduled) - Date.now();
      const delayed = flight.actual && Date.parse(flight.actual) > Date.parse(flight.scheduled);
      const status =
        remaining <= 0
          ? 'landed'
          : delayed
            ? 'delayed'
            : flight.type === 'departure' && remaining < 90 * 60000
              ? 'boarding'
              : 'scheduled';
      if (status === flight.status) continue;
      db.prepare('UPDATE flights SET status=?,actual=? WHERE id=?').run(
        status,
        flight.actual || (status === 'landed' ? flight.scheduled : null),
        flight.id,
      );
      if (status === 'landed')
        db.prepare(
          "UPDATE reservations SET status='checked_in',checkedAt=? WHERE flightId=? AND id LIKE 'reservation-%' AND version=1 AND status='confirmed'",
        ).run(new Date(Date.parse(flight.scheduled) - 30 * 60000).toISOString(), flight.id);
      changed++;
    }
    if (changed)
      audit(
        db,
        { id: 'admin' },
        'Simulacao',
        'system',
        'clock',
        `${changed} voos originais sincronizados com o horario local. Voos editados preservados.`,
      );
    return changed;
  });
  console.log(`${count} voos de demonstracao sincronizados.`);
} finally {
  db.close();
}

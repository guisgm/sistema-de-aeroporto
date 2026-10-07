import { openDatabase, transaction, today } from '../server/database.mjs';
import { audit } from '../server/operations.mjs';

const db = await openDatabase();
try {
  const flights = await db.all(
    "SELECT * FROM flights WHERE id LIKE 'flight-0-%' AND version=1",
    [],
  );
  const count = await transaction(db, async () => {
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
      await db.execute('UPDATE flights SET status=?,actual=? WHERE id=?', [
        status,
        flight.actual || (status === 'landed' ? flight.scheduled : null),
        flight.id,
      ]);
      if (status === 'landed')
        await db.execute(
          "UPDATE reservations SET status='checked_in',checkedAt=? WHERE flightId=? AND id LIKE 'reservation-%' AND version=1 AND status='confirmed'",
          [new Date(Date.parse(flight.scheduled) - 30 * 60000).toISOString(), flight.id],
        );
      changed++;
    }
    if (changed)
      await audit(
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
  await db.close();
}

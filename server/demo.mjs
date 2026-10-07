import { passwordHash, transaction, today, localTime } from './database.mjs';

export async function seed(db) {
  return transaction(db, async () => {
    const users = [
      ['admin', 'Marina Costa', 'admin@aerohub.local', 'admin'],
      ['operator', 'Rafael Lima', 'operador@aerohub.local', 'operator'],
      ['attendant', 'Camila Santos', 'atendente@aerohub.local', 'attendant'],
    ];
    for (const u of users)
      await db.execute('INSERT INTO users VALUES (?,?,?,?,?)', [
        ...u,
        passwordHash('AeroHub@2026!'),
      ]);
    const airlines = [
      ['latam', 'LATAM Airlines', 'LA', '#7d3456', 'Brasil', 'operacoes@latam.example'],
      ['gol', 'GOL Linhas Aereas', 'G3', '#de7134', 'Brasil', 'operacoes@gol.example'],
      ['azul', 'Azul Linhas Aereas', 'AD', '#247ba5', 'Brasil', 'operacoes@azul.example'],
      ['tap', 'TAP Air Portugal', 'TP', '#577e40', 'Portugal', 'operacoes@tap.example'],
    ];
    for (const a of airlines)
      await db.execute(
        'INSERT INTO airlines(id,name,code,color,country,contact) VALUES (?,?,?,?,?,?)',
        [...a],
      );
    for (let t = 1; t <= 3; t++) {
      await db.execute('INSERT INTO terminals(id,name,kind) VALUES (?,?,?)', [
        `t${t}`,
        `Terminal ${t}`,
        t === 3 ? 'Internacional' : 'Domestico',
      ]);
      for (let g = 1; g <= 4; g++)
        await db.execute('INSERT INTO gates(id,code,terminalId,status) VALUES (?,?,?,?)', [
          `g${t}${g}`,
          `${['A', 'B', 'C'][t - 1]}${String(g).padStart(2, '0')}`,
          `t${t}`,
          t === 2 && g === 4 ? 'blocked' : 'available',
        ]);
    }
    for (const [airline] of airlines) {
      for (let n = 0; n < 5; n++) {
        await db.execute(
          'INSERT INTO aircraft(id,registration,model,capacity,airlineId,status,maintenanceDate,notes) VALUES (?,?,?,?,?,?,?,?)',
          [
            `${airline}-${n}`,
            `PR-${{ latam: 'LA', gol: 'GO', azul: 'AZ', tap: 'TA' }[airline]}${n}`,
            n === 4 ? 'Airbus A330-900' : n % 2 ? 'Boeing 737-800' : 'Airbus A320neo',
            n === 4 ? 298 : 180,
            airline,
            n === 4 && airline === 'gol' ? 'maintenance' : 'available',
            today(),
            n === 4 && airline === 'gol' ? 'Inspecao preventiva de motores' : '',
          ],
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
    const availableGates = await db.all(
      "SELECT id FROM gates WHERE status='available' ORDER BY id",
      [],
    );
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
        await db.execute(
          'INSERT INTO flights(id,number,airlineId,aircraftId,origin,destination,originCode,destinationCode,type,scheduled,actual,duration,gateId,status,notes) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
          [
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
          ],
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
      await db.execute(
        'INSERT INTO passengers(id,name,documentType,document,birthDate,email,phone,nationality) VALUES (?,?,?,?,?,?,?,?)',
        [
          passengerId,
          names[i],
          'passport',
          `BR${String(102340 + i)}`,
          '1990-06-15',
          `passageiro${i + 1}@example.com`,
          '11999990000',
          'Brasileira',
        ],
      );
      const flightIndex = [25, 27, 29, 9, 24, 30, 22][i % 7];
      const cancelled = flightIndex === 9;
      await db.execute(
        'INSERT INTO reservations(id,locator,passengerId,flightId,seat,status,checkedAt,createdAt) VALUES (?,?,?,?,?,?,?,?)',
        [
          `reservation-${i}`,
          `GRU${String(i + 1).padStart(3, '0')}`,
          passengerId,
          `flight-0-${flightIndex}`,
          `${Math.floor(i / 7) + 1}${'ABCDEF'[i % 6]}`,
          cancelled ? 'cancelled' : i % 3 === 0 ? 'checked_in' : 'confirmed',
          i % 3 === 0 && !cancelled ? new Date().toISOString() : null,
          new Date().toISOString(),
        ],
      );
    }
    await db.execute('INSERT INTO audit VALUES (?,?,?,?,?,?,?)', [
      'initial',
      new Date().toISOString(),
      'admin',
      'Inicializacao',
      'system',
      'seed',
      'Base de simulacao criada com dados ficticios.',
    ]);
  });
}

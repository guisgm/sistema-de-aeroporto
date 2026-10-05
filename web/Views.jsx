import React, { useState } from 'react';
import {
  Plus,
  Download,
  Pencil,
  ArrowUpRight,
  Ticket,
  CheckCircle2,
  Ban,
  Plane,
  Building2,
  Wrench,
  Clock3,
  AlertTriangle,
  ArrowRight,
  Printer,
  CalendarDays,
} from 'lucide-react';
import {
  SearchInput,
  Pagination,
  FlightTable,
  IconButton,
  Badge,
  Avatar,
  AirlineMark,
  Empty,
} from './components';
import {
  localDay,
  normalize,
  can,
  time,
  dateTime,
  effective,
  activeFlight,
  statusLabels,
} from './model';

export function FlightsView({ state, index, user, date, onFlight, onCreate, onExport }) {
  const [search, setSearch] = useState(''),
    [status, setStatus] = useState(''),
    [airline, setAirline] = useState(''),
    [terminal, setTerminal] = useState(''),
    [type, setType] = useState('all'),
    [page, setPage] = useState(1);
  const filter = (setter) => (value) => {
    setter(value);
    setPage(1);
  };
  const filtered = state.flights
    .filter(
      (f) =>
        localDay(f.scheduled) === date &&
        (!status || f.status === status) &&
        (!airline || f.airlineId === airline) &&
        (!terminal || index.gates[f.gateId].terminalId === terminal) &&
        (type === 'all' || f.type === type) &&
        normalize(
          [f.number, f.destination, f.origin, index.airlines[f.airlineId].name].join(' '),
        ).includes(normalize(search)),
    )
    .sort((a, b) => effective(a) - effective(b));
  const safePage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 10)));
  return (
    <section className="section">
      <div className="view-toolbar">
        <div className="tabs">
          <button className={type === 'all' ? 'active' : ''} onClick={() => filter(setType)('all')}>
            Todos os voos
          </button>
          <button
            className={type === 'departure' ? 'active' : ''}
            onClick={() => filter(setType)('departure')}
          >
            Partidas
          </button>
          <button
            className={type === 'arrival' ? 'active' : ''}
            onClick={() => filter(setType)('arrival')}
          >
            Chegadas
          </button>
        </div>
        <div className="toolbar-actions">
          <button className="button secondary" onClick={() => onExport('flights')}>
            <Download size={16} />
            Exportar
          </button>
          {can(user, 'flights') && (
            <button className="button primary" onClick={() => onCreate('flights')}>
              <Plus size={17} />
              Novo voo
            </button>
          )}
        </div>
      </div>
      <div className="filters">
        <SearchInput
          value={search}
          onChange={filter(setSearch)}
          placeholder="Buscar voo ou destino"
        />
        <select
          aria-label="Filtrar por status"
          value={status}
          onChange={(e) => filter(setStatus)(e.target.value)}
        >
          <option value="">Todos os status</option>
          {['scheduled', 'boarding', 'delayed', 'cancelled', 'landed', 'maintenance'].map((s) => (
            <option key={s} value={s}>
              {statusLabels[s]}
            </option>
          ))}
        </select>
        <select
          aria-label="Filtrar por companhia"
          value={airline}
          onChange={(e) => filter(setAirline)(e.target.value)}
        >
          <option value="">Todas as companhias</option>
          {state.airlines.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
        <select
          aria-label="Filtrar por terminal"
          value={terminal}
          onChange={(e) => filter(setTerminal)(e.target.value)}
        >
          <option value="">Todos os terminais</option>
          {state.terminals.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </div>
      <FlightTable
        flights={filtered.slice((safePage - 1) * 10, safePage * 10)}
        index={index}
        onOpen={onFlight}
      />
      <Pagination page={safePage} setPage={setPage} total={filtered.length} />
    </section>
  );
}

export function PassengersView({
  state,
  index,
  user,
  onEdit,
  onCreate,
  onReservation,
  onCheckin,
  onBoarding,
  onCancel,
  selected,
}) {
  const [search, setSearch] = useState(selected?.name || ''),
    [tab, setTab] = useState('passengers'),
    [page, setPage] = useState(1);
  const filtered =
    tab === 'passengers'
      ? state.passengers.filter((p) =>
          normalize([p.name, p.email, p.document].join(' ')).includes(normalize(search)),
        )
      : state.reservations.filter((r) =>
          normalize(
            [
              r.locator,
              index.passengers[r.passengerId].name,
              index.flights[r.flightId].number,
            ].join(' '),
          ).includes(normalize(search)),
        );
  const safePage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 10)));
  return (
    <section className="section">
      <div className="view-toolbar">
        <div className="tabs">
          <button
            className={tab === 'passengers' ? 'active' : ''}
            onClick={() => {
              setTab('passengers');
              setSearch('');
              setPage(1);
            }}
          >
            Passageiros<span>{state.passengers.length}</span>
          </button>
          <button
            className={tab === 'reservations' ? 'active' : ''}
            onClick={() => {
              setTab('reservations');
              setSearch('');
              setPage(1);
            }}
          >
            Reservas<span>{state.reservations.length}</span>
          </button>
        </div>
        <div className="toolbar-actions">
          {can(user, 'reservations') && (
            <button className="button secondary" onClick={() => onReservation()}>
              <Ticket size={16} />
              Nova reserva
            </button>
          )}
          {can(user, 'passengers') && (
            <button className="button primary" onClick={() => onCreate('passengers')}>
              <Plus size={16} />
              Novo passageiro
            </button>
          )}
        </div>
      </div>
      <div className="filters">
        <SearchInput
          value={search}
          onChange={(v) => {
            setSearch(v);
            setPage(1);
          }}
          placeholder={
            tab === 'passengers'
              ? 'Buscar nome, documento ou e-mail'
              : 'Buscar reserva, passageiro ou voo'
          }
        />
      </div>
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            {tab === 'passengers' ? (
              <tr>
                <th>Passageiro</th>
                <th>Documento</th>
                <th>Contato</th>
                <th>Reservas</th>
                <th>Ações</th>
              </tr>
            ) : (
              <tr>
                <th>Reserva / Passageiro</th>
                <th>Voo</th>
                <th>Assento</th>
                <th>Status</th>
                <th>Ações</th>
              </tr>
            )}
          </thead>
          <tbody>
            {filtered.slice((safePage - 1) * 10, safePage * 10).map((row) =>
              tab === 'passengers' ? (
                <tr key={row.id}>
                  <td>
                    <div className="person-cell">
                      <Avatar name={row.name} />
                      <div>
                        <strong>{row.name}</strong>
                        <small>{row.nationality}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <strong>{row.documentType === 'cpf' ? 'CPF' : 'Passaporte'}</strong>
                    <small>•••• {row.document.slice(-4)}</small>
                  </td>
                  <td>
                    <span>{row.email}</span>
                    <small>{row.phone}</small>
                  </td>
                  <td>
                    <button
                      className="text-link"
                      onClick={() => {
                        setTab('reservations');
                        setSearch(row.name);
                        setPage(1);
                      }}
                    >
                      {state.reservations.filter((r) => r.passengerId === row.id).length} reservas
                      <ArrowRight size={13} />
                    </button>
                  </td>
                  <td>
                    <div className="row-actions">
                      {can(user, 'passengers') && (
                        <IconButton
                          icon={Pencil}
                          label={`Editar ${row.name}`}
                          onClick={() => onEdit('passengers', row)}
                        />
                      )}{' '}
                      {can(user, 'reservations') && (
                        <IconButton
                          icon={Ticket}
                          label={`Reservar para ${row.name}`}
                          onClick={() => onReservation(null, row)}
                        />
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                <tr key={row.id}>
                  <td>
                    <strong className="mono">{row.locator}</strong>
                    <small>{index.passengers[row.passengerId].name}</small>
                  </td>
                  <td>
                    <strong>{index.flights[row.flightId].number}</strong>
                    <small>
                      {dateTime(index.flights[row.flightId].scheduled)} ·{' '}
                      {index.flights[row.flightId].destinationCode}
                    </small>
                  </td>
                  <td>
                    <span className="gate-code">{row.seat}</span>
                  </td>
                  <td>
                    <Badge status={row.status} />
                  </td>
                  <td>
                    <div className="row-actions">
                      {row.status === 'confirmed' && can(user, 'checkin') && (
                        <button className="button mini secondary" onClick={() => onCheckin(row)}>
                          <CheckCircle2 size={15} />
                          Check-in
                        </button>
                      )}
                      {row.status === 'checked_in' && (
                        <IconButton
                          icon={Printer}
                          label={`Cartão de embarque ${row.locator}`}
                          onClick={() => onBoarding(row)}
                        />
                      )}{' '}
                      {row.status !== 'cancelled' && can(user, 'reservations') && (
                        <IconButton
                          icon={Ban}
                          label={`Cancelar reserva ${row.locator}`}
                          onClick={() => onCancel(row)}
                        />
                      )}
                    </div>
                  </td>
                </tr>
              ),
            )}
          </tbody>
        </table>
        {!filtered.length && <Empty />}
      </div>
      <Pagination page={safePage} setPage={setPage} total={filtered.length} />
    </section>
  );
}

export function AirlinesView({ state, index, user, onEdit, onCreate, onHistory }) {
  const [search, setSearch] = useState('');
  const airlines = state.airlines.filter((a) =>
    normalize(a.name + ' ' + a.code).includes(normalize(search)),
  );
  return (
    <>
      <div className="standalone-toolbar">
        <SearchInput value={search} onChange={setSearch} placeholder="Buscar companhia aérea" />
        {can(user, 'airlines') && (
          <button className="button primary" onClick={() => onCreate('airlines')}>
            <Plus size={16} />
            Nova companhia
          </button>
        )}
      </div>
      <div className="airline-list">
        {airlines.map((a) => {
          const flights = state.flights.filter((f) => f.airlineId === a.id),
            aircraft = state.aircraft.filter((p) => p.airlineId === a.id),
            delays = flights.filter(
              (f) =>
                f.status === 'delayed' ||
                (f.actual && effective(f) > new Date(f.scheduled).getTime()),
            ).length,
            cancel = flights.filter((f) => f.status === 'cancelled').length;
          return (
            <article className="airline-card" key={a.id}>
              <header>
                <AirlineMark airline={a} />
                <div>
                  <h2>{a.name}</h2>
                  <p>
                    {a.code} · {a.country}
                  </p>
                </div>
                {can(user, 'airlines') && (
                  <IconButton
                    icon={Pencil}
                    label={`Editar ${a.name}`}
                    onClick={() => onEdit('airlines', a)}
                  />
                )}
              </header>
              <div className="airline-metrics">
                <div>
                  <strong>{aircraft.length}</strong>
                  <span>Aeronaves</span>
                </div>
                <div>
                  <strong>{flights.length}</strong>
                  <span>Voos no período</span>
                </div>
                <div>
                  <strong className="warning-text">
                    {flights.length ? Math.round((delays / flights.length) * 100) : 0}%
                  </strong>
                  <span>Atrasos</span>
                </div>
                <div>
                  <strong>{cancel}</strong>
                  <span>Cancelamentos</span>
                </div>
              </div>
              <div className="fleet-list">
                <span className="eyebrow">FROTA</span>
                {aircraft.map((p) => (
                  <div key={p.id}>
                    <span className="mono">{p.registration}</span>
                    <small>{p.model}</small>
                    <Badge status={p.status} />
                  </div>
                ))}
                {!aircraft.length && <p className="muted">Nenhuma aeronave cadastrada.</p>}
              </div>
              <footer>
                <span>{a.contact}</span>
                <button className="text-link" onClick={() => onHistory(a)}>
                  Histórico
                  <ArrowUpRight size={14} />
                </button>
              </footer>
            </article>
          );
        })}
      </div>
      {!airlines.length && <Empty />}
    </>
  );
}

export function AircraftView({ state, index, user, onEdit, onCreate }) {
  const [search, setSearch] = useState(''),
    [status, setStatus] = useState(''),
    [page, setPage] = useState(1);
  const filtered = state.aircraft.filter(
    (a) =>
      (!status || a.status === status) &&
      normalize(a.registration + ' ' + a.model + ' ' + index.airlines[a.airlineId].name).includes(
        normalize(search),
      ),
  );
  const safePage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 10)));
  return (
    <>
      <div className="inline-metrics">
        {[
          ['Frota total', state.aircraft.length],
          ['Disponíveis', state.aircraft.filter((a) => a.status === 'available').length],
          ['Em manutenção', state.aircraft.filter((a) => a.status === 'maintenance').length],
        ].map(([label, value]) => (
          <div key={label}>
            <Plane size={19} />
            <strong>{value}</strong>
            <span>{label}</span>
          </div>
        ))}
      </div>
      <section className="section">
        <div className="view-toolbar">
          <h2>Frota de aeronaves</h2>
          {can(user, 'aircraft') && (
            <button className="button primary" onClick={() => onCreate('aircraft')}>
              <Plus size={16} />
              Nova aeronave
            </button>
          )}
        </div>
        <div className="filters">
          <SearchInput
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(1);
            }}
            placeholder="Buscar matrícula, modelo ou companhia"
          />
          <select
            aria-label="Status da aeronave"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(1);
            }}
          >
            <option value="">Todos os status</option>
            {['available', 'maintenance', 'unavailable'].map((s) => (
              <option key={s} value={s}>
                {statusLabels[s]}
              </option>
            ))}
          </select>
        </div>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Aeronave</th>
                <th>Companhia</th>
                <th>Capacidade</th>
                <th>Status</th>
                <th>Última manutenção</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.slice((safePage - 1) * 10, safePage * 10).map((a) => (
                <tr key={a.id}>
                  <td>
                    <strong className="mono">{a.registration}</strong>
                    <small>{a.model}</small>
                  </td>
                  <td>
                    <div className="airline-cell">
                      <AirlineMark airline={index.airlines[a.airlineId]} small />
                      <span>{index.airlines[a.airlineId].name}</span>
                    </div>
                  </td>
                  <td>{a.capacity} passageiros</td>
                  <td>
                    <Badge status={a.status} />
                  </td>
                  <td>
                    {new Date(`${a.maintenanceDate}T12:00:00`).toLocaleDateString('pt-BR')}
                    <small>{a.notes || 'Sem observações'}</small>
                  </td>
                  <td>
                    {can(user, 'aircraft') && (
                      <IconButton
                        icon={Pencil}
                        label={`Editar aeronave ${a.registration}`}
                        onClick={() => onEdit('aircraft', a)}
                      />
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!filtered.length && <Empty />}
        </div>
        <Pagination page={safePage} setPage={setPage} total={filtered.length} />
      </section>
    </>
  );
}

export function GatesView({ state, index, user, date, onEdit, onCreate, onFlight }) {
  const [terminal, setTerminal] = useState('');
  const flights = state.flights.filter((f) => localDay(f.scheduled) === date && activeFlight(f));
  return (
    <>
      <div className="standalone-toolbar">
        <select
          aria-label="Selecionar terminal"
          value={terminal}
          onChange={(e) => setTerminal(e.target.value)}
        >
          <option value="">Todos os terminais</option>
          {state.terminals.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
        <div className="toolbar-actions">
          {can(user, 'terminals') && (
            <button className="button secondary" onClick={() => onCreate('terminals')}>
              <Building2 size={16} />
              Novo terminal
            </button>
          )}
          {can(user, 'gates') && (
            <button className="button primary" onClick={() => onCreate('gates')}>
              <Plus size={16} />
              Novo portão
            </button>
          )}
        </div>
      </div>
      {state.terminals
        .filter((t) => !terminal || terminal === t.id)
        .map((t) => (
          <section className="terminal-section" key={t.id}>
            <header className="terminal-title">
              <div>
                <Building2 size={21} />
                <h2>{t.name}</h2>
                <span>{t.kind === 'Domestico' ? 'Doméstico' : t.kind}</span>
              </div>
              {can(user, 'terminals') && (
                <IconButton
                  icon={Pencil}
                  label={`Editar ${t.name}`}
                  onClick={() => onEdit('terminals', t)}
                />
              )}
            </header>
            <div className="gate-grid">
              {state.gates
                .filter((g) => g.terminalId === t.id)
                .map((g) => {
                  const assigned = flights
                    .filter((f) => f.gateId === g.id)
                    .sort((a, b) => effective(a) - effective(b));
                  return (
                    <article
                      className={`gate-card ${g.status === 'blocked' ? 'blocked' : ''}`}
                      key={g.id}
                    >
                      <header>
                        <span className="gate-large">{g.code}</span>
                        {can(user, 'gates') && (
                          <IconButton
                            icon={Pencil}
                            label={`Editar portão ${g.code}`}
                            onClick={() => onEdit('gates', g)}
                          />
                        )}
                      </header>
                      <Badge status={g.status} />
                      <div className="gate-schedule">
                        {assigned.length ? (
                          assigned.map((f) => (
                            <button key={f.id} onClick={() => onFlight(f)}>
                              <span className="mono">{time(f.actual || f.scheduled)}</span>
                              <strong>{f.number}</strong>
                              <small>
                                {f.type === 'departure' ? f.destinationCode : f.originCode}
                              </small>
                              <ArrowUpRight size={13} />
                            </button>
                          ))
                        ) : (
                          <p>
                            {g.status === 'blocked'
                              ? 'Indisponível para alocação'
                              : 'Nenhum voo alocado nesta data'}
                          </p>
                        )}
                      </div>
                      <footer>{assigned.length} voos na programação</footer>
                    </article>
                  );
                })}
            </div>
            {!state.gates.some((g) => g.terminalId === t.id) && (
              <Empty title="Nenhum portão neste terminal" />
            )}
          </section>
        ))}
    </>
  );
}

export function AlertsView({ alerts, onOpen }) {
  return (
    <section className="section">
      <div className="view-toolbar">
        <h2>{alerts.length} alertas operacionais</h2>
        <span className="live-label">
          <i />
          Monitoramento ativo
        </span>
      </div>
      <div className="full-alerts">
        {alerts.map((a) => (
          <button className={`operational-alert ${a.tone}`} key={a.id} onClick={() => onOpen(a)}>
            <AlertTriangle size={20} />
            <div>
              <strong>{a.title}</strong>
              <p>{a.detail}</p>
            </div>
            <ArrowUpRight size={18} />
          </button>
        ))}
        {!alerts.length && (
          <Empty
            title="Nenhum alerta operacional"
            detail="Os voos e recursos desta data estão sem ocorrências."
          />
        )}
      </div>
    </section>
  );
}

export function HistoryView({ state }) {
  const [search, setSearch] = useState(''),
    [page, setPage] = useState(1);
  const filtered = state.audit.filter((a) =>
    normalize(a.detail + ' ' + a.userName + ' ' + a.action).includes(normalize(search)),
  );
  const safePage = Math.min(page, Math.max(1, Math.ceil(filtered.length / 15)));
  return (
    <section className="section">
      <div className="view-toolbar">
        <h2>Registro de atividades</h2>
        <span className="muted">Últimos 300 eventos</span>
      </div>
      <div className="filters">
        <SearchInput
          value={search}
          onChange={(v) => {
            setSearch(v);
            setPage(1);
          }}
          placeholder="Buscar ação, usuário ou registro"
        />
      </div>
      <div className="table-scroll">
        <table className="data-table history-table">
          <thead>
            <tr>
              <th>Data e hora</th>
              <th>Responsável</th>
              <th>Ação</th>
              <th>Registro</th>
            </tr>
          </thead>
          <tbody>
            {filtered.slice((safePage - 1) * 15, safePage * 15).map((a) => (
              <tr key={a.id}>
                <td className="mono">{dateTime(a.createdAt)}</td>
                <td>{a.userName}</td>
                <td>
                  <span className="action-label">{a.action}</span>
                </td>
                <td>{a.detail}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!filtered.length && <Empty />}
      </div>
      <Pagination page={safePage} setPage={setPage} total={filtered.length} size={15} />
    </section>
  );
}

export function ReportsView({ state, index, date, user, onExport }) {
  const flights = state.flights.filter((f) => localDay(f.scheduled) === date),
    completed = flights.filter((f) => f.status === 'landed').length;
  const reports = [
    {
      table: 'flights',
      title: 'Movimentação de voos',
      description: 'Rotas, horários, portões e situação operacional.',
      icon: Plane,
      total: flights.length,
    },
    {
      table: 'airlines',
      title: 'Companhias aéreas',
      description: 'Companhias, códigos IATA e contatos operacionais.',
      icon: Building2,
      total: state.airlines.length,
    },
    ...(can(user, 'passengers')
      ? [
          {
            table: 'passengers',
            title: 'Cadastro de passageiros',
            description: 'Nomes, contatos e nacionalidades. Documentos omitidos.',
            icon: Ticket,
            total: state.passengers.length,
          },
        ]
      : []),
    {
      table: 'audit',
      title: 'Histórico de atividades',
      description: 'Ações registradas, responsáveis e alterações.',
      icon: Clock3,
      total: state.audit.length,
    },
  ];
  return (
    <>
      <div className="inline-metrics">
        <div>
          <CheckCircle2 size={20} />
          <strong>{flights.length ? Math.round((completed / flights.length) * 100) : 0}%</strong>
          <span>Operações concluídas</span>
        </div>
        <div>
          <Clock3 size={20} />
          <strong>{flights.filter((f) => f.status === 'delayed').length}</strong>
          <span>Voos com atraso</span>
        </div>
        <div>
          <Ticket size={20} />
          <strong>
            {
              state.reservations.filter(
                (r) =>
                  r.status === 'checked_in' &&
                  localDay(index.flights[r.flightId].scheduled) === date,
              ).length
            }
          </strong>
          <span>Check-ins da data</span>
        </div>
      </div>
      <section className="section">
        <div className="view-toolbar">
          <h2>Relatórios operacionais</h2>
          <span className="muted">Formato CSV · compatível com Excel</span>
        </div>
        <div className="reports-list">
          {reports.map((r) => (
            <div className="report-row" key={r.table}>
              <span className="report-icon">
                <r.icon size={22} />
              </span>
              <div>
                <h3>{r.title}</h3>
                <p>{r.description}</p>
              </div>
              <span>{r.total} registros</span>
              <button className="button secondary" onClick={() => onExport(r.table)}>
                <Download size={16} />
                Exportar CSV
              </button>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

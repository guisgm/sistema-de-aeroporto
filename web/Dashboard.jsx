import React from 'react';
import {
  Plane,
  PlaneTakeoff,
  Clock3,
  Ban,
  ArrowUpRight,
  ArrowRight,
  AlertTriangle,
  CheckCircle2,
  MapPin,
  Activity,
} from 'lucide-react';
import { AirlineMark, Badge, FlightTable, IconButton, Empty } from './components';
import { localDay, time, effective, activeFlight } from './model';

export default function Dashboard({ state, index, date, alerts, onFlight, navigate, tab, setTab }) {
  const flights = state.flights.filter((f) => localDay(f.scheduled) === date);
  const boarding = flights.filter((f) => f.status === 'boarding').length;
  const delayed = flights.filter((f) => f.status === 'delayed').length;
  const cancelled = flights.filter((f) => f.status === 'cancelled').length;
  const list = flights
    .filter((f) => f.type === tab)
    .sort(
      (a, b) => Number(!activeFlight(a)) - Number(!activeFlight(b)) || effective(a) - effective(b),
    );
  const gateIds = new Set(flights.filter((f) => activeFlight(f)).map((f) => f.gateId));
  const totals = Array.from({ length: 12 }, (_, i) => ({
    hour: 6 + i * 1.5,
    count: flights.filter((f) => {
      const h = Number(time(f.scheduled).slice(0, 2)) + Number(time(f.scheduled).slice(3)) / 60;
      return h >= 6 + i * 1.5 && h < 7.5 + i * 1.5;
    }).length,
  }));
  const max = Math.max(...totals.map((t) => t.count), 1);
  return (
    <>
      <div className="overview-kpis">
        {[
          {
            label: 'Voos do dia',
            value: flights.length,
            icon: Plane,
            context: `${flights.filter((f) => f.type === 'departure').length} partidas · ${flights.filter((f) => f.type === 'arrival').length} chegadas`,
            tone: 'neutral',
          },
          {
            label: 'Em embarque',
            value: boarding,
            icon: PlaneTakeoff,
            context: 'Operações em andamento',
            tone: 'positive',
          },
          {
            label: 'Voos atrasados',
            value: delayed,
            icon: Clock3,
            context: delayed ? 'Acompanhamento necessário' : 'Operação dentro do previsto',
            tone: 'warning',
          },
          {
            label: 'Voos cancelados',
            value: cancelled,
            icon: Ban,
            context: cancelled ? 'Reservas encerradas' : 'Nenhum cancelamento',
            tone: 'danger',
          },
        ].map((k) => (
          <section className={`kpi ${k.tone}`} key={k.label}>
            <div className="kpi-top">
              <span>{k.label}</span>
              <k.icon size={19} />
            </div>
            <div className="kpi-value">
              {String(k.value).padStart(2, '0')}
              <span className={`kpi-dot ${k.tone}`} />
            </div>
            <p>{k.context}</p>
          </section>
        ))}
      </div>
      <div className="dashboard-columns">
        <div className="dashboard-main">
          <section className="section flight-board">
            <div className="section-heading">
              <div>
                <h2>Movimentação de voos</h2>
                <span>Programação diária · Horários de Brasília</span>
              </div>
              <button className="text-link" onClick={() => navigate('flights')}>
                Ver todos
                <ArrowUpRight size={15} />
              </button>
            </div>
            <div className="board-tabs">
              <div className="tabs">
                <button
                  className={tab === 'departure' ? 'active' : ''}
                  onClick={() => setTab('departure')}
                >
                  <PlaneTakeoff size={16} />
                  Partidas<span>{flights.filter((f) => f.type === 'departure').length}</span>
                </button>
                <button
                  className={tab === 'arrival' ? 'active' : ''}
                  onClick={() => setTab('arrival')}
                >
                  <Plane size={16} />
                  Chegadas<span>{flights.filter((f) => f.type === 'arrival').length}</span>
                </button>
              </div>
              <span className="live-label">
                <i />
                Atualizado
              </span>
            </div>
            <FlightTable flights={list.slice(0, 6)} index={index} onOpen={onFlight} compact />
            <footer className="board-footer">
              <span>
                {Math.min(list.length, 6)} de {list.length}{' '}
                {tab === 'departure' ? 'partidas' : 'chegadas'}
              </span>
              <button className="text-link" onClick={() => navigate('flights')}>
                Abrir painel de voos
                <ArrowRight size={15} />
              </button>
            </footer>
          </section>
          <section className="section gate-overview">
            <div className="section-heading">
              <div>
                <h2>Terminais e portões</h2>
                <span>{gateIds.size} portões alocados na programação</span>
              </div>
              <button className="text-link" onClick={() => navigate('gates')}>
                Gerenciar
                <ArrowUpRight size={15} />
              </button>
            </div>
            <div className="terminal-overview">
              {state.terminals.map((t) => {
                const gates = state.gates.filter((g) => g.terminalId === t.id);
                return (
                  <div className="terminal-group" key={t.id}>
                    <div>
                      <strong>{t.name}</strong>
                      <small>{t.kind === 'Domestico' ? 'Doméstico' : t.kind}</small>
                    </div>
                    <div className="mini-gates">
                      {gates.map((g) => (
                        <button
                          key={g.id}
                          onClick={() => navigate('gates')}
                          className={`mini-gate ${g.status === 'blocked' ? 'blocked' : gateIds.has(g.id) ? 'allocated' : 'free'}`}
                          title={`Portão ${g.code}: ${g.status === 'blocked' ? 'bloqueado' : gateIds.has(g.id) ? 'alocado' : 'livre'}`}
                        >
                          <MapPin size={13} />
                          {g.code}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
            <div className="gate-legend">
              <span>
                <i className="allocated" />
                Alocado
              </span>
              <span>
                <i className="free" />
                Livre
              </span>
              <span>
                <i className="blocked" />
                Bloqueado
              </span>
            </div>
          </section>
        </div>
        <aside className="dashboard-aside">
          <section className="section alerts-section">
            <div className="section-heading">
              <h2>Alertas operacionais</h2>
              <span className="count-pill">{alerts.length}</span>
            </div>
            <div className="alerts-list">
              {alerts.slice(0, 3).map((a) => (
                <button
                  className={`operational-alert ${a.tone}`}
                  key={a.id}
                  onClick={() =>
                    a.entity === 'flights'
                      ? onFlight(a.row)
                      : navigate(a.entity === 'aircraft' ? 'aircraft' : 'gates')
                  }
                >
                  <AlertTriangle size={17} />
                  <div>
                    <strong>{a.title}</strong>
                    <p>{a.detail}</p>
                  </div>
                  <ArrowUpRight size={13} />
                </button>
              ))}
              {!alerts.length && (
                <div className="all-clear">
                  <CheckCircle2 size={22} />
                  <strong>Operação sem alertas</strong>
                </div>
              )}
            </div>
            <button className="alert-footer text-link" onClick={() => navigate('alerts')}>
              Todos os alertas
              <ArrowRight size={14} />
            </button>
          </section>
          <section className="section traffic-section">
            <div className="section-heading">
              <div>
                <h2>Fluxo de operações</h2>
                <span>Distribuição de voos por horário</span>
              </div>
              <Activity size={17} />
            </div>
            <div className="traffic-total">
              <strong>{flights.length}</strong>
              <span>movimentos previstos</span>
            </div>
            <div
              className="traffic-chart"
              role="img"
              aria-label={`Distribuição de ${flights.length} voos entre 6 e 24 horas`}
            >
              {totals.map((t, i) => (
                <div
                  className="chart-column"
                  key={i}
                  title={`${Math.floor(t.hour)}h: ${t.count} voos`}
                >
                  <div
                    className={`chart-bar ${i === 4 || i === 5 ? 'highlight' : ''}`}
                    style={{ height: `${(t.count / max) * 100}%` }}
                  />
                </div>
              ))}
            </div>
            <div className="chart-labels">
              <span>06h</span>
              <span>12h</span>
              <span>18h</span>
              <span>24h</span>
            </div>
          </section>
          <section className="airport-image">
            <img src="/airport.jpg" alt="Aeronave no pátio de um aeroporto" />
            <div>
              <span>
                <i />
                BASE OPERACIONAL
              </span>
              <strong>São Paulo · Guarulhos</strong>
              <p>
                GRU / SBGR
                <ArrowUpRight size={17} />
              </p>
            </div>
          </section>
        </aside>
      </div>
      <div className="system-footer">
        <span>
          <i />
          Todos os serviços disponíveis
        </span>
        <span>Horários de Brasília · UTC−03:00</span>
      </div>
    </>
  );
}

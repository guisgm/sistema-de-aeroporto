import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  LayoutDashboard,
  Plane,
  Users,
  Building2,
  Warehouse,
  MapPin,
  ChartNoAxesCombined,
  History,
  Search,
  Bell,
  ChevronDown,
  ChevronRight,
  LogOut,
  Menu,
  X,
  Plus,
  RefreshCw,
  CalendarDays,
  AlertTriangle,
  CheckCircle2,
  ShieldCheck,
  ArrowUpRight,
  ArrowRight,
  Eye,
  EyeOff,
} from 'lucide-react';
import { api, setCsrf, download } from './api';
import { Avatar, IconButton, Loading, Modal, Empty, FlightTable } from './components';
import {
  indexState,
  localDay,
  dateLabel,
  dateTime,
  normalize,
  roles,
  can,
  operationalAlerts,
  time,
} from './model';
import { EntityForm, FlightDetail, ReservationForm, BoardingPass, ConfirmDialog } from './forms';
import Dashboard from './Dashboard';
import {
  FlightsView,
  PassengersView,
  AirlinesView,
  AircraftView,
  GatesView,
  AlertsView,
  HistoryView,
  ReportsView,
} from './Views';

const pages = [
  {
    id: 'dashboard',
    label: 'Visão geral',
    icon: LayoutDashboard,
    title: 'Visão geral da operação',
    description: 'Aeroporto Internacional de São Paulo · Guarulhos',
  },
  {
    id: 'flights',
    label: 'Voos',
    icon: Plane,
    title: 'Gestão de voos',
    description: 'Programação, rotas e acompanhamento operacional.',
  },
  {
    id: 'passengers',
    label: 'Passageiros',
    icon: Users,
    title: 'Passageiros e reservas',
    description: 'Atendimento, assentos e check-in em um único fluxo.',
  },
  {
    id: 'airlines',
    label: 'Companhias aéreas',
    icon: Building2,
    title: 'Companhias aéreas',
    description: 'Parceiros operacionais, frotas e desempenho.',
  },
  {
    id: 'aircraft',
    label: 'Aeronaves',
    icon: Warehouse,
    title: 'Gestão de aeronaves',
    description: 'Disponibilidade da frota e controle de manutenção.',
  },
  {
    id: 'gates',
    label: 'Terminais e portões',
    icon: MapPin,
    title: 'Terminais e portões',
    description: 'Alocação de recursos e programação por portão.',
  },
  {
    id: 'reports',
    label: 'Relatórios',
    icon: ChartNoAxesCombined,
    title: 'Relatórios',
    description: 'Dados operacionais para análise e acompanhamento.',
  },
  {
    id: 'history',
    label: 'Histórico',
    icon: History,
    title: 'Histórico de alterações',
    description: 'Rastreabilidade das ações e decisões operacionais.',
  },
  {
    id: 'alerts',
    label: 'Alertas operacionais',
    icon: Bell,
    title: 'Alertas operacionais',
    description: 'Ocorrências que precisam da atenção da equipe.',
  },
];
const route = () =>
  pages.some((p) => p.id === location.hash.slice(1)) ? location.hash.slice(1) : 'dashboard';

function Brand({ dark = false }) {
  return (
    <div className={`brand ${dark ? 'dark' : ''}`}>
      <span className="brand-symbol">
        <Plane size={22} />
      </span>
      <div>
        <strong>
          Aero<span>Hub</span>
        </strong>
        <small>AIRPORT OPERATIONS</small>
      </div>
    </div>
  );
}

function Login({ onLogin }) {
  const [email, setEmail] = useState('admin@aerohub.local'),
    [password, setPassword] = useState(''),
    [error, setError] = useState(''),
    [busy, setBusy] = useState(false),
    [show, setShow] = useState(false);
  async function submit(e, credentials) {
    e?.preventDefault();
    setBusy(true);
    setError('');
    try {
      const session = await api('/login', {
        method: 'POST',
        body: credentials || { email, password },
      });
      setCsrf(session.csrf);
      onLogin(session.user);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="login-screen">
      <div className="login-photo">
        <img src="/airport.jpg" alt="Pátio de aeroporto com aeronave" />
      </div>
      <div className="login-shade" />
      <div className="login-top">
        <Brand />
        <span className="environment-light">AMBIENTE DE SIMULAÇÃO</span>
      </div>
      <main className="login-content">
        <div className="login-form">
          <span className="login-eyebrow">
            <ShieldCheck size={17} />
            ACESSO AO CENTRO DE OPERAÇÕES
          </span>
          <h1>Bem-vindo ao AeroHub.</h1>
          <p>Entre com sua conta operacional.</p>
          <form onSubmit={submit}>
            <label className="field">
              <span>E-mail</span>
              <input
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <label className="field">
              <span>Senha</span>
              <div className="password-input">
                <input
                  type={show ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <IconButton
                  icon={show ? EyeOff : Eye}
                  label={show ? 'Ocultar senha' : 'Mostrar senha'}
                  onClick={() => setShow(!show)}
                  type="button"
                />
              </div>
            </label>
            {error && (
              <p className="form-error" role="alert">
                {error}
              </p>
            )}
            <button className="button primary login-submit" disabled={busy}>
              {busy ? 'Entrando…' : 'Entrar no sistema'}
              <ArrowRight size={18} />
            </button>
          </form>
          <div className="demo-access">
            <span>Acessos de simulação</span>
            <div>
              {[
                ['admin', 'Administrador'],
                ['operador', 'Operador'],
                ['atendente', 'Atendente'],
              ].map(([key, label]) => (
                <button
                  key={key}
                  disabled={busy}
                  onClick={() =>
                    submit(null, { email: `${key}@aerohub.local`, password: 'AeroHub@2026!' })
                  }
                >
                  {label}
                  <ArrowUpRight size={13} />
                </button>
              ))}
            </div>
          </div>
          <footer>
            <span>
              <i />
              Conexão local · Dados fictícios
            </span>
            <span>GRU / SBGR</span>
          </footer>
        </div>
      </main>
    </div>
  );
}

export default function App() {
  const [user, setUser] = useState(null),
    [sessionLoading, setSessionLoading] = useState(true),
    [state, setState] = useState(null),
    [loading, setLoading] = useState(false),
    [error, setError] = useState(''),
    [page, setPage] = useState(route),
    [date, setDate] = useState(''),
    [tab, setTab] = useState('departure'),
    [mobile, setMobile] = useState(false),
    [search, setSearch] = useState(''),
    [searchOpen, setSearchOpen] = useState(false),
    [profile, setProfile] = useState(false),
    [modal, setModal] = useState(null),
    [toast, setToast] = useState(null),
    [selectedPassenger, setSelectedPassenger] = useState(null),
    [clock, setClock] = useState(new Date());
  const toastTimer = useRef(),
    searchRef = useRef(),
    profileRef = useRef(),
    requestId = useRef(0);
  const index = useMemo(() => (state ? indexState(state) : {}), [state]);
  const alerts = useMemo(() => (state ? operationalAlerts(state, date) : []), [state, date]);
  const current = pages.find((p) => p.id === page);
  const notify = useCallback((message, tone = 'success') => {
    clearTimeout(toastTimer.current);
    setToast({ message, tone });
    toastTimer.current = setTimeout(() => setToast(null), 5500);
  }, []);
  const refresh = useCallback(
    async (silent = false) => {
      const id = ++requestId.current;
      if (!silent) setLoading(true);
      setError('');
      try {
        const data = await api('/state');
        if (id === requestId.current) {
          setState(data);
          setDate((d) => d || data.today);
        }
      } catch (e) {
        if (id === requestId.current) {
          if (e.status === 401) {
            setUser(null);
            setState(null);
            setModal(null);
          } else if (silent) notify(e.message, 'error');
          else setError(e.message);
        }
      } finally {
        if (id === requestId.current) setLoading(false);
      }
    },
    [notify],
  );
  useEffect(() => {
    api('/session')
      .then((s) => {
        setCsrf(s.csrf);
        setUser(s.user);
      })
      .catch(() => {})
      .finally(() => setSessionLoading(false));
  }, []);
  useEffect(() => {
    if (user) {
      refresh();
      const timer = setInterval(() => refresh(true), 60000);
      return () => clearInterval(timer);
    }
  }, [user, refresh]);
  useEffect(() => {
    const timer = setInterval(() => setClock(new Date()), 30000);
    const hash = () => {
      setPage(route());
      setMobile(false);
      setSelectedPassenger(null);
    };
    window.addEventListener('hashchange', hash);
    return () => {
      clearInterval(timer);
      window.removeEventListener('hashchange', hash);
      clearTimeout(toastTimer.current);
    };
  }, []);
  useEffect(() => {
    const outside = (e) => {
      if (searchRef.current && !searchRef.current.contains(e.target)) setSearchOpen(false);
      if (profileRef.current && !profileRef.current.contains(e.target)) setProfile(false);
    };
    document.addEventListener('mousedown', outside);
    return () => document.removeEventListener('mousedown', outside);
  }, []);
  const navigate = (id) => {
    setPage(id);
    location.hash = id;
    setMobile(false);
    setSearchOpen(false);
    setProfile(false);
  };
  const edit = (entity, row) => setModal({ kind: 'form', entity, row });
  const create = (entity) => setModal({ kind: 'form', entity });
  const openFlight = (row) => setModal({ kind: 'flight', row });
  const reserve = (flight, passenger) => setModal({ kind: 'reservation', flight, passenger });
  async function save(entity, body, id) {
    await api(`/${entity}${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body });
    await refresh(true);
    notify(
      entity === 'reservations'
        ? 'Reserva confirmada.'
        : id
          ? 'Alterações salvas.'
          : 'Registro cadastrado.',
    );
  }
  async function exportData(entity) {
    try {
      await download(entity, date);
      notify('Relatório exportado.');
    } catch (e) {
      notify(e.message, 'error');
    }
  }
  async function checkin(row) {
    try {
      const updated = await api(`/reservations/${row.id}/checkin`, {
        method: 'POST',
        body: { version: row.version },
      });
      await refresh(true);
      setModal({ kind: 'boarding', row: updated });
      notify('Check-in realizado com sucesso.');
    } catch (e) {
      notify(e.message, 'error');
    }
  }
  function cancel(row) {
    setModal({
      kind: 'confirm',
      title: 'Cancelar reserva?',
      detail: `A reserva ${row.locator} será cancelada e o assento ${row.seat} ficará disponível. Esta ação será registrada.`,
      confirm: async () => {
        await api(`/reservations/${row.id}/cancel`, {
          method: 'POST',
          body: { version: row.version },
        });
        await refresh(true);
        notify('Reserva cancelada.');
      },
    });
  }
  async function logout() {
    try {
      await api('/logout', { method: 'POST' });
      setUser(null);
      setState(null);
      setModal(null);
      setProfile(false);
      setCsrf('');
    } catch (e) {
      notify(e.message, 'error');
    }
  }
  const searchResults = useMemo(() => {
    if (!state || search.trim().length < 2) return [];
    const term = normalize(search);
    return [
      ...state.flights
        .filter((f) => normalize(f.number + ' ' + f.destination + ' ' + f.origin).includes(term))
        .slice(0, 4)
        .map((f) => ({
          type: 'flights',
          row: f,
          title: f.number,
          detail: `${f.originCode} → ${f.destinationCode} · ${dateTime(f.scheduled)}`,
          icon: Plane,
        })),
      ...state.passengers
        .filter((p) => normalize(p.name + ' ' + p.document).includes(term))
        .slice(0, 3)
        .map((p) => ({
          type: 'passengers',
          row: p,
          title: p.name,
          detail: 'Passageiro',
          icon: Users,
        })),
      ...state.airlines
        .filter((a) => normalize(a.name + ' ' + a.code).includes(term))
        .slice(0, 3)
        .map((a) => ({
          type: 'airlines',
          row: a,
          title: a.name,
          detail: `Companhia · ${a.code}`,
          icon: Building2,
        })),
    ];
  }, [state, search]);
  if (sessionLoading) return <Loading />;
  if (!user) return <Login onLogin={setUser} />;
  function openSearch(result) {
    setSearchOpen(false);
    setSearch('');
    if (result.type === 'flights') openFlight(result.row);
    else if (result.type === 'passengers') {
      navigate('passengers');
      setSelectedPassenger(result.row);
    } else navigate('airlines');
  }
  return (
    <div className="app-shell">
      {mobile && <div className="sidebar-backdrop" onClick={() => setMobile(false)} />}
      <aside className={`sidebar ${mobile ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <Brand />
          <IconButton
            icon={X}
            label="Fechar menu"
            className="mobile-close icon-button"
            onClick={() => setMobile(false)}
          />
        </div>
        <div className="airport-switch">
          <span className="airport-code">GRU</span>
          <div>
            <strong>Guarulhos</strong>
            <small>São Paulo, Brasil</small>
          </div>
          <MapPin size={15} />
        </div>
        <span className="nav-section-label">OPERAÇÕES</span>
        <nav>
          {pages.slice(0, 6).map((p) => (
            <button
              key={p.id}
              className={page === p.id ? 'active' : ''}
              onClick={() => navigate(p.id)}
            >
              <p.icon size={18} />
              <span>{p.label}</span>
              {p.id === 'flights' && state && (
                <span className="nav-count">
                  {state.flights.filter((f) => localDay(f.scheduled) === date).length}
                </span>
              )}
            </button>
          ))}
        </nav>
        <span className="nav-section-label secondary-nav-label">CONTROLE</span>
        <nav>
          {pages.slice(6).map((p) => (
            <button
              key={p.id}
              className={page === p.id ? 'active' : ''}
              onClick={() => navigate(p.id)}
            >
              <p.icon size={18} />
              <span>{p.label}</span>
              {p.id === 'alerts' && alerts.length > 0 && (
                <span className="alert-count">{alerts.length}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="environment">
            <i />
            Ambiente de simulação
          </div>
          <button className="sidebar-user" onClick={() => setProfile((v) => !v)}>
            <Avatar name={user.name} />
            <div>
              <strong>{user.name}</strong>
              <small>{roles[user.role]}</small>
            </div>
            <ChevronRight size={16} />
          </button>
          <span className="sidebar-version">AeroHub Operations · v1.0</span>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <IconButton
              icon={Menu}
              label="Abrir menu"
              className="mobile-menu icon-button"
              onClick={() => setMobile(true)}
            />
            <span>Centro de operações</span>
            <ChevronRight size={13} />
            <strong>{current.label}</strong>
          </div>
          <div className="topbar-right">
            <div className="global-search" ref={searchRef}>
              <Search size={16} />
              <input
                aria-label="Busca global"
                placeholder="Buscar no aeroporto…"
                value={search}
                onFocus={() => setSearchOpen(true)}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setSearchOpen(true);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Escape') setSearchOpen(false);
                }}
              />
              {searchOpen && search.length >= 2 && (
                <div className="search-results">
                  {searchResults.map((r) => (
                    <button key={r.type + r.row.id} onClick={() => openSearch(r)}>
                      <r.icon size={17} />
                      <div>
                        <strong>{r.title}</strong>
                        <small>{r.detail}</small>
                      </div>
                      <ArrowUpRight size={13} />
                    </button>
                  ))}
                  {!searchResults.length && (
                    <div className="search-empty">Nenhum resultado encontrado.</div>
                  )}
                </div>
              )}
            </div>
            <div className="topbar-divider" />
            <button
              className="notification-button"
              aria-label={`${alerts.length} alertas operacionais`}
              title="Alertas operacionais"
              onClick={() => navigate('alerts')}
            >
              <Bell size={19} />
              {alerts.length > 0 && <i />}
            </button>
            <div className="profile-control" ref={profileRef}>
              <button
                className="profile-button"
                aria-label="Menu da conta"
                aria-expanded={profile}
                onClick={() => setProfile((v) => !v)}
              >
                <Avatar name={user.name} small />
                <ChevronDown size={13} />
              </button>
              {profile && (
                <div className="profile-dropdown">
                  <strong>{user.name}</strong>
                  <small>
                    {roles[user.role]} · {user.email}
                  </small>
                  <div className="profile-permission">
                    <ShieldCheck size={15} />
                    {user.role === 'admin'
                      ? 'Acesso administrativo'
                      : user.role === 'operator'
                        ? 'Operações de voo e recursos'
                        : 'Passageiros, reservas e check-in'}
                  </div>
                  <button onClick={logout}>
                    <LogOut size={16} />
                    Sair do sistema
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        <main className="main-content">
          <div className="page-heading">
            <div>
              <div className="page-eyebrow">
                <span className="live-dot" />
                CENTRO DE CONTROLE OPERACIONAL
              </div>
              <h1>{current.title}</h1>
              <p>{current.description}</p>
            </div>
            <div className="page-actions">
              <label className="date-control">
                <CalendarDays size={16} />
                <input
                  aria-label="Data operacional"
                  type="date"
                  value={date}
                  onChange={(e) => e.target.value && setDate(e.target.value)}
                />
              </label>
              <IconButton
                icon={RefreshCw}
                label="Atualizar dados"
                onClick={() => refresh()}
                disabled={loading}
              />
              {page === 'dashboard' && can(user, 'flights') && (
                <button
                  className="button primary"
                  onClick={() => create('flights')}
                  disabled={!state}
                >
                  <Plus size={17} />
                  Novo voo
                </button>
              )}
            </div>
          </div>
          {loading && !state ? (
            <Loading />
          ) : error ? (
            <div className="error-state">
              <AlertTriangle size={28} />
              <h2>Não foi possível carregar os dados</h2>
              <p>{error}</p>
              <button className="button primary" onClick={() => refresh()}>
                <RefreshCw size={16} />
                Tentar novamente
              </button>
            </div>
          ) : (
            state && (
              <>
                <div className="operation-strip">
                  <span>
                    <i />
                    Operação ativa <b>GRU / SBGR</b>
                  </span>
                  <span>
                    {new Intl.DateTimeFormat('pt-BR', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                      timeZone: 'America/Sao_Paulo',
                    }).format(new Date(`${date}T12:00:00-03:00`))}
                    <em>·</em>
                    <b>{time(clock)}</b>
                    <span className="timezone"> BRT</span>
                  </span>
                </div>
                {page === 'dashboard' && (
                  <Dashboard
                    {...{ state, index, date, alerts, navigate, tab, setTab }}
                    onFlight={openFlight}
                  />
                )}
                {page === 'flights' && (
                  <FlightsView
                    key={date}
                    {...{ state, index, user, date }}
                    onFlight={openFlight}
                    onCreate={create}
                    onExport={exportData}
                  />
                )}
                {page === 'passengers' && (
                  <PassengersView
                    key={selectedPassenger?.id || 'passengers'}
                    {...{ state, index, user }}
                    selected={selectedPassenger}
                    onEdit={edit}
                    onCreate={create}
                    onReservation={reserve}
                    onCheckin={checkin}
                    onBoarding={(row) => setModal({ kind: 'boarding', row })}
                    onCancel={cancel}
                  />
                )}
                {page === 'airlines' && (
                  <AirlinesView
                    {...{ state, index, user }}
                    onEdit={edit}
                    onCreate={create}
                    onHistory={(row) => setModal({ kind: 'airline-history', row })}
                  />
                )}
                {page === 'aircraft' && (
                  <AircraftView {...{ state, index, user }} onEdit={edit} onCreate={create} />
                )}
                {page === 'gates' && (
                  <GatesView
                    {...{ state, index, user, date }}
                    onEdit={edit}
                    onCreate={create}
                    onFlight={openFlight}
                  />
                )}
                {page === 'reports' && (
                  <ReportsView {...{ state, index, date, user }} onExport={exportData} />
                )}
                {page === 'history' && <HistoryView state={state} />}
                {page === 'alerts' && (
                  <AlertsView
                    alerts={alerts}
                    onOpen={(a) =>
                      a.entity === 'flights'
                        ? openFlight(a.row)
                        : can(user, a.entity)
                          ? edit(a.entity, a.row)
                          : navigate(a.entity === 'aircraft' ? 'aircraft' : 'gates')
                    }
                  />
                )}
              </>
            )
          )}
        </main>
      </div>
      {modal && state && (
        <>
          {modal.kind === 'form' && (
            <EntityForm
              key={modal.entity + (modal.row?.id || 'new')}
              {...{ state, index, user }}
              entity={modal.entity}
              row={modal.row}
              onSave={save}
              onClose={() => setModal(null)}
            />
          )}
          {modal.kind === 'flight' && (
            <FlightDetail
              flight={index.flights[modal.row.id] || modal.row}
              {...{ state, index, user }}
              onClose={() => setModal(null)}
              onEdit={(row) => edit('flights', row)}
              onReservation={reserve}
            />
          )}
          {modal.kind === 'reservation' && (
            <ReservationForm
              {...{ state, index }}
              flight={modal.flight}
              passenger={modal.passenger}
              onSave={save}
              onClose={() => setModal(null)}
            />
          )}
          {modal.kind === 'boarding' && (
            <BoardingPass
              reservation={state.reservations.find((r) => r.id === modal.row.id) || modal.row}
              index={index}
              onClose={() => setModal(null)}
            />
          )}
          {modal.kind === 'confirm' && (
            <ConfirmDialog
              title={modal.title}
              detail={modal.detail}
              onConfirm={modal.confirm}
              onClose={() => setModal(null)}
            />
          )}
          {modal.kind === 'airline-history' && (
            <Modal
              title={`Histórico · ${modal.row.name}`}
              subtitle="Voos registrados no período de simulação."
              wide
              onClose={() => setModal(null)}
            >
              <div className="airline-history-scroll">
                <FlightTable
                  showDate
                  flights={state.flights
                    .filter((f) => f.airlineId === modal.row.id)
                    .sort((a, b) => new Date(b.scheduled) - new Date(a.scheduled))}
                  index={index}
                  onOpen={openFlight}
                />
              </div>
            </Modal>
          )}
        </>
      )}
      {toast && (
        <div role={toast.tone === 'error' ? 'alert' : 'status'} className={`toast ${toast.tone}`}>
          {toast.tone === 'error' ? <AlertTriangle size={19} /> : <CheckCircle2 size={19} />}
          <span>{toast.message}</span>
          <IconButton icon={X} label="Fechar mensagem" onClick={() => setToast(null)} />
        </div>
      )}
    </div>
  );
}

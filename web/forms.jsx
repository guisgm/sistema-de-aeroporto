import React, { useState } from 'react';
import { Save, AlertTriangle, Check, Plane, X, Printer, Ticket, ArrowRight } from 'lucide-react';
import { Modal, Field, Badge, AirlineMark, IconButton } from './components';
import {
  fromInputTime,
  inputTime,
  time,
  dateLabel,
  can,
  seatName,
  activeFlight,
  effective,
  statusLabels,
  dateTime,
} from './model';

const entityTitles = {
  flights: 'voo',
  passengers: 'passageiro',
  airlines: 'companhia aérea',
  aircraft: 'aeronave',
  gates: 'portão',
  terminals: 'terminal',
};
export function EntityForm({ entity, row, state, index, user, onSave, onClose }) {
  const defaultFlight = {
    number: '',
    airlineId: state.airlines[0]?.id || '',
    aircraftId: '',
    origin: 'São Paulo',
    originCode: 'GRU',
    destination: '',
    destinationCode: '',
    type: 'departure',
    scheduled: `${state.today}T18:00`,
    actual: '',
    duration: 90,
    gateId: state.gates.find((g) => g.status === 'available')?.id || '',
    status: 'scheduled',
    notes: '',
  };
  const defaults = {
    flights: defaultFlight,
    passengers: {
      name: '',
      documentType: 'cpf',
      document: '',
      birthDate: '',
      email: '',
      phone: '',
      nationality: 'Brasileira',
    },
    airlines: { name: '', code: '', country: 'Brasil', contact: '', color: '#457b69' },
    aircraft: {
      registration: '',
      model: '',
      capacity: 180,
      airlineId: state.airlines[0]?.id || '',
      status: 'available',
      maintenanceDate: state.today,
      notes: '',
    },
    gates: { code: '', terminalId: state.terminals[0]?.id || '', status: 'available' },
    terminals: { name: '', kind: 'Domestico' },
  };
  const [form, setForm] = useState(
    row
      ? {
          ...row,
          ...(entity === 'flights'
            ? { scheduled: inputTime(row.scheduled), actual: inputTime(row.actual) }
            : {}),
        }
      : defaults[entity],
  );
  const [error, setError] = useState(''),
    [fields, setFields] = useState({}),
    [busy, setBusy] = useState(false),
    [critical, setCritical] = useState(false);
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  const input = (key, type = 'text', extra = {}) => (
    <input
      required
      value={form[key] ?? ''}
      type={type}
      onChange={(e) => set(key, type === 'number' ? Number(e.target.value) : e.target.value)}
      {...extra}
    />
  );
  const select = (key, options) => (
    <select required value={form[key] ?? ''} onChange={(e) => set(key, e.target.value)}>
      {options.map(([value, label]) => (
        <option key={value} value={value}>
          {label}
        </option>
      ))}
    </select>
  );
  const options = (rows) => rows.map((r) => [r.id, r.name || r.code || r.registration]);
  const field = (key, label, content, wide = false) => (
    <Field key={key} label={label} error={fields[key]} wide={wide}>
      {content || input(key)}
    </Field>
  );
  const isCritical =
    !!row &&
    ((entity === 'flights' && ['cancelled', 'maintenance'].includes(form.status)) ||
      (entity === 'aircraft' && form.status !== row.status) ||
      (entity === 'gates' && form.status === 'blocked' && row.status !== 'blocked'));
  async function submit(e) {
    e.preventDefault();
    setError('');
    setFields({});
    if (isCritical && !critical) {
      setCritical(true);
      return;
    }
    setBusy(true);
    try {
      const value = { ...form };
      if (entity === 'flights') {
        value.scheduled = fromInputTime(form.scheduled);
        value.actual = fromInputTime(form.actual);
      }
      await onSave(entity, value, row?.id);
      onClose();
    } catch (e) {
      setError(e.message);
      setFields(e.fields || {});
      setCritical(false);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={`${row ? 'Editar' : 'Cadastrar'} ${entityTitles[entity]}`}
      subtitle={
        row
          ? 'Alterações serão registradas no histórico operacional.'
          : 'Preencha os dados do novo registro.'
      }
      onClose={() => !busy && onClose()}
      wide={entity === 'flights'}
    >
      <form onSubmit={submit}>
        <div className="form-grid">
          {entity === 'flights' && (
            <>
              {field(
                'number',
                'Número do voo',
                input('number', 'text', { placeholder: 'LA 1234', maxLength: 8 }),
              )}
              {field(
                'type',
                'Operação',
                select('type', [
                  ['departure', 'Partida'],
                  ['arrival', 'Chegada'],
                ]),
              )}
              {field(
                'airlineId',
                'Companhia aérea',
                <select
                  value={form.airlineId}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, airlineId: e.target.value, aircraftId: '' }))
                  }
                >
                  {options(state.airlines).map(([v, l]) => (
                    <option key={v} value={v}>
                      {l}
                    </option>
                  ))}
                </select>,
              )}
              {field(
                'aircraftId',
                'Aeronave',
                <select
                  required
                  value={form.aircraftId}
                  onChange={(e) => set('aircraftId', e.target.value)}
                >
                  <option value="">Selecione a aeronave</option>
                  {state.aircraft
                    .filter((a) => a.airlineId === form.airlineId)
                    .map((a) => (
                      <option
                        key={a.id}
                        value={a.id}
                        disabled={a.status !== 'available' && a.id !== row?.aircraftId}
                      >
                        {a.registration} · {a.model}
                        {a.status !== 'available' ? ' · Indisponível' : ''}
                      </option>
                    ))}
                </select>,
              )}
              {field('origin', 'Cidade de origem')}
              {field(
                'originCode',
                'IATA da origem',
                input('originCode', 'text', { maxLength: 3, placeholder: 'GRU' }),
              )}
              {field('destination', 'Cidade de destino')}
              {field(
                'destinationCode',
                'IATA do destino',
                input('destinationCode', 'text', { maxLength: 3, placeholder: 'REC' }),
              )}
              {field(
                'scheduled',
                'Horário previsto (Brasília)',
                input('scheduled', 'datetime-local'),
              )}
              {field(
                'actual',
                form.status === 'delayed' ? 'Nova previsão (Brasília)' : 'Horário real (Brasília)',
                input('actual', 'datetime-local', {
                  required: ['delayed', 'landed'].includes(form.status),
                }),
              )}
              {field(
                'duration',
                'Duração do voo (min)',
                input('duration', 'number', { min: 20, max: 1440 }),
              )}
              {field(
                'gateId',
                'Portão',
                select(
                  'gateId',
                  state.gates
                    .filter((g) => g.status === 'available' || g.id === row?.gateId)
                    .map((g) => [g.id, `${g.code} · ${index.terminals[g.terminalId].name}`]),
                ),
              )}
              {field(
                'status',
                'Status',
                select(
                  'status',
                  ['scheduled', 'boarding', 'delayed', 'cancelled', 'landed', 'maintenance'].map(
                    (s) => [s, statusLabels[s]],
                  ),
                ),
              )}
              {field(
                'notes',
                'Observações / Motivo',
                <textarea
                  value={form.notes}
                  onChange={(e) => set('notes', e.target.value)}
                  maxLength={500}
                  required={['cancelled', 'maintenance'].includes(form.status)}
                />,
                true,
              )}
            </>
          )}
          {entity === 'passengers' && (
            <>
              {field('name', 'Nome completo', input('name', 'text', { minLength: 3 }), true)}
              {field(
                'documentType',
                'Tipo de documento',
                select('documentType', [
                  ['cpf', 'CPF'],
                  ['passport', 'Passaporte'],
                ]),
              )}
              {field(
                'document',
                'Documento',
                input('document', 'text', {
                  placeholder: form.documentType === 'cpf' ? '000.000.000-00' : 'BR123456',
                }),
              )}
              {field(
                'birthDate',
                'Data de nascimento',
                input('birthDate', 'date', { max: state.today, min: '1900-01-01' }),
              )}
              {field('nationality', 'Nacionalidade')}
              {field('email', 'E-mail', input('email', 'email'))}
              {field(
                'phone',
                'Telefone com DDD',
                input('phone', 'tel', { placeholder: '(11) 99999-0000' }),
              )}
            </>
          )}
          {entity === 'airlines' && (
            <>
              {field('name', 'Nome da companhia', null, true)}
              {field('code', 'Código IATA', input('code', 'text', { maxLength: 3 }))}
              {field('country', 'País')}
              {field('contact', 'E-mail operacional', input('contact', 'email'))}
              {field('color', 'Identificação visual', input('color', 'color'))}
            </>
          )}
          {entity === 'aircraft' && (
            <>
              {field(
                'registration',
                'Matrícula',
                input('registration', 'text', { placeholder: 'PR-ABC' }),
              )}
              {field('model', 'Modelo', input('model', 'text', { placeholder: 'Airbus A320neo' }))}
              {field(
                'capacity',
                'Capacidade de passageiros',
                input('capacity', 'number', { min: 6, max: 600 }),
              )}
              {field('airlineId', 'Companhia', select('airlineId', options(state.airlines)))}
              {field(
                'status',
                'Status operacional',
                select(
                  'status',
                  ['available', 'maintenance', 'unavailable'].map((s) => [s, statusLabels[s]]),
                ),
              )}
              {field(
                'maintenanceDate',
                'Data da última manutenção',
                input('maintenanceDate', 'date'),
              )}
              {field(
                'notes',
                'Registro de manutenção',
                <textarea
                  value={form.notes}
                  maxLength={500}
                  onChange={(e) => set('notes', e.target.value)}
                />,
                true,
              )}
            </>
          )}
          {entity === 'gates' && (
            <>
              {field('code', 'Código do portão', input('code', 'text', { placeholder: 'A05' }))}
              {field('terminalId', 'Terminal', select('terminalId', options(state.terminals)))}
              {field(
                'status',
                'Disponibilidade',
                select('status', [
                  ['available', 'Disponível'],
                  ['blocked', 'Bloqueado'],
                ]),
              )}
            </>
          )}
          {entity === 'terminals' && (
            <>
              {field('name', 'Nome do terminal')}
              {field(
                'kind',
                'Tipo de operação',
                select('kind', [
                  ['Domestico', 'Doméstico'],
                  ['Internacional', 'Internacional'],
                  ['Misto', 'Misto'],
                ]),
              )}
            </>
          )}
        </div>
        {error && (
          <p className="form-error" role="alert">
            <AlertTriangle size={17} />
            {error}
          </p>
        )}
        {critical && (
          <div className="critical-confirm">
            <AlertTriangle size={19} />
            <div>
              <strong>Confirmar ação operacional</strong>
              <p>
                {entity === 'flights' && form.status === 'cancelled'
                  ? 'O cancelamento também encerrará todas as reservas deste voo.'
                  : 'Esta alteração afeta a disponibilidade operacional do aeroporto.'}
              </p>
            </div>
          </div>
        )}
        <footer className="modal-footer">
          <button type="button" className="button secondary" disabled={busy} onClick={onClose}>
            Voltar
          </button>
          <button className={`button ${critical ? 'danger' : 'primary'}`} disabled={busy}>
            <Save size={16} />
            {busy ? 'Salvando…' : critical ? 'Confirmar alteração' : 'Salvar registro'}
          </button>
        </footer>
      </form>
    </Modal>
  );
}

export function FlightDetail({ flight: f, state, index, user, onClose, onEdit, onReservation }) {
  const a = index.airlines[f.airlineId],
    aircraft = index.aircraft[f.aircraftId],
    gate = index.gates[f.gateId];
  const bookings = state.reservations.filter(
    (r) => r.flightId === f.id && r.status !== 'cancelled',
  );
  return (
    <Modal title={`Voo ${f.number}`} subtitle={a.name} onClose={onClose}>
      <div className="detail-body">
        <div className="flight-route-large">
          <div>
            <b>{f.originCode}</b>
            <span>{f.origin}</span>
          </div>
          <Plane size={25} />
          <div>
            <b>{f.destinationCode}</b>
            <span>{f.destination}</span>
          </div>
        </div>
        <div className="detail-status">
          <Badge status={f.status} />
          <span>
            {dateLabel(
              new Date(f.scheduled).toLocaleDateString('en-CA', { timeZone: 'America/Sao_Paulo' }),
            )}
          </span>
        </div>
        <dl className="detail-grid">
          <div>
            <dt>Horário previsto</dt>
            <dd>{time(f.scheduled)}</dd>
          </div>
          <div>
            <dt>{f.status === 'delayed' ? 'Nova previsão' : 'Horário real'}</dt>
            <dd>{time(f.actual)}</dd>
          </div>
          <div>
            <dt>Portão / Terminal</dt>
            <dd>
              {gate.code} · {index.terminals[gate.terminalId].name}
            </dd>
          </div>
          <div>
            <dt>Aeronave</dt>
            <dd>
              {aircraft.registration} · {aircraft.model}
            </dd>
          </div>
          <div>
            <dt>Passageiros / Capacidade</dt>
            <dd>
              {bookings.length} / {aircraft.capacity}
            </dd>
          </div>
          <div>
            <dt>Check-ins realizados</dt>
            <dd>{bookings.filter((r) => r.status === 'checked_in').length}</dd>
          </div>
        </dl>
        {f.notes && (
          <div className="note">
            <strong>Observações operacionais</strong>
            <p>{f.notes}</p>
          </div>
        )}
        <h3 className="small-heading">Últimas alterações</h3>
        {state.audit
          .filter((a) => a.entityId === f.id)
          .slice(0, 3)
          .map((a) => (
            <div className="detail-audit" key={a.id}>
              <strong>
                {a.action} · {a.userName}
              </strong>
              <small>{dateTime(a.createdAt)}</small>
              <p>{a.detail}</p>
            </div>
          ))}
        {!state.audit.some((a) => a.entityId === f.id) && (
          <p className="muted">Voo importado da base de simulação.</p>
        )}
      </div>
      <footer className="modal-footer">
        {can(user, 'reservations') && f.type === 'departure' && activeFlight(f) && (
          <button className="button secondary" onClick={() => onReservation(f)}>
            <Ticket size={16} />
            Nova reserva
          </button>
        )}
        {can(user, 'flights') && !['landed', 'cancelled'].includes(f.status) && (
          <button className="button primary" onClick={() => onEdit(f)}>
            <Save size={16} />
            Editar voo
          </button>
        )}
      </footer>
    </Modal>
  );
}

export function ReservationForm({ state, index, onSave, onClose, flight, passenger }) {
  const [form, setForm] = useState({
      passengerId: passenger?.id || '',
      flightId: flight?.id || '',
      seat: '',
    }),
    [error, setError] = useState(''),
    [busy, setBusy] = useState(false);
  const selected = index.flights[form.flightId],
    plane = selected ? index.aircraft[selected.aircraftId] : null;
  const occupied = new Set(
    state.reservations
      .filter((r) => r.flightId === form.flightId && r.status !== 'cancelled')
      .map((r) => r.seat),
  );
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await onSave('reservations', form);
      onClose();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title="Nova reserva"
      subtitle="Reserva de partida · Base GRU"
      onClose={() => !busy && onClose()}
    >
      <form onSubmit={submit}>
        <div className="form-grid">
          <Field label="Passageiro" wide>
            <select
              required
              value={form.passengerId}
              onChange={(e) => setForm((f) => ({ ...f, passengerId: e.target.value }))}
            >
              <option value="">Selecione o passageiro</option>
              {state.passengers.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} · {p.document.slice(-4)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Voo de partida" wide>
            <select
              required
              value={form.flightId}
              onChange={(e) => setForm((f) => ({ ...f, flightId: e.target.value, seat: '' }))}
            >
              <option value="">Selecione o voo</option>
              {state.flights
                .filter(
                  (f) => f.type === 'departure' && activeFlight(f) && effective(f) > Date.now(),
                )
                .sort((a, b) => effective(a) - effective(b))
                .map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.number} · {f.destination} · {dateTime(f.scheduled)}
                  </option>
                ))}
            </select>
          </Field>
        </div>
        {selected && (
          <div className="seat-section">
            <div className="seat-heading">
              <strong>{plane.model}</strong>
              <span>
                {occupied.size} / {plane.capacity} reservados
              </span>
            </div>
            <div className="seat-legend">
              <span>
                <i />
                Livre
              </span>
              <span>
                <i className="taken" />
                Ocupado
              </span>
              <span>
                <i className="selected" />
                Selecionado
              </span>
            </div>
            <div className="seat-map">
              {Array.from({ length: Math.ceil(plane.capacity / 6) }, (_, row) => (
                <div className="seat-row" key={row}>
                  <small>{row + 1}</small>
                  {Array.from({ length: 6 }, (_, col) => {
                    const name = seatName(row * 6 + col);
                    return row * 6 + col < plane.capacity ? (
                      <button
                        type="button"
                        key={col}
                        disabled={occupied.has(name)}
                        aria-label={`Assento ${name}${occupied.has(name) ? ' ocupado' : ''}`}
                        aria-pressed={form.seat === name}
                        className={`seat ${form.seat === name ? 'selected' : ''} ${col === 3 ? 'aisle' : ''}`}
                        onClick={() => setForm((f) => ({ ...f, seat: name }))}
                      >
                        {name}
                      </button>
                    ) : (
                      <span key={col} />
                    );
                  })}
                </div>
              ))}
            </div>
            <p className="seat-selected">
              Assento selecionado: <strong>{form.seat || '—'}</strong>
            </p>
          </div>
        )}
        {error && (
          <p role="alert" className="form-error">
            {error}
          </p>
        )}
        <footer className="modal-footer">
          <button type="button" className="button secondary" onClick={onClose} disabled={busy}>
            Voltar
          </button>
          <button className="button primary" disabled={busy || !form.seat}>
            <Ticket size={16} />
            {busy ? 'Reservando…' : 'Confirmar reserva'}
          </button>
        </footer>
      </form>
    </Modal>
  );
}

export function BoardingPass({ reservation: r, index, onClose }) {
  const f = index.flights[r.flightId],
    p = index.passengers[r.passengerId],
    a = index.airlines[f.airlineId],
    g = index.gates[f.gateId];
  if (r.status !== 'checked_in' || ['cancelled', 'maintenance'].includes(f.status)) {
    return (
      <Modal
        title="Cartão de embarque indisponível"
        subtitle={`Reserva ${r.locator}`}
        onClose={onClose}
      >
        <div className="confirm-body">
          <AlertTriangle size={26} />
          <p>
            A reserva ou o voo não permite emitir um cartão de embarque. Consulte a situação atual
            da operação.
          </p>
        </div>
      </Modal>
    );
  }
  return (
    <Modal title="Cartão de embarque" subtitle={`Reserva ${r.locator}`} onClose={onClose}>
      <div className="boarding-pass">
        <header>
          <div className="pass-brand">
            <AirlineMark airline={a} />
            <strong>{a.name}</strong>
          </div>
          <span>BOARDING PASS</span>
        </header>
        <div className="pass-name">
          <small>PASSAGEIRO</small>
          <h3>{p.name}</h3>
        </div>
        <div className="flight-route-large">
          <div>
            <b>{f.originCode}</b>
            <span>{f.origin}</span>
          </div>
          <Plane size={24} />
          <div>
            <b>{f.destinationCode}</b>
            <span>{f.destination}</span>
          </div>
        </div>
        <div className="pass-data">
          <div>
            <small>VOO</small>
            <b>{f.number}</b>
          </div>
          <div>
            <small>DATA</small>
            <b>
              {dateLabel(
                new Date(f.scheduled).toLocaleDateString('en-CA', {
                  timeZone: 'America/Sao_Paulo',
                }),
              )}
            </b>
          </div>
          <div>
            <small>PARTIDA</small>
            <b>{time(f.actual || f.scheduled)}</b>
          </div>
        </div>
        <div className="pass-tear">
          <div>
            <small>PORTÃO</small>
            <b>{g.code}</b>
          </div>
          <div>
            <small>ASSENTO</small>
            <b>{r.seat}</b>
          </div>
          <div>
            <small>LOCALIZADOR</small>
            <b>{r.locator}</b>
          </div>
        </div>
        <div className="pass-footer">
          <Check size={17} />
          <span>Check-in confirmado · Documento original obrigatório</span>
        </div>
        <p className="simulation-stamp">SIMULAÇÃO · SEM VALIDADE PARA VIAGEM</p>
      </div>
      <footer className="modal-footer">
        <button className="button primary" onClick={() => window.print()}>
          <Printer size={16} />
          Imprimir cartão
        </button>
      </footer>
    </Modal>
  );
}

export function ConfirmDialog({ title, detail, onConfirm, onClose }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState('');
  async function confirm() {
    setBusy(true);
    try {
      await onConfirm();
      onClose();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }
  return (
    <Modal title={title} onClose={() => !busy && onClose()}>
      <div className="confirm-body">
        <AlertTriangle size={30} />
        <p>{detail}</p>
        {error && (
          <p role="alert" className="form-error">
            {error}
          </p>
        )}
      </div>
      <footer className="modal-footer">
        <button className="button secondary" disabled={busy} onClick={onClose}>
          Voltar
        </button>
        <button className="button danger" disabled={busy} onClick={confirm}>
          {busy ? 'Processando…' : 'Confirmar'}
        </button>
      </footer>
    </Modal>
  );
}

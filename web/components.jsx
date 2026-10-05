import React, { useEffect, useRef, useId } from 'react';
import {
  X,
  Search,
  Plane,
  Inbox,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  ArrowUpRight,
} from 'lucide-react';
import { statusLabels, time, initials, dateLabel, localDay } from './model';

export function IconButton({ icon: Icon, label, ...props }) {
  return (
    <button className="icon-button" aria-label={label} title={label} {...props}>
      <Icon size={17} />
    </button>
  );
}
export function Badge({ status }) {
  return (
    <span className={`badge ${status}`}>
      <i />
      {statusLabels[status] || status}
    </span>
  );
}
export function AirlineMark({ airline, small = false }) {
  return (
    <span
      className={`airline-mark ${small ? 'small' : ''}`}
      style={{ '--airline': airline?.color || '#567' }}
    >
      {airline?.code || '—'}
    </span>
  );
}
export function Avatar({ name, small = false }) {
  return <span className={`avatar ${small ? 'small' : ''}`}>{initials(name)}</span>;
}
export function Empty({
  title = 'Nenhum registro encontrado',
  detail = 'Altere os filtros ou cadastre um novo registro.',
}) {
  return (
    <div className="empty">
      <Inbox size={30} />
      <strong>{title}</strong>
      <p>{detail}</p>
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading">
      <LoaderCircle size={25} className="spin" />
      <span>Carregando operações…</span>
    </div>
  );
}
export function Modal({ title, subtitle, children, onClose, wide = false }) {
  const ref = useRef(null),
    closeRef = useRef(onClose);
  closeRef.current = onClose;
  useEffect(() => {
    const previous = document.activeElement;
    const handler = (event) => {
      if (event.key === 'Escape') closeRef.current();
      if (event.key === 'Tab') {
        const items = [
          ...ref.current.querySelectorAll(
            'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]',
          ),
        ];
        if (event.shiftKey && document.activeElement === items[0]) {
          event.preventDefault();
          items.at(-1)?.focus();
        } else if (!event.shiftKey && document.activeElement === items.at(-1)) {
          event.preventDefault();
          items[0]?.focus();
        }
      }
    };
    document.addEventListener('keydown', handler);
    document.body.style.overflow = 'hidden';
    ref.current.querySelector('input,select,button')?.focus();
    return () => {
      document.removeEventListener('keydown', handler);
      document.body.style.overflow = '';
      previous?.focus();
    };
  }, []);
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        className={`modal ${wide ? 'wide' : ''}`}
      >
        <header>
          <div>
            <h2 id="modal-title">{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <IconButton icon={X} label="Fechar" onClick={onClose} />
        </header>
        {children}
      </section>
    </div>
  );
}
export function Field({ label, error, children, wide = false }) {
  const id = useId();
  return (
    <div className={`field ${wide ? 'full' : ''}`}>
      <label id={`${id}-label`} htmlFor={id}>
        {label}
      </label>
      {React.cloneElement(children, {
        id,
        'aria-labelledby': `${id}-label`,
        'aria-invalid': !!error,
        'aria-describedby': error ? `${id}-error` : undefined,
      })}
      {error && (
        <small id={`${id}-error`} className="field-error">
          {error}
        </small>
      )}
    </div>
  );
}
export function SearchInput({ value, onChange, placeholder = 'Buscar…' }) {
  return (
    <div className="search-input">
      <Search size={16} />
      <input
        aria-label={placeholder}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      {value && <IconButton icon={X} label="Limpar busca" onClick={() => onChange('')} />}
    </div>
  );
}
export function Pagination({ page, setPage, total, size = 10 }) {
  const pages = Math.max(1, Math.ceil(total / size));
  return (
    <footer className="pagination">
      <span>
        {total
          ? `${(page - 1) * size + 1}–${Math.min(page * size, total)} de ${total} registros`
          : '0 registros'}
      </span>
      <div>
        <IconButton
          icon={ChevronLeft}
          label="Página anterior"
          disabled={page <= 1}
          onClick={() => setPage(page - 1)}
        />
        <span>
          {page} / {pages}
        </span>
        <IconButton
          icon={ChevronRight}
          label="Próxima página"
          disabled={page >= pages}
          onClick={() => setPage(page + 1)}
        />
      </div>
    </footer>
  );
}
export function FlightTable({ flights, index, onOpen, compact = false, showDate = false }) {
  return (
    <div className="table-scroll">
      <table className={`data-table flight-table ${compact ? 'compact' : ''}`}>
        <thead>
          <tr>
            <th>Voo / Companhia</th>
            <th>Rota</th>
            <th>Horário</th>
            <th>Portão</th>
            <th>Status</th>
            <th>
              <span className="sr-only">Detalhes</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {flights.map((f) => {
            const airline = index.airlines[f.airlineId],
              gate = index.gates[f.gateId];
            return (
              <tr key={f.id}>
                <td>
                  <div className="flight-identity">
                    <AirlineMark airline={airline} />
                    <div>
                      <button className="text-link strong" onClick={() => onOpen(f)}>
                        {f.number}
                      </button>
                      <small>{airline.name}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="route">
                    <b>{f.originCode}</b>
                    <Plane size={13} />
                    <b>{f.destinationCode}</b>
                  </div>
                  <small>{f.type === 'departure' ? f.destination : f.origin}</small>
                </td>
                <td>
                  <strong className={f.status === 'delayed' ? 'time-delayed' : ''}>
                    {time(f.actual || f.scheduled)}
                  </strong>
                  {f.actual && f.actual !== f.scheduled ? (
                    <small className="struck">{time(f.scheduled)}</small>
                  ) : (
                    <small>{f.actual ? 'Real' : 'Previsto'}</small>
                  )}
                  {showDate && <small>{dateLabel(localDay(f.scheduled))}</small>}
                </td>
                <td>
                  <span className="gate-code">{gate.code}</span>
                  <small>{index.terminals[gate.terminalId].name}</small>
                </td>
                <td>
                  <Badge status={f.status} />
                </td>
                <td>
                  <IconButton
                    icon={ArrowUpRight}
                    label={`Detalhes do voo ${f.number}`}
                    onClick={() => onOpen(f)}
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {!flights.length && <Empty />}
    </div>
  );
}

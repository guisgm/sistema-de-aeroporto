import { z } from 'zod';

const text = (min = 2, max = 120) =>
  z
    .string()
    .trim()
    .min(min, `Informe pelo menos ${min} caracteres.`)
    .max(max, `Limite de ${max} caracteres.`);
const id = text(1, 80);
const version = z.number().int().positive().optional();
const date = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Data invalida.')
  .refine((v) => {
    const parsed = new Date(`${v}T12:00:00Z`);
    return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === v;
  }, 'Data invalida.');
const instant = z
  .string()
  .datetime({ offset: true, message: 'Horario invalido.' })
  .transform((value) => new Date(value).toISOString());
export const schemas = {
  airlines: z.object({
    name: text(),
    code: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z0-9]{2,3}$/, 'Codigo IATA deve ter 2 ou 3 caracteres.'),
    country: text(),
    contact: z.string().trim().email('E-mail invalido.'),
    color: z.string().regex(/^#[a-fA-F0-9]{6}$/),
    version,
  }),
  terminals: z.object({
    name: text(),
    kind: z.enum(['Domestico', 'Internacional', 'Misto']),
    version,
  }),
  gates: z.object({
    code: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z][0-9]{2,3}$/, 'Use um codigo como A01.'),
    terminalId: id,
    status: z.enum(['available', 'blocked']),
    version,
  }),
  aircraft: z.object({
    registration: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z0-9]{2}-[A-Z0-9]{3,5}$/, 'Use uma matricula como PR-ABC.'),
    model: text(),
    capacity: z.number().int().min(6).max(600),
    airlineId: id,
    status: z.enum(['available', 'maintenance', 'unavailable']),
    maintenanceDate: date,
    notes: text(0, 500),
    version,
  }),
  flights: z.object({
    number: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z0-9]{2,3}\s?\d{2,4}$/, 'Use um numero como LA 1234.'),
    airlineId: id,
    aircraftId: id,
    origin: text(),
    destination: text(),
    originCode: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z]{3}$/),
    destinationCode: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[A-Z]{3}$/),
    type: z.enum(['departure', 'arrival']),
    scheduled: instant,
    actual: instant.nullable(),
    duration: z.number().int().min(20).max(1440),
    gateId: id,
    status: z.enum(['scheduled', 'boarding', 'delayed', 'cancelled', 'landed', 'maintenance']),
    notes: text(0, 500),
    version,
  }),
  passengers: z.object({
    name: text(3),
    documentType: z.enum(['cpf', 'passport']),
    document: text(6, 20).transform((v) => v.replace(/[.\-\s]/g, '').toUpperCase()),
    birthDate: date,
    email: z.string().trim().email('E-mail invalido.'),
    phone: z
      .string()
      .transform((v) => v.replace(/\D/g, ''))
      .pipe(z.string().min(10, 'Informe telefone com DDD.').max(15)),
    nationality: text(),
    version,
  }),
  reservations: z.object({
    passengerId: id,
    flightId: id,
    seat: z
      .string()
      .trim()
      .toUpperCase()
      .regex(/^[1-9]\d?[A-F]$/, 'Assento invalido.'),
  }),
};

export function validCpf(value) {
  if (!/^\d{11}$/.test(value) || /^(\d)\1{10}$/.test(value)) return false;
  for (let size = 9; size <= 10; size++) {
    let sum = 0;
    for (let i = 0; i < size; i++) sum += Number(value[i]) * (size + 1 - i);
    const digit = (sum * 10) % 11;
    if (Number(value[size]) !== (digit === 10 ? 0 : digit)) return false;
  }
  return true;
}

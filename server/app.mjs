import express from 'express';
import { randomBytes, scryptSync, timingSafeEqual } from 'node:crypto';
import { ZodError } from 'zod';
import {
  readState,
  saveEntity,
  createReservation,
  updateReservation,
  csvExport,
  audit,
} from './operations.mjs';
import { BusinessError } from './errors.mjs';

export function createApp(db) {
  const app = express(),
    sessions = new Map(),
    attempts = new Map();
  app.disable('x-powered-by');
  app.use((req, res, next) => {
    res.set({
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'same-origin',
    });
    if (req.path.startsWith('/api')) res.set('Cache-Control', 'no-store');
    if (
      !['GET', 'HEAD', 'OPTIONS'].includes(req.method) &&
      req.headers.origin &&
      req.headers.origin !== `${req.protocol}://${req.headers.host}`
    )
      return res.status(403).json({ message: 'Origem da requisicao nao autorizada.' });
    next();
  });
  app.use(express.json({ limit: '64kb' }));
  const cookieName = 'aerohub_session';
  const sessionOptions = {
    httpOnly: true,
    sameSite: 'strict',
    secure: process.env.COOKIE_SECURE === 'true',
    path: '/',
    maxAge: 8 * 3600000,
  };
  app.post('/api/login', (req, res) => {
    const ip = req.ip,
      now = Date.now(),
      attempt = attempts.get(ip);
    if (attempt && attempt.expires > now && attempt.count >= 10)
      return res.status(429).json({ message: 'Muitas tentativas de acesso. Aguarde 15 minutos.' });
    const email = String(req.body?.email || '')
        .trim()
        .toLowerCase(),
      password = String(req.body?.password || '');
    if (password.length > 128 || email.length > 200)
      return res.status(400).json({ message: 'Credenciais invalidas.' });
    const user = db.prepare('SELECT * FROM users WHERE email=?').get(email);
    const hash = (user?.password || '00000000000000000000000000000000:' + '00'.repeat(64)).split(
      ':',
    );
    const match = timingSafeEqual(scryptSync(password, hash[0], 64), Buffer.from(hash[1], 'hex'));
    if (!user || !match) {
      attempts.set(ip, {
        count: attempt && attempt.expires > now ? attempt.count + 1 : 1,
        expires: now + 15 * 60000,
      });
      return res.status(401).json({ message: 'E-mail ou senha incorretos.' });
    }
    attempts.delete(ip);
    for (const [key, value] of sessions) if (value.expires < now) sessions.delete(key);
    const token = randomBytes(32).toString('hex'),
      csrf = randomBytes(24).toString('hex');
    const safe = { id: user.id, name: user.name, email: user.email, role: user.role };
    sessions.set(token, { user: safe, csrf, expires: now + 8 * 3600000 });
    audit(db, safe, 'Acesso', 'users', safe.id, 'Sessao iniciada.');
    res.cookie(cookieName, token, sessionOptions).json({ user: safe, csrf });
  });
  app.use('/api', (req, res, next) => {
    const cookies = Object.fromEntries(
      (req.headers.cookie || '')
        .split(';')
        .filter((v) => v.includes('='))
        .map((v) => v.trim().split('=')),
    );
    const session = sessions.get(cookies[cookieName]);
    if (!session || session.expires < Date.now())
      return res.status(401).json({ message: 'Sua sessao expirou. Entre novamente.' });
    req.user = session.user;
    req.session = session;
    req.token = cookies[cookieName];
    if (!['GET', 'HEAD'].includes(req.method) && req.headers['x-csrf-token'] !== session.csrf)
      return res.status(403).json({ message: 'Requisicao invalida. Atualize a pagina.' });
    next();
  });
  app.get('/api/session', (req, res) => res.json({ user: req.user, csrf: req.session.csrf }));
  app.post('/api/logout', (req, res) => {
    sessions.delete(req.token);
    res.clearCookie(cookieName, sessionOptions);
    res.json({ ok: true });
  });
  app.get('/api/state', (req, res) => res.json(readState(db, req.user)));
  app.post('/api/reservations', (req, res) =>
    res.status(201).json(createReservation(db, req.user, req.body)),
  );
  app.post('/api/reservations/:id/checkin', (req, res) =>
    res.json(updateReservation(db, req.user, req.params.id, 'checkin', req.body.version)),
  );
  app.post('/api/reservations/:id/cancel', (req, res) =>
    res.json(updateReservation(db, req.user, req.params.id, 'cancel', req.body.version)),
  );
  app.get('/api/export/:table', (req, res) => {
    res.set({
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': `attachment; filename="aerohub-${req.params.table}.csv"`,
    });
    res.send(csvExport(db, req.user, req.params.table, req.query.date));
  });
  app.post('/api/:table', (req, res) =>
    res.status(201).json(saveEntity(db, req.user, req.params.table, req.body)),
  );
  app.put('/api/:table/:id', (req, res) =>
    res.json(saveEntity(db, req.user, req.params.table, req.body, req.params.id)),
  );
  app.use('/api', (req, res) => res.status(404).json({ message: 'Rota nao encontrada.' }));
  app.use((error, req, res, next) => {
    if (!req.path.startsWith('/api')) return next(error);
    if (error instanceof ZodError)
      return res.status(422).json({
        message: error.issues.map((i) => `${i.path.join('.')}: ${i.message}`).join(' '),
        fields: Object.fromEntries(error.issues.map((i) => [i.path[0], i.message])),
      });
    if (error instanceof BusinessError)
      return res.status(error.status).json({ message: error.message });
    if (error.code?.startsWith('SQLITE_CONSTRAINT') || error.message?.includes('UNIQUE constraint'))
      return res.status(409).json({
        message: 'Ja existe um registro com esses dados. Verifique codigos, documentos e assentos.',
      });
    if (error instanceof SyntaxError && 'body' in error)
      return res.status(400).json({ message: 'Dados enviados em formato invalido.' });
    console.error('Falha na API:', error);
    res.status(500).json({ message: 'Nao foi possivel concluir a operacao. Tente novamente.' });
  });
  return app;
}

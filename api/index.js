const fs = require('fs');
const path = require('path');
const url = require('url');

const TMP_DB = '/tmp/roleta_db.json';
const SEED_DB = path.join(__dirname, '..', 'server', 'data', 'db.json');

function getDb() {
  try {
    if (fs.existsSync(TMP_DB)) {
      return JSON.parse(fs.readFileSync(TMP_DB, 'utf8'));
    }
  } catch (e) {}

  try {
    const data = JSON.parse(fs.readFileSync(SEED_DB, 'utf8'));
    fs.writeFileSync(TMP_DB, JSON.stringify(data), 'utf8');
    return data;
  } catch (e) {
    const defaultData = {
      adminPin: '1234',
      campaigns: [{ id: 1, name: 'Promoção Outubro', slug: 'outubro', active: true }],
      prizes: [
        { id: 1, campaignId: 1, name: '5% de Desconto', weight: 40, quantity: 500, colorHex: '#10B981' },
        { id: 2, campaignId: 1, name: '10% de Desconto', weight: 30, quantity: 200, colorHex: '#2563EB' },
        { id: 3, campaignId: 1, name: '15% de Desconto', weight: 20, quantity: 50, colorHex: '#8B5CF6' },
        { id: 4, campaignId: 1, name: '20% de Desconto', weight: 8, quantity: 20, colorHex: '#F59E0B' },
        { id: 5, campaignId: 1, name: '50% de Desconto', weight: 2, quantity: 5, colorHex: '#EF4444' },
        { id: 6, campaignId: 1, name: 'Brinde Especial', weight: 10, quantity: 30, colorHex: '#EC4899' }
      ],
      accessCodes: [
        { id: 1, campaignId: 1, code: 'ROULET-8K42P', status: 'DISPONIVEL' },
        { id: 2, campaignId: 1, code: 'RLT-7X92KP', status: 'DISPONIVEL' },
        { id: 3, campaignId: 1, code: 'RLT-4M8Q2A', status: 'DISPONIVEL' },
        { id: 4, campaignId: 1, code: 'ROULET-USED01', status: 'UTILIZADO' }
      ],
      spins: []
    };
    try { fs.writeFileSync(TMP_DB, JSON.stringify(defaultData), 'utf8'); } catch (err) {}
    return defaultData;
  }
}

function saveDb(data) {
  try {
    fs.writeFileSync(TMP_DB, JSON.stringify(data, null, 2), 'utf8');
  } catch (e) {
    console.error('Error saving db:', e);
  }
}

function parseJsonBody(req) {
  return new Promise(resolve => {
    if (req.body && typeof req.body === 'object') return resolve(req.body);
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try { resolve(body ? JSON.parse(body) : {}); }
      catch (e) { resolve({}); }
    });
  });
}

module.exports = async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-admin-pin');

  if (method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  // 1. PUBLIC API: Get Wheel Data
  if (pathname.endsWith('/public/data')) {
    const db = getDb();
    const activeCamp = db.campaigns.find(c => c.active) || db.campaigns[0] || null;
    const prizes = db.prizes.filter(p => !activeCamp || p.campaignId === activeCamp.id);
    return res.status(200).json({ campaign: activeCamp, prizes });
  }

  // 2. PUBLIC API: Validate Code
  if (pathname.endsWith('/validate-code')) {
    const body = await parseJsonBody(req);
    const code = (body.code || '').trim().toUpperCase();

    if (!code) {
      return res.status(400).json({ valid: false, message: 'Digite um código para continuar.' });
    }

    const db = getDb();
    const codeEntity = db.accessCodes.find(c => c.code.toUpperCase() === code);

    if (!codeEntity) {
      return res.status(404).json({ valid: false, message: 'Código inválido. Verifique o código informado.' });
    }
    if (codeEntity.status === 'UTILIZADO' || codeEntity.usedAt) {
      return res.status(400).json({ valid: false, message: 'Este código já foi utilizado.' });
    }
    if (codeEntity.status !== 'DISPONIVEL') {
      return res.status(400).json({ valid: false, message: 'Código inativo.' });
    }

    return res.status(200).json({ valid: true, code: codeEntity.code });
  }

  // 3. PUBLIC API: Spin Wheel (Atomic Weighted Lottery)
  if (pathname.endsWith('/spin')) {
    const body = await parseJsonBody(req);
    const code = (body.code || '').trim().toUpperCase();

    const db = getDb();
    const idx = db.accessCodes.findIndex(c => c.code.toUpperCase() === code);

    if (idx === -1) {
      return res.status(404).json({ success: false, message: 'Código inválido.' });
    }

    const codeEntity = db.accessCodes[idx];
    if (codeEntity.status === 'UTILIZADO' || codeEntity.usedAt) {
      return res.status(400).json({ success: false, message: 'Este código já foi utilizado.' });
    }

    const eligiblePrizes = db.prizes.filter(p => p.campaignId === codeEntity.campaignId && (p.unlimitedQuantity || p.quantity > 0));
    if (eligiblePrizes.length === 0) {
      return res.status(400).json({ success: false, message: 'Prêmios esgotados.' });
    }

    const totalWeight = eligiblePrizes.reduce((sum, p) => sum + Math.max(1, p.weight), 0);
    let pick = Math.floor(Math.random() * totalWeight);
    let won = eligiblePrizes[0];

    for (const p of eligiblePrizes) {
      const w = Math.max(1, p.weight);
      if (pick < w) {
        won = p;
        break;
      }
      pick -= w;
    }

    if (!won.unlimitedQuantity) {
      const pIdx = db.prizes.findIndex(p => p.id === won.id);
      if (pIdx !== -1) db.prizes[pIdx].quantity = Math.max(0, db.prizes[pIdx].quantity - 1);
    }

    const now = Date.now();
    db.accessCodes[idx].status = 'UTILIZADO';
    db.accessCodes[idx].usedAt = now;
    db.accessCodes[idx].prizeId = won.id;
    db.accessCodes[idx].prizeName = won.name;

    const spinId = (db.spins.length > 0 ? Math.max(...db.spins.map(s => s.id)) : 0) + 1;
    db.spins.push({
      id: spinId,
      campaignId: codeEntity.campaignId,
      accessCodeId: codeEntity.id,
      code: codeEntity.code,
      prizeId: won.id,
      prizeName: won.name,
      clientName: 'Não informado',
      observation: '',
      createdAt: now
    });

    saveDb(db);

    const allPrizes = db.prizes.filter(p => p.campaignId === codeEntity.campaignId);
    const winningIndex = Math.max(0, allPrizes.findIndex(p => p.id === won.id));

    return res.status(200).json({
      success: true,
      prize: won,
      winningIndex,
      code: codeEntity.code
    });
  }

  // 4. ADMIN LOGIN
  if (pathname.endsWith('/admin/login')) {
    const body = await parseJsonBody(req);
    const db = getDb();
    if (body.pin === (db.adminPin || '1234')) {
      return res.status(200).json({ success: true });
    }
    return res.status(401).json({ success: false, message: 'PIN incorreto.' });
  }

  // Helper auth
  const pin = req.headers['x-admin-pin'] || parsedUrl.query.pin;
  const db = getDb();
  if (pin !== (db.adminPin || '1234')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  // 5. ADMIN DATA
  if (pathname.endsWith('/admin/data')) {
    return res.status(200).json(db);
  }

  // 6. ADMIN EXPORT CSV
  if (pathname.endsWith('/admin/export-csv')) {
    let csv = 'Código,Cliente,Prêmio,Data,Hora,Campanha,Status,Observação\n';
    db.spins.forEach(s => {
      const d = new Date(s.createdAt);
      csv += `"${s.code}","${s.clientName || ''}","${s.prizeName || ''}",${d.toLocaleDateString('pt-BR')},${d.toLocaleTimeString('pt-BR')},"Campanha","Utilizado","${s.observation || ''}"\n`;
    });
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename="resultados_roleta.csv"');
    return res.status(200).send('\uFEFF' + csv);
  }

  // 7. ADMIN BULK CODES
  if (pathname.endsWith('/admin/codes/generate')) {
    const body = await parseJsonBody(req);
    const count = Math.min(500, Math.max(1, parseInt(body.count) || 50));
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    const existing = new Set(db.accessCodes.map(c => c.code));
    let nextId = (db.accessCodes.length > 0 ? Math.max(...db.accessCodes.map(c => c.id)) : 0) + 1;

    for (let i = 0; i < count; i++) {
      let code = '';
      do {
        let rand = '';
        for (let j = 0; j < 6; j++) rand += chars.charAt(Math.floor(Math.random() * chars.length));
        code = `RLT-${rand}`;
      } while (existing.has(code));
      existing.add(code);
      db.accessCodes.push({
        id: nextId++,
        campaignId: 1,
        code,
        status: 'DISPONIVEL',
        usedAt: null,
        prizeId: null,
        prizeName: null,
        clientName: '',
        observation: '',
        createdAt: Date.now()
      });
    }
    saveDb(db);
    return res.status(200).json({ success: true, count });
  }

  // 8. ADMIN UPDATE SPIN CLIENT
  if (pathname.endsWith('/admin/spin/update')) {
    const body = await parseJsonBody(req);
    const spin = db.spins.find(s => s.id === body.spinId);
    if (spin) {
      spin.clientName = (body.clientName || '').trim() || 'Não informado';
      spin.observation = (body.observation || '').trim();
      const code = db.accessCodes.find(c => c.code === spin.code);
      if (code) {
        code.clientName = spin.clientName;
        code.observation = spin.observation;
      }
      saveDb(db);
    }
    return res.status(200).json({ success: true });
  }

  return res.status(404).send('Not Found');
};

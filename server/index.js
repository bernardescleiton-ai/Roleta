const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const url = require('url');

const PORT = 3000;
const DB_PATH = path.join(__dirname, 'data', 'db.json');

// Helper to read DB
function readDb() {
  try {
    const raw = fs.readFileSync(DB_PATH, 'utf8');
    return JSON.parse(raw);
  } catch (e) {
    console.error('Error reading db:', e);
    return { adminPin: '1234', campaigns: [], prizes: [], accessCodes: [], spins: [] };
  }
}

// Helper to write DB atomically
function writeDb(data) {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(data, null, 2), 'utf8');
  } catch (e) {
    console.error('Error writing db:', e);
  }
}

// Parse JSON body
function parseBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({});
      }
    });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, DELETE, PUT');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, x-admin-pin');

  if (method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // 1. PUBLIC ROUTES
  // Client Roleta View
  if (pathname === '/' || pathname === '/roleta' || pathname.startsWith('/roleta/')) {
    const html = fs.readFileSync(path.join(__dirname, 'public', 'index.html'), 'utf8');
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
    return;
  }

  // Independent Admin View
  if (pathname === '/admin' || pathname === '/admin/') {
    const html = fs.readFileSync(path.join(__dirname, 'public', 'admin.html'), 'utf8');
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
    return;
  }

  // 2. PUBLIC API: Get Wheel Data
  if (pathname === '/api/public/data' && method === 'GET') {
    const db = readDb();
    const activeCampaign = db.campaigns.find(c => c.active) || db.campaigns[0] || null;
    const prizes = db.prizes.filter(p => !activeCampaign || p.campaignId === activeCampaign.id);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ campaign: activeCampaign, prizes }));
    return;
  }

  // 3. PUBLIC API: Validate Code
  if (pathname === '/api/validate-code' && method === 'POST') {
    const body = await parseBody(req);
    const code = (body.code || '').trim().toUpperCase();

    if (!code) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ valid: false, message: 'Digite um código para continuar.' }));
      return;
    }

    const db = readDb();
    const codeEntity = db.accessCodes.find(c => c.code.toUpperCase() === code);

    if (!codeEntity) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ valid: false, message: 'Código inválido. Verifique o código informado.' }));
      return;
    }

    if (codeEntity.status === 'UTILIZADO' || codeEntity.usedAt) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ valid: false, message: 'Este código já foi utilizado.' }));
      return;
    }

    if (codeEntity.status !== 'DISPONIVEL') {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ valid: false, message: 'Código inativo ou indisponível.' }));
      return;
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ valid: true, code: codeEntity.code }));
    return;
  }

  // 4. PUBLIC API: Spin Wheel (Atomic Lottery Draw)
  if (pathname === '/api/spin' && method === 'POST') {
    const body = await parseBody(req);
    const code = (body.code || '').trim().toUpperCase();

    const db = readDb();
    const codeIndex = db.accessCodes.findIndex(c => c.code.toUpperCase() === code);

    if (codeIndex === -1) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'Código inválido.' }));
      return;
    }

    const codeEntity = db.accessCodes[codeIndex];
    if (codeEntity.status === 'UTILIZADO' || codeEntity.usedAt) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'Este código já foi utilizado.' }));
      return;
    }

    // Filter active prizes with remaining stock
    const eligiblePrizes = db.prizes.filter(p => p.campaignId === codeEntity.campaignId && (p.unlimitedQuantity || p.quantity > 0));
    if (eligiblePrizes.length === 0) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'Todos os prêmios desta roleta estão esgotados.' }));
      return;
    }

    // Weighted Probability Selection
    const totalWeight = eligiblePrizes.reduce((sum, p) => sum + Math.max(1, p.weight), 0);
    let randomPick = Math.floor(Math.random() * totalWeight);
    let selectedPrize = eligiblePrizes[0];

    for (const prize of eligiblePrizes) {
      const w = Math.max(1, prize.weight);
      if (randomPick < w) {
        selectedPrize = prize;
        break;
      }
      randomPick -= w;
    }

    // Atomically decrement stock
    if (!selectedPrize.unlimitedQuantity) {
      const prizeIdx = db.prizes.findIndex(p => p.id === selectedPrize.id);
      if (prizeIdx !== -1) {
        db.prizes[prizeIdx].quantity = Math.max(0, db.prizes[prizeIdx].quantity - 1);
      }
    }

    const now = Date.now();

    // Mark code as used
    db.accessCodes[codeIndex].status = 'UTILIZADO';
    db.accessCodes[codeIndex].usedAt = now;
    db.accessCodes[codeIndex].prizeId = selectedPrize.id;
    db.accessCodes[codeIndex].prizeName = selectedPrize.name;

    // Record Spin Entry
    const spinId = (db.spins.length > 0 ? Math.max(...db.spins.map(s => s.id)) : 0) + 1;
    const newSpin = {
      id: spinId,
      campaignId: codeEntity.campaignId,
      accessCodeId: codeEntity.id,
      code: codeEntity.code,
      prizeId: selectedPrize.id,
      prizeName: selectedPrize.name,
      clientName: 'Não informado',
      observation: '',
      createdAt: now
    };
    db.spins.push(newSpin);

    // Persist immediately
    writeDb(db);

    // Calculate index on full prize list for wheel alignment
    const allCampaignPrizes = db.prizes.filter(p => p.campaignId === codeEntity.campaignId);
    const winningIndex = Math.max(0, allCampaignPrizes.findIndex(p => p.id === selectedPrize.id));

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      success: true,
      prize: selectedPrize,
      winningIndex,
      code: codeEntity.code
    }));
    return;
  }

  // 5. ADMIN AUTH
  if (pathname === '/api/admin/login' && method === 'POST') {
    const body = await parseBody(req);
    const db = readDb();
    if (body.pin === (db.adminPin || '1234')) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    } else {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'PIN incorreto.' }));
    }
    return;
  }

  // Helper check admin PIN
  function isAuth(req) {
    const pin = req.headers['x-admin-pin'] || parsedUrl.query.pin;
    const db = readDb();
    return pin === (db.adminPin || '1234');
  }

  // 6. ADMIN API: All Data
  if (pathname === '/api/admin/data' && method === 'GET') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const db = readDb();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(db));
    return;
  }

  // 7. ADMIN API: Update Spin Client Name & Obs
  if (pathname === '/api/admin/spin/update' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const db = readDb();
    const spin = db.spins.find(s => s.id === body.spinId);
    if (spin) {
      spin.clientName = (body.clientName || '').trim() || 'Não informado';
      spin.observation = (body.observation || '').trim();

      // Sync to code
      const codeEntity = db.accessCodes.find(c => c.code === spin.code);
      if (codeEntity) {
        codeEntity.clientName = spin.clientName;
        codeEntity.observation = spin.observation;
      }
      writeDb(db);
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // 8. ADMIN API: Generate Bulk Codes
  if (pathname === '/api/admin/codes/generate' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const count = Math.min(500, Math.max(1, parseInt(body.count) || 50));
    const campaignId = parseInt(body.campaignId) || 1;
    const db = readDb();

    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    const existing = new Set(db.accessCodes.map(c => c.code));
    let nextId = (db.accessCodes.length > 0 ? Math.max(...db.accessCodes.map(c => c.id)) : 0) + 1;

    for (let i = 0; i < count; i++) {
      let code = '';
      do {
        let randPart = '';
        for (let j = 0; j < 6; j++) {
          randPart += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        code = `RLT-${randPart}`;
      } while (existing.has(code));

      existing.add(code);
      db.accessCodes.push({
        id: nextId++,
        campaignId,
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

    writeDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, count }));
    return;
  }

  // ADMIN API: Save Prize (Create or Update)
  if (pathname === '/api/admin/prize/save' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const db = readDb();
    const id = body.id ? parseInt(body.id) : null;
    const name = (body.name || '').trim();
    const description = (body.description || '').trim();
    const weight = Math.max(1, parseInt(body.weight) || 10);
    const quantity = parseInt(body.quantity) || 0;
    const unlimitedQuantity = Boolean(body.unlimitedQuantity);
    const colorHex = body.colorHex || '#10B981';
    const campaignId = parseInt(body.campaignId) || 1;

    if (!name) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'Nome do prêmio é obrigatório.' }));
      return;
    }

    if (id) {
      const pIdx = db.prizes.findIndex(p => p.id === id);
      if (pIdx !== -1) {
        db.prizes[pIdx] = {
          ...db.prizes[pIdx],
          name,
          description,
          weight,
          quantity,
          unlimitedQuantity,
          colorHex
        };
      }
    } else {
      const newId = (db.prizes.length > 0 ? Math.max(...db.prizes.map(p => p.id)) : 0) + 1;
      db.prizes.push({
        id: newId,
        campaignId,
        name,
        description,
        weight,
        quantity,
        unlimitedQuantity,
        active: true,
        colorHex
      });
    }

    writeDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // ADMIN API: Delete Prize
  if (pathname === '/api/admin/prize/delete' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const id = parseInt(body.id);
    const db = readDb();

    if (db.prizes.length <= 2) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, message: 'A roleta precisa de pelo menos 2 prêmios.' }));
      return;
    }

    db.prizes = db.prizes.filter(p => p.id !== id);
    writeDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // 9. ADMIN API: Export CSV
  if (pathname === '/api/admin/export-csv' && method === 'GET') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'text/plain' });
      res.end('Unauthorized');
      return;
    }
    const db = readDb();
    let csv = 'Código,Cliente,Prêmio,Data,Hora,Campanha,Status,Observação\n';

    db.spins.forEach(s => {
      const d = new Date(s.createdAt);
      const dateStr = d.toLocaleDateString('pt-BR');
      const timeStr = d.toLocaleTimeString('pt-BR');
      const camp = db.campaigns.find(c => c.id === s.campaignId);
      const campName = camp ? camp.name : 'Promoção';

      csv += `"${s.code}","${(s.clientName || '').replace(/"/g, '""')}","${(s.prizeName || '').replace(/"/g, '""')}",${dateStr},${timeStr},"${campName}","Utilizado","${(s.observation || '').replace(/"/g, '""')}"\n`;
    });

    res.writeHead(200, {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': 'attachment; filename="resultados_roleta.csv"'
    });
    res.end('\uFEFF' + csv);
    return;
  }

  // ADMIN API: Update Campaign Texts (Headline / Subtitle / Name)
  if (pathname === '/api/admin/campaign/update' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const db = readDb();
    const camp = db.campaigns.find(c => c.id === (parseInt(body.id) || 1)) || db.campaigns[0];
    if (camp) {
      if (body.name) camp.name = body.name.trim();
      if (body.title) camp.title = body.title.trim();
      if (body.subtitle !== undefined) camp.subtitle = body.subtitle.trim();
      writeDb(db);
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, campaign: camp }));
    return;
  }

  // 10. ADMIN API: Settings PIN
  if (pathname === '/api/admin/settings' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const body = await parseBody(req);
    const db = readDb();
    if (body.adminPin && body.adminPin.length >= 4) {
      db.adminPin = body.adminPin;
    }
    if (typeof body.whatsappTemplate === 'string') {
      db.whatsappTemplate = body.whatsappTemplate;
    }
    writeDb(db);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // 11. ADMIN API: Reset Demo
  if (pathname === '/api/admin/reset-demo' && method === 'POST') {
    if (!isAuth(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Unauthorized' }));
      return;
    }
    const now = Date.now();
    const demoDb = {
      adminPin: '1234',
      campaigns: [
        { id: 1, name: 'Promoção Outubro', slug: 'outubro', startDate: now - 86400000, endDate: now + 2592000000, active: true, createdAt: now }
      ],
      prizes: [
        { id: 1, campaignId: 1, name: '5% de Desconto', description: 'Válido acima de R$ 50', weight: 40, quantity: 500, unlimitedQuantity: false, active: true, colorHex: '#10B981' },
        { id: 2, campaignId: 1, name: '10% de Desconto', description: 'Válido para toda a loja', weight: 30, quantity: 200, unlimitedQuantity: false, active: true, colorHex: '#2563EB' },
        { id: 3, campaignId: 1, name: '15% de Desconto', description: 'Produtos selecionados', weight: 20, quantity: 50, unlimitedQuantity: false, active: true, colorHex: '#8B5CF6' },
        { id: 4, campaignId: 1, name: '20% de Desconto', description: 'Cliente fiel', weight: 8, quantity: 20, unlimitedQuantity: false, active: true, colorHex: '#F59E0B' },
        { id: 5, campaignId: 1, name: '50% de Desconto', description: 'Super prêmio da sorte!', weight: 2, quantity: 5, unlimitedQuantity: false, active: true, colorHex: '#EF4444' },
        { id: 6, campaignId: 1, name: 'Brinde Especial', description: 'Retire no balcão', weight: 10, quantity: 30, unlimitedQuantity: false, active: true, colorHex: '#EC4899' }
      ],
      accessCodes: [
        { id: 1, campaignId: 1, code: 'ROULET-8K42P', status: 'DISPONIVEL', usedAt: null, prizeId: null, prizeName: null, clientName: '', observation: '', createdAt: now },
        { id: 2, campaignId: 1, code: 'RLT-7X92KP', status: 'DISPONIVEL', usedAt: null, prizeId: null, prizeName: null, clientName: '', observation: '', createdAt: now },
        { id: 3, campaignId: 1, code: 'RLT-4M8Q2A', status: 'DISPONIVEL', usedAt: null, prizeId: null, prizeName: null, clientName: '', observation: '', createdAt: now },
        { id: 4, campaignId: 1, code: 'RLT-9ZK31B', status: 'DISPONIVEL', usedAt: null, prizeId: null, prizeName: null, clientName: '', observation: '', createdAt: now },
        { id: 5, campaignId: 1, code: 'ROULET-USED01', status: 'UTILIZADO', usedAt: now - 3600000, prizeId: 2, prizeName: '10% de Desconto', clientName: 'Maria Oliveira', observation: 'Cliente loja física', createdAt: now - 7200000 }
      ],
      spins: [
        { id: 1, campaignId: 1, accessCodeId: 5, code: 'ROULET-USED01', prizeId: 2, prizeName: '10% de Desconto', clientName: 'Maria Oliveira', observation: 'Cliente loja física', createdAt: now - 3600000 }
      ]
    };
    writeDb(demoDb);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    return;
  }

  // Static files serving
  let staticPath = null;
  if (pathname === '/' || pathname === '/index.html' || pathname === '/roleta') {
    staticPath = path.join(__dirname, 'public', 'index.html');
  } else if (pathname === '/admin' || pathname === '/admin.html') {
    staticPath = path.join(__dirname, 'public', 'admin.html');
  } else {
    const candidate = path.join(__dirname, 'public', pathname);
    if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
      staticPath = candidate;
    }
  }

  if (staticPath && fs.existsSync(staticPath)) {
    const ext = path.extname(staticPath).toLowerCase();
    const contentTypes = {
      '.html': 'text/html; charset=utf-8',
      '.css': 'text/css',
      '.js': 'application/javascript',
      '.json': 'application/json',
      '.png': 'image/png',
      '.jpg': 'image/jpeg'
    };
    res.writeHead(200, { 'Content-Type': contentTypes[ext] || 'text/plain' });
    fs.createReadStream(staticPath).pipe(res);
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Roleta Web Server running on port ${PORT}`);
});

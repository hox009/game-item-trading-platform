/*
 * Zero-dependency mock backend for local UI preview.
 *
 * The real platform is 7 Spring Cloud microservices + a Python AI service and
 * needs Docker (Nacos/MySQL/Redis/Kafka/RabbitMQ). This single Node process
 * emulates the same REST contract with in-memory data so the frontend can be
 * demoed without any infrastructure.
 *
 *   node mock-server/server.js
 *
 * Listens on :8080 (gateway APIs) and :8087 (AI assistant), matching the Vite
 * dev proxy. NOT for production — it is a preview stub only.
 */
const http = require('http');

// ---------------- In-memory state ----------------
const db = {
  users: new Map(),      // username -> {id, username, password, role}
  wallets: new Map(),    // userId -> balance
  items: [],             // item objects
  orders: [],            // order objects
  notifications: [],     // notification objects
  stock: new Map(),      // skuId -> {total, frozen}
};
let ids = { user: 0, item: 0, sku: 0, order: 0, notif: 0 };

function seed() {
  const catalog = [
    ['Dragon Lore', 'CS2', 'rifle-skin', 'AWP | Dragon Lore, a legendary sniper skin.', [['Factory New', 12500], ['Field-Tested', 8200]]],
    ['Butterfly Fade', 'CS2', 'knife-skin', 'Butterfly Knife | Fade, full gradient.', [['Factory New', 2600], ['Minimal Wear', 2100]]],
    ['AK-47 Redline', 'CS2', 'rifle-skin', 'AK-47 | Redline, the classic.', [['Field-Tested', 42.5], ['Minimal Wear', 88]]],
    ['Prisma Gloves', 'CS2', 'gloves', 'Sport Gloves | Prisma.', [['Field-Tested', 640], ['Well-Worn', 520]]],
    ['Phantom Reaver', 'Valorant', 'rifle-skin', 'Reaver Phantom bundle skin.', [['Standard', 35], ['Deluxe', 55]]],
    ['Dota Arcana Set', 'Dota2', 'arcana', 'Legendary arcana for a carry hero.', [['Base', 35], ['Golden', 60]]],
    ['Genshin Starter', 'Genshin', 'account', 'AR55 account with 5-star characters.', [['NA', 180], ['EU', 175]]],
    ['LoL Elementalist', 'LoL', 'skin', 'Elementalist Lux ultimate skin.', [['Base', 25]]],
    ['Neon Talon', 'CS2', 'knife-skin', 'Talon Knife | Neon, vibrant finish.', [['Factory New', 980], ['Field-Tested', 760]]],
    ['Vortex Sentinel', 'Valorant', 'bundle', 'Full weapon bundle with VFX.', [['Standard', 70]]],
  ];
  for (const [title, game, category, description, skus] of catalog) {
    const itemId = ++ids.item;
    const skuObjs = skus.map(([spec, price]) => {
      const skuId = ++ids.sku;
      db.stock.set(skuId, { total: 100, frozen: 0 });
      return { id: skuId, spec, price };
    });
    db.items.push({
      id: itemId, sellerId: 1, title, game, category, description,
      status: 'ON_SHELF', minPrice: Math.min(...skus.map((s) => s[1])), skus: skuObjs,
    });
  }
}
seed();

// ---------------- Helpers ----------------
const ok = (data) => ({ code: 0, message: 'success', data, timestamp: Date.now() });
const fail = (code, message) => ({ code, message, data: null, timestamp: Date.now() });

function encodeToken(user) {
  return Buffer.from(JSON.stringify({ userId: user.id, username: user.username, role: user.role })).toString('base64');
}
function decodeToken(req) {
  const auth = req.headers['authorization'] || '';
  if (!auth.startsWith('Bearer ')) return null;
  try {
    return JSON.parse(Buffer.from(auth.slice(7), 'base64').toString('utf8'));
  } catch {
    return null;
  }
}
function send(res, body, status = 200) {
  const json = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': '*',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
  });
  res.end(json);
}
function readBody(req) {
  return new Promise((resolve) => {
    let data = '';
    req.on('data', (c) => (data += c));
    req.on('end', () => {
      try { resolve(data ? JSON.parse(data) : {}); } catch { resolve({}); }
    });
  });
}
function getWallet(userId) {
  if (!db.wallets.has(userId)) db.wallets.set(userId, 0);
  return db.wallets.get(userId);
}

// ---------------- Gateway API (:8080) ----------------
async function gateway(req, res, url) {
  const p = url.pathname;
  const m = req.method;

  if (m === 'OPTIONS') return send(res, ok(null));

  // Auth
  if (p === '/api/users/register' && m === 'POST') {
    const b = await readBody(req);
    if (db.users.has(b.username)) return send(res, fail(2002, 'username already exists'));
    const user = { id: ++ids.user, username: b.username, password: b.password, role: b.role || 'BUYER' };
    db.users.set(b.username, user);
    db.wallets.set(user.id, 0);
    return send(res, ok({ id: user.id, username: user.username, role: user.role, balance: 0 }));
  }
  if (p === '/api/users/login' && m === 'POST') {
    const b = await readBody(req);
    const user = db.users.get(b.username);
    if (!user || user.password !== b.password) return send(res, fail(2003, 'invalid username or password'));
    return send(res, ok({ token: encodeToken(user), userId: user.id, username: user.username, role: user.role }));
  }

  // Items (public browsing)
  if (p === '/api/items' && m === 'GET') {
    let list = db.items.filter((i) => i.status === 'ON_SHELF');
    const kw = url.searchParams.get('keyword');
    const game = url.searchParams.get('game');
    const maxPrice = url.searchParams.get('maxPrice');
    if (kw) list = list.filter((i) => i.title.toLowerCase().includes(kw.toLowerCase()));
    if (game) list = list.filter((i) => i.game.toLowerCase() === game.toLowerCase());
    if (maxPrice) list = list.filter((i) => i.minPrice <= Number(maxPrice));
    return send(res, ok({ content: list, totalElements: list.length }));
  }
  const itemMatch = p.match(/^\/api\/items\/(\d+)$/);
  if (itemMatch && m === 'GET') {
    const item = db.items.find((i) => i.id === Number(itemMatch[1]));
    return item ? send(res, ok(item)) : send(res, fail(3000, 'item not found'));
  }
  if (p === '/api/items/mine' && m === 'GET') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const mine = db.items.filter((i) => i.sellerId === auth.userId).sort((a, b) => b.id - a.id);
    return send(res, ok(mine));
  }
  const shelfMatch = p.match(/^\/api\/items\/(\d+)\/(off-shelf|on-shelf)$/);
  if (shelfMatch && m === 'POST') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const item = db.items.find((i) => i.id === Number(shelfMatch[1]));
    if (!item) return send(res, fail(3000, 'item not found'));
    if (item.sellerId !== auth.userId) return send(res, fail(3000, 'not your item'));
    item.status = shelfMatch[2] === 'off-shelf' ? 'OFF_SHELF' : 'ON_SHELF';
    return send(res, ok(item));
  }
  if (p === '/api/items' && m === 'POST') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const b = await readBody(req);
    const itemId = ++ids.item;
    const skuObjs = (b.skus || []).map((s) => {
      const skuId = ++ids.sku;
      db.stock.set(skuId, { total: 100, frozen: 0 });
      return { id: skuId, spec: s.spec, price: Number(s.price) };
    });
    const item = {
      id: itemId, sellerId: auth.userId, title: b.title, game: b.game, category: b.category,
      description: b.description || '', status: 'ON_SHELF',
      minPrice: skuObjs.length ? Math.min(...skuObjs.map((s) => s.price)) : 0, skus: skuObjs,
    };
    db.items.push(item);
    return send(res, ok(item));
  }

  // Orders (auth)
  if (p === '/api/orders' && m === 'GET') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const mine = db.orders.filter((o) => o.buyerId === auth.userId).sort((a, b) => b.id - a.id);
    return send(res, ok(mine));
  }
  if (p === '/api/orders' && m === 'POST') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const b = await readBody(req);
    const item = db.items.find((i) => i.id === Number(b.itemId));
    if (!item) return send(res, fail(3000, 'item not found'));
    const sku = item.skus.find((s) => s.id === Number(b.skuId));
    if (!sku) return send(res, fail(3000, 'sku not found'));
    const qty = Number(b.quantity) || 1;
    const stock = db.stock.get(sku.id);
    if (stock.total - stock.frozen < qty) return send(res, fail(4000, 'stock not enough'));
    stock.frozen += qty;
    const order = {
      id: ++ids.order, buyerId: auth.userId, sellerId: item.sellerId, itemId: item.id,
      skuId: sku.id, quantity: qty, amount: sku.price * qty, status: 'STOCK_FROZEN',
      createdAt: new Date().toISOString(), paidAt: null,
    };
    db.orders.push(order);
    return send(res, ok(order));
  }
  const payMatch = p.match(/^\/api\/orders\/(\d+)\/pay$/);
  if (payMatch && m === 'POST') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const order = db.orders.find((o) => o.id === Number(payMatch[1]) && o.buyerId === auth.userId);
    if (!order) return send(res, fail(5000, 'order not found'));
    if (order.status !== 'STOCK_FROZEN') return send(res, fail(5001, 'illegal order status'));
    if (getWallet(auth.userId) < order.amount) return send(res, fail(6001, 'balance not enough — please recharge'));
    db.wallets.set(auth.userId, getWallet(auth.userId) - order.amount);
    db.wallets.set(order.sellerId, getWallet(order.sellerId) + order.amount);
    const stock = db.stock.get(order.skuId);
    stock.total -= order.quantity;
    stock.frozen -= order.quantity;
    order.status = 'PAID';
    order.paidAt = new Date().toISOString();
    db.notifications.push({
      id: ++ids.notif, userId: auth.userId, title: 'Payment successful',
      content: `Your order #${order.id} has been paid.`, refOrderId: order.id,
      read: false, createdAt: new Date().toISOString(),
    });
    return send(res, ok(order));
  }
  const cancelMatch = p.match(/^\/api\/orders\/(\d+)\/cancel$/);
  if (cancelMatch && m === 'POST') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const order = db.orders.find((o) => o.id === Number(cancelMatch[1]) && o.buyerId === auth.userId);
    if (!order) return send(res, fail(5000, 'order not found'));
    if (!['CREATED', 'STOCK_FROZEN'].includes(order.status)) return send(res, fail(5001, 'illegal order status'));
    const stock = db.stock.get(order.skuId);
    stock.frozen = Math.max(0, stock.frozen - order.quantity);
    order.status = 'CANCELLED';
    return send(res, ok(order));
  }

  // Payments
  if (p === '/api/payments/recharge' && m === 'POST') {
    const b = await readBody(req);
    const uid = Number(b.userId);
    db.wallets.set(uid, getWallet(uid) + Number(b.amount));
    return send(res, ok({ userId: uid, balance: getWallet(uid) }));
  }
  const walletMatch = p.match(/^\/api\/payments\/wallet\/(\d+)$/);
  if (walletMatch && m === 'GET') {
    const uid = Number(walletMatch[1]);
    return send(res, ok({ userId: uid, balance: getWallet(uid) }));
  }

  // Notifications
  if (p === '/api/notifications' && m === 'GET') {
    const auth = decodeToken(req);
    if (!auth) return send(res, fail(2001, 'unauthorized'), 401);
    const mine = db.notifications.filter((n) => n.userId === auth.userId).sort((a, b) => b.id - a.id);
    return send(res, ok(mine));
  }
  const readMatch = p.match(/^\/api\/notifications\/(\d+)\/read$/);
  if (readMatch && m === 'POST') {
    const n = db.notifications.find((x) => x.id === Number(readMatch[1]));
    if (n) n.read = true;
    return send(res, ok(null));
  }

  return send(res, fail(1002, 'not found'), 404);
}

// ---------------- AI assistant (:8087) ----------------
async function assistant(req, res, url) {
  if (req.method === 'OPTIONS') return send(res, ok(null));
  if (url.pathname === '/actuator/health') return send(res, { status: 'UP', llm: 'fallback' });
  if (url.pathname === '/api/assistant/chat' && req.method === 'POST') {
    const b = await readBody(req);
    const text = (b.message || '').toLowerCase();

    const orderMatch = text.match(/(?:order|订单)\s*#?(\d+)/);
    if (orderMatch) {
      const o = db.orders.find((x) => x.id === Number(orderMatch[1]));
      const answer = o ? `Order #${o.id} is currently '${o.status}'.` : `I couldn't find order #${orderMatch[1]}.`;
      return send(res, { answer, tools_used: ['get_order_status'], resolved_autonomously: !!o, mode: 'demo' });
    }
    if (/price|worth|pricing|价|值|定价/.test(text)) {
      return send(res, {
        answer: 'Fair pricing sits within ~5-10% of the median of the last 20 comparable sales. Knife skins and StatTrak variants command a premium; price at or slightly below median to sell quickly.',
        tools_used: ['pricing_guidance'], resolved_autonomously: true, mode: 'demo',
      });
    }
    const priceMatch = text.match(/(?:under|below|<|以内|不超过|低于)\s*\$?(\d+(?:\.\d+)?)/);
    let list = db.items;
    if (priceMatch) list = list.filter((i) => i.minPrice <= Number(priceMatch[1]));
    const kw = text.replace(/[^a-z0-9 ]/g, ' ').split(/\s+/).find((w) => w.length > 3 && !['find', 'search', 'under', 'below', 'show'].includes(w));
    if (kw) list = list.filter((i) => i.title.toLowerCase().includes(kw));
    const count = list.length;
    return send(res, {
      answer: count ? `Found ${count} matching item(s): ${list.slice(0, 3).map((i) => i.title).join(', ')}.` : 'No matching items found.',
      tools_used: ['search_items'], resolved_autonomously: count > 0, mode: 'demo',
    });
  }
  return send(res, fail(1002, 'not found'), 404);
}

// ---------------- Servers ----------------
http.createServer((req, res) => gateway(req, res, new URL(req.url, 'http://localhost:8080')))
  .listen(8080, () => console.log('[mock] gateway API on http://localhost:8080'));

http.createServer((req, res) => assistant(req, res, new URL(req.url, 'http://localhost:8087')))
  .listen(8087, () => console.log('[mock] AI assistant on http://localhost:8087'));

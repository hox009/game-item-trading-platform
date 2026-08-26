import { useEffect, useState } from 'react';
import { api } from '../api/client';

const STATUS_STYLES = {
  CREATED: 'bg-slate-100 text-slate-600',
  STOCK_FROZEN: 'bg-amber-100 text-amber-700',
  PAID: 'bg-green-100 text-green-700',
  COMPLETED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-red-100 text-red-600',
};

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    try {
      const data = await api.get('/api/orders');
      setOrders(data ?? []);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const act = async (id, action) => {
    setBusyId(id);
    setError('');
    try {
      await api.post(`/api/orders/${id}/${action}`);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <h1 className="mb-4 text-xl font-bold">My Orders</h1>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {orders.length === 0 ? (
        <p className="text-slate-500">No orders yet.</p>
      ) : (
        <div className="space-y-3">
          {orders.map((o) => (
            <div key={o.id} className="flex items-center justify-between rounded-xl bg-white p-4 shadow">
              <div>
                <div className="font-semibold">Order #{o.id}</div>
                <div className="text-sm text-slate-500">
                  SKU #{o.skuId} · qty {o.quantity} · ${Number(o.amount).toFixed(2)}
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span className={`rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[o.status] || ''}`}>
                  {o.status}
                </span>
                {o.status === 'STOCK_FROZEN' && (
                  <>
                    <button
                      onClick={() => act(o.id, 'pay')}
                      disabled={busyId === o.id}
                      className="rounded bg-brand px-3 py-1 text-sm text-white hover:bg-brand-dark disabled:opacity-50"
                    >
                      Pay
                    </button>
                    <button
                      onClick={() => act(o.id, 'cancel')}
                      disabled={busyId === o.id}
                      className="rounded bg-slate-100 px-3 py-1 text-sm hover:bg-slate-200 disabled:opacity-50"
                    >
                      Cancel
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

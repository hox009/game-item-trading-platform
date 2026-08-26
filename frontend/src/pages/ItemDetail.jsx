import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function ItemDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .get(`/api/items/${id}`)
      .then(setItem)
      .catch((err) => setError(err.message));
  }, [id]);

  const buy = async (skuId) => {
    if (!user) {
      navigate('/login');
      return;
    }
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const order = await api.post('/api/orders', {
        itemId: Number(id),
        skuId,
        quantity: 1,
      });
      setNotice(`Order #${order.id} created (status: ${order.status}). Go to My Orders to pay.`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  if (error && !item) return <p className="text-red-600">{error}</p>;
  if (!item) return <p className="text-slate-500">Loading…</p>;

  return (
    <div className="rounded-xl bg-white p-6 shadow">
      <div className="text-xs uppercase text-brand">{item.game}</div>
      <h1 className="text-2xl font-bold">{item.title}</h1>
      <p className="mb-1 text-slate-500">{item.category}</p>
      <p className="mb-4 text-slate-600">{item.description}</p>

      {notice && (
        <p className="mb-3 rounded bg-green-50 p-2 text-sm text-green-700">
          {notice} <Link to="/orders" className="font-semibold underline">Go to My Orders</Link>
        </p>
      )}
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

      <h2 className="mb-2 font-semibold">Variants</h2>
      <div className="divide-y">
        {item.skus.map((sku) => (
          <div key={sku.id} className="flex items-center justify-between py-3">
            <div>
              <div className="font-medium">{sku.spec}</div>
              <div className="text-sm text-slate-500">SKU #{sku.id}</div>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-lg font-bold">${Number(sku.price).toFixed(2)}</span>
              <button
                onClick={() => buy(sku.id)}
                disabled={busy}
                className="rounded bg-brand px-4 py-1.5 text-white hover:bg-brand-dark disabled:opacity-50"
              >
                Buy
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

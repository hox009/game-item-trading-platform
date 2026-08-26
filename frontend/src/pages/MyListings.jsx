import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';

export default function MyListings() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    try {
      const data = await api.get('/api/items/mine');
      setItems(data ?? []);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const toggle = async (item) => {
    const action = item.status === 'ON_SHELF' ? 'off-shelf' : 'on-shelf';
    setBusyId(item.id);
    setError('');
    try {
      await api.post(`/api/items/${item.id}/${action}`);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-bold">My Listings</h1>
        <Link to="/publish" className="rounded bg-brand px-4 py-2 text-sm text-white hover:bg-brand-dark">
          + Publish
        </Link>
      </div>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {items.length === 0 ? (
        <p className="text-slate-500">You haven't published anything yet.</p>
      ) : (
        <div className="space-y-3">
          {items.map((item) => (
            <div key={item.id} className="flex items-center justify-between rounded-xl bg-white p-4 shadow">
              <div>
                <Link to={`/items/${item.id}`} className="font-semibold hover:text-brand">
                  {item.title}
                </Link>
                <div className="text-sm text-slate-500">
                  {item.game} · {item.category} · {item.skus.length} SKU · from ${Number(item.minPrice).toFixed(2)}
                </div>
              </div>
              <div className="flex items-center gap-3">
                <span
                  className={
                    'rounded-full px-3 py-1 text-xs font-medium ' +
                    (item.status === 'ON_SHELF' ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600')
                  }
                >
                  {item.status}
                </span>
                <button
                  onClick={() => toggle(item)}
                  disabled={busyId === item.id}
                  className="rounded bg-slate-100 px-3 py-1 text-sm hover:bg-slate-200 disabled:opacity-50"
                >
                  {item.status === 'ON_SHELF' ? 'Take down' : 'Put on shelf'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

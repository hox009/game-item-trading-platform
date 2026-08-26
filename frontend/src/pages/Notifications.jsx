import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function Notifications() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const data = await api.get('/api/notifications');
      setItems(data ?? []);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const markRead = async (id) => {
    try {
      await api.post(`/api/notifications/${id}/read`);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-4 text-xl font-bold">Notifications</h1>
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      {items.length === 0 ? (
        <p className="text-slate-500">No notifications yet. Pay for an order to get one.</p>
      ) : (
        <div className="space-y-2">
          {items.map((n) => (
            <div
              key={n.id}
              className={`flex items-start justify-between rounded-xl bg-white p-4 shadow ${n.read ? 'opacity-60' : ''}`}
            >
              <div>
                <div className="font-semibold">
                  {!n.read && <span className="mr-2 inline-block h-2 w-2 rounded-full bg-brand align-middle" />}
                  {n.title}
                </div>
                <div className="text-sm text-slate-600">{n.content}</div>
              </div>
              {!n.read && (
                <button
                  onClick={() => markRead(n.id)}
                  className="rounded bg-slate-100 px-3 py-1 text-sm hover:bg-slate-200"
                >
                  Mark read
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

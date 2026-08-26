import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';

export default function Items() {
  const [items, setItems] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [game, setGame] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { size: 24 };
      if (keyword) params.keyword = keyword;
      if (game) params.game = game;
      if (maxPrice) params.maxPrice = maxPrice;
      const page = await api.get('/api/items', { params });
      setItems(page?.content ?? []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = (e) => {
    e.preventDefault();
    load();
  };

  return (
    <div>
      <form onSubmit={onSearch} className="mb-6 flex flex-wrap gap-2">
        <input
          className="flex-1 rounded border px-3 py-2"
          placeholder="Search title..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <input
          className="w-32 rounded border px-3 py-2"
          placeholder="Game"
          value={game}
          onChange={(e) => setGame(e.target.value)}
        />
        <input
          className="w-32 rounded border px-3 py-2"
          placeholder="Max price"
          type="number"
          value={maxPrice}
          onChange={(e) => setMaxPrice(e.target.value)}
        />
        <button className="rounded bg-brand px-4 py-2 text-white hover:bg-brand-dark">
          Search
        </button>
      </form>

      {error && <p className="text-red-600">{error}</p>}
      {loading ? (
        <p className="text-slate-500">Loading…</p>
      ) : items.length === 0 ? (
        <p className="text-slate-500">No items found.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item) => (
            <Link
              key={item.id}
              to={`/items/${item.id}`}
              className="rounded-xl bg-white p-4 shadow transition hover:shadow-md"
            >
              <div className="mb-1 text-xs uppercase text-brand">{item.game}</div>
              <div className="font-semibold">{item.title}</div>
              <div className="text-sm text-slate-500">{item.category}</div>
              <div className="mt-2 text-lg font-bold text-slate-800">
                from ${Number(item.minPrice).toFixed(2)}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

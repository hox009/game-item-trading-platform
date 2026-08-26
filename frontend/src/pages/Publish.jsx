import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function Publish() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [game, setGame] = useState('CS2');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [skus, setSkus] = useState([{ spec: '', price: '' }]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const updateSku = (i, field, value) => {
    setSkus((prev) => prev.map((s, idx) => (idx === i ? { ...s, [field]: value } : s)));
  };
  const addSku = () => setSkus((prev) => [...prev, { spec: '', price: '' }]);
  const removeSku = (i) => setSkus((prev) => prev.filter((_, idx) => idx !== i));

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const payload = {
        title,
        game,
        category,
        description,
        skus: skus
          .filter((s) => s.spec && s.price)
          .map((s) => ({ spec: s.spec, price: Number(s.price) })),
      };
      const item = await api.post('/api/items', payload);
      navigate(`/items/${item.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-lg rounded-xl bg-white p-6 shadow">
      <h1 className="mb-4 text-xl font-bold">Publish an item</h1>
      <form onSubmit={submit} className="space-y-3">
        <input className="w-full rounded border px-3 py-2" placeholder="Title"
          value={title} onChange={(e) => setTitle(e.target.value)} required />
        <div className="flex gap-2">
          <input className="w-1/2 rounded border px-3 py-2" placeholder="Game (e.g. CS2)"
            value={game} onChange={(e) => setGame(e.target.value)} required />
          <input className="w-1/2 rounded border px-3 py-2" placeholder="Category"
            value={category} onChange={(e) => setCategory(e.target.value)} required />
        </div>
        <textarea className="w-full rounded border px-3 py-2" rows="2" placeholder="Description"
          value={description} onChange={(e) => setDescription(e.target.value)} />

        <div>
          <div className="mb-1 flex items-center justify-between">
            <span className="font-semibold">SKU variants</span>
            <button type="button" onClick={addSku} className="text-sm text-brand hover:underline">
              + add variant
            </button>
          </div>
          {skus.map((s, i) => (
            <div key={i} className="mb-2 flex gap-2">
              <input className="flex-1 rounded border px-3 py-2" placeholder="Spec (e.g. Factory New)"
                value={s.spec} onChange={(e) => updateSku(i, 'spec', e.target.value)} />
              <input className="w-28 rounded border px-3 py-2" type="number" placeholder="Price"
                value={s.price} onChange={(e) => updateSku(i, 'price', e.target.value)} />
              {skus.length > 1 && (
                <button type="button" onClick={() => removeSku(i)}
                  className="rounded bg-slate-100 px-2 hover:bg-slate-200">✕</button>
              )}
            </div>
          ))}
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={busy}
          className="w-full rounded bg-brand py-2 text-white hover:bg-brand-dark disabled:opacity-50">
          {busy ? '...' : 'Publish'}
        </button>
      </form>
    </div>
  );
}

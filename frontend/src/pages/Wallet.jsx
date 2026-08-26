import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function Wallet() {
  const { user } = useAuth();
  const [balance, setBalance] = useState(null);
  const [amount, setAmount] = useState('100');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = async () => {
    try {
      const data = await api.get(`/api/payments/wallet/${user.userId}`);
      setBalance(data.balance);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const recharge = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const data = await api.post('/api/payments/recharge', {
        userId: user.userId,
        amount: Number(amount),
      });
      setBalance(data.balance);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-md">
      <h1 className="mb-4 text-xl font-bold">My Wallet</h1>
      <div className="mb-6 rounded-xl bg-gradient-to-br from-brand to-brand-dark p-6 text-white shadow">
        <div className="text-sm opacity-80">Balance</div>
        <div className="text-4xl font-bold">
          {balance === null ? '…' : `$${Number(balance).toFixed(2)}`}
        </div>
        <div className="mt-2 text-sm opacity-80">{user.username}</div>
      </div>

      <form onSubmit={recharge} className="rounded-xl bg-white p-4 shadow">
        <h2 className="mb-3 font-semibold">Recharge</h2>
        {error && <p className="mb-2 text-sm text-red-600">{error}</p>}
        <div className="flex gap-2">
          <input
            className="flex-1 rounded border px-3 py-2"
            type="number"
            min="1"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <button
            disabled={busy}
            className="rounded bg-brand px-4 py-2 text-white hover:bg-brand-dark disabled:opacity-50"
          >
            Add funds
          </button>
        </div>
        <div className="mt-3 flex gap-2">
          {[100, 500, 1000].map((v) => (
            <button
              key={v}
              type="button"
              onClick={() => setAmount(String(v))}
              className="rounded bg-slate-100 px-3 py-1 text-sm hover:bg-slate-200"
            >
              +${v}
            </button>
          ))}
        </div>
      </form>
    </div>
  );
}

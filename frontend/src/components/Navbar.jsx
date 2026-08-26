import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [balance, setBalance] = useState(null);

  // Refresh the wallet balance chip whenever the route changes.
  useEffect(() => {
    if (!user) {
      setBalance(null);
      return;
    }
    api
      .get(`/api/payments/wallet/${user.userId}`)
      .then((d) => setBalance(d.balance))
      .catch(() => setBalance(null));
  }, [user, location.pathname]);

  const handleLogout = () => {
    logout();
    navigate('/items');
  };

  return (
    <header className="bg-white shadow-sm">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <Link to="/items" className="text-lg font-bold text-brand-dark">
          🎮 Game Item Trading
        </Link>
        <div className="flex items-center gap-4 text-sm">
          <Link to="/items" className="hover:text-brand">Catalog</Link>
          {user && <Link to="/orders" className="hover:text-brand">My Orders</Link>}
          {user && <Link to="/wallet" className="hover:text-brand">Wallet</Link>}
          {user && <Link to="/notifications" className="hover:text-brand">Notifications</Link>}
          {user && user.role === 'SELLER' && (
            <Link to="/my-listings" className="hover:text-brand">My Listings</Link>
          )}
          {user && user.role === 'SELLER' && (
            <Link to="/publish" className="hover:text-brand">Publish</Link>
          )}
          {user ? (
            <>
              {balance !== null && (
                <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
                  ${Number(balance).toFixed(2)}
                </span>
              )}
              <span className="text-slate-500">
                {user.username} <span className="text-xs">({user.role})</span>
              </span>
              <button
                onClick={handleLogout}
                className="rounded bg-slate-100 px-3 py-1 hover:bg-slate-200"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="hover:text-brand">Login</Link>
              <Link
                to="/register"
                className="rounded bg-brand px-3 py-1 text-white hover:bg-brand-dark"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}

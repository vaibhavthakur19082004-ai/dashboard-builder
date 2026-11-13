import React, { useState } from 'react';
import Login from './components/Login.jsx';
import DashboardPage from './components/DashboardPage.jsx';

export default function App() {
  const [session, setSession] = useState(() => {
    try { return JSON.parse(localStorage.getItem('session')); } catch { return null; }
  });

  const onAuth = (res) => {
    localStorage.setItem('token', res.token);
    localStorage.setItem('session', JSON.stringify({ username: res.username, role: res.role }));
    setSession({ username: res.username, role: res.role });
  };
  const logout = () => {
    localStorage.clear();
    setSession(null);
  };

  return session ? <DashboardPage session={session} onLogout={logout} /> : <Login onAuth={onAuth} />;
}

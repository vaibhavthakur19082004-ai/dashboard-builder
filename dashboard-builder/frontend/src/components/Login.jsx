import React, { useState } from 'react';
import { api } from '../api.js';

export default function Login({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: '', password: '', workspaceName: '' });
  const [error, setError] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      onAuth(mode === 'login' ? await api.login(form) : await api.register(form));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="auth">
      <form className="card" onSubmit={submit}>
        <h1>Dashboard Builder</h1>
        <p className="muted">{mode === 'login' ? 'Sign in to your workspace' : 'Create a new workspace'}</p>
        <input placeholder="Username" value={form.username} onChange={set('username')} required />
        <input type="password" placeholder="Password (min 8 chars)" value={form.password} onChange={set('password')} required />
        {mode === 'register' && (
          <input placeholder="Workspace name" value={form.workspaceName} onChange={set('workspaceName')} required />
        )}
        {error && <div className="error">{error}</div>}
        <button className="primary">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <button type="button" className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'New here? Create a workspace' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}

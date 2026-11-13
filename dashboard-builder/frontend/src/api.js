const BASE = '/api';

export const getToken = () => localStorage.getItem('token');

async function request(path, opts = {}) {
  const token = getToken();
  const res = await fetch(BASE + path, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) {
    let msg = res.statusText;
    try {
      const b = await res.json();
      msg = b.details?.length ? `${b.message}: ${b.details.join(', ')}` : b.message || msg;
    } catch { /* ignore */ }
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  return res.status === 204 ? null : res.json();
}

const json = (method, body) => ({ method, body: JSON.stringify(body) });

export const api = {
  login: (b) => request('/auth/login', json('POST', b)),
  register: (b) => request('/auth/register', json('POST', b)),
  schema: () => request('/metrics/schema'),
  preview: (q) => request('/metrics/preview', json('POST', q)),
  dashboards: () => request('/dashboards'),
  dashboard: (id) => request(`/dashboards/${id}`),
  createDashboard: (name) => request('/dashboards', json('POST', { name })),
  deleteDashboard: (id) => request(`/dashboards/${id}`, { method: 'DELETE' }),
  addWidget: (id, w) => request(`/dashboards/${id}/widgets`, json('POST', w)),
  deleteWidget: (id) => request(`/widgets/${id}`, { method: 'DELETE' }),
  widgetData: (id) => request(`/widgets/${id}/data`),
  reorder: (id, widgetIds) => request(`/dashboards/${id}/widgets/order`, json('PUT', { widgetIds })),
  async download(id, format) {
    const res = await fetch(`${BASE}/dashboards/${id}/export?format=${format}`, {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (!res.ok) throw new Error('Export failed');
    const url = URL.createObjectURL(await res.blob());
    const a = document.createElement('a');
    a.href = url;
    a.download = `dashboard-${id}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

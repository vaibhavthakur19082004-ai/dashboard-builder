import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import Widget from './Widget.jsx';
import WidgetBuilder from './WidgetBuilder.jsx';

const PALETTE = [
  { type: 'BAR', label: '📊 Bar chart' },
  { type: 'LINE', label: '📈 Line graph' },
  { type: 'KPI', label: '🔢 KPI card' },
];

export default function DashboardPage({ session, onLogout }) {
  const canEdit = session.role !== 'VIEWER';
  const [list, setList] = useState([]);
  const [current, setCurrent] = useState(null);
  const [builderType, setBuilderType] = useState(null);
  const [dragId, setDragId] = useState(null);
  const [error, setError] = useState('');

  const guard = (fn) => async (...args) => {
    setError('');
    try { await fn(...args); } catch (e) { setError(e.message); }
  };

  const loadList = useCallback(async () => setList(await api.dashboards()), []);
  const open = guard(async (id) => setCurrent(await api.dashboard(id)));
  useEffect(() => { guard(loadList)(); }, [loadList]);

  const create = guard(async () => {
    const name = window.prompt('Dashboard name');
    if (!name?.trim()) return;
    const d = await api.createDashboard(name.trim());
    await loadList();
    await open(d.id);
  });
  const remove = guard(async () => {
    if (!window.confirm('Delete this dashboard?')) return;
    await api.deleteDashboard(current.id);
    setCurrent(null);
    await loadList();
  });
  const saveWidget = guard(async (w) => {
    await api.addWidget(current.id, w);
    setBuilderType(null);
    await open(current.id);
  });
  const deleteWidget = guard(async (id) => {
    await api.deleteWidget(id);
    await open(current.id);
  });
  const dropOnWidget = guard(async (targetId) => {
    if (dragId == null || dragId === targetId) return;
    const ids = current.widgets.map((w) => w.id);
    ids.splice(ids.indexOf(dragId), 1);
    ids.splice(ids.indexOf(targetId), 0, dragId);
    await api.reorder(current.id, ids);
    await open(current.id);
  });

  return (
    <div className="layout">
      <aside>
        <h3>Dashboards</h3>
        {list.map((d) => (
          <button key={d.id} className={current?.id === d.id ? 'nav active' : 'nav'} onClick={() => open(d.id)}>{d.name}</button>
        ))}
        {canEdit && <button className="primary" onClick={create}>+ New dashboard</button>}
        <div className="spacer" />
        <div className="muted">{session.username} · {session.role}</div>
        <button className="link" onClick={onLogout}>Log out</button>
      </aside>

      <main>
        {error && <div className="error">{error}</div>}
        {!current ? <p className="muted">Select or create a dashboard.</p> : (
          <>
            <div className="toolbar">
              <h2>{current.name}</h2>
              <div>
                <button onClick={guard(() => api.download(current.id, 'csv'))}>Export CSV</button>
                <button onClick={guard(() => api.download(current.id, 'xlsx'))}>Export Excel</button>
                {canEdit && <button className="danger" onClick={remove}>Delete</button>}
              </div>
            </div>

            {canEdit && (
              <div className="palette">
                <span className="muted">Drag a chart type onto the canvas:</span>
                {PALETTE.map((p) => (
                  <div key={p.type} className="chip" draggable
                       onDragStart={(e) => e.dataTransfer.setData('chartType', p.type)}>{p.label}</div>
                ))}
              </div>
            )}

            <div className="canvas"
                 onDragOver={(e) => e.preventDefault()}
                 onDrop={(e) => {
                   const t = e.dataTransfer.getData('chartType');
                   if (t && canEdit) setBuilderType(t);
                 }}>
              {current.widgets.length === 0 && <p className="muted">Empty dashboard – drop a chart here.</p>}
              {current.widgets.map((w) => (
                <Widget key={w.id} widget={w} canEdit={canEdit} onDelete={deleteWidget}
                        dragProps={canEdit ? {
                          draggable: true,
                          onDragStart: () => setDragId(w.id),
                          onDragOver: (e) => e.preventDefault(),
                          onDrop: (e) => { e.stopPropagation(); dropOnWidget(w.id); },
                        } : {}} />
              ))}
            </div>
          </>
        )}
      </main>

      {builderType && <WidgetBuilder initialType={builderType} onSave={saveWidget} onCancel={() => setBuilderType(null)} />}
    </div>
  );
}

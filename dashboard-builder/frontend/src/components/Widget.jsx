import React, { useEffect, useState } from 'react';
import { ResponsiveContainer, BarChart, Bar, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';
import { api } from '../api.js';

/** Pure renderer: used both by saved widgets and by the builder's live preview. */
export function Chart({ type, data }) {
  if (!data) return <div className="muted">Loading…</div>;
  if (type === 'KPI') {
    const total = data.reduce((s, p) => s + Number(p.value), 0);
    return <div className="kpi">{total.toLocaleString(undefined, { maximumFractionDigits: 2 })}</div>;
  }
  const Cmp = type === 'LINE' ? LineChart : BarChart;
  return (
    <ResponsiveContainer width="100%" height={220}>
      <Cmp data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="label" />
        <YAxis />
        <Tooltip />
        {type === 'LINE' ? <Line dataKey="value" stroke="#4f46e5" strokeWidth={2} /> : <Bar dataKey="value" fill="#4f46e5" />}
      </Cmp>
    </ResponsiveContainer>
  );
}

export default function Widget({ widget, canEdit, onDelete, dragProps }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.widgetData(widget.id).then(setData).catch((e) => setError(e.message));
  }, [widget.id]);

  return (
    <div className="widget" {...dragProps}>
      <div className="widget-head">
        <strong>{widget.title}</strong>
        {canEdit && <button className="link danger" onClick={() => onDelete(widget.id)}>Delete</button>}
      </div>
      {error ? <div className="error">{error}</div> : <Chart type={widget.chartType} data={data} />}
    </div>
  );
}

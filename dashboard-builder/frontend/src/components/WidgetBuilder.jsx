import React, { useEffect, useState } from 'react';
import { api } from '../api.js';
import { Chart } from './Widget.jsx';

export default function WidgetBuilder({ initialType, onSave, onCancel }) {
  const [schema, setSchema] = useState(null);
  const [title, setTitle] = useState('');
  const [chartType, setChartType] = useState(initialType);
  const [aggregation, setAggregation] = useState('SUM');
  const [measure, setMeasure] = useState('amount');
  const [dimension, setDimension] = useState('region');
  const [filter, setFilter] = useState({ field: '', op: 'EQ', value: '' });
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => { api.schema().then(setSchema); }, []);

  const query = {
    dimension: chartType === 'KPI' ? null : dimension,
    aggregation,
    measure: aggregation === 'COUNT' ? null : measure,
    filters: filter.field && filter.value !== '' ? [filter] : [],
  };

  useEffect(() => {
    if (!schema) return;
    setError('');
    api.preview(query).then(setPreview).catch((e) => { setPreview(null); setError(e.message); });
  }, [schema, chartType, aggregation, measure, dimension, filter.field, filter.op, filter.value]);

  if (!schema) return null;
  const sel = (value, set, options) => (
    <select value={value} onChange={(e) => set(e.target.value)}>{options.map((o) => <option key={o}>{o}</option>)}</select>
  );

  return (
    <div className="modal">
      <div className="card builder">
        <h2>New widget</h2>
        <input placeholder="Widget title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <div className="row">
          <label>Chart {sel(chartType, setChartType, ['BAR', 'LINE', 'KPI'])}</label>
          <label>Aggregation {sel(aggregation, setAggregation, schema.aggregations)}</label>
          {aggregation !== 'COUNT' && <label>Measure {sel(measure, setMeasure, schema.measures)}</label>}
          {chartType !== 'KPI' && <label>Group by {sel(dimension, setDimension, schema.dimensions)}</label>}
        </div>
        <div className="row">
          <label>Filter
            <select value={filter.field} onChange={(e) => setFilter({ ...filter, field: e.target.value })}>
              <option value="">(none)</option>
              {schema.filterFields.map((f) => <option key={f}>{f}</option>)}
            </select>
          </label>
          {filter.field && sel(filter.op, (op) => setFilter({ ...filter, op }), schema.operators)}
          {filter.field && <input placeholder="value" value={filter.value} onChange={(e) => setFilter({ ...filter, value: e.target.value })} />}
        </div>
        <div className="preview">
          {error ? <div className="error">{error}</div> : <Chart type={chartType} data={preview} />}
        </div>
        <div className="row end">
          <button onClick={onCancel}>Cancel</button>
          <button className="primary" disabled={!title.trim() || !!error}
                  onClick={() => onSave({ title: title.trim(), chartType, query })}>Add to dashboard</button>
        </div>
      </div>
    </div>
  );
}

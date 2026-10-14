import React, { useState } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { api } from '../api';

export default function AddTransactionModal({
  isOpen,
  onClose,
  categories,
  onSuccess,
}) {
  const todayStr = new Date().toISOString().split('T')[0];

  const [formData, setFormData] = useState({
    amount: '',
    date: todayStr,
    category: categories && categories.length > 0 ? categories[0].name : 'Salary',
    description: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (Number(formData.amount) <= 0) {
        throw new Error('Amount must be a positive decimal value.');
      }
      if (formData.date > todayStr) {
        throw new Error('Date cannot be a future date.');
      }
      if (!formData.category) {
        throw new Error('Please select a category.');
      }

      await api.transactions.create({
        amount: Number(formData.amount),
        date: formData.date,
        category: formData.category,
        description: formData.description.trim() || undefined,
      });

      // Reset and close
      setFormData({
        amount: '',
        date: todayStr,
        category: categories && categories.length > 0 ? categories[0].name : 'Salary',
        description: '',
      });
      onClose();
      if (onSuccess) await onSuccess();
    } catch (err) {
      setError(err.message || 'Failed to create transaction.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-dialog">
        <div className="modal-header">
          <h3 className="modal-title">Record Transaction</h3>
          <button onClick={onClose} className="modal-close">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && (
              <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Amount (₹)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="e.g. 5000.00"
                value={formData.amount}
                onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                className="form-input"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Transaction Date (Cannot be future date)</label>
              <input
                type="date"
                required
                max={todayStr}
                value={formData.date}
                onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                className="form-input"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Category</label>
              <select
                required
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                className="form-select"
              >
                {categories.map((c) => (
                  <option key={c.name} value={c.name}>
                    {c.name} ({c.type})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Description (Optional)</label>
              <input
                type="text"
                placeholder="e.g. Monthly groceries, Freelance design project"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="form-input"
              />
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              onClick={onClose}
              className="btn btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Recording...' : 'Add Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

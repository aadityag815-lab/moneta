import React, { useState } from 'react';
import {
  Plus,
  Filter,
  Edit2,
  Trash2,
  Search,
  RotateCcw,
  AlertCircle,
  X,
  Lock,
} from 'lucide-react';
import { api } from '../api';
import { formatCurrency, formatDate } from '../utils/formatters';

export default function Transactions({
  transactions,
  categories,
  onRefresh,
  onOpenAdd,
}) {
  // Filter state
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Edit modal state
  const [editingTx, setEditingTx] = useState(null);
  const [editForm, setEditForm] = useState({ amount: '', description: '', category: '' });
  const [editError, setEditError] = useState(null);
  const [editLoading, setEditLoading] = useState(false);

  // Delete modal state
  const [deletingTx, setDeletingTx] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const resetFilters = () => {
    setStartDate('');
    setEndDate('');
    setSelectedCategory('');
    setSelectedType('');
    setSearchQuery('');
  };

  // Client-side quick filter + server filters passed to parent if needed
  const filteredTransactions = transactions.filter((t) => {
    if (startDate && t.date < startDate) return false;
    if (endDate && t.date > endDate) return false;
    if (selectedCategory && t.category !== selectedCategory) return false;
    if (selectedType && t.type !== selectedType) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const matchDesc = (t.description || '').toLowerCase().includes(q);
      const matchCat = (t.category || '').toLowerCase().includes(q);
      if (!matchDesc && !matchCat) return false;
    }
    return true;
  });

  const handleStartEdit = (tx) => {
    setEditingTx(tx);
    setEditForm({
      amount: tx.amount,
      description: tx.description || '',
      category: tx.category,
    });
    setEditError(null);
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    setEditLoading(true);
    setEditError(null);

    try {
      if (Number(editForm.amount) <= 0) {
        throw new Error('Amount must be a positive decimal value.');
      }
      await api.transactions.update(editingTx.id, {
        amount: Number(editForm.amount),
        description: editForm.description,
        category: editForm.category,
      });
      setEditingTx(null);
      await onRefresh();
    } catch (err) {
      setEditError(err.message || 'Failed to update transaction.');
    } finally {
      setEditLoading(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingTx) return;
    setDeleteLoading(true);
    setDeleteError(null);

    try {
      await api.transactions.delete(deletingTx.id);
      setDeletingTx(null);
      await onRefresh();
    } catch (err) {
      setDeleteError(err.message || 'Failed to delete transaction.');
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <div>
      {/* Top Controls & Filter Card */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '16px', marginBottom: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={18} style={{ color: '#047857' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: '600', color: '#0f172a' }}>Filter Transactions</h3>
          </div>
          <div style={{ display: 'flex', gap: '12px' }}>
            <button onClick={resetFilters} className="btn btn-secondary btn-sm">
              <RotateCcw size={14} />
              Reset Filters
            </button>
            <button onClick={onOpenAdd} className="btn btn-primary btn-sm">
              <Plus size={16} />
              Add Transaction
            </button>
          </div>
        </div>

        {/* Filter controls row */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px' }}>
          <div>
            <label className="form-label" style={{ fontSize: '0.8rem' }}>Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="form-input"
              style={{ padding: '8px 12px' }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '0.8rem' }}>End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="form-input"
              style={{ padding: '8px 12px' }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '0.8rem' }}>Category</label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="form-select"
              style={{ padding: '8px 12px' }}
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c.name} value={c.name}>
                  {c.name} ({c.type})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '0.8rem' }}>Type</label>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              className="form-select"
              style={{ padding: '8px 12px' }}
            >
              <option value="">All Types</option>
              <option value="INCOME">Income Only</option>
              <option value="EXPENSE">Expense Only</option>
            </select>
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '0.8rem' }}>Search</label>
            <div style={{ position: 'relative' }}>
              <Search size={16} style={{ position: 'absolute', left: '10px', top: '10px', color: '#94a3b8' }} />
              <input
                type="text"
                placeholder="Search description..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="form-input"
                style={{ padding: '8px 12px 8px 32px' }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Transactions Table Card */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">All Transactions</h3>
            <p className="card-subtitle">
              Showing {filteredTransactions.length} of {transactions.length} total entries (sorted newest first)
            </p>
          </div>
        </div>

        {filteredTransactions.length > 0 ? (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Type</th>
                  <th style={{ textAlign: 'right' }}>Amount</th>
                  <th style={{ textAlign: 'center', width: '100px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.map((tx) => (
                  <tr key={tx.id}>
                    <td style={{ whiteSpace: 'nowrap', color: '#64748b' }}>
                      {formatDate(tx.date)}
                    </td>
                    <td>
                      <span className={`badge ${tx.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                        {tx.category}
                      </span>
                    </td>
                    <td style={{ color: '#0f172a', fontWeight: '500' }}>
                      {tx.description || '—'}
                    </td>
                    <td>
                      <span style={{ fontSize: '0.75rem', fontWeight: '600', color: tx.type === 'INCOME' ? '#047857' : '#dc2626' }}>
                        {tx.type}
                      </span>
                    </td>
                    <td style={{
                      textAlign: 'right',
                      fontWeight: '600',
                      color: tx.type === 'INCOME' ? '#047857' : '#dc2626',
                      whiteSpace: 'nowrap',
                    }}>
                      {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
                    </td>
                    <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          onClick={() => handleStartEdit(tx)}
                          className="btn btn-ghost btn-sm"
                          title="Edit transaction"
                          style={{ padding: '4px 6px' }}
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          onClick={() => setDeletingTx(tx)}
                          className="btn btn-ghost btn-sm"
                          title="Delete transaction"
                          style={{ padding: '4px 6px', color: '#dc2626' }}
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <p className="empty-state-title">No transactions found</p>
            <p className="empty-state-desc">
              {transactions.length === 0
                ? 'Record your first transaction to get started.'
                : 'No transactions match the selected filters.'}
            </p>
            {transactions.length === 0 ? (
              <button onClick={onOpenAdd} className="btn btn-primary btn-sm">
                <Plus size={16} />
                Add Transaction
              </button>
            ) : (
              <button onClick={resetFilters} className="btn btn-secondary btn-sm">
                Clear Filters
              </button>
            )}
          </div>
        )}
      </div>

      {/* Edit Transaction Modal */}
      {editingTx && (
        <div className="modal-overlay">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3 className="modal-title">Edit Transaction</h3>
              <button onClick={() => setEditingTx(null)} className="modal-close">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveEdit}>
              <div className="modal-body">
                {editError && (
                  <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                    {editError}
                  </div>
                )}

                {/* Locked Date field notice */}
                <div className="form-group">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <label className="form-label" style={{ margin: 0 }}>Transaction Date</label>
                    <span style={{ fontSize: '0.75rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Lock size={12} /> Date cannot be modified
                    </span>
                  </div>
                  <input
                    type="date"
                    disabled
                    value={editingTx.date}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={editForm.amount}
                    onChange={(e) => setEditForm({ ...editForm, amount: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Category</label>
                  <select
                    required
                    value={editForm.category}
                    onChange={(e) => setEditForm({ ...editForm, category: e.target.value })}
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
                    value={editForm.description}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                    className="form-input"
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setEditingTx(null)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={editLoading}
                  className="btn btn-primary"
                >
                  {editLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingTx && (
        <div className="modal-overlay">
          <div className="modal-dialog" style={{ maxWidth: '420px' }}>
            <div className="modal-header">
              <h3 className="modal-title">Delete Transaction</h3>
              <button onClick={() => setDeletingTx(null)} className="modal-close">
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              {deleteError && (
                <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                  {deleteError}
                </div>
              )}
              <div style={{ display: 'flex', gap: '14px', alignItems: 'flex-start' }}>
                <div style={{
                  padding: '8px',
                  borderRadius: '8px',
                  backgroundColor: '#fef2f2',
                  color: '#dc2626',
                }}>
                  <AlertCircle size={22} />
                </div>
                <div>
                  <p style={{ fontWeight: '500', color: '#0f172a', marginBottom: '4px' }}>
                    Are you sure you want to delete this transaction?
                  </p>
                  <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    {deletingTx.category} — {formatCurrency(deletingTx.amount)} on {formatDate(deletingTx.date)}.
                    This will update your reports and savings goals progress.
                  </p>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button
                onClick={() => setDeletingTx(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmDelete}
                disabled={deleteLoading}
                className="btn btn-danger"
              >
                {deleteLoading ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

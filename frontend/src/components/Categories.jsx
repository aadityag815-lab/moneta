import React, { useState } from 'react';
import {
  FolderTree,
  Plus,
  Trash2,
  Lock,
  Tag,
  AlertCircle,
  CheckCircle2,
  TrendingUp,
  TrendingDown,
} from 'lucide-react';
import { api } from '../api';

export default function Categories({ categories, onRefresh }) {
  const [name, setName] = useState('');
  const [type, setType] = useState('EXPENSE');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [deletingCat, setDeletingCat] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const defaultCategories = categories.filter((c) => !c.isCustom);
  const customCategories = categories.filter((c) => c.isCustom);

  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;

    setLoading(true);
    setError(null);
    setSuccess(null);

    try {
      await api.categories.create({
        name: name.trim(),
        type,
      });
      setSuccess(`Custom category "${name.trim()}" created successfully!`);
      setName('');
      await onRefresh();
    } catch (err) {
      setError(err.message || 'Failed to create category.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCategory = async () => {
    if (!deletingCat) return;

    setDeleteLoading(true);
    setDeleteError(null);

    try {
      await api.categories.delete(deletingCat.name);
      setDeletingCat(null);
      await onRefresh();
    } catch (err) {
      setDeleteError(err.message || 'Failed to delete category.');
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '24px', marginBottom: '28px' }}>
        {/* Create Category Card */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Create Custom Category</h3>
              <p className="card-subtitle">Add your own income or expense classifications</p>
            </div>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              backgroundColor: '#ecfdf5',
              color: '#047857',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Plus size={20} />
            </div>
          </div>

          {error && (
            <div className="alert alert-error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="alert alert-success">
              <CheckCircle2 size={16} />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleCreateCategory}>
            <div className="form-group">
              <label className="form-label">Category Name</label>
              <input
                type="text"
                required
                placeholder="e.g. Freelance, Investments, Subscriptions"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="form-input"
              />
            </div>

            <div className="form-group">
              <label className="form-label">Category Type</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <label style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '12px',
                  borderRadius: '10px',
                  border: `1.5px solid ${type === 'INCOME' ? '#047857' : '#e2e8f0'}`,
                  backgroundColor: type === 'INCOME' ? '#ecfdf5' : '#ffffff',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}>
                  <input
                    type="radio"
                    name="categoryType"
                    value="INCOME"
                    checked={type === 'INCOME'}
                    onChange={() => setType('INCOME')}
                    style={{ accentColor: '#047857' }}
                  />
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <TrendingUp size={16} color="#047857" />
                    <span style={{ fontSize: '0.875rem', fontWeight: '600', color: type === 'INCOME' ? '#047857' : '#0f172a' }}>
                      INCOME
                    </span>
                  </div>
                </label>

                <label style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '12px',
                  borderRadius: '10px',
                  border: `1.5px solid ${type === 'EXPENSE' ? '#dc2626' : '#e2e8f0'}`,
                  backgroundColor: type === 'EXPENSE' ? '#fef2f2' : '#ffffff',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}>
                  <input
                    type="radio"
                    name="categoryType"
                    value="EXPENSE"
                    checked={type === 'EXPENSE'}
                    onChange={() => setType('EXPENSE')}
                    style={{ accentColor: '#dc2626' }}
                  />
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <TrendingDown size={16} color="#dc2626" />
                    <span style={{ fontSize: '0.875rem', fontWeight: '600', color: type === 'EXPENSE' ? '#dc2626' : '#0f172a' }}>
                      EXPENSE
                    </span>
                  </div>
                </label>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || !name.trim()}
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '6px' }}
            >
              {loading ? 'Creating...' : 'Create Category'}
            </button>
          </form>
        </div>

        {/* Custom Categories List */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Custom Categories</h3>
              <p className="card-subtitle">Categories created by you ({customCategories.length})</p>
            </div>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              backgroundColor: '#f0fdf4',
              color: '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Tag size={18} />
            </div>
          </div>

          {customCategories.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {customCategories.map((c) => (
                <div
                  key={c.name}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: '10px',
                    border: '1px solid #e2e8f0',
                    backgroundColor: '#fafaf9',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ fontWeight: '600', color: '#0f172a' }}>{c.name}</span>
                    <span className={`badge ${c.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                      {c.type}
                    </span>
                  </div>
                  <button
                    onClick={() => { setDeletingCat(c); setDeleteError(null); }}
                    className="btn btn-ghost btn-sm"
                    style={{ color: '#dc2626', padding: '6px 8px' }}
                    title="Delete custom category"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '36px 12px' }}>
              <p className="empty-state-title">No custom categories yet</p>
              <p className="empty-state-desc">Use the form on the left to add your custom categories.</p>
            </div>
          )}
        </div>
      </div>

      {/* Default Predefined Categories */}
      <div className="card">
        <div className="card-header">
          <div>
            <h3 className="card-title">Default System Categories</h3>
            <p className="card-subtitle">
              Predefined categories provided by the platform (protected from modification or deletion)
            </p>
          </div>
          <span className="badge badge-system" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Lock size={12} />
            Read Only
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '14px' }}>
          {defaultCategories.map((c) => (
            <div
              key={c.name}
              style={{
                padding: '14px 18px',
                borderRadius: '10px',
                border: '1px solid #e2e8f0',
                backgroundColor: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div>
                <div style={{ fontWeight: '600', color: '#0f172a', marginBottom: '4px' }}>{c.name}</div>
                <span className={`badge ${c.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                  {c.type}
                </span>
              </div>
              <Lock size={15} style={{ color: '#94a3b8' }} />
            </div>
          ))}
        </div>
      </div>

      {/* Delete Category Modal */}
      {deletingCat && (
        <div className="modal-overlay">
          <div className="modal-dialog" style={{ maxWidth: '420px' }}>
            <div className="modal-header">
              <h3 className="modal-title">Delete Category</h3>
              <button onClick={() => setDeletingCat(null)} className="modal-close">
                ×
              </button>
            </div>
            <div className="modal-body">
              {deleteError && (
                <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                  <AlertCircle size={16} />
                  <span>{deleteError}</span>
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
                    Delete category "{deletingCat.name}"?
                  </p>
                  <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    Note: Categories currently referenced by transactions cannot be deleted.
                  </p>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button
                onClick={() => setDeletingCat(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteCategory}
                disabled={deleteLoading}
                className="btn btn-danger"
              >
                {deleteLoading ? 'Deleting...' : 'Delete Category'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

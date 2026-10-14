import React, { useState } from 'react';
import {
  Target,
  Plus,
  Edit2,
  Trash2,
  Calendar,
  AlertCircle,
  X,
  TrendingUp,
} from 'lucide-react';
import { api } from '../api';
import { formatCurrency, formatDate, formatPercentage } from '../utils/formatters';

export default function Goals({ goals, onRefresh }) {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [addForm, setAddForm] = useState({
    goalName: '',
    targetAmount: '',
    targetDate: '',
    startDate: new Date().toISOString().split('T')[0],
  });
  const [addError, setAddError] = useState(null);
  const [addLoading, setAddLoading] = useState(false);

  // Edit modal
  const [editingGoal, setEditingGoal] = useState(null);
  const [editForm, setEditForm] = useState({ targetAmount: '', targetDate: '', goalName: '' });
  const [editError, setEditError] = useState(null);
  const [editLoading, setEditLoading] = useState(false);

  // Delete modal
  const [deletingGoal, setDeletingGoal] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const todayStr = new Date().toISOString().split('T')[0];

  const handleCreateGoal = async (e) => {
    e.preventDefault();
    setAddLoading(true);
    setAddError(null);

    try {
      if (Number(addForm.targetAmount) <= 0) {
        throw new Error('Target amount must be a positive decimal value.');
      }
      if (addForm.targetDate <= todayStr) {
        throw new Error('Target date must be a future date.');
      }

      await api.goals.create({
        goalName: addForm.goalName.trim(),
        targetAmount: Number(addForm.targetAmount),
        targetDate: addForm.targetDate,
        startDate: addForm.startDate || todayStr,
      });

      setIsAddOpen(false);
      setAddForm({
        goalName: '',
        targetAmount: '',
        targetDate: '',
        startDate: todayStr,
      });
      await onRefresh();
    } catch (err) {
      setAddError(err.message || 'Failed to create savings goal.');
    } finally {
      setAddLoading(false);
    }
  };

  const handleStartEdit = (goal) => {
    setEditingGoal(goal);
    setEditForm({
      targetAmount: goal.targetAmount,
      targetDate: goal.targetDate,
      goalName: goal.goalName,
    });
    setEditError(null);
  };

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    setEditLoading(true);
    setEditError(null);

    try {
      if (Number(editForm.targetAmount) <= 0) {
        throw new Error('Target amount must be a positive decimal value.');
      }
      if (editForm.targetDate <= todayStr) {
        throw new Error('Target date must be a future date.');
      }

      await api.goals.update(editingGoal.id, {
        targetAmount: Number(editForm.targetAmount),
        targetDate: editForm.targetDate,
        goalName: editForm.goalName,
      });

      setEditingGoal(null);
      await onRefresh();
    } catch (err) {
      setEditError(err.message || 'Failed to update savings goal.');
    } finally {
      setEditLoading(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!deletingGoal) return;
    setDeleteLoading(true);
    setDeleteError(null);

    try {
      await api.goals.delete(deletingGoal.id);
      setDeletingGoal(null);
      await onRefresh();
    } catch (err) {
      setDeleteError(err.message || 'Failed to delete savings goal.');
    } finally {
      setDeleteLoading(false);
    }
  };

  return (
    <div>
      {/* Header action */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: '700', color: '#0f172a' }}>Savings Goals</h2>
          <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
            Set financial milestones and track your accumulated net savings
          </p>
        </div>
        <button onClick={() => { setIsAddOpen(true); setAddError(null); }} className="btn btn-primary">
          <Plus size={18} />
          Create New Goal
        </button>
      </div>

      {/* Goals Grid */}
      {goals && goals.length > 0 ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
          {goals.map((goal) => {
            const rawPct = Number(goal.progressPercentage || 0);
            const clampedPct = Math.max(0, Math.min(100, rawPct));
            const isCompleted = rawPct >= 100;

            return (
              <div key={goal.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '10px',
                      backgroundColor: isCompleted ? '#ecfdf5' : '#f0fdf4',
                      color: isCompleted ? '#059669' : '#047857',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}>
                      <Target size={22} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '1.05rem', fontWeight: '600', color: '#0f172a' }}>
                        {goal.goalName}
                      </h4>
                      <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                        Tracking since {formatDate(goal.startDate)}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '4px' }}>
                    <button
                      onClick={() => handleStartEdit(goal)}
                      className="btn btn-ghost btn-sm"
                      title="Edit Goal"
                    >
                      <Edit2 size={15} />
                    </button>
                    <button
                      onClick={() => { setDeletingGoal(goal); setDeleteError(null); }}
                      className="btn btn-ghost btn-sm"
                      style={{ color: '#dc2626' }}
                      title="Delete Goal"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>

                {/* Amount and percentage */}
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '8px' }}>
                    <div>
                      <span style={{ fontSize: '1.45rem', fontWeight: '700', color: '#047857' }}>
                        {formatCurrency(goal.currentProgress)}
                      </span>
                      <span style={{ fontSize: '0.85rem', color: '#64748b', marginLeft: '6px' }}>
                        of {formatCurrency(goal.targetAmount)}
                      </span>
                    </div>
                    <span style={{
                      fontWeight: '700',
                      fontSize: '0.95rem',
                      color: rawPct < 0 ? '#dc2626' : '#047857',
                    }}>
                      {rawPct}%
                    </span>
                  </div>

                  {/* Visual Progress Bar */}
                  <div className="progress-container" style={{ height: '10px' }}>
                    <div
                      className="progress-bar-fill"
                      style={{
                        width: `${clampedPct}%`,
                        background: rawPct < 0 ? '#ef4444' : 'linear-gradient(90deg, #059669, #10b981)',
                      }}
                    />
                  </div>
                </div>

                {/* Footer stats: Remaining Amount and Target Date */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingTop: '12px',
                  borderTop: '1px solid #e2e8f0',
                  fontSize: '0.825rem',
                  color: '#64748b',
                }}>
                  <div>
                    <span>Remaining: </span>
                    <strong style={{ color: '#0f172a' }}>{formatCurrency(goal.remainingAmount)}</strong>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Calendar size={14} />
                    <span>Target: {formatDate(goal.targetDate)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="card">
          <div className="empty-state">
            <Target className="empty-state-icon" style={{ width: '56px', height: '56px' }} />
            <p className="empty-state-title" style={{ fontSize: '1.15rem' }}>No Savings Goals Created</p>
            <p className="empty-state-desc">
              Create savings goals to automatically track your savings progress against real income and expenses.
            </p>
            <button onClick={() => setIsAddOpen(true)} className="btn btn-primary">
              <Plus size={18} />
              Set Your First Goal
            </button>
          </div>
        </div>
      )}

      {/* Create Goal Modal */}
      {isAddOpen && (
        <div className="modal-overlay">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3 className="modal-title">Create Savings Goal</h3>
              <button onClick={() => setIsAddOpen(false)} className="modal-close">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateGoal}>
              <div className="modal-body">
                {addError && (
                  <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                    <AlertCircle size={16} />
                    <span>{addError}</span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Goal Name</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Emergency Fund, New Car, Vacation"
                    value={addForm.goalName}
                    onChange={(e) => setAddForm({ ...addForm, goalName: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Target Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    placeholder="50000.00"
                    value={addForm.targetAmount}
                    onChange={(e) => setAddForm({ ...addForm, targetAmount: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Target Date (Future Date)</label>
                  <input
                    type="date"
                    required
                    min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
                    value={addForm.targetDate}
                    onChange={(e) => setAddForm({ ...addForm, targetDate: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Tracking Start Date (Defaults to Today)</label>
                  <input
                    type="date"
                    value={addForm.startDate}
                    onChange={(e) => setAddForm({ ...addForm, startDate: e.target.value })}
                    className="form-input"
                  />
                  <span style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px', display: 'block' }}>
                    Calculates: Total Income − Total Expenses since this date.
                  </span>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setIsAddOpen(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={addLoading}
                  className="btn btn-primary"
                >
                  {addLoading ? 'Creating...' : 'Create Goal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Goal Modal */}
      {editingGoal && (
        <div className="modal-overlay">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3 className="modal-title">Update Savings Goal</h3>
              <button onClick={() => setEditingGoal(null)} className="modal-close">
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveEdit}>
              <div className="modal-body">
                {editError && (
                  <div className="alert alert-error" style={{ marginBottom: '16px' }}>
                    <AlertCircle size={16} />
                    <span>{editError}</span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Goal Name</label>
                  <input
                    type="text"
                    value={editForm.goalName}
                    onChange={(e) => setEditForm({ ...editForm, goalName: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Target Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={editForm.targetAmount}
                    onChange={(e) => setEditForm({ ...editForm, targetAmount: e.target.value })}
                    className="form-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Target Date (Future Date)</label>
                  <input
                    type="date"
                    required
                    min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
                    value={editForm.targetDate}
                    onChange={(e) => setEditForm({ ...editForm, targetDate: e.target.value })}
                    className="form-input"
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setEditingGoal(null)}
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

      {/* Delete Goal Modal */}
      {deletingGoal && (
        <div className="modal-overlay">
          <div className="modal-dialog" style={{ maxWidth: '420px' }}>
            <div className="modal-header">
              <h3 className="modal-title">Delete Goal</h3>
              <button onClick={() => setDeletingGoal(null)} className="modal-close">
                <X size={20} />
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
                    Delete "{deletingGoal.goalName}"?
                  </p>
                  <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    This will permanently delete this savings goal. Your transactions will not be affected.
                  </p>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button
                onClick={() => setDeletingGoal(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmDelete}
                disabled={deleteLoading}
                className="btn btn-danger"
              >
                {deleteLoading ? 'Deleting...' : 'Delete Goal'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

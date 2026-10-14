import React from 'react';
import { Menu, Plus, Calendar } from 'lucide-react';

export default function Header({
  activeTab,
  onOpenAddTransaction,
  setMobileOpen,
}) {
  const titles = {
    overview: 'Financial Overview',
    transactions: 'Transaction History',
    categories: 'Category Management',
    goals: 'Savings Goals',
    reports: 'Reports & Analytics',
  };

  const todayStr = new Date().toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });

  return (
    <header className="top-header">
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <button
          onClick={() => setMobileOpen((prev) => !prev)}
          className="mobile-toggle"
          aria-label="Toggle menu"
        >
          <Menu size={22} />
        </button>
        <h2 className="top-header-title">{titles[activeTab] || 'Dashboard'}</h2>
      </div>

      <div className="top-header-actions">
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontSize: '0.85rem',
          color: '#64748b',
          backgroundColor: '#f8fafc',
          padding: '6px 12px',
          borderRadius: '8px',
          border: '1px solid #e2e8f0',
        }}>
          <Calendar size={16} />
          <span>{todayStr}</span>
        </div>

        <button
          onClick={onOpenAddTransaction}
          className="btn btn-primary btn-sm"
          style={{ height: '36px' }}
        >
          <Plus size={16} />
          <span>New Transaction</span>
        </button>
      </div>
    </header>
  );
}

import React from 'react';
import {
  LayoutDashboard,
  ArrowLeftRight,
  FolderTree,
  Target,
  BarChart3,
  LogOut,
  Wallet,
  X,
} from 'lucide-react';

export default function Sidebar({
  activeTab,
  setActiveTab,
  user,
  onLogout,
  mobileOpen,
  setMobileOpen,
}) {
  const navItems = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'transactions', label: 'Transactions', icon: ArrowLeftRight },
    { id: 'categories', label: 'Categories', icon: FolderTree },
    { id: 'goals', label: 'Savings Goals', icon: Target },
    { id: 'reports', label: 'Reports & Analytics', icon: BarChart3 },
  ];

  const handleSelect = (id) => {
    setActiveTab(id);
    if (setMobileOpen) setMobileOpen(false);
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  return (
    <aside className={`sidebar ${mobileOpen ? 'open' : ''}`}>
      <div className="sidebar-header">
        <div className="brand-icon">
          <Wallet size={22} />
        </div>
        <div style={{ flex: 1 }}>
          <div className="brand-title">Moneta</div>
          <div className="brand-subtitle">Finance Manager</div>
        </div>
        {mobileOpen && (
          <button
            onClick={() => setMobileOpen(false)}
            className="mobile-toggle"
            aria-label="Close sidebar"
          >
            <X size={20} />
          </button>
        )}
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => handleSelect(item.id)}
              className={`nav-item ${isActive ? 'active' : ''}`}
            >
              <Icon size={19} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="user-badge">
          <div className="user-avatar">{getInitials(user?.fullName)}</div>
          <div className="user-info">
            <div className="user-name">{user?.fullName || 'User'}</div>
            <div className="user-email">{user?.username || ''}</div>
          </div>
          <button
            onClick={onLogout}
            title="Log out"
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '6px',
            }}
          >
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </aside>
  );
}

import React, { useState, useEffect } from 'react';
import { api } from './api';
import AuthView from './components/AuthView';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import Overview from './components/Overview';
import Transactions from './components/Transactions';
import Categories from './components/Categories';
import Goals from './components/Goals';
import Reports from './components/Reports';
import AddTransactionModal from './components/AddTransactionModal';

export default function App() {
  const [user, setUser] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');
  const [mobileOpen, setMobileOpen] = useState(false);

  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [goals, setGoals] = useState([]);
  const [isAddTxOpen, setIsAddTxOpen] = useState(false);

  const loadFinancialData = async () => {
    try {
      const [txRes, catRes, goalsRes] = await Promise.all([
        api.transactions.getAll(),
        api.categories.getAll(),
        api.goals.getAll(),
      ]);
      setTransactions(txRes.transactions || []);
      setCategories(catRes.categories || []);
      setGoals(goalsRes.goals || []);
    } catch (err) {
      console.error('Error fetching data:', err);
    }
  };

  const checkAuth = async () => {
    try {
      const currentUser = await api.auth.me();
      setUser(currentUser);
      await loadFinancialData();
    } catch (err) {
      setUser(null);
    } finally {
      setInitialLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();

    const handleAuthExpired = () => {
      setUser(null);
    };

    window.addEventListener('auth:expired', handleAuthExpired);
    return () => {
      window.removeEventListener('auth:expired', handleAuthExpired);
    };
  }, []);

  const handleAuthSuccess = async (authenticatedUser) => {
    setUser(authenticatedUser);
    await loadFinancialData();
  };

  const handleLogout = async () => {
    try {
      await api.auth.logout();
    } catch (e) {
      // ignore
    } finally {
      setUser(null);
      setTransactions([]);
      setCategories([]);
      setGoals([]);
    }
  };

  if (initialLoading) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f8faf9',
        color: '#064e3b',
        fontWeight: '600',
        fontSize: '1.1rem',
      }}>
        Loading Moneta...
      </div>
    );
  }

  if (!user) {
    return <AuthView onAuthSuccess={handleAuthSuccess} />;
  }

  return (
    <div className="app-layout">
      {/* Sidebar Navigation */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        user={user}
        onLogout={handleLogout}
        mobileOpen={mobileOpen}
        setMobileOpen={setMobileOpen}
      />

      {/* Main Wrapper */}
      <div className="main-wrapper">
        <Header
          activeTab={activeTab}
          onOpenAddTransaction={() => setIsAddTxOpen(true)}
          setMobileOpen={setMobileOpen}
        />

        <main className="page-content">
          {activeTab === 'overview' && (
            <Overview
              transactions={transactions}
              goals={goals}
              onNavigate={(tab) => setActiveTab(tab)}
              onOpenAddTransaction={() => setIsAddTxOpen(true)}
            />
          )}

          {activeTab === 'transactions' && (
            <Transactions
              transactions={transactions}
              categories={categories}
              onRefresh={loadFinancialData}
              onOpenAdd={() => setIsAddTxOpen(true)}
            />
          )}

          {activeTab === 'categories' && (
            <Categories
              categories={categories}
              onRefresh={loadFinancialData}
            />
          )}

          {activeTab === 'goals' && (
            <Goals
              goals={goals}
              onRefresh={loadFinancialData}
            />
          )}

          {activeTab === 'reports' && (
            <Reports />
          )}
        </main>
      </div>

      {/* Global Add Transaction Modal */}
      <AddTransactionModal
        isOpen={isAddTxOpen}
        onClose={() => setIsAddTxOpen(false)}
        categories={categories}
        onSuccess={loadFinancialData}
      />
    </div>
  );
}

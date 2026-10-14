import React from 'react';
import {
  TrendingUp,
  TrendingDown,
  PiggyBank,
  Percent,
  ArrowRight,
  Target,
  Plus,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { formatCurrency, formatDate, formatPercentage } from '../utils/formatters';

const PIE_COLORS = ['#047857', '#10b981', '#3b82f6', '#f59e0b', '#ec4899', '#8b5cf6', '#6366f1', '#64748b'];

export default function Overview({
  transactions,
  goals,
  onNavigate,
  onOpenAddTransaction,
}) {
  // Compute overall financial totals
  const totalIncome = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((sum, t) => sum + Number(t.amount || 0), 0);

  const totalExpenses = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + Number(t.amount || 0), 0);

  const netSavings = totalIncome - totalExpenses;
  const savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;

  // Monthly breakdown for Income vs Expenses Bar Chart
  const monthlyDataMap = {};
  transactions.forEach((t) => {
    if (!t.date) return;
    const monthKey = t.date.substring(0, 7); // "YYYY-MM"
    if (!monthlyDataMap[monthKey]) {
      monthlyDataMap[monthKey] = { month: monthKey, income: 0, expenses: 0 };
    }
    if (t.type === 'INCOME') {
      monthlyDataMap[monthKey].income += Number(t.amount || 0);
    } else {
      monthlyDataMap[monthKey].expenses += Number(t.amount || 0);
    }
  });

  const chartData = Object.values(monthlyDataMap)
    .sort((a, b) => a.month.localeCompare(b.month))
    .slice(-6)
    .map((d) => {
      const [year, month] = d.month.split('-');
      const date = new Date(year, month - 1);
      const label = date.toLocaleDateString('en-IN', { month: 'short' });
      return {
        name: label,
        Income: d.income,
        Expenses: d.expenses,
      };
    });

  // Category breakdown for Expense Donut Chart
  const expenseCatMap = {};
  transactions
    .filter((t) => t.type === 'EXPENSE')
    .forEach((t) => {
      const cat = t.category || 'Other';
      expenseCatMap[cat] = (expenseCatMap[cat] || 0) + Number(t.amount || 0);
    });

  const pieData = Object.entries(expenseCatMap).map(([name, value]) => ({
    name,
    value,
  }));

  // Top 5 recent transactions
  const recentTransactions = transactions.slice(0, 5);

  return (
    <div>
      {/* 4 Metric Summary Cards */}
      <div className="metrics-grid">
        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Total Income</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#ecfdf5', color: '#047857' }}>
              <TrendingUp size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: '#047857' }}>
            {formatCurrency(totalIncome)}
          </div>
          <div className="metric-trend">Lifetime accumulated income</div>
        </div>

        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Total Expenses</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#fef2f2', color: '#dc2626' }}>
              <TrendingDown size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: '#dc2626' }}>
            {formatCurrency(totalExpenses)}
          </div>
          <div className="metric-trend">Total recorded expenditures</div>
        </div>

        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Net Savings</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#eff6ff', color: '#2563eb' }}>
              <PiggyBank size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: netSavings >= 0 ? '#0f172a' : '#dc2626' }}>
            {formatCurrency(netSavings)}
          </div>
          <div className="metric-trend">Income minus expenses</div>
        </div>

        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Savings Rate</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#f0fdf4', color: '#16a34a' }}>
              <Percent size={20} />
            </div>
          </div>
          <div className="metric-value">
            {formatPercentage(savingsRate)}
          </div>
          <div className="metric-trend">Share of income saved</div>
        </div>
      </div>

      {/* Financial Charts Section */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '24px', marginBottom: '28px' }}>
        {/* Income vs Expenses Bar Chart */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Income vs Expenses</h3>
              <p className="card-subtitle">Monthly cash flow trends</p>
            </div>
          </div>
          <div style={{ height: '280px', width: '100%' }}>
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" stroke="#94a3b8" fontSize={12} />
                  <YAxis stroke="#94a3b8" fontSize={12} tickFormatter={(val) => `₹${val >= 1000 ? `${(val/1000).toFixed(0)}k` : val}`} />
                  <Tooltip
                    formatter={(val) => [formatCurrency(val), '']}
                    contentStyle={{ backgroundColor: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px rgba(0,0,0,0.05)' }}
                  />
                  <Bar dataKey="Income" fill="#059669" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Expenses" fill="#ef4444" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state" style={{ padding: '60px 0' }}>
                <p>No transaction data available yet to display chart.</p>
              </div>
            )}
          </div>
        </div>

        {/* Spending Breakdown Donut Chart */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Spending Breakdown</h3>
              <p className="card-subtitle">Expense distribution by category</p>
            </div>
          </div>
          <div style={{ height: '280px', width: '100%' }}>
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={pieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={65}
                    outerRadius={95}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(val) => [formatCurrency(val), 'Spent']}
                    contentStyle={{ backgroundColor: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}
                  />
                  <Legend verticalAlign="bottom" height={36} iconType="circle" />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state" style={{ padding: '60px 0' }}>
                <p>No expense data recorded yet.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Grid: Recent Transactions & Savings Goals Preview */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '24px' }}>
        {/* Recent Transactions */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Recent Transactions</h3>
              <p className="card-subtitle">Latest activity across all accounts</p>
            </div>
            <button
              onClick={() => onNavigate('transactions')}
              className="btn btn-ghost btn-sm"
              style={{ color: '#047857', fontWeight: '600' }}
            >
              View all
              <ArrowRight size={14} />
            </button>
          </div>

          {recentTransactions.length > 0 ? (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Category</th>
                    <th>Description</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {recentTransactions.map((t) => (
                    <tr key={t.id}>
                      <td style={{ whiteSpace: 'nowrap', color: '#64748b' }}>{formatDate(t.date)}</td>
                      <td>
                        <span className={`badge ${t.type === 'INCOME' ? 'badge-income' : 'badge-expense'}`}>
                          {t.category}
                        </span>
                      </td>
                      <td style={{ color: '#0f172a', fontWeight: '500' }}>{t.description || '—'}</td>
                      <td style={{
                        textAlign: 'right',
                        fontWeight: '600',
                        color: t.type === 'INCOME' ? '#047857' : '#dc2626',
                        whiteSpace: 'nowrap'
                      }}>
                        {t.type === 'INCOME' ? '+' : '-'}{formatCurrency(t.amount)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <p className="empty-state-title">No transactions yet</p>
              <p className="empty-state-desc">Start tracking your income and expenses today.</p>
              <button onClick={onOpenAddTransaction} className="btn btn-primary btn-sm">
                <Plus size={16} />
                Add Transaction
              </button>
            </div>
          )}
        </div>

        {/* Savings Goals Preview */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Savings Goals</h3>
              <p className="card-subtitle">Progress toward your targets</p>
            </div>
            <button
              onClick={() => onNavigate('goals')}
              className="btn btn-ghost btn-sm"
              style={{ color: '#047857', fontWeight: '600' }}
            >
              Manage goals
              <ArrowRight size={14} />
            </button>
          </div>

          {goals && goals.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {goals.slice(0, 3).map((g) => {
                const pct = Math.max(0, Math.min(100, Number(g.progressPercentage || 0)));
                return (
                  <div key={g.id} style={{
                    padding: '16px',
                    borderRadius: '12px',
                    border: '1px solid #e2e8f0',
                    backgroundColor: '#fafaf9',
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <span style={{ fontWeight: '600', color: '#0f172a' }}>{g.goalName}</span>
                      <span style={{ fontSize: '0.875rem', fontWeight: '600', color: '#047857' }}>
                        {g.progressPercentage}%
                      </span>
                    </div>

                    <div className="progress-container" style={{ marginBottom: '10px' }}>
                      <div className="progress-bar-fill" style={{ width: `${pct}%` }} />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: '#64748b' }}>
                      <span>Saved: {formatCurrency(g.currentProgress)}</span>
                      <span>Target: {formatCurrency(g.targetAmount)}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="empty-state">
              <Target className="empty-state-icon" />
              <p className="empty-state-title">No savings goals set</p>
              <p className="empty-state-desc">Create financial goals and automatically track your progress.</p>
              <button onClick={() => onNavigate('goals')} className="btn btn-primary btn-sm">
                <Plus size={16} />
                Create Goal
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

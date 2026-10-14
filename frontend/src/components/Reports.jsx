import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  Calendar,
  TrendingUp,
  TrendingDown,
  PiggyBank,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { api } from '../api';
import { formatCurrency, formatPercentage } from '../utils/formatters';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

export default function Reports() {
  const currentDate = new Date();
  const [reportType, setReportType] = useState('monthly'); // 'monthly' | 'yearly'
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth() + 1);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      let res;
      if (reportType === 'monthly') {
        res = await api.reports.getMonthly(selectedYear, selectedMonth);
      } else {
        res = await api.reports.getYearly(selectedYear);
      }
      setData(res);
    } catch (err) {
      setError(err.message || 'Failed to fetch financial report.');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [reportType, selectedYear, selectedMonth]);

  // Aggregate sums
  const incomeEntries = data && data.totalIncome ? Object.entries(data.totalIncome) : [];
  const expenseEntries = data && data.totalExpenses ? Object.entries(data.totalExpenses) : [];

  const totalIncomeSum = incomeEntries.reduce((sum, [, val]) => sum + Number(val), 0);
  const totalExpenseSum = expenseEntries.reduce((sum, [, val]) => sum + Number(val), 0);
  const netSavings = data ? Number(data.netSavings || 0) : 0;

  // Transform for charts
  const incomeChartData = incomeEntries.map(([cat, val]) => ({
    name: cat,
    amount: Number(val),
  }));

  const expenseChartData = expenseEntries.map(([cat, val]) => ({
    name: cat,
    amount: Number(val),
  }));

  const years = [2023, 2024, 2025, 2026, 2027];

  return (
    <div>
      {/* Top Filter and Mode Switcher */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}>
          {/* Switcher Buttons */}
          <div style={{
            display: 'flex',
            backgroundColor: '#f1f5f9',
            borderRadius: '10px',
            padding: '4px',
          }}>
            <button
              onClick={() => setReportType('monthly')}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: reportType === 'monthly' ? '#ffffff' : 'transparent',
                color: reportType === 'monthly' ? '#047857' : '#64748b',
                fontWeight: reportType === 'monthly' ? '600' : '500',
                cursor: 'pointer',
                boxShadow: reportType === 'monthly' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
              }}
            >
              Monthly Report
            </button>
            <button
              onClick={() => setReportType('yearly')}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: reportType === 'yearly' ? '#ffffff' : 'transparent',
                color: reportType === 'yearly' ? '#047857' : '#64748b',
                fontWeight: reportType === 'yearly' ? '600' : '500',
                cursor: 'pointer',
                boxShadow: reportType === 'yearly' ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
              }}
            >
              Yearly Report
            </button>
          </div>

          {/* Selectors */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            {reportType === 'monthly' && (
              <div>
                <select
                  value={selectedMonth}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                  className="form-select"
                  style={{ minWidth: '140px', padding: '8px 12px' }}
                >
                  {MONTH_NAMES.map((name, index) => (
                    <option key={name} value={index + 1}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div>
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(Number(e.target.value))}
                className="form-select"
                style={{ minWidth: '100px', padding: '8px 12px' }}
              >
                {years.map((y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ))}
              </select>
            </div>

            <button
              onClick={fetchReport}
              disabled={loading}
              className="btn btn-secondary btn-sm"
              title="Refresh Report"
            >
              <RefreshCw size={15} className={loading ? 'spin' : ''} />
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={16} />
          <span>{error}</span>
        </div>
      )}

      {/* Report Header Title */}
      <div style={{ marginBottom: '20px' }}>
        <h3 style={{ fontSize: '1.2rem', fontWeight: '700', color: '#0f172a' }}>
          {reportType === 'monthly'
            ? `${MONTH_NAMES[selectedMonth - 1]} ${selectedYear} Financial Report`
            : `Annual Financial Report for ${selectedYear}`}
        </h3>
        <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
          Aggregated by verified database transactions
        </p>
      </div>

      {/* 3 Summary Cards */}
      <div className="metrics-grid" style={{ marginBottom: '28px' }}>
        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Period Income</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#ecfdf5', color: '#047857' }}>
              <TrendingUp size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: '#047857' }}>
            {formatCurrency(totalIncomeSum)}
          </div>
          <div className="metric-trend">{incomeEntries.length} income categories</div>
        </div>

        <div className="metric-card">
          <div className="metric-top">
            <span className="metric-label">Period Expenses</span>
            <div className="metric-icon-wrap" style={{ backgroundColor: '#fef2f2', color: '#dc2626' }}>
              <TrendingDown size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: '#dc2626' }}>
            {formatCurrency(totalExpenseSum)}
          </div>
          <div className="metric-trend">{expenseEntries.length} expense categories</div>
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
          <div className="metric-trend">Total income minus expenses</div>
        </div>
      </div>

      {/* Breakdown Charts Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '24px', marginBottom: '28px' }}>
        {/* Income by Category */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Income by Category</h3>
              <p className="card-subtitle">Sources of earnings during this period</p>
            </div>
          </div>
          <div style={{ height: '240px' }}>
            {incomeChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={incomeChartData} layout="vertical" margin={{ left: 20, right: 20, top: 10, bottom: 10 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
                  <XAxis type="number" stroke="#94a3b8" fontSize={11} tickFormatter={(v) => `₹${v}`} />
                  <YAxis type="category" dataKey="name" stroke="#64748b" fontSize={12} width={100} />
                  <Tooltip formatter={(v) => [formatCurrency(v), 'Earned']} />
                  <Bar dataKey="amount" fill="#059669" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state" style={{ padding: '60px 0' }}>
                <p>No income recorded for this period.</p>
              </div>
            )}
          </div>
        </div>

        {/* Expenses by Category */}
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">Expenses by Category</h3>
              <p className="card-subtitle">Spending breakdown across classifications</p>
            </div>
          </div>
          <div style={{ height: '240px' }}>
            {expenseChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={expenseChartData} layout="vertical" margin={{ left: 20, right: 20, top: 10, bottom: 10 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
                  <XAxis type="number" stroke="#94a3b8" fontSize={11} tickFormatter={(v) => `₹${v}`} />
                  <YAxis type="category" dataKey="name" stroke="#64748b" fontSize={12} width={100} />
                  <Tooltip formatter={(v) => [formatCurrency(v), 'Spent']} />
                  <Bar dataKey="amount" fill="#ef4444" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty-state" style={{ padding: '60px 0' }}>
                <p>No expenses recorded for this period.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Detailed Tables */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '24px' }}>
        {/* Income Breakdown Table */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Income Breakdown</h3>
          </div>
          {incomeEntries.length > 0 ? (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th style={{ textAlign: 'right' }}>Share</th>
                  </tr>
                </thead>
                <tbody>
                  {incomeEntries.map(([cat, amt]) => {
                    const share = totalIncomeSum > 0 ? (Number(amt) / totalIncomeSum) * 100 : 0;
                    return (
                      <tr key={cat}>
                        <td style={{ fontWeight: '500' }}>{cat}</td>
                        <td style={{ textAlign: 'right', fontWeight: '600', color: '#047857' }}>
                          {formatCurrency(amt)}
                        </td>
                        <td style={{ textAlign: 'right', color: '#64748b' }}>
                          {share.toFixed(1)}%
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '36px 0' }}>
              <p>No income recorded.</p>
            </div>
          )}
        </div>

        {/* Expense Breakdown Table */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Expense Breakdown</h3>
          </div>
          {expenseEntries.length > 0 ? (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th style={{ textAlign: 'right' }}>Share</th>
                  </tr>
                </thead>
                <tbody>
                  {expenseEntries.map(([cat, amt]) => {
                    const share = totalExpenseSum > 0 ? (Number(amt) / totalExpenseSum) * 100 : 0;
                    return (
                      <tr key={cat}>
                        <td style={{ fontWeight: '500' }}>{cat}</td>
                        <td style={{ textAlign: 'right', fontWeight: '600', color: '#dc2626' }}>
                          {formatCurrency(amt)}
                        </td>
                        <td style={{ textAlign: 'right', color: '#64748b' }}>
                          {share.toFixed(1)}%
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '36px 0' }}>
              <p>No expenses recorded.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

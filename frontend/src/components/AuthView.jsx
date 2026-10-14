import React, { useState } from 'react';
import { api } from '../api';
import { Wallet, Lock, Mail, User, Phone, Eye, EyeOff, ArrowRight } from 'lucide-react';

export default function AuthView({ onAuthSuccess }) {
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    fullName: '',
    phoneNumber: '',
  });

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccessMsg(null);

    try {
      if (isLogin) {
        await api.auth.login({
          username: formData.username.trim(),
          password: formData.password,
        });
        const user = await api.auth.me();
        onAuthSuccess(user);
      } else {
        await api.auth.register({
          username: formData.username.trim(),
          password: formData.password,
          fullName: formData.fullName.trim(),
          phoneNumber: formData.phoneNumber.trim(),
        });
        setSuccessMsg('Account created successfully! Logging you in...');
        // Automatically log in after registration
        await api.auth.login({
          username: formData.username.trim(),
          password: formData.password,
        });
        const user = await api.auth.me();
        onAuthSuccess(user);
      }
    } catch (err) {
      setError(err.message || 'An error occurred during authentication.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f8faf9',
      padding: '24px',
    }}>
      <div style={{
        maxWidth: '440px',
        width: '100%',
        backgroundColor: '#ffffff',
        borderRadius: '16px',
        border: '1px solid #e2e8f0',
        boxShadow: '0 10px 25px -5px rgba(0,0,0,0.05), 0 8px 10px -6px rgba(0,0,0,0.03)',
        padding: '36px',
      }}>
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            width: '48px',
            height: '48px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #047857, #10b981)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            marginBottom: '14px',
          }}>
            <Wallet size={26} />
          </div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: '700', color: '#064e3b', letterSpacing: '-0.02em' }}>
            Moneta
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#64748b', marginTop: '4px' }}>
            Personal Finance & Wealth Manager
          </p>
        </div>

        {/* Tab switch */}
        <div style={{
          display: 'flex',
          backgroundColor: '#f1f5f9',
          borderRadius: '10px',
          padding: '4px',
          marginBottom: '24px',
        }}>
          <button
            type="button"
            onClick={() => { setIsLogin(true); setError(null); }}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '8px',
              border: 'none',
              background: isLogin ? '#ffffff' : 'transparent',
              color: isLogin ? '#064e3b' : '#64748b',
              fontWeight: isLogin ? '600' : '500',
              cursor: 'pointer',
              boxShadow: isLogin ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => { setIsLogin(false); setError(null); }}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '8px',
              border: 'none',
              background: !isLogin ? '#ffffff' : 'transparent',
              color: !isLogin ? '#064e3b' : '#64748b',
              fontWeight: !isLogin ? '600' : '500',
              cursor: 'pointer',
              boxShadow: !isLogin ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            Register
          </button>
        </div>

        {error && (
          <div className="alert alert-error" style={{ marginBottom: '20px' }}>
            {error}
          </div>
        )}

        {successMsg && (
          <div className="alert alert-success" style={{ marginBottom: '20px' }}>
            {successMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <>
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <div style={{ position: 'relative' }}>
                  <User size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: '#94a3b8' }} />
                  <input
                    type="text"
                    name="fullName"
                    required
                    placeholder="John Doe"
                    value={formData.fullName}
                    onChange={handleChange}
                    className="form-input"
                    style={{ paddingLeft: '38px' }}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Phone Number</label>
                <div style={{ position: 'relative' }}>
                  <Phone size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: '#94a3b8' }} />
                  <input
                    type="tel"
                    name="phoneNumber"
                    required
                    placeholder="+91 98765 43210"
                    value={formData.phoneNumber}
                    onChange={handleChange}
                    className="form-input"
                    style={{ paddingLeft: '38px' }}
                  />
                </div>
              </div>
            </>
          )}

          <div className="form-group">
            <label className="form-label">Email Address (Username)</label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: '#94a3b8' }} />
              <input
                type="email"
                name="username"
                required
                placeholder="you@example.com"
                value={formData.username}
                onChange={handleChange}
                className="form-input"
                style={{ paddingLeft: '38px' }}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{ position: 'absolute', left: '12px', top: '12px', color: '#94a3b8' }} />
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                required
                placeholder="••••••••"
                value={formData.password}
                onChange={handleChange}
                className="form-input"
                style={{ paddingLeft: '38px', paddingRight: '40px' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '10px',
                  background: 'none',
                  border: 'none',
                  color: '#94a3b8',
                  cursor: 'pointer',
                }}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {!isLogin && (
              <span style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px', display: 'block' }}>
                Must be at least 6 characters.
              </span>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{ width: '100%', padding: '12px', fontSize: '0.95rem', marginTop: '10px' }}
          >
            {loading ? 'Please wait...' : (
              <>
                {isLogin ? 'Sign In' : 'Create Account'}
                <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}

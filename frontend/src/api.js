const BASE_URL = import.meta.env.VITE_API_URL || '';

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const config = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    credentials: 'include',
  };

  try {
    const response = await fetch(url, config);
    let data = null;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    }

    if (!response.ok) {
      const errorMsg =
        (data && (data.message || data.error)) ||
        `Request failed with status ${response.status}`;
      const error = new Error(errorMsg);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    if (error.status === 401 && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
      // Dispatch custom event to notify app of session expiration
      window.dispatchEvent(new CustomEvent('auth:expired'));
    }
    throw error;
  }
}

export const api = {
  // Authentication
  auth: {
    register: (data) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(data) }),
    login: (data) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(data) }),
    logout: () => request('/api/auth/logout', { method: 'POST' }),
    me: () => request('/api/auth/me', { method: 'GET' }),
  },

  // Categories
  categories: {
    getAll: () => request('/api/categories', { method: 'GET' }),
    create: (data) => request('/api/categories', { method: 'POST', body: JSON.stringify(data) }),
    delete: (name) => request(`/api/categories/${encodeURIComponent(name)}`, { method: 'DELETE' }),
  },

  // Transactions
  transactions: {
    getAll: (params = {}) => {
      const query = new URLSearchParams();
      if (params.startDate) query.append('startDate', params.startDate);
      if (params.endDate) query.append('endDate', params.endDate);
      if (params.category) query.append('category', params.category);
      if (params.categoryId) query.append('categoryId', params.categoryId);
      if (params.type) query.append('type', params.type);

      const qs = query.toString();
      return request(`/api/transactions${qs ? `?${qs}` : ''}`, { method: 'GET' });
    },
    create: (data) => request('/api/transactions', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/api/transactions/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/api/transactions/${id}`, { method: 'DELETE' }),
  },

  // Savings Goals
  goals: {
    getAll: () => request('/api/goals', { method: 'GET' }),
    getById: (id) => request(`/api/goals/${id}`, { method: 'GET' }),
    create: (data) => request('/api/goals', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/api/goals/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/api/goals/${id}`, { method: 'DELETE' }),
  },

  // Reports
  reports: {
    getMonthly: (year, month) => request(`/api/reports/monthly/${year}/${month}`, { method: 'GET' }),
    getYearly: (year) => request(`/api/reports/yearly/${year}`, { method: 'GET' }),
  },
};

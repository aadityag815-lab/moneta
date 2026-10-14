export const formatCurrency = (amount) => {
  if (amount === undefined || amount === null || isNaN(amount)) {
    return '₹0.00';
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

export const formatDate = (dateString) => {
  if (!dateString) return '';
  try {
    const [year, month, day] = dateString.split('-');
    if (!year || !month || !day) return dateString;
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch (e) {
    return dateString;
  }
};

export const formatPercentage = (percent) => {
  if (percent === undefined || percent === null || isNaN(percent)) {
    return '0%';
  }
  return `${Number(percent).toFixed(1)}%`;
};

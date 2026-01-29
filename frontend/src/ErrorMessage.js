import React from 'react'

function ErrorMessage({ error, onDismiss }) {
  if (!error) return null;

  return (
    <div className="error-banner">
      <div className="error-content">
        <span className="error-icon">⚠️</span>
        <span>{error.message}</span>
      </div>
      <button onClick={onDismiss} className="dismiss-btn">✕</button>
    </div>
  )
};
    
export default ErrorMessage
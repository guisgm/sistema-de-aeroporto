import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-500.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/dm-sans/latin-700.css';
import '@fontsource/manrope/latin-500.css';
import '@fontsource/manrope/latin-600.css';
import '@fontsource/manrope/latin-700.css';
import '@fontsource/manrope/latin-800.css';
import './styles.css';

class ErrorBoundary extends React.Component {
  state = { error: false };
  static getDerivedStateFromError() {
    return { error: true };
  }
  componentDidCatch(error) {
    console.error('Falha de interface:', error);
  }
  render() {
    return this.state.error ? (
      <div className="fatal">
        <h1>Não foi possível exibir esta página.</h1>
        <p>Seus dados salvos permanecem disponíveis.</p>
        <button className="button primary" onClick={() => location.reload()}>
          Recarregar sistema
        </button>
      </div>
    ) : (
      this.props.children
    );
  }
}
createRoot(document.getElementById('root')).render(
  <ErrorBoundary>
    <App />
  </ErrorBoundary>,
);

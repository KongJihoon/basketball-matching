import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { AppProviders } from './app/providers';
import './shared/styles/globals.css';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('React를 표시할 root 요소를 찾지 못했습니다.');
}

createRoot(rootElement).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
);

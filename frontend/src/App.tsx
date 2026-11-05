import { BrowserRouter } from 'react-router';
import { AppRouter } from './app/router';

export default function App() {
  return (
    <BrowserRouter>
      <AppRouter />
    </BrowserRouter>
  );
}

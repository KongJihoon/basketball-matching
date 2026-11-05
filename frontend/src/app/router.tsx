import { Navigate, Route, Routes } from 'react-router';
import { AppLayout } from './layouts/AppLayout';
import { GameListPage } from '../pages/games/GameListPage';
import { NotFoundPage } from '../pages/NotFoundPage';

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/games" replace />} />
        <Route path="games" element={<GameListPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

import { Navigate, Route, Routes } from 'react-router';
import { AppLayout } from './layouts/AppLayout';
import { GameListPage } from '../pages/games/GameListPage';
import { GameDetailPage } from '../pages/games/GameDetailPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { SignupPage } from '../pages/auth/SignupPage';
import { KakaoCompletePage } from '../pages/auth/KakaoCompletePage';

export function AppRouter() {
  return (
    <Routes>
      <Route path="auth/kakao/complete" element={<KakaoCompletePage />} />

      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/games" replace />} />
        <Route path="games" element={<GameListPage />} />
        <Route path="games/:gameId" element={<GameDetailPage />} />
        <Route path="login" element={<Navigate to="/games?login=1" replace />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

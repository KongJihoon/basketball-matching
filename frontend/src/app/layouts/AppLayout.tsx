import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useSearchParams } from 'react-router';
import { LoginModal } from '../../features/auth/LoginModal';
import { logout, refreshSession } from '../../features/auth/auth.api';
import type { AuthSession } from '../../features/auth/auth.api';
import { ApiError } from '../../shared/api/http';

export type AppOutletContext = {
  session: AuthSession | null;
  restoring: boolean;
  openParticipationLogin: () => void;
};

const menuClassName =
  'inline-flex min-h-10 items-center whitespace-nowrap rounded-lg px-3 text-sm font-semibold text-muted hover:bg-surface';

export function AppLayout() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [restoring, setRestoring] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const [authError, setAuthError] = useState('');
  const logoutInFlight = useRef(false);
  const loginMode = searchParams.get('login');
  const loginRequested = loginMode === '1' || loginMode === 'participation';

  useEffect(() => {
    let active = true;

    async function restore() {
      try {
        const restored = await refreshSession();
        if (active) setSession(restored);
      } catch (error) {
        if (active && !(error instanceof ApiError && error.status === 401)) {
          setAuthError('로그인 상태를 확인하지 못했습니다. 다시 로그인해 주세요.');
        }
      } finally {
        if (active) setRestoring(false);
      }
    }

    void restore();

    return () => {
      active = false;
    };
  }, []);

  function openLogin(mode: '1' | 'participation') {
    setAuthError('');
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        next.set('login', mode);
        return next;
      },
      { replace: true, preventScrollReset: true },
    );
  }

  function closeLogin() {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        next.delete('login');
        return next;
      },
      { replace: true, preventScrollReset: true },
    );
  }

  async function handleLogout() {
    if (!session || logoutInFlight.current) return;

    logoutInFlight.current = true;
    setLoggingOut(true);
    setAuthError('');

    try {
      try {
        await logout(session.accessToken);
      } catch (error) {
        if (!(error instanceof ApiError && error.errorCode === 'EXPIRED_TOKEN')) {
          throw error;
        }

        const renewed = await refreshSession();

        if (renewed.user.userId !== session.user.userId) {
          setSession(renewed);
          setAuthError('로그인 계정이 바뀌었습니다. 계정을 확인한 뒤 다시 눌러 주세요.');
          return;
        }

        setSession(renewed);
        await logout(renewed.accessToken);
      }

      setSession(null);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setSession(null);
        setAuthError('로그인 세션이 만료되었습니다.');
      } else {
        setAuthError(
          error instanceof ApiError
            ? error.message
            : '로그아웃을 완료하지 못했습니다. 다시 시도해 주세요.',
        );
      }
    } finally {
      logoutInFlight.current = false;
      setLoggingOut(false);
    }
  }

  const outletContext: AppOutletContext = {
    session,
    restoring,
    openParticipationLogin: () => openLogin('participation'),
  };

  return (
    <div className="flex min-h-dvh flex-col bg-page text-foreground">
      <a
        href="#main-content"
        className="sr-only z-50 rounded-lg bg-foreground px-4 py-3 text-white focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        본문으로 바로가기
      </a>

      <header className="border-b border-line">
        <div className="mx-auto flex min-h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Link to="/games" className="rounded-lg text-lg font-bold tracking-tight text-brand-ink">
            Basketball Matching
          </Link>

          <nav aria-label="주 메뉴">
            <NavLink
              to="/games"
              className={({ isActive }) =>
                [
                  'inline-flex min-h-11 items-center rounded-xl px-4 text-sm font-semibold transition-colors',
                  isActive ? 'bg-brand-soft text-brand-ink' : 'text-muted hover:bg-surface',
                ].join(' ')
              }
            >
              경기 찾기
            </NavLink>

            {restoring ? (
              <span role="status" className="px-3 text-sm text-muted">
                로그인 확인 중...
              </span>
            ) : session ? (
              <>
                <span className="max-w-40 truncate px-2 text-sm font-semibold text-foreground">
                  {session.user.nickname}
                </span>
                <button
                  type="button"
                  disabled={loggingOut}
                  className={`${menuClassName} disabled:opacity-60`}
                  onClick={() => void handleLogout()}
                >
                  {loggingOut ? '로그아웃 중...' : '로그아웃'}
                </button>
              </>
            ) : (
              <>
                <button type="button" className={menuClassName} onClick={() => openLogin('1')}>
                  로그인
                </button>
                <Link
                  to="/signup"
                  className={`${menuClassName} bg-brand-soft text-brand-ink hover:bg-brand-soft`}
                >
                  회원가입
                </Link>
              </>
            )}
          </nav>
        </div>

        {authError && (
          <p role="alert" className="mx-auto max-w-6xl text-sm text-red-700 sm:px-6">
            {authError}
          </p>
        )}
      </header>

      <main
        id="main-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-12"
      >
        <Outlet context={outletContext} />
      </main>

      <footer className="border-t border-line bg-surface">
        <div className="mx-auto max-w-6xl px-4 py-6 text-sm text-muted sm:px-6">
          함께 뛰는 즐거움, Basketball Matching
        </div>
      </footer>

      {!restoring && !session && loginRequested && (
        <LoginModal
          forParticipation={loginMode === 'participation'}
          onClose={closeLogin}
          onSuccess={(nextSession) => {
            setSession(nextSession);
            setAuthError('');
            closeLogin();
          }}
        />
      )}
    </div>
  );
}

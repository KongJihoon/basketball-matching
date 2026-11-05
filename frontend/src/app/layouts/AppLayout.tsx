import { Link, NavLink, Outlet } from 'react-router';

export function AppLayout() {
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
          </nav>
        </div>
      </header>

      <main
        id="main-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6 sm:py-12"
      >
        <Outlet />
      </main>

      <footer className="border-t border-line bg-surface">
        <div className="mx-auto max-w-6xl px-4 py-6 text-sm text-muted sm:px-6">
          함께 뛰는 즐거움, Basketball Matching
        </div>
      </footer>
    </div>
  );
}

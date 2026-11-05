import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <section className="py-16 text-center">
      <p className="text-sm font-semibold text-brand-ink">404</p>

      <h1 className="mt-3 text-2xl font-bold tracking-tight">페이지를 찾을 수 없습니다.</h1>

      <p className="mt-3 leading-7 text-muted">
        주소를 확인하거나 경기 찾기 화면으로 이동해 주세요.
      </p>

      <Link
        to="/games"
        className="mt-8 inline-flex min-h-12 items-center justify-center rounded-xl bg-brand px-6 font-semibold text-foreground transition-colors hover:bg-brand-hover"
      >
        경기 찾기로 이동
      </Link>
    </section>
  );
}

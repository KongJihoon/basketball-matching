import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useOutletContext, useSearchParams } from 'react-router';
import type { AppOutletContext } from '../../app/layouts/AppLayout';
import { getMyGames } from '../../features/games/my-game-api';
import type { MyGame, MyGameTab } from '../../features/games/my-game-api';
import { ApiError } from '../../shared/api/http';

const myGamesPath = '/mypage/games';

const tabs: { value: MyGameTab; label: string }[] = [
  { value: 'upcoming', label: '예정·진행 중' },
  { value: 'completed', label: '지난 경기' },
];

const buttonClassName =
  'inline-flex min-h-11 items-center justify-center rounded-xl border border-line px-4 text-sm font-semibold hover:bg-surface';

const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  month: 'long',
  day: 'numeric',
  weekday: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

function readPage(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) return 0;

  const page = Number(value);
  return page <= 2_147_483_647 ? page : 0;
}

function makeSearch(tab: MyGameTab, page: number): string {
  const params = new URLSearchParams({ tab });

  if (page > 0) params.set('page', String(page));

  return `?${params.toString()}`;
}

function MyGameCard({
  game,
  now,
  returnSearch,
}: {
  game: MyGame;
  now: number;
  returnSearch: string;
}) {
  const start = `${game.startDateTime}+09:00`;
  const startsAt = Date.parse(start);
  const endsAt = Date.parse(`${game.endDateTime}+09:00`);
  const finished = now >= endsAt;
  const inProgress = !finished && now >= startsAt;
  const status = finished ? '종료' : inProgress ? '진행 중' : '예정';

  return (
    <li className="flex flex-col gap-4 rounded-2xl border border-line bg-page p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <time dateTime={start} className="text-sm font-semibold text-brand-ink">
          {dateTimeFormatter.format(new Date(start))}
        </time>
        <h2 className="mt-1 wrap-break-word text-lg font-bold">{game.title}</h2>
        <p className="mt-1 wrap-break-word text-sm text-muted">{game.placeName}</p>
      </div>

      <div className="flex flex-wrap items-center gap-2 sm:shrink-0 sm:justify-end">
        <span className="rounded-full bg-surface px-3 py-1 text-xs font-semibold text-muted">
          {status}
        </span>
        <span className="rounded-full bg-brand-soft px-3 py-1 text-xs font-semibold text-brand-ink">
          {game.creator ? '내가 만든 경기' : '참가 확정'}
        </span>
        <Link
          to={`/games/${game.gameId}`}
          state={{ myGamesSearch: returnSearch }}
          className={buttonClassName}
        >
          {game.creator && !inProgress && !finished ? '경기 관리' : '경기 상세'}
        </Link>
      </div>
    </li>
  );
}

function MyGamesList({ userId, accessToken }: { userId: number; accessToken: string }) {
  const [searchParams] = useSearchParams();
  const tab = searchParams.get('tab') === 'completed' ? 'completed' : 'upcoming';
  const page = readPage(searchParams.get('page'));
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const query = useQuery({
    queryKey: ['my-games', userId, tab, page],
    queryFn: ({ signal }) => getMyGames(tab, page, accessToken, signal),
  });

  const games = query.data;

  const emptyMessage =
    tab === 'upcoming' ? '예정되거나 진행 중인 경기가 없어요.' : '지난 경기가 없어요.';

  return (
    <div className="mx-auto w-full max-w-4xl space-y-5">
      <div>
        <h1 className="text-2xl font-bold sm:text-3xl">내 경기</h1>
        {games && <p className="mt-1 text-sm text-muted">총 {games.totalElements}경기</p>}
      </div>

      <nav aria-label="내 경기 목록 구분" className="flex flex-wrap gap-2">
        {tabs.map(({ value, label }) => {
          const seleted = value === tab;

          return (
            <Link
              key={value}
              to={{ pathname: myGamesPath, search: makeSearch(value, 0) }}
              aria-current={seleted ? 'page' : undefined}
              className={`${buttonClassName} ${
                seleted ? 'bg-brand-soft text-brand-ink hover:bg-brand-soft' : 'text-muted'
              }`}
            >
              {label}
            </Link>
          );
        })}
      </nav>

      {query.isPending && (
        <p role="status" className="rounded-2xl border border-line p-6 text-sm text-muted">
          내 경기를 불러오고 있어요.
        </p>
      )}

      {query.isError && (
        <div className="space-y-3 rounded-2xl border border-line p-6">
          <p role="alert" className="text-sm text-red-700">
            {query.error instanceof ApiError
              ? query.error.message
              : '내 경기 목록을 불러오지 못했어요.'}
          </p>

          <button
            type="button"
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
            className={buttonClassName}
          >
            다시 시도
          </button>
        </div>
      )}

      {games && games.content.length === 0 && (
        <div className="space-y-3 rounded-2xl border border-line p-6 text-sm text-muted">
          <p>{page > 0 ? '이 페이지를 표시할 경기가 없어요.' : emptyMessage}</p>
          {page > 0 && (
            <Link
              to={{ pathname: myGamesPath, search: makeSearch(tab, 0) }}
              className={buttonClassName}
            >
              첫 페이지로
            </Link>
          )}
        </div>
      )}

      {games && games.content.length > 0 && (
        <ul className="space-y-3">
          {games.content.map((game) => (
            <MyGameCard
              key={game.gameId}
              game={game}
              now={now}
              returnSearch={makeSearch(tab, page)}
            />
          ))}
        </ul>
      )}

      {games && games.totalPages > 1 && page < games.totalPages && (
        <nav aria-label="내 경기 페이지" className="flex items-center justify-center gap-3">
          {page > 0 && (
            <Link
              to={{ pathname: myGamesPath, search: makeSearch(tab, page - 1) }}
              className={buttonClassName}
            >
              이전
            </Link>
          )}
          <span className="text-sm tabular-nums text-muted">
            {page + 1} / {games.totalPages}
          </span>
          {page + 1 < games.totalPages && (
            <Link
              to={{ pathname: myGamesPath, search: makeSearch(tab, page + 1) }}
              className={buttonClassName}
            >
              다음
            </Link>
          )}
        </nav>
      )}
    </div>
  );
}

export function MyGamesPage() {
  const { session, restoring } = useOutletContext<AppOutletContext>();
  const [searchParams] = useSearchParams();

  if (restoring) {
    return (
      <p role="status" className="text-sm text-muted">
        로그인 상태를 확인하고 있어요.
      </p>
    );
  }

  if (!session) {
    const loginParams = new URLSearchParams(searchParams);
    loginParams.set('login', '1');

    return (
      <section className="mx-auto max-w-4xl space-y-4">
        <h1 className="text-2xl font-bold">내 경기</h1>
        <p className="text-sm text-muted">내 경기를 보려면 로그인해 주세요.</p>
        <Link
          to={{ pathname: myGamesPath, search: `?${loginParams.toString()}` }}
          className={buttonClassName}
        >
          로그인
        </Link>
      </section>
    );
  }

  if (session.user.userType !== 'USER') {
    return (
      <p role="alert" className="text-sm text-muted">
        일반 회원 계정에서 내 경기를 볼 수 있어요.
      </p>
    );
  }

  return <MyGamesList userId={session.user.userId} accessToken={session.accessToken} />;
}

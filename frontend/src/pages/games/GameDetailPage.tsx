import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router';
import { GameVenueMap } from '../../features/games/GameVenueMap';
import { getGameDetail } from '../../features/games/game-detail.api';
import type { GameDetail } from '../../features/games/game-detail.api';
import { ApiError } from '../../shared/api/http';

const matchFormatLabels: Record<GameDetail['matchFormat'], string> = {
  THREE_ON_THREE: '3대3',
  FIVE_ON_FIVE: '5대5',
};

const genderLabels: Record<GameDetail['matchGenderType'], string> = {
  MALE_ONLY: '남성',
  FEMALE_ONLY: '여성',
  MIXED: '혼성',
};

const fieldLabels: Record<GameDetail['fieldStatus'], string> = {
  INDOOR: '실내',
  OUTDOOR: '실외',
};

const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  month: 'long',
  day: 'numeric',
  weekday: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

const actionClassName =
  'inline-flex min-h-11 items-center rounded-xl border border-line bg-page px-4 text-sm font-semibold hover:bg-surface disabled:opacity-50';

function GameDateTime({ value }: { value: string }) {
  const dateTime = `${value}+09:00`;

  return <time dateTime={dateTime}>{dateTimeFormatter.format(new Date(dateTime))}</time>;
}

function GameDetailContent({ game }: { game: GameDetail }) {
  const [openedAt] = useState(() => Date.now());
  const startsAt = Date.parse(`${game.startDateTime}+09:00`);
  const endsAt = Date.parse(`${game.endDateTime}+09:00`);
  const schedule = openedAt >= endsAt ? '종료' : openedAt >= startsAt ? '진행 중' : '예정';
  const recruiting = game.gameStatus === 'RECRUITING';

  return (
    <article aria-labelledby="game-detail-title" className="space-y-6">
      <header className="rounded-3xl bg-brand-soft px-6 py-8 sm:px-8">
        <p className="text-sm font-semibold text-brand-ink">
          <GameDateTime value={game.startDateTime} />
        </p>
        <h1 id="game-detail-title" className="mt-2 wrap-break-word text-2xl font-bold sm:text-4xl">
          {game.title}
        </h1>
      </header>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_19rem] lg:gap-8">
        <aside
          aria-labelledby="game_summary_title"
          className="self-start rounded-2xl border border-ling bg-page p-5 lg:col-start-2 lg:row-start-1"
        >
          <h2 id="game-summary-title" className="text-lg font-bold">
            모집 예약
          </h2>
          <div className="mt-3 flex flex-wrap gap-2 text-sm font-semibold">
            <span className="rounded-full bg-surface px-3 py-1 text-muted">{schedule}</span>
            <span
              className={`rounded-full px-3 py-1 ${
                recruiting ? 'bg-brand-soft text-brand-ink' : 'bg-surface text-muted'
              }`}
            >
              {recruiting ? '모집 중' : '마감'}
            </span>
          </div>

          <dl className="mt-5 space-y-3 border-t border-line pt-5 text-sm">
            <div className="flex flex-wrap justify-between gap-x-4 gap-y-1">
              <dt className="text-muted">시작</dt>
              <dd className="font-semibold">
                <GameDateTime value={game.startDateTime} />
              </dd>
            </div>
            <div className="flex flex-wrap justify-between gap-x-4 gap-y-1">
              <dt className="text-muted">종료</dt>
              <dd className="font-semibold">
                <GameDateTime value={game.endDateTime} />
              </dd>
            </div>
            <div className="flex flex-wrap justify-between gap-x-4 gap-y-1">
              <dt className="text-muted">참가 인원</dt>
              <dd className="font-semibold tabular-nums text-brand-ink">
                {game.participantCount}명 / 정원 {game.headCount}명
              </dd>
            </div>
          </dl>
        </aside>

        <div>
          <section aria-labelledby="game-info-title" className="py-6">
            <h2 id="game-info-title" className="text-lg font-bold">
              경기 정보
            </h2>

            <dl className="mt-4 grid grid-cols-2 gap-4 text-sm sm:grid-cols-4">
              <div>
                <dt className="text-muted">경기 형식</dt>
                <dd className="mt-1 font-semibold">{matchFormatLabels[game.matchFormat]}</dd>
              </div>
              <div>
                <dt className="text-muted">성별 조건</dt>
                <dd className="mt-1 font-semibold">{genderLabels[game.matchGenderType]}</dd>
              </div>
              <div>
                <dt className="text-muted">실내외</dt>
                <dd className="mt-1 font-semibold">{fieldLabels[game.fieldStatus]}</dd>
              </div>
              <div>
                <dt className="text-muted">주최자</dt>
                <dd className="mt-1 wrap-break-word font-semibold">{game.creatorNickname}</dd>
              </div>
            </dl>
          </section>

          <section aria-labelledby="game-venue-title" className="py-6">
            <h2 id="game-venue-title" className="text-lg font-bold">
              경기장
            </h2>
            <p className="mt-3 wrap-break-word font-semibold">{game.placeName}</p>
            <p className="mt-1 wrap-break-word text-sm text-muted">{game.address}</p>
            <GameVenueMap
              latitude={game.latitude}
              longitude={game.longitude}
              placeName={game.placeName}
            />
          </section>

          <section aria-labelledby="game-description-title" className="py-6">
            <h2 id="game-description-title" className="text-lg font-bold">
              경기 설명
            </h2>
            <p className="mt-3 whitespace-pre-wrap wrap-break-word leading-7 text-muted">
              {game.content}
            </p>
          </section>
        </div>
      </div>
    </article>
  );
}

function isNotFound(error: unknown) {
  return error instanceof ApiError && error.status === 404;
}

function GameDetailQuery({ gameId }: { gameId: string }) {
  const query = useQuery({
    queryKey: ['game-detail', gameId],
    queryFn: ({ signal }) => getGameDetail(gameId, signal),
    retry: (failureCount, error) => !isNotFound(error) && failureCount < 1,
  });

  if (query.isPending) {
    return (
      <p role="status" className="py-8 text-muted">
        경기 정보를 불러오고 있어요.
      </p>
    );
  }

  if (query.isError) {
    if (isNotFound(query.error)) {
      return (
        <p role="alert" className="py-8 text-muted">
          경기를 찾을 수 없어요.
        </p>
      );
    }

    return (
      <div className="space-y-3 py-8">
        <p role="alert" className="text-red-700">
          경기 정보를 불러오지 못했어요.
        </p>
        <button
          type="button"
          className={actionClassName}
          disabled={query.isFetching}
          onClick={() => void query.refetch()}
        >
          다시 시도
        </button>
      </div>
    );
  }

  return <GameDetailContent game={query.data} />;
}

export function GameDetailPage() {
  const { gameId } = useParams<'gameId'>();
  const [searchParams] = useSearchParams();
  const listParams = new URLSearchParams(searchParams);
  listParams.delete('login');

  const validGameId =
    gameId && /^[1-9]\d{0,18}$/.test(gameId) && BigInt(gameId) <= 9_223_372_036_854_775_807n
      ? gameId
      : null;

  return (
    <div className="space-y-4">
      <Link to={{ pathname: '/games', search: listParams.toString() }} className={actionClassName}>
        경기 목록
      </Link>

      {validGameId ? (
        <GameDetailQuery key={validGameId} gameId={validGameId} />
      ) : (
        <p role="alert" className="py-8 text-muted">
          경기 주소를 확인해 주세요.
        </p>
      )}
    </div>
  );
}

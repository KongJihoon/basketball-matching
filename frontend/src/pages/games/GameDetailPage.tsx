import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useOutletContext, useParams, useSearchParams, useLocation } from 'react-router';
import type { AppOutletContext } from '../../app/layouts/AppLayout';
import { GameVenueMap } from '../../features/games/GameVenueMap';
import { getGameDetail } from '../../features/games/game-detail.api';
import type { GameDetail } from '../../features/games/game-detail.api';
import { ApiError } from '../../shared/api/http';
import {
  cancelGameParticipation,
  getMyGameParticipation,
  joinGame,
} from '../../features/games/game-participation.api';
import { GameParticipationDialog } from '../../features/games/GameParticipationDialog';

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

const primaryButtonClassName =
  'min-h-12 w-full rounded-xl bg-brand px-4 text-sm font-bold text-white transition-colors enabled:hover:bg-brand-hover disabled:cursor-not-allowed disabled:bg-surface disabled:text-muted';

const participationCutoffMs = 30 * 60 * 1000;

function GameDateTime({ value }: { value: string }) {
  const dateTime = `${value}+09:00`;

  return <time dateTime={dateTime}>{dateTimeFormatter.format(new Date(dateTime))}</time>;
}

function participationErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError && error.status === 401) {
    return '로그인 세션을 확인해 주세요. 페이지를 새로고침 한 뒤에 다시 시도할 수 있습니다.';
  }

  return error instanceof ApiError ? error.message : fallback;
}

type MemberParticipationActionsProps = {
  game: GameDetail;
  gameId: string;
  userId: number;
  accessToken: string;
  deadline: number;
  hasOpenSpot: boolean;
};

function MemberParticipationActions({
  game,
  gameId,
  userId,
  accessToken,
  deadline,
  hasOpenSpot,
}: MemberParticipationActionsProps) {
  const queryClient = useQueryClient();
  const [checkedAt, setCheckedAt] = useState(() => Date.now());
  const [confirmationAction, setConfirmationAction] = useState<'join' | 'cancel' | null>(null);
  const participationKey = ['my-game-participation', gameId, userId] as const;

  const participantQuery = useQuery({
    queryKey: participationKey,
    queryFn: ({ signal }) => getMyGameParticipation(gameId, accessToken, signal),
    retry: false,
  });

  const participationMutation = useMutation({
    mutationFn: async (action: 'join' | 'cancel') => {
      if (action === 'join') {
        await joinGame(gameId, accessToken);
      } else {
        await cancelGameParticipation(gameId, accessToken);
      }
    },
    retry: false,
    onSuccess: () => setConfirmationAction(null),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: participationKey }),
        queryClient.invalidateQueries({ queryKey: ['game-detail', gameId] }),
        queryClient.invalidateQueries({ queryKey: ['games'] }),
        queryClient.invalidateQueries({ queryKey: ['my-games', userId] }),
      ]);
    },
  });

  if (participantQuery.isPending) {
    return (
      <p role="status" className="mt-5 text-sm text-muted">
        내 참가 상태를 확인하고 있어요.
      </p>
    );
  }

  if (!participantQuery.data) {
    return (
      <div className="mt-5 space-y-2">
        <p role="alert" className="text-sm text-red-700">
          {participationErrorMessage(participantQuery.error, '내 참가 상태를 확인하지 못했어요.')}
        </p>
        <button
          type="button"
          disabled={participantQuery.isFetching}
          className={actionClassName}
          onClick={() => void participantQuery.refetch()}
        >
          다시 시도
        </button>
      </div>
    );
  }

  const { status } = participantQuery.data;
  const isAccepted = status === 'ACCEPT';
  const canRejoin = status === null || status === 'CANCEL';
  const beforeDeadline = checkedAt < deadline;
  const action = isAccepted ? 'cancel' : 'join';
  const canSubmit =
    !participantQuery.isError && beforeDeadline && (isAccepted || (canRejoin && hasOpenSpot));
  const isBusy = participationMutation.isPending || participantQuery.isFetching;
  const canConfirm = canSubmit && !isBusy && confirmationAction === action;

  let buttonLabel = '신청하기';
  if (isAccepted) {
    buttonLabel = beforeDeadline ? '참가 취소' : '취소 기한 종료';
  } else if (!canRejoin) {
    buttonLabel = '신청 불가';
  } else if (!beforeDeadline || !hasOpenSpot) {
    buttonLabel = '모집 종료';
  }

  const queryError = participantQuery.isError
    ? participationErrorMessage(participantQuery.error, '내 참가 상태를 다시 확인하지 못했어요.')
    : undefined;

  let dialogError = queryError;
  if (participationMutation.isError) {
    dialogError = participationErrorMessage(
      participationMutation.error,
      '참가 요청을 처리하지 못했어요.',
    );
  } else if (!dialogError && !isBusy && (!canSubmit || confirmationAction !== action)) {
    dialogError = '신청 또는 취소 조건이 바뀌었어요. 창을 닫고 현재 상태를 확인해 주세요.';
  }

  function openConfirmation() {
    const now = Date.now();
    setCheckedAt(now);
    if (isBusy || !canSubmit || now >= deadline) return;

    participationMutation.reset();
    setConfirmationAction(action);
  }

  function confirmParticipation() {
    const now = Date.now();
    setCheckedAt(now);
    if (!confirmationAction || !canConfirm || now >= deadline) return;

    participationMutation.mutate(confirmationAction);
  }

  return (
    <div className="mt-5 space-y-2">
      <button
        type="button"
        disabled={!canSubmit || isBusy}
        aria-haspopup={canSubmit ? 'dialog' : undefined}
        className={primaryButtonClassName}
        onClick={openConfirmation}
      >
        {participationMutation.isPending ? '처리 중…' : buttonLabel}
      </button>

      {confirmationAction && (
        <GameParticipationDialog
          action={confirmationAction}
          gameTitle={game.title}
          startTime={dateTimeFormatter.format(new Date(`${game.startDateTime}+09:00`))}
          placeName={game.placeName}
          conditions={`${matchFormatLabels[game.matchFormat]} · ${genderLabels[game.matchGenderType]}`}
          isPending={participationMutation.isPending}
          canConfirm={canConfirm}
          errorMessage={dialogError}
          onClose={() => setConfirmationAction(null)}
          onConfirm={confirmParticipation}
        />
      )}

      {queryError && !confirmationAction && (
        <div className="space-y-2">
          <p role="alert" className="text-sm text-red-700">
            {queryError}
          </p>
          <button
            type="button"
            disabled={participantQuery.isFetching}
            className={actionClassName}
            onClick={() => void participantQuery.refetch()}
          >
            다시 시도
          </button>
        </div>
      )}

      {participationMutation.isSuccess && (
        <p role="status" className="text-sm text-brand-ink">
          {participationMutation.variables === 'cancel'
            ? '참가를 취소했어요.'
            : '참가가 확정됐어요.'}
        </p>
      )}
    </div>
  );
}

function GameDetailContent({ game, gameId }: { game: GameDetail; gameId: string }) {
  const { session, restoring, openParticipationLogin } = useOutletContext<AppOutletContext>();
  const [checkedAt, setCheckedAt] = useState(() => Date.now());
  const startsAt = Date.parse(`${game.startDateTime}+09:00`);
  const endsAt = Date.parse(`${game.endDateTime}+09:00`);
  const schedule = checkedAt >= endsAt ? '종료' : checkedAt >= startsAt ? '진행 중' : '예정';
  const recruiting = game.gameStatus === 'RECRUITING';
  const deadline = startsAt - participationCutoffMs;
  const hasOpenSpot = recruiting && game.participantCount < game.headCount;
  const canRequestJoin = hasOpenSpot && checkedAt < deadline;

  function handleGuestJoin() {
    const now = Date.now();
    setCheckedAt(now);

    if (canRequestJoin && now < deadline) {
      openParticipationLogin();
    }
  }

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
          aria-labelledby="game-summary-title"
          className="self-start rounded-2xl border border-line bg-page p-5 lg:col-start-2 lg:row-start-1"
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

          {restoring ? (
            <p role="status" className="mt-5 text-sm text-muted">
              로그인 상태를 확인하고 있어요.
            </p>
          ) : !session ? (
            <button
              type="button"
              disabled={!canRequestJoin}
              aria-haspopup={canRequestJoin ? 'dialog' : undefined}
              onClick={handleGuestJoin}
              className={`mt-5 ${primaryButtonClassName}`}
            >
              {canRequestJoin ? '신청하기' : '모집 종료'}
            </button>
          ) : (
            session.user.userType === 'USER' &&
            session.user.userId !== game.creatorId && (
              <MemberParticipationActions
                key={session.user.userId}
                game={game}
                gameId={gameId}
                userId={session.user.userId}
                accessToken={session.accessToken}
                deadline={deadline}
                hasOpenSpot={hasOpenSpot}
              />
            )
          )}
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

  return <GameDetailContent game={query.data} gameId={gameId} />;
}

export function GameDetailPage() {
  const { gameId } = useParams<'gameId'>();
  const [searchParams] = useSearchParams();
  const listParams = new URLSearchParams(searchParams);
  listParams.delete('login');
  const { state } = useLocation();
  const myGamesSearch = (state as { myGamesSearch?: unknown } | null)?.myGamesSearch;
  const cameFromMyGames = typeof myGamesSearch === 'string';

  const backTo = cameFromMyGames
    ? { pathname: '/mypage/games', search: myGamesSearch }
    : { pathname: '/games', search: listParams.toString() };

  const validGameId =
    gameId && /^[1-9]\d{0,18}$/.test(gameId) && BigInt(gameId) <= 9_223_372_036_854_775_807n
      ? gameId
      : null;

  return (
    <div className="space-y-4">
      <Link to={backTo} className={actionClassName}>
        {cameFromMyGames ? '내 경기로' : '경기 목록'}
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

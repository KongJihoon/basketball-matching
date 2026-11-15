import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router';
import { getGames } from '../../features/games/game.api';
import type {
  CityName,
  FieldStatus,
  GameListItem,
  GameListParams,
  GameSortType,
  GameStatus,
} from '../../features/games/game.api';

const cityLabels: Record<CityName, string> = {
  SEOUL: '서울',
  GYEONGGI: '경기',
  INCHEON: '인천',
  GANGWON: '강원',
  DAEJEON: '대전',
  SEJONG: '세종',
  CHUNGNAM: '충남',
  CHUNGBUK: '충북',
  DAEGU: '대구',
  GYEONGBUK: '경북',
  GYEONGNAM: '경남',
  BUSAN: '부산',
  ULSAN: '울산',
  GWANGJU: '광주',
  JEONNAM: '전남',
  JEONBUK: '전북',
  JEJU: '제주',
};

const statusLabels: Record<GameStatus, string> = {
  RECRUITING: '모집 중',
  CLOSED: '마감',
};

const matchFormatLabels: Record<GameListItem['matchFormat'], string> = {
  THREE_ON_THREE: '3대3',
  FIVE_ON_FIVE: '5대5',
};

const genderLabels: Record<GameListItem['matchGenderType'], string> = {
  MALE_ONLY: '남성',
  FEMALE_ONLY: '여성',
  MIXED: '혼성',
};

const fieldStatusLabels: Record<FieldStatus, string> = {
  INDOOR: '실내',
  OUTDOOR: '실외',
};

const sortLabels: Record<GameSortType, string> = {
  START_TIME_ASC: '시작 시간 빠른 순',
  LATEST: '최신 등록 순',
};

const buttonClassName =
  'min-h-11 rounded-xl border border-line px-4 text-sm font-medium hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50';

const inputClassName = 'min-h-11 rounded-xl border border-line bg-page px-3 text-sm';

function readPage(value: string | null): number {
  if (!value || !/^\d+$/.test(value)) return 0;

  const page = Number(value);
  return page <= 2_147_483_647 ? page : 0;
}

function readDate(value: string | null): string {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000')) {
    return '';
  }

  const date = new Date(`${value}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return '';

  return date.toISOString().slice(0, 10) === value ? value : '';
}

function readOption<T extends string>(labels: Record<T, string>, value: string | null): T | '' {
  if (value === null || !Object.hasOwn(labels, value)) return '';

  return value as T;
}

type FilterGroupProps = {
  label: string;
  options: Record<string, string>;
  value: string;
  onChange: (value: string) => void;
};

function FilterGroup({ label, options, value, onChange }: FilterGroupProps) {
  const entries = [['', '전체'], ...Object.entries(options)];

  return (
    <fieldset>
      <legend className="mb-2 text-sm font-semibold">{label}</legend>

      <div className="flex flex-wrap gap-2">
        {entries.map(([key, text]) => (
          <button
            key={key}
            type="button"
            aria-pressed={key === value}
            onClick={() => onChange(key)}
            className={`min-h-11 rounded-full border px-3 text-sm font-medium ${
              key === value
                ? 'border-brand bg-brand-soft text-brand-ink'
                : 'border-line bg-page text-muted hover:bg-surface'
            }`}
          >
            {text}
          </button>
        ))}
      </div>
    </fieldset>
  );
}

function GameListRow({ game, listSearch }: { game: GameListItem; listSearch: string }) {
  const date = game.startDateTime.slice(0, 10);
  const time = game.startDateTime.slice(11, 16);
  const badgeClassName =
    game.gameStatus === 'RECRUITING' ? 'bg-brand-soft text-brand-ink' : 'bg-surface text-muted';

  return (
    <li>
      <Link
        to={{ pathname: `/games/${game.gameId}`, search: listSearch }}
        className="grid grid-cols-[4.75rem_minmax(0,1fr)] gap-3 px-3 py-5 hover:bg-surface focus-visible:outline-offset-[-2px] sm:grid-cols-[6rem_minmax(0,1fr)_auto] sm:gap-5 sm:px-5"
      >
        <time dateTime={game.startDateTime} className="flex flex-col">
          <span className="text-xs text-muted">{date}</span>
          <span className="mt-1 text-xl font-bold tabular-nums">{time}</span>
        </time>

        <div className="min-w-0">
          <h3 className="wrap-break-word font-bold">{game.title}</h3>
          <p className="mt-1 wrap-break-word text-sm">{game.placeName}</p>
          <p className="mt-1 wrap-break-word text-sm text-muted">{game.address}</p>
          <p className="mt-2 text-sm text-muted">
            {matchFormatLabels[game.matchFormat]} · {genderLabels[game.matchGenderType]}
          </p>
        </div>

        <div className="col-start-2 flex flex-wrap items-center gap-2 sm:col-start-3 sm:flex-col sm:items-end sm:justify-self-end">
          <span className={`rounded-full px-3 py-1 text-sm font-semibold ${badgeClassName}`}>
            {statusLabels[game.gameStatus]}
          </span>
          <span className="text-sm text-muted">
            참가 {game.participantCount}/{game.headCount}명
          </span>
        </div>
      </Link>
    </li>
  );
}

export function GameListPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const listParams = new URLSearchParams(searchParams);
  listParams.delete('login');
  const listSearch = listParams.toString();

  const conditions: GameListParams = {
    page: readPage(searchParams.get('page')),
    date: readDate(searchParams.get('date')),
    cityName: readOption(cityLabels, searchParams.get('cityName')),
    matchFormat: readOption(matchFormatLabels, searchParams.get('matchFormat')),
    matchGenderType: readOption(genderLabels, searchParams.get('matchGenderType')),
    fieldStatus: readOption(fieldStatusLabels, searchParams.get('fieldStatus')),
    gameStatus: readOption(statusLabels, searchParams.get('gameStatus')),
    sortType: readOption(sortLabels, searchParams.get('sortType')) || 'START_TIME_ASC',
  };

  const { page, date, cityName, matchFormat, matchGenderType, fieldStatus, gameStatus, sortType } =
    conditions;

  const gameQuery = useQuery({
    queryKey: ['games', conditions],
    queryFn: ({ signal }) => getGames(conditions, signal),
  });

  function updateListParams(changes: Record<string, string>) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);

      for (const [key, value] of Object.entries(changes)) {
        if (value) {
          next.set(key, value);
        } else {
          next.delete(key);
        }
      }

      return next;
    });
  }

  function changeCondition(key: Exclude<keyof GameListParams, 'page'>, value: string) {
    updateListParams({ [key]: value, page: '' });
  }

  function movePage(nextPage: number) {
    updateListParams({ page: nextPage === 0 ? '' : String(nextPage) });
  }

  const games = gameQuery.isSuccess ? gameQuery.data : undefined;
  const emptyMessage =
    date || cityName || matchFormat || matchGenderType || fieldStatus || gameStatus
      ? '선택한 조건에 맞는 경기가 없어요.'
      : '예정된 경기가 없어요.';

  return (
    <div>
      <section
        aria-labelledby="game-page-title"
        className="rounded-3xl bg-brand-soft px-6 py-10 sm:px-10 sm:py-14"
      >
        <h1
          id="game-page-title"
          className="text-2xl leading-tight font-bold tracking-tight sm:text-4xl"
        >
          원하는 시간,
          <br />
          함께 뛸 사람
        </h1>
      </section>

      <div className="mt-8 grid gap-6 lg:grid-cols-[15rem_minmax(0,1fr)] lg:gap-8">
        <aside
          aria-label="경기 필터"
          className="self-start space-y-5 rounded-2xl border border-line bg-page p-4"
        >
          <label className="flex flex-col gap-2 text-sm font-semibold">
            지역
            <select
              value={cityName}
              onChange={(event) => changeCondition('cityName', event.currentTarget.value)}
              className={inputClassName}
            >
              <option value="">전체 지역</option>
              {Object.entries(cityLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <FilterGroup
            label="모집 상태"
            options={statusLabels}
            value={gameStatus}
            onChange={(value) => changeCondition('gameStatus', value)}
          />

          <FilterGroup
            label="경기 형식"
            options={matchFormatLabels}
            value={matchFormat}
            onChange={(value) => changeCondition('matchFormat', value)}
          />

          <FilterGroup
            label="성별"
            options={genderLabels}
            value={matchGenderType}
            onChange={(value) => changeCondition('matchGenderType', value)}
          />

          <FilterGroup
            label="실내외"
            options={fieldStatusLabels}
            value={fieldStatus}
            onChange={(value) => changeCondition('fieldStatus', value)}
          />
        </aside>

        <section aria-labelledby="games-list-title" className="min-w-0">
          <div className="flex flex-wrap items-end justify-between gap-3 border-b border-line pb-4">
            <div>
              <h2 id="games-list-title" className="text-xl font-bold">
                경기 목록
              </h2>
              {games && <p className="mt-1 text-sm text-muted">{games.totalElements}</p>}
            </div>

            <div className="flex flex-wrap items-end gap-2">
              <label className="flex flex-col gap-1 text-sm font-medium">
                경기 날짜
                <input
                  type="date"
                  value={date}
                  onChange={(event) => {
                    changeCondition('date', readDate(event.currentTarget.value));
                  }}
                  className={inputClassName}
                />
              </label>
              <button
                type="button"
                aria-pressed={!date}
                onClick={() => changeCondition('date', '')}
                className={`${buttonClassName} ${!date ? 'bg-brand-soft text-brand-ink' : ''}`}
              >
                전체 예정
              </button>
              <div className="flex max-w-full items-center gap-2 text-sm font-medium">
                <label
                  htmlFor="game-list-sort"
                  className="inline-flex min-h-11 shrink-0 items-center rounded-xl border border-line bg-brand-soft px-4 text-brand-ink"
                >
                  정렬
                </label>
                <select
                  id="game-list-sort"
                  value={sortType}
                  onChange={(event) => changeCondition('sortType', event.currentTarget.value)}
                  className="min-h-11 min-w-0 cursor-pointer rounded-xl border border-line bg-page px-3"
                >
                  {Object.entries(sortLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          {gameQuery.isPending && (
            <p role="status" className="px-4 py-10 text-center text-sm text-muted">
              경기 목록을 불러오고 있어요.
            </p>
          )}

          {gameQuery.isError && (
            <div className="space-y-3 px-4 py-10 text-center">
              <p role="alert" className="text-sm text-red-700">
                경기 목록을 불러오지 못했어요.
              </p>
              <button
                type="button"
                onClick={() => void gameQuery.refetch()}
                disabled={gameQuery.isFetching}
                className={buttonClassName}
              >
                다시 시도
              </button>
            </div>
          )}

          {games && (
            <>
              {games.content.length > 0 ? (
                <ul className="divide-y divide-line border-b border-line">
                  {games.content.map((game) => (
                    <GameListRow key={game.gameId} game={game} listSearch={listSearch} />
                  ))}
                </ul>
              ) : (
                <div className="space-y-3 px-4 py-10 text-center text-sm text-muted">
                  <p role="status">
                    {page > 0 && games.totalElements > 0
                      ? '이 페이지에 표시할 경기가 없어요.'
                      : emptyMessage}
                  </p>

                  {page > 0 && (
                    <button type="button" onClick={() => movePage(0)} className={buttonClassName}>
                      첫 페이지로
                    </button>
                  )}
                </div>
              )}

              {games.totalPages > 1 && page < games.totalPages && (
                <nav
                  aria-label="경기 목록 페이지"
                  className="mt-5 flex items-center justify-center gap-4"
                >
                  <button
                    type="button"
                    onClick={() => movePage(page - 1)}
                    disabled={page === 0}
                    className={buttonClassName}
                  >
                    이전
                  </button>

                  <span className="text-sm tabular-nums text-muted">
                    {page + 1} / {games.totalPages}
                  </span>

                  <button
                    type="button"
                    onClick={() => movePage(page + 1)}
                    disabled={page + 1 >= games.totalPages}
                    className={buttonClassName}
                  >
                    다음
                  </button>
                </nav>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}

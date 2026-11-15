import { requestJson } from '../../shared/api/http';
import type { CommonResponse } from '../../shared/api/types';

export type CityName =
  | 'SEOUL'
  | 'GYEONGGI'
  | 'INCHEON'
  | 'GANGWON'
  | 'DAEJEON'
  | 'SEJONG'
  | 'CHUNGNAM'
  | 'CHUNGBUK'
  | 'DAEGU'
  | 'GYEONGBUK'
  | 'GYEONGNAM'
  | 'BUSAN'
  | 'ULSAN'
  | 'GWANGJU'
  | 'JEONNAM'
  | 'JEONBUK'
  | 'JEJU';

export type GameStatus = 'RECRUITING' | 'CLOSED';
export type MatchFormat = 'THREE_ON_THREE' | 'FIVE_ON_FIVE';
export type MatchGenderType = 'MALE_ONLY' | 'FEMALE_ONLY' | 'MIXED';
export type FieldStatus = 'INDOOR' | 'OUTDOOR';
export type GameSortType = 'START_TIME_ASC' | 'LATEST';

export type GameListItem = {
  gameId: number;
  title: string;
  placeName: string;
  address: string;
  startDateTime: string;
  headCount: number;
  participantCount: number;
  matchGenderType: MatchGenderType;
  matchFormat: MatchFormat;
  gameStatus: GameStatus;
};

type GameListPageData = {
  content: GameListItem[];
  totalPages: number;
  totalElements: number;
};

export type GameListParams = {
  page: number;
  date: string;
  cityName: CityName | '';
  matchFormat: MatchFormat | '';
  matchGenderType: MatchGenderType | '';
  fieldStatus: FieldStatus | '';
  gameStatus: GameStatus | '';
  sortType: GameSortType;
};

export async function getGames(
  { page, sortType, ...filters }: GameListParams,
  signal: AbortSignal,
): Promise<GameListPageData> {
  const params = new URLSearchParams({
    page: String(page),
    size: '10',
    sortType,
  });

  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value);
  }

  const response = await requestJson<CommonResponse<GameListPageData>>(
    `/games?${params.toString()}`,
    { signal },
  );

  return response.data;
}

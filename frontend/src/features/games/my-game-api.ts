import { requestJson } from '../../shared/api/http';
import type { CommonResponse } from '../../shared/api/types';
import type { CityName, GameListItem } from './game.api';

export type MyGameTab = 'upcoming' | 'completed';

export type MyCompletedGame = Omit<GameListItem, 'gameStatus'> & {
  cityName: CityName;
  endDateTime: string;
  creator: boolean;
};

export type MyUpcomingGame = GameListItem & {
  cityName: CityName;
  endDateTime: string;
  creator: boolean;
};

export type MyGame = MyUpcomingGame | MyCompletedGame;

type MyGamePageData = {
  content: MyGame[];
  totalPages: number;
  totalElements: number;
};

export async function getMyGames(
  tab: MyGameTab,
  page: number,
  accessToken: string,
  signal?: AbortSignal,
): Promise<MyGamePageData> {
  const params = new URLSearchParams({
    page: String(page),
    size: '10',
  });

  const response = await requestJson<CommonResponse<MyGamePageData>>(
    `/mypage/games/${tab}?${params.toString()}`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
      signal,
    },
  );

  return response.data;
}

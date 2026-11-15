import { requestJson } from '../../shared/api/http';
import type { CommonResponse } from '../../shared/api/types';
import type { CityName, FieldStatus, GameListItem } from './game.api';

export type GameDetail = GameListItem & {
  content: string;
  endDateTime: string;
  fieldStatus: FieldStatus;
  latitude: number | null;
  longitude: number | null;
  cityName: CityName;
  creatorId: number;
  creatorNickname: string;
};

export async function getGameDetail(gameId: string, signal: AbortSignal): Promise<GameDetail> {
  const response = await requestJson<CommonResponse<GameDetail>>(`/games/${gameId}`, {
    signal,
  });

  return response.data;
}

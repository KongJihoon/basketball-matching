import { requestJson } from '../../shared/api/http';
import type { CheckResponse, CommonResponse } from '../../shared/api/types';

export type ParticipationStatus = 'ACCEPT' | 'CANCEL' | 'KICKOUT' | 'DELETE';

export type MyGameParticipation = {
  participantId: number | null;
  status: ParticipationStatus | null;
};

export type JoinGameResponse = {
  participationId: number;
  gameId: number;
  status: ParticipationStatus;
  joinedAt: string;
};

export async function getMyGameParticipation(
  gameId: string,
  accessToken: string,
  signal?: AbortSignal,
): Promise<MyGameParticipation> {
  const response = await requestJson<CommonResponse<MyGameParticipation>>(
    `/games/${gameId}/participations/me`,
    {
      headers: { Authorization: `Bearer ${accessToken}` },
      signal,
    },
  );

  return response.data;
}

export async function joinGame(gameId: string, accessToken: string): Promise<JoinGameResponse> {
  const response = await requestJson<CommonResponse<JoinGameResponse>>(
    `/games/${gameId}/participations`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${accessToken}` },
    },
  );

  return response.data;
}

export function cancelGameParticipation(
  gameId: string,
  accessToken: string,
): Promise<CheckResponse> {
  return requestJson<CheckResponse>(`/games/${gameId}/participations/me/cancel`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

import { requestJson } from '../../shared/api/http';
import type { CommonResponse, CheckResponse } from '../../shared/api/types';
import type { SignupRequest } from './signup.api';

type CsrfResponse = {
  headerName: string;
  token: string;
};

export type LoginInput = {
  email: string;
  password: string;
};

export type AuthSession = {
  accessToken: string;
  user: {
    userId: number;
    email: string;
    nickname: string;
    userType: 'USER' | 'ADMIN';
  };
};

export type KakaoSignupRequest = Omit<SignupRequest, 'email' | 'password' | 'checkPassword'> & {
  ticket: string;
};

async function getCsrfHeaders(): Promise<Headers> {
  const { headerName, token } = await requestJson<CsrfResponse>('/auth/csrf', {
    credentials: 'same-origin',
  });

  const headers = new Headers();
  headers.set(headerName, token);
  return headers;
}

export async function login(input: LoginInput): Promise<AuthSession> {
  const headers = await getCsrfHeaders();
  const response = await requestJson<CommonResponse<AuthSession>>('/auth/login', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
    json: input,
  });

  return response.data;
}

export async function exchangeKakaoLoginTicket(ticket: string): Promise<void> {
  const headers = await getCsrfHeaders();

  await requestJson<CommonResponse<AuthSession>>('/auth/oauth2/token', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
    json: { ticket },
  });
}

export async function signupWithKakao(input: KakaoSignupRequest): Promise<AuthSession> {
  const headers = await getCsrfHeaders();
  const response = await requestJson<CommonResponse<AuthSession>>('/auth/oauth2/signup', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
    json: input,
  });

  return response.data;
}

async function refresh(): Promise<AuthSession> {
  const headers = await getCsrfHeaders();
  const response = await requestJson<CommonResponse<AuthSession>>('/auth/token/refresh', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
  });

  return response.data;
}

let refreshInFlight: Promise<AuthSession> | null = null;

export function refreshSession(): Promise<AuthSession> {
  if (!refreshInFlight) {
    refreshInFlight = refresh().finally(() => {
      refreshInFlight = null;
    });
  }

  return refreshInFlight;
}

export async function logout(accessToken: string): Promise<void> {
  const headers = await getCsrfHeaders();
  headers.set('Authorization', `Bearer ${accessToken}`);

  await requestJson<CheckResponse>('/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
  });
}

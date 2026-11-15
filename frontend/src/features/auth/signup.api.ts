import { requestJson } from '../../shared/api/http';
import type { CheckResponse, CommonResponse } from '../../shared/api/types';

export type SignupPosition = 'NONE' | 'GUARD' | 'FORWARD' | 'CENTER';
export type SignupGender = 'MALE' | 'FEMALE';

export type SignupRequest = {
  email: string;
  password: string;
  checkPassword: string;
  nickname: string;
  name: string;
  birth: string;
  phone: string;
  address: string;
  addressDetail: string | null;
  zipCode: string;
  position: SignupPosition;
  genderType: SignupGender;
};

type SignupResult = {
  nickname: string;
};

export function checkSignupNickname(nickname: string): Promise<CheckResponse> {
  const param = new URLSearchParams({ nickname });

  return requestJson<CheckResponse>(`/users/availability/nickname?${param.toString()}`);
}

export function signup(request: SignupRequest): Promise<CommonResponse<SignupResult>> {
  return requestJson<CommonResponse<SignupResult>>('/users/signup', {
    method: 'POST',
    json: request,
  });
}

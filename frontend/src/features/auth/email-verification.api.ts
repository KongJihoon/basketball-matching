import { requestJson } from '../../shared/api/http';
import type { CheckResponse } from '../../shared/api/types';

export async function requestSignupEmailCode(email: string): Promise<CheckResponse> {
  const params = new URLSearchParams({ email });

  await requestJson<CheckResponse>(`/users/availability/email?${params.toString()}`);

  return requestJson<CheckResponse>('/email-verifications', {
    method: 'POST',
    json: { email },
  });
}

export function confirmSignupEmailCode({
  email,
  code,
}: {
  email: string;
  code: string;
}): Promise<CheckResponse> {
  return requestJson<CheckResponse>('/email-verifications/confirm', {
    method: 'POST',
    json: { email, code },
  });
}

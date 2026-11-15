import type { FieldErrorDetail } from './types';

const API_BASE_URI = '/api/v1';

type JsonRequestOptions = Omit<RequestInit, 'body'> & {
  json?: unknown;
};

export class ApiError extends Error {
  readonly status: number;
  readonly errorCode: string;
  readonly details: FieldErrorDetail[];

  constructor(
    status: number,
    errorCode: string,
    message: string,
    details: FieldErrorDetail[] = [],
  ) {
    super(message);

    this.name = 'ApiError';
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isFieldErrorDetail(value: unknown): value is FieldErrorDetail {
  return (
    isRecord(value) &&
    typeof value.field === 'string' &&
    typeof value.code === 'string' &&
    typeof value.message === 'string'
  );
}

export async function requestJson<T>(
  path: `/${string}`,
  options: JsonRequestOptions = {},
): Promise<T> {
  const { json, ...requestOptions } = options;
  const headers = new Headers(requestOptions.headers);

  if (!headers.has('ACCEPT')) {
    headers.set('ACCEPT', 'application/json');
  }

  if (json !== undefined) {
    headers.set('Content-Type', 'application/json');
  }

  const body = json === undefined ? undefined : JSON.stringify(json);

  let response: Response;
  let responseText: string;

  try {
    response = await fetch(`${API_BASE_URI}${path}`, {
      ...requestOptions,
      headers,
      body,
    });

    responseText = await response.text();
  } catch (error) {
    if (requestOptions.signal?.aborted) {
      throw error;
    }

    throw new ApiError(
      0,
      'NETWORK_ERROR',
      '서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    );
  }

  let payload: unknown;

  try {
    payload = JSON.parse(responseText);
  } catch {
    payload = null;
  }

  if (!response.ok) {
    if (isRecord(payload)) {
      const details = Array.isArray(payload.details)
        ? payload.details.filter(isFieldErrorDetail)
        : [];

      throw new ApiError(
        response.status,
        typeof payload.errorCode === 'string' ? payload.errorCode : 'HTTP_ERROR',
        typeof payload.errorMessage === 'string'
          ? payload.errorMessage
          : '요청을 처리하지 못했습니다.',
        details,
      );
    }

    throw new ApiError(
      response.status,
      'HTTP_ERROR',
      '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    );
  }

  if (payload === null) {
    throw new ApiError(response.status, 'INVALID_RESPONSE', '서버 응답 형식을 확인할 수 없습니다.');
  }

  return payload as T;
}

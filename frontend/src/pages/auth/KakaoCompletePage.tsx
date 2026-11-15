import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { exchangeKakaoLoginTicket } from '../../features/auth/auth.api';
import { takeKakaoGameReturn } from '../../features/auth/kakao-return';
import { SignupDetailsForm } from '../../features/auth/SignupDetailsForm';
import { ApiError } from '../../shared/api/http';

type KakaoCallback =
  | { type: 'login'; ticket: string }
  | { type: 'signup'; ticket: string }
  | { type: 'error'; message: string };

function readKakaoCallback(): KakaoCallback {
  const params = new URLSearchParams(window.location.hash.slice(1));
  const error = params.get('error');

  if (error === 'OAUTH_ACCOUNT_LINK_REQUIRED') {
    return {
      type: 'error',
      message: '같은 이메일로 가입한 계정이 있습니다. 기존 계정으로 로그인해 주세요.',
    };
  }

  if (error === 'OAUTH_CODE_NOT_FOUND') {
    return { type: 'error', message: '카카오 인증이 완료되지 않았습니다. 다시 시도해 주세요.' };
  }

  if (error) {
    return { type: 'error', message: '카카오 인증을 완료하지 못했습니다. 다시 시도해 주세요.' };
  }

  const flowType = params.get('flowType');
  const ticket = params.get('ticket');

  if (ticket && flowType === 'LOGIN') return { type: 'login', ticket };
  if (ticket && flowType === 'SIGNUP') return { type: 'signup', ticket };

  return {
    type: 'error',
    message: '카카오 로그인 정보가 없거나 만료되었습니다. 다시 시도해 주세요.',
  };
}
function openReturnPage() {
  window.location.replace(takeKakaoGameReturn());
}

export function KakaoCompletePage() {
  const navigate = useNavigate();
  const [callback] = useState(readKakaoCallback);
  const [exchangeError, setExchangeError] = useState('');
  const exchangeRequest = useRef<Promise<void> | null>(null);

  useEffect(() => {
    const { pathname, search, hash } = window.location;
    if (hash) {
      window.history.replaceState(window.history.state, '', pathname + search);
    }

    if (callback.type !== 'login') return;

    let active = true;
    exchangeRequest.current ??= exchangeKakaoLoginTicket(callback.ticket);

    void exchangeRequest.current.then(
      () => {
        if (active) openReturnPage();
      },
      (error: unknown) => {
        if (!active) return;

        setExchangeError(
          error instanceof ApiError && error.errorCode === 'OAUTH_TICKET_INVALID'
            ? '카카오 로그인 요청이 만료되었거나 이미 사용되었습니다. 다시 시도해 주세요.'
            : '카카오 로그인을 완료하지 못했습니다. 다시 시도해 주세요.',
        );
      },
    );

    return () => {
      active = false;
    };
  }, [callback]);
  return (
    <main className="grid min-h-dvh place-items-center bg-surface px-4 py-8 text-foreground">
      <section
        aria-labelledby="kakao-complete-title"
        className="w-full max-w-md rounded-2xl border border-line bg-page p-6"
      >
        <h1 id="kakao-complete-title" className="text-xl font-bold">
          {callback.type === 'signup' ? '카카오 회원가입' : '카카오 로그인'}
        </h1>

        {callback.type === 'signup' ? (
          <>
            <p className="mt-2 text-sm text-muted">농구 매칭에 사용할 정보를 입력해 주세요.</p>
            <SignupDetailsForm
              mode="kakao"
              ticket={callback.ticket}
              onBack={() => void navigate(takeKakaoGameReturn(), { replace: true })}
              onComplete={openReturnPage}
            />
          </>
        ) : callback.type === 'login' && !exchangeError ? (
          <p role="status" className="mt-3 text-sm text-muted">
            로그인 정보를 확인하고 있습니다.
          </p>
        ) : (
          <>
            <p role="alert" className="mt-3 text-sm text-red-700">
              {callback.type === 'error' ? callback.message : exchangeError}
            </p>
            <Link
              to="/login"
              replace
              onClick={() => takeKakaoGameReturn()}
              className="mt-5 inline-flex min-h-11 items-center rounded-lg bg-brand-soft px-4 text-sm font-semibold text-brand-ink"
            >
              로그인으로 돌아가기
            </Link>
          </>
        )}
      </section>
    </main>
  );
}

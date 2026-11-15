import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation } from 'react-router';
import { ApiError } from '../../shared/api/http';
import { login } from './auth.api';
import type { AuthSession } from './auth.api';
import { rememberKakaoGameReturn } from './kakao-return';

type LoginModalProps = {
  forParticipation: boolean;
  onClose: () => void;
  onSuccess: (session: AuthSession) => void;
};

const inputClassName = 'mt-1.5 h-12 w-full rounded-lg border border-line bg-page px-3 text-sm';

export function LoginModal({ forParticipation, onClose, onSuccess }: LoginModalProps) {
  const { pathname, search } = useLocation();
  const dialogRef = useRef<HTMLDialogElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const submitting = useRef(false);
  const [showPassword, setShowPassword] = useState(false);
  const [pending, setPending] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const dialog = dialogRef.current;

    if (!dialog) return;

    dialog.showModal();
    emailRef.current?.focus();

    return () => dialog.close();
  }, []);

  function close() {
    if (!submitting.current) onClose();
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submitting.current) return;

    const values = new FormData(event.currentTarget);
    const email = String(values.get('email') ?? '').trim();
    const password = String(values.get('password') ?? '');

    submitting.current = true;
    setPending(true);
    setErrorMessage('');

    try {
      const session = await login({ email, password });
      onSuccess(session);
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? (error.details[0]?.message ?? error.message)
          : '로그인 요청을 처리하지 못했습니다. 다시 시도해 주세요.',
      );
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="login-title"
      aria-describedby={forParticipation ? 'login-description' : undefined}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
      className="fixed inset-0 m-auto h-fit max-h-[calc(100dvh_-_2rem)] w-[calc(100%_-_2rem)] max-w-sm overflow-y-auto rounded-2xl border border-line bg-page p-0 text-foreground shadow-2xl backdrop:bg-black/55"
    >
      <div className="relative p-6">
        <button
          type="button"
          aria-label="로그인 창 닫기"
          disabled={pending}
          onClick={close}
          className="absolute top-3 right-3 grid size-9 place-items-center rounded-lg text-2xl text-muted hover:bg-surface disabled:opacity-50"
        >
          <span aria-hidden="true">×</span>
        </button>

        <h1 id="login-title" className="pr-8 text-xl font-bold text-brand-ink">
          Basketball Matching 로그인
        </h1>

        {forParticipation && (
          <p id="login-description" className="mt-2 text-sm text-muted">
            경기에 신청하려면 로그인해 주세요.
          </p>
        )}

        <form className="mt-6" onSubmit={handleSubmit} onChange={() => setErrorMessage('')}>
          <fieldset disabled={pending} className="space-y-4">
            <label className="block text-sm font-medium">
              이메일
              <input
                ref={emailRef}
                name="email"
                type="email"
                autoComplete="username"
                required
                className={inputClassName}
              />
            </label>

            <div>
              <label htmlFor="login-password" className="block text-sm font-medium">
                비밀번호
              </label>
              <div className="relative">
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  className={`${inputClassName} pr-20`}
                />
                <button
                  type="button"
                  aria-controls="login-password"
                  onClick={() => setShowPassword((visible) => !visible)}
                  className="absolute right-2 bottom-1 grid h-10 place-items-center rounded-md px-2 text-xs font-semibold text-muted hover:bg-surface"
                >
                  {showPassword ? '숨기기' : '보기'}
                </button>
              </div>
            </div>

            {errorMessage && (
              <p role="alert" className="text-sm text-red-700">
                {errorMessage}
              </p>
            )}

            <button
              type="submit"
              className="h-12 w-full rounded-lg bg-brand text-sm font-semibold text-white enabled:hover:bg-brand-hover disabled:opacity-60"
            >
              {pending ? '로그인 중...' : '로그인'}
            </button>
          </fieldset>
        </form>

        {!pending && (
          <>
            <p className="mt-5 text-center text-sm text-muted">
              처음 이용하시나요?{' '}
              <Link to="/signup" className="font-semibold text-brand-ink hover:underline">
                회원가입
              </Link>
            </p>

            <section aria-labelledby="social-login-title" className="mt-6">
              <div className="flex items-center gap-3">
                <span aria-hidden="true" className="h-px flex-1 bg-line" />
                <h2 id="social-login-title" className="text-xs font-medium text-muted">
                  간편 로그인
                </h2>
                <span aria-hidden="true" className="h-px flex-1 bg-line" />
              </div>

              <a
                href="/api/v1/auth/oauth2/kakao/authorization"
                onClick={() => rememberKakaoGameReturn(forParticipation ? pathname + search : null)}
                className="mt-4 block w-full rounded-lg transition-opacity hover:opacity-90"
              >
                <img
                  src="/images/kakao-login-symbol.png"
                  alt="카카오 로그인"
                  width={1008}
                  height={138}
                  className="block h-auto w-full"
                />
              </a>
            </section>
          </>
        )}
      </div>
    </dialog>
  );
}

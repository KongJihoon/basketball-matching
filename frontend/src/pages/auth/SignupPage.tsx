import { useEffect, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'react-router';
import {
  confirmSignupEmailCode,
  requestSignupEmailCode,
} from '../../features/auth/email-verification.api';
import { SignupDetailsForm } from '../../features/auth/SignupDetailsForm';
import { ApiError } from '../../shared/api/http';

const cardClassName = 'mx-auto max-w-md rounded-2xl border border-line bg-page p-6';
const inputClassName = 'mt-2 w-full rounded-xl border border-gray-400 bg-page px-4 py-3';
const primaryButtonClassName =
  'inline-flex h-12 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white enabled:hover:bg-brand-hover disabled:bg-surface disabled:text-muted';

function getErrorMessage(error: Error, field: string) {
  if (error instanceof ApiError) {
    return error.details.find((detail) => detail.field === field)?.message ?? error.message;
  }
  return '요청을 처리하지 못했습니다. 다시 시도해 주세요.';
}

function FieldError({ id, message }: { id: string; message?: string }) {
  return (
    <span id={id} aria-live="polite" className="block text-xs font-normal text-red-700">
      {message && <span className="mt-2 block">{message}</span>}
    </span>
  );
}

function SignupSuccessToast() {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setVisible(false), 4000);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return null;

  return (
    <div className="fixed inset-x-4 top-4 z-50 flex items-center gap-3 rounded-xl bg-brand-ink px-4 py-3 text-white shadow-lg sm:right-6 sm:left-auto sm:top-6 sm:w-80">
      <span aria-hidden="true">✓</span>
      <p role="status" className="flex-1 text-sm font-semibold">
        회원가입이 완료됐습니다.
      </p>
      <button
        type="button"
        aria-label="알림 닫기"
        className="grid size-8 place-items-center rounded-lg text-xl hover:bg-white/20"
        onClick={() => setVisible(false)}
      >
        <span aria-hidden="true">×</span>
      </button>
    </div>
  );
}

export function SignupPage() {
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [code, setCode] = useState('');
  const [codeEdited, setCodeEdited] = useState(false);
  const [requestedEmail, setRequestedEmail] = useState<string | null>(null);
  const [verifiedEmail, setVerifiedEmail] = useState<string | null>(null);
  const [showDetails, setShowDetails] = useState(false);
  const [resendBlocked, setResendBlocked] = useState(false);
  const [notice, setNotice] = useState('');
  const [completedNickname, setCompletedNickname] = useState<string | null>(null);

  const requestCode = useMutation({
    mutationFn: requestSignupEmailCode,
    onSuccess: (_, submittedEmail) => {
      setRequestedEmail(submittedEmail);
      setCode('');
      setCodeEdited(false);
      setResendBlocked(true);
    },
  });

  const confirmCode = useMutation({
    mutationFn: confirmSignupEmailCode,
    onSuccess: (_, submitted) => setVerifiedEmail(submitted.email),
  });

  useEffect(() => {
    if (!resendBlocked) return;
    const timer = window.setTimeout(() => setResendBlocked(false), 30_000);
    return () => window.clearTimeout(timer);
  }, [resendBlocked]);

  const pending = requestCode.isPending || confirmCode.isPending;
  const codeError =
    codeEdited && !/^[0-9]{6}$/.test(code) ? '인증번호는 숫자 6자리로 입력해 주세요.' : undefined;

  function resetEmailVerification() {
    setRequestedEmail(null);
    setVerifiedEmail(null);
    setCode('');
    setCodeEdited(false);
    requestCode.reset();
    confirmCode.reset();
  }

  if (completedNickname !== null) {
    return (
      <section className={cardClassName}>
        <h1 className="text-2xl font-bold">회원가입 완료</h1>
        <p className="mt-2 text-muted">{completedNickname}님, 환영합니다.</p>
        <Link to="/games" className={`mt-6 ${primaryButtonClassName}`}>
          경기 찾기로 이동
        </Link>
        <SignupSuccessToast />
      </section>
    );
  }

  if (showDetails && verifiedEmail) {
    return (
      <section className={cardClassName}>
        <p className="mb-2 text-sm font-semibold text-brand-ink">2 / 2 회원 정보</p>
        <h1 className="text-2xl font-bold">회원가입</h1>
        <SignupDetailsForm
          mode="email"
          email={verifiedEmail}
          onBack={() => setShowDetails(false)}
          onEmailExpired={() => {
            resetEmailVerification();
            setNotice('이메일 인증이 만료됐습니다. 다시 인증해 주세요.');
            setShowDetails(false);
          }}
          onComplete={setCompletedNickname}
        />
      </section>
    );
  }

  return (
    <section className={cardClassName}>
      <p className="mb-2 text-sm font-semibold text-brand-ink">1 / 2 이메일 인증</p>
      <h1 className="text-2xl font-bold">회원가입</h1>
      <p className="mt-2 text-muted">가입에 사용할 이메일을 인증해 주세요.</p>
      {notice && (
        <p role="alert" className="mt-3 text-sm text-red-700">
          {notice}
        </p>
      )}

      <form
        className="mt-6"
        onSubmit={(event) => {
          event.preventDefault();
          if (pending || resendBlocked || verifiedEmail) return;
          setNotice('');
          confirmCode.reset();
          requestCode.mutate(email.trim());
        }}
      >
        <label className="block font-medium">
          이메일
          <input
            type="email"
            autoComplete="email"
            required
            disabled={pending}
            value={email}
            aria-invalid={Boolean(emailError)}
            aria-describedby="signup-error-email"
            className={inputClassName}
            onInvalid={(event) => {
              event.preventDefault();
              setEmailError(
                event.currentTarget.validity.valueMissing
                  ? '이메일을 입력해 주세요.'
                  : '이메일 형식으로 입력해 주세요.',
              );
            }}
            onChange={(event) => {
              const input = event.currentTarget;
              setEmail(input.value);
              setEmailError(
                input.validity.valueMissing
                  ? '이메일을 입력해 주세요.'
                  : input.validity.typeMismatch
                    ? '이메일 형식으로 입력해 주세요.'
                    : '',
              );
              setNotice('');
              resetEmailVerification();
            }}
          />
          <FieldError id="signup-error-email" message={emailError} />
        </label>

        {!verifiedEmail && (
          <button
            type="submit"
            disabled={pending || resendBlocked}
            className={`mt-4 ${primaryButtonClassName}`}
          >
            {requestCode.isPending ? '요청 중…' : '인증번호 전송'}
          </button>
        )}
        {resendBlocked && !verifiedEmail && (
          <p className="mt-2 text-sm text-muted">요청 후 30초가 지나면 다시 보낼 수 있습니다.</p>
        )}
        {requestCode.isError && (
          <p role="alert" className="mt-2 text-sm text-red-700">
            {getErrorMessage(requestCode.error, 'email')}
          </p>
        )}
      </form>

      {requestedEmail && !verifiedEmail && (
        <form
          className="mt-6 border-t border-line pt-6"
          onSubmit={(event) => {
            event.preventDefault();
            if (!pending) confirmCode.mutate({ email: requestedEmail, code });
          }}
        >
          <p role="status" className="mb-4 text-sm text-muted">
            인증번호를 요청했습니다. 이메일을 확인해 주세요.
          </p>
          <label className="block font-medium">
            인증번호 6자리
            <input
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="[0-9]{6}"
              maxLength={6}
              required
              disabled={pending}
              value={code}
              className={inputClassName}
              aria-invalid={Boolean(codeError)}
              aria-describedby="signup-error-code"
              onInvalid={(event) => {
                event.preventDefault();
                setCodeEdited(true);
              }}
              onChange={(event) => {
                setCode(event.target.value);
                setCodeEdited(true);
                confirmCode.reset();
              }}
            />
            <FieldError id="signup-error-code" message={codeError} />
          </label>
          <button type="submit" disabled={pending} className={`mt-4 ${primaryButtonClassName}`}>
            {confirmCode.isPending ? '확인 중…' : '인증번호 확인'}
          </button>
          {confirmCode.isError && (
            <p role="alert" className="mt-2 text-sm text-red-700">
              {getErrorMessage(confirmCode.error, 'code')}
            </p>
          )}
        </form>
      )}

      {verifiedEmail && (
        <div className="mt-6">
          <p role="status" className="rounded-xl bg-brand-soft p-4 text-brand-ink">
            이메일 인증을 완료했습니다.
          </p>
          <button
            type="button"
            onClick={() => setShowDetails(true)}
            className={`mt-4 ${primaryButtonClassName}`}
          >
            다음
          </button>
        </div>
      )}
    </section>
  );
}

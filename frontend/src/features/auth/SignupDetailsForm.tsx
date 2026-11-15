import { useCallback, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { ApiError } from '../../shared/api/http';
import { signupWithKakao } from './auth.api';
import { SignupAddressFields } from './SignupAddressFields';
import { checkSignupNickname, signup } from './signup.api';
import type { SignupGender, SignupPosition } from './signup.api';

type SignupDetailsFormProps =
  | {
      mode: 'email';
      email: string;
      onBack: () => void;
      onEmailExpired: () => void;
      onComplete: (nickname: string) => void;
    }
  | {
      mode: 'kakao';
      ticket: string;
      onBack: () => void;
      onComplete: () => void;
    };

const kakaoAuthorizationPath = '/api/v1/auth/oauth2/kakao/authorization';
const inputClassName = 'mt-2 w-full rounded-xl border border-gray-400 bg-page px-4 py-3';
const primaryButtonClassName =
  'inline-flex h-12 w-full items-center justify-center rounded-xl bg-brand px-4 text-sm font-semibold text-white enabled:hover:bg-brand-hover disabled:opacity-60';
const secondaryButtonClassName =
  'inline-flex h-12 items-center justify-center rounded-xl border border-line px-4 text-sm font-semibold text-brand-ink enabled:hover:bg-brand-soft disabled:opacity-60';
const passwordRule = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[~!@#$%^&*()])[a-zA-Z0-9~!@#$%^&*()]{8,}$/;

const positions: { value: SignupPosition; label: string }[] = [
  { value: 'NONE', label: '상관없음' },
  { value: 'GUARD', label: '가드' },
  { value: 'FORWARD', label: '포워드' },
  { value: 'CENTER', label: '센터' },
];

const genders: { value: SignupGender; label: string }[] = [
  { value: 'MALE', label: '남자' },
  { value: 'FEMALE', label: '여자' },
];

const fieldMessages: Partial<Record<string, string>> = {
  password: '비밀번호는 8자 이상 입력해 주세요.',
  checkPassword: '비밀번호를 한 번 더 입력해 주세요.',
  nickname: '닉네임은 2~12자로 입력해 주세요.',
  name: '이름을 입력해 주세요.',
  birth: '생년월일을 모두 선택해 주세요.',
  genderType: '성별을 선택해 주세요.',
  phone: '010-1234-5678 형식으로 입력해 주세요.',
  zipCode: '우편번호는 숫자 5자리로 입력해 주세요.',
  address: '기본 주소를 입력해 주세요.',
  addressDetail: '상세 주소는 255자 이하로 입력해 주세요.',
  position: '포지션을 선택해 주세요.',
};

type FieldErrors = Partial<Record<string, string>>;

function fieldText(fields: FormData, field: string) {
  return String(fields.get(field) ?? '');
}

function errorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : '요청을 처리하지 못했습니다. 다시 시도해 주세요.';
}

function FieldError({ field, message }: { field: string; message?: string }) {
  return (
    <span
      id={`signup-error-${field}`}
      aria-live="polite"
      className={message ? 'mt-2 block text-xs font-normal text-red-700' : 'sr-only'}
    >
      {message}
    </span>
  );
}

function validate(form: HTMLFormElement, emailSignup: boolean) {
  const fields = new FormData(form);
  const errors: FieldErrors = {};

  for (const input of Array.from(form.elements)) {
    if (
      (input instanceof HTMLInputElement || input instanceof HTMLSelectElement) &&
      input.name &&
      input.willValidate &&
      !input.validity.valid
    ) {
      errors[input.name] = fieldMessages[input.name] ?? input.validationMessage;
    }
  }

  if (emailSignup) {
    const password = fieldText(fields, 'password');
    const confirmation = fieldText(fields, 'checkPassword');

    if (!passwordRule.test(password)) {
      errors.password =
        password.length < 8 ? fieldMessages.password : '영문, 숫자, 특수문자를 포함해 주세요.';
    }

    if (confirmation && password !== confirmation) {
      errors.checkpassword = '비밀번호가 일치하지 않습니다.';
    }
  }

  const nickname = fieldText(fields, 'nickname').trim();
  if (nickname.length < 2 || nickname.length > 12) errors.nickname = fieldMessages.nickname;
  if (!fieldText(fields, 'name').trim()) errors.name = fieldMessages.name;
  if (!fieldText(fields, 'address').trim()) errors.address = fieldMessages.address;
  if (fields.getAll('birth').filter(Boolean).length !== 3) errors.birth = fieldMessages.birth;

  return { fields, errors };
}

export function SignupDetailsForm(props: SignupDetailsFormProps) {
  const [nickname, setNickname] = useState('');
  const [year, setYear] = useState('');
  const [month, setMonth] = useState('');
  const [day, setDay] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState('');
  const [restartRequired, setRestartRequired] = useState(false);
  const submitting = useRef(false);

  const emailSignup = props.mode === 'email';
  const trimmedNickname = nickname.trim();
  const validNickname = trimmedNickname.length >= 2 && trimmedNickname.length <= 12;
  const nicknameCheck = useMutation({ mutationFn: checkSignupNickname });
  const nicknameVerified =
    nicknameCheck.data?.success === true && nicknameCheck.variables === trimmedNickname;

  const registration = useMutation({
    mutationFn: async (fields: FormData) => {
      const position = positions.find(({ value }) => value === fields.get('position'));
      const gender = genders.find(({ value }) => value === fields.get('genderType'));

      if (!position || !gender) {
        throw new Error('포지션과 성별을 확인해주세요.');
      }

      const details = {
        nickname: trimmedNickname,
        name: fieldText(fields, 'name').trim(),
        birth: `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`,
        phone: fieldText(fields, 'phone').trim(),
        address: fieldText(fields, 'address').trim(),
        addressDetail: fieldText(fields, 'addressDetail').trim() || null,
        zipCode: fieldText(fields, 'zipCode').trim(),
        position: position.value,
        genderType: gender.value,
      };

      if (props.mode === 'kakao') {
        await signupWithKakao({ ...details, ticket: props.ticket });
        props.onComplete();
        return;
      }

      const { data } = await signup({
        ...details,
        email: props.email,
        password: fieldText(fields, 'password'),
        checkPassword: fieldText(fields, 'checkPassword'),
      });
      props.onComplete(data.nickname);
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        if (props.mode === 'email' && error.errorCode === 'EMAIL_NOT_VERIFIED') {
          props.onEmailExpired();
          return;
        }

        const errors: FieldErrors = {};
        for (const detail of error.details) {
          if (Object.hasOwn(fieldMessages, detail.field)) {
            errors[detail.field] ??= detail.message;
          }
        }
        if (Object.keys(errors).length) {
          setFieldErrors(errors);
          return;
        }

        if (props.mode === 'email' && error.errorCode === 'ALREADY_EXIST_NICKNAME') {
          nicknameCheck.reset();
          setFieldErrors({ nickname: error.message });
          return;
        }

        if (props.mode === 'email' && error.errorCode === 'PASSWORD_NOT_MATCH') {
          setFieldErrors({ checkPassword: error.message });
          return;
        }
      }
      if (props.mode === 'kakao') setRestartRequired(true);
      setFormError(errorMessage(error));
    },
    onSettled: () => {
      submitting.current = false;
    },
  });

  const clearAddressErrors = useCallback(() => {
    setFieldErrors((current) => ({
      ...current,
      zipCode: undefined,
      address: undefined,
      addressDetail: undefined,
    }));
  }, []);

  function fieldAttributes(field: string) {
    return {
      'aria-invalid': Boolean(fieldErrors[field]),
      'aria-describedby': `signup-error-${field}`,
    };
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submitting.current || nicknameCheck.isPending || restartRequired) return;

    setFormError('');
    const { fields, errors } = validate(event.currentTarget, emailSignup);
    if (!nicknameVerified) errors.nickname ??= '닉네임 중복 확인을 먼저 해 주세요.';
    setFieldErrors(errors);

    if (Object.keys(errors).length > 0) return;

    submitting.current = true;
    registration.mutate(fields);
  }

  const currentYear = new Date().getFullYear();
  const daysInMonth = year && month ? new Date(Number(year), Number(month), 0).getDate() : 0;
  const birthFields = [
    {
      id: 'signup-year',
      label: '년',
      autoComplete: 'bday-year',
      value: year,
      options: Array.from({ length: currentYear - 1899 }, (_, index) => currentYear - index),
      update: (value: string) => {
        setYear(value);
        setDay('');
      },
    },
    {
      id: 'signup-month',
      label: '월',
      autoComplete: 'bday-month',
      value: month,
      options: Array.from({ length: 12 }, (_, index) => index + 1),
      update: (value: string) => {
        setMonth(value);
        setDay('');
      },
    },
    {
      id: 'signup-day',
      label: '일',
      autoComplete: 'bday-day',
      value: day,
      options: Array.from({ length: daysInMonth }, (_, index) => index + 1),
      update: setDay,
    },
  ];

  return (
    <form
      noValidate
      onSubmit={handleSubmit}
      onChange={(event) => {
        const input = event.target;
        if (!(input instanceof HTMLInputElement || input instanceof HTMLSelectElement)) return;

        if (!input.name) {
          return;
        }

        const { fields, errors } = validate(event.currentTarget, emailSignup);
        if (input.id === 'signup-year' || input.id === 'signup-month') {
          errors.birth = fieldMessages.birth;
        }

        setFieldErrors((current) => ({
          ...current,
          [input.name]: errors[input.name],
          ...(input.name === 'password' && fields.get('checkPassword')
            ? { checkPassword: errors.checkPassword }
            : {}),
        }));
      }}
      className="mt-6"
    >
      <fieldset disabled={registration.isPending || restartRequired} className="space-y-5">
        {props.mode === 'email' && (
          <>
            <label className="block font-medium">
              인증된 이메일
              <input
                type="email"
                value={props.email}
                readOnly
                className={`${inputClassName} bg-surface`}
              />
            </label>
            <label className="block font-medium">
              비밀번호
              <input
                {...fieldAttributes('password')}
                name="password"
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                className={inputClassName}
              />
              <FieldError field="password" message={fieldErrors.password} />
              <span className="mt-2 block text-sm font-normal text-muted">
                영문, 숫자, 특수문자를 포함해 8자 이상
              </span>
            </label>
            <label className="block font-medium">
              비밀번호 확인
              <input
                {...fieldAttributes('checkPassword')}
                name="checkPassword"
                type="password"
                autoComplete="new-password"
                required
                className={inputClassName}
              />
              <FieldError field="checkPassword" message={fieldErrors.checkPassword} />
            </label>
          </>
        )}

        <div>
          <label htmlFor="signup-nickname" className="block font-medium">
            닉네임
          </label>
          <div className="flex items-end gap-2">
            <input
              {...fieldAttributes('nickname')}
              id="signup-nickname"
              name="nickname"
              aria-describedby="signup-error-nickname signup-nickname-status"
              autoComplete="nickname"
              required
              minLength={2}
              maxLength={12}
              value={nickname}
              readOnly={nicknameCheck.isPending}
              className={`${inputClassName} min-w-0`}
              onChange={(event) => {
                setNickname(event.target.value);
                nicknameCheck.reset();
                setFormError('');
              }}
            />
            <button
              type="button"
              className={`${secondaryButtonClassName} shrink-0`}
              disabled={!validNickname || nicknameCheck.isPending || nicknameVerified}
              onClick={() => {
                setFieldErrors((current) => ({ ...current, nickname: undefined }));
                nicknameCheck.mutate(trimmedNickname);
              }}
            >
              {nicknameCheck.isPending ? '확인 중…' : '중복 확인'}
            </button>
          </div>
          <FieldError field="nickname" message={fieldErrors.nickname} />
          <div id="signup-nickname-status" aria-live="polite" className="mt-2 text-sm">
            {nicknameVerified && <p className="text-brand-ink">사용 가능한 닉네임입니다.</p>}
            {nicknameCheck.isError && (
              <p className="text-red-700">{errorMessage(nicknameCheck.error)}</p>
            )}
          </div>
        </div>

        <label className="block font-medium">
          이름
          <input
            {...fieldAttributes('name')}
            name="name"
            autoComplete="name"
            required
            className={inputClassName}
          />
          <FieldError field="name" message={fieldErrors.name} />
        </label>

        <fieldset>
          <legend className="font-medium">생년월일</legend>
          <div className="grid grid-cols-3 gap-2">
            {birthFields.map(({ id, label, autoComplete, value, options, update }) => (
              <select
                {...fieldAttributes('birth')}
                key={id}
                id={id}
                name="birth"
                aria-label={`출생 ${label}`}
                autoComplete={autoComplete}
                required
                disabled={id === 'signup-day' && daysInMonth === 0}
                value={value}
                onChange={(event) => update(event.target.value)}
                className={inputClassName}
              >
                <option value="">{label}</option>
                {options.map((option) => (
                  <option key={option} value={option}>
                    {option}
                    {label}
                  </option>
                ))}
              </select>
            ))}
          </div>
          <FieldError field="birth" message={fieldErrors.birth} />
        </fieldset>

        <fieldset>
          <legend className="font-medium">성별</legend>
          <div className="mt-2 grid grid-cols-2 gap-2">
            {genders.map(({ value, label }) => (
              <label key={value} className="block cursor-pointer">
                <input
                  {...fieldAttributes('genderType')}
                  type="radio"
                  name="genderType"
                  value={value}
                  required
                  className="peer sr-only"
                />
                <span className="block rounded-xl border border-line px-4 py-3 text-center peer-checked:border-brand peer-checked:bg-brand-soft peer-checked:text-brand-ink peer-focus-visible:outline-2 peer-focus-visible:outline-brand-ink">
                  {label}
                </span>
              </label>
            ))}
          </div>
          <FieldError field="genderType" message={fieldErrors.genderType} />
        </fieldset>

        <label className="block font-medium">
          휴대폰 번호
          <input
            {...fieldAttributes('phone')}
            name="phone"
            type="tel"
            autoComplete="tel"
            pattern="01[016789]-[0-9]{3,4}-[0-9]{4}"
            placeholder="010-1234-5678"
            required
            className={inputClassName}
          />
          <FieldError field="phone" message={fieldErrors.phone} />
        </label>

        <SignupAddressFields errors={fieldErrors} onAddressSelected={clearAddressErrors} />

        <label className="block font-medium">
          선호 포지션
          <select
            {...fieldAttributes('position')}
            name="position"
            defaultValue=""
            required
            className={inputClassName}
          >
            <option value="" disabled>
              포지션을 선택해 주세요
            </option>
            {positions.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <FieldError field="position" message={fieldErrors.position} />
        </label>

        <div className="grid grid-cols-[1fr_2fr] gap-3 border-t border-line pt-5">
          <button type="button" onClick={props.onBack} className={secondaryButtonClassName}>
            {emailSignup ? '이전' : '취소'}
          </button>
          <button
            type="submit"
            disabled={nicknameCheck.isPending}
            className={primaryButtonClassName}
          >
            {registration.isPending ? '가입 중…' : '회원가입'}
          </button>
        </div>
      </fieldset>

      {formError && (
        <p role="alert" className="mt-3 text-sm text-red-700">
          {formError}
        </p>
      )}
      {restartRequired && (
        <a href={kakaoAuthorizationPath} className={`mt-3 ${secondaryButtonClassName}`}>
          카카오 인증 다시 시작
        </a>
      )}
    </form>
  );
}

import { useEffect, useRef, useState } from 'react';

type PostcodeResult = {
  address: string;
  zonecode: string;
};

type PostcodeConstructor = new (options: {
  oncomplete: (result: PostcodeResult) => void;
  width: string;
  height: string;
}) => {
  embed: (container: HTMLElement, options: { autoClose: boolean }) => void;
};

declare global {
  interface Window {
    kakao?: {
      Postcode?: PostcodeConstructor;
    };
  }
}

const postcodeScriptUrl = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';

let postcodePromise: Promise<PostcodeConstructor> | undefined;

function loadPostcode(): Promise<PostcodeConstructor> {
  if (window.kakao?.Postcode) {
    return Promise.resolve(window.kakao.Postcode);
  }

  postcodePromise ??= new Promise<PostcodeConstructor>((resolve, reject) => {
    const script = document.createElement('script');

    script.src = postcodeScriptUrl;
    script.async = true;

    script.onload = () => {
      const Postcode = window.kakao?.Postcode;

      if (Postcode) {
        resolve(Postcode);
      } else {
        script.remove();
        reject(new Error('주소 검색을 초기화하지 못했습니다.'));
      }
    };

    script.onerror = () => {
      script.remove();
      reject(new Error('주소 검색을 불러오지 못했습니다.'));
    };
    document.head.append(script);
  }).catch((error: unknown) => {
    postcodePromise = undefined;
    throw error;
  });

  return postcodePromise;
}

const inputClassName = 'mt-2 w-full rounded-xl border border-line bg-page px-4 py-3';

type SignupAddressFieldsProps = {
  errors: Partial<Record<'zipCode' | 'address' | 'addressDetail', string>>;
  onAddressSelected: () => void;
};

export function SignupAddressFields({ errors, onAddressSelected }: SignupAddressFieldsProps) {
  const [address, setAddress] = useState('');
  const [zipCode, setZipCode] = useState('');
  const [addressDetail, setAddressDetail] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchError, setSearchError] = useState('');
  const searchContainer = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = searchContainer.current;

    if (!searchOpen || !container) {
      return;
    }

    let active = true;

    void loadPostcode()
      .then((Postcode) => {
        if (!active) {
          return;
        }

        new Postcode({
          width: '100%',
          height: '100%',
          oncomplete: ({ address: selectedAddress, zonecode }) => {
            if (!active) {
              return;
            }

            setAddress(selectedAddress);
            setZipCode(zonecode);
            setAddressDetail('');
            onAddressSelected();
            setSearchOpen(false);
          },
        }).embed(container, { autoClose: false });
      })
      .catch(() => {
        if (active) {
          setSearchError('주소 검색을 불러오지 못했습니다. 주소를 직접 입력해 주세요.');
          setSearchOpen(false);
        }
      });

    return () => {
      active = false;
      container.replaceChildren();
    };
  }, [searchOpen, onAddressSelected]);

  return (
    <fieldset className="space-y-3">
      <legend className="font-medium">주소</legend>

      <div className="flex items-end gap-2">
        <label className="min-w-0 flex-1 text-sm">
          우편번호
          <input
            aria-invalid={Boolean(errors.zipCode)}
            aria-describedby="signup-error-zipCode"
            name="zipCode"
            autoComplete="postal-code"
            inputMode="numeric"
            pattern="[0-9]{5}"
            maxLength={5}
            required
            value={zipCode}
            onChange={(event) => setZipCode(event.target.value)}
            className={inputClassName}
            placeholder="숫자 5자리"
          />
          <span id="signup-error-zipCode" aria-live="polite" className="block text-xs text-red-700">
            {errors.zipCode && <span className="mt-1 block">{errors.zipCode}</span>}
          </span>
        </label>
        <button
          type="button"
          aria-expanded={searchOpen}
          className="shrink-0 rounded-xl border border-line px-4 py-3 text-sm font-semibold text-brand-ink hover:bg-brand-soft"
          onClick={() => {
            setSearchError('');
            setSearchOpen((open) => !open);
          }}
        >
          {searchOpen ? '검색 닫기' : '주소 검색'}
        </button>
      </div>

      {searchOpen && (
        <div
          ref={searchContainer}
          aria-label="카카오 주소 검색"
          className="h-96 overflow-hidden rounded-xl border border-line"
        />
      )}

      {searchError && (
        <p role="alert" className="text-sm text-red-700">
          {searchError}
        </p>
      )}

      <label className="block text-sm">
        기본 주소
        <input
          aria-invalid={Boolean(errors.address)}
          aria-describedby="signup-error-address"
          name="address"
          autoComplete="address-line1"
          required
          maxLength={255}
          value={address}
          onChange={(event) => setAddress(event.target.value)}
          className={inputClassName}
        />
        <span id="signup-error-address" aria-live="polite" className="block text-xs text-red-700">
          {errors.address && <span className="mt-1 block">{errors.address}</span>}
        </span>
      </label>

      <label className="block text-sm">
        상세 주소 <span className="text-muted">(선택)</span>
        <input
          aria-invalid={Boolean(errors.addressDetail)}
          aria-describedby="signup-error-addressDetail"
          name="addressDetail"
          autoComplete="address-line2"
          maxLength={255}
          value={addressDetail}
          onChange={(event) => setAddressDetail(event.target.value)}
          className={inputClassName}
          placeholder="동·호수 등"
        />
        <span
          id="signup-error-addressDetail"
          aria-live="polite"
          className="block text-xs text-red-700"
        >
          {errors.addressDetail && <span className="mt-1 block">{errors.addressDetail}</span>}
        </span>
      </label>
    </fieldset>
  );
}

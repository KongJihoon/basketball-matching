const storageKey = 'basketball:kakao-game-return';

function getGameReturnPath(value: string): string | null {
  try {
    const url = new URL(value, window.location.origin);

    if (url.origin !== window.location.origin || !/^\/games\/[1-9]\d{0,18}$/.test(url.pathname)) {
      return null;
    }

    url.searchParams.delete('login');
    return `${url.pathname}${url.search}`;
  } catch {
    return null;
  }
}

export function rememberKakaoGameReturn(value: string | null) {
  const path = value ? getGameReturnPath(value) : null;

  try {
    if (path) {
      window.sessionStorage.setItem(storageKey, path);
    } else {
      window.sessionStorage.removeItem(storageKey);
    }
  } catch {
    // 저장소를 사용할 수 없으면 콜백에서 기본 경로를 사용한다.
  }
}

export function takeKakaoGameReturn(): string {
  try {
    const path = window.sessionStorage.getItem(storageKey);

    window.sessionStorage.removeItem(storageKey);
    return (path && getGameReturnPath(path)) || '/games';
  } catch {
    return '/games';
  }
}

import { useEffect, useRef, useState } from 'react';

type KakaoLatLng = {
  getLat(): number;
  getLng(): number;
};

type KakaoMap = {
  setCenter(center: KakaoLatLng): void;
  relayout(): void;
};

type KakaoMarker = {
  setMap(map: KakaoMap | null): void;
};

type KakaoMaps = {
  load(callback: () => void): void;
  LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
  Map: new (container: HTMLElement, options: { center: KakaoLatLng; level: number }) => KakaoMap;
  Marker: new (options: { map: KakaoMap; position: KakaoLatLng; title: string }) => KakaoMarker;
};

type MapWindow = Window & { kakao?: { maps?: KakaoMaps } };

let mapsPromise: Promise<KakaoMaps> | undefined;

function loadKakaoMaps(appKey: string): Promise<KakaoMaps> {
  const browser = window as MapWindow;

  if (browser.kakao?.maps?.Map) {
    return Promise.resolve(browser.kakao.maps);
  }

  mapsPromise ??= new Promise<KakaoMaps>((resolve, reject) => {
    const script = document.createElement('script');
    const url = new URL('https://dapi.kakao.com/v2/maps/sdk.js');

    url.searchParams.set('appkey', appKey);
    url.searchParams.set('autoload', 'false');

    script.src = url.toString();
    script.async = true;

    const timeoutId = window.setTimeout(fail, 10_000);

    function fail() {
      window.clearTimeout(timeoutId);
      script.remove();
      reject(new Error('카카오 지도 SDK를 불러오지 못했습니다'));
    }

    script.onerror = fail;
    script.onload = () => {
      const maps = browser.kakao?.maps;

      if (!maps?.load) {
        fail();
        return;
      }

      maps.load(() => {
        if (!maps.Map || !maps.LatLng || !maps.Marker) {
          fail();
          return;
        }

        window.clearTimeout(timeoutId);
        resolve(maps);
      });
    };

    document.head.appendChild(script);
  }).catch((error: unknown) => {
    mapsPromise = undefined;
    throw error;
  });

  return mapsPromise;
}

type GameVenueMapProps = {
  latitude: number | null;
  longitude: number | null;
  placeName: string;
};

function VenueMapCanvas({
  latitude,
  longitude,
  placeName,
  appKey,
}: {
  latitude: number;
  longitude: number;
  placeName: string;
  appKey: string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);

  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');

  useEffect(() => {
    const container = containerRef.current;

    if (!container) return;

    let active = true;
    let marker: KakaoMarker | undefined;
    let observer: ResizeObserver | undefined;

    void loadKakaoMaps(appKey)
      .then((maps) => {
        if (!active) return;

        const center = new maps.LatLng(latitude, longitude);
        const map = new maps.Map(container, { center, level: 4 });

        marker = new maps.Marker({ map, position: center, title: placeName });
        observer = new ResizeObserver(() => {
          map.relayout();
          map.setCenter(center);
        });

        observer.observe(container);

        setStatus('ready');
      })
      .catch(() => {
        if (active) setStatus('error');
      });

    return () => {
      active = false;
      observer?.disconnect();
      marker?.setMap(null);
      container.replaceChildren();
    };
  }, [appKey, latitude, longitude, placeName]);

  if (status === 'error') {
    return <p className="mt-4 text-sm text-muted">지도를 불러오지 못했습니다.</p>;
  }

  return (
    <div className="relative mt-4 overflow-hidden rounded-xl border border-line bg-surface">
      <div
        ref={containerRef}
        role="region"
        aria-label={`${placeName} 위치 지도`}
        className="h-64 w-full sm:h-72"
      />
      {status === 'loading' && (
        <p
          role="status"
          className="absolute inset-0 z-10 grid place-items-center bg-surface text-sm text-muted"
        >
          지도를 불러오고 있습니다.
        </p>
      )}
    </div>
  );
}

export function GameVenueMap({ latitude, longitude, placeName }: GameVenueMapProps) {
  const appKey = import.meta.env.VITE_KAKAO_MAP_APP_KEY?.trim();

  if (
    latitude === null ||
    longitude === null ||
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    Math.abs(latitude) > 90 ||
    Math.abs(longitude) > 180
  ) {
    return <p className="mt-4 text-sm text-muted">경기장의 지도 위치를 확인할 수 없어요.</p>;
  }

  if (!appKey) {
    return <p className="mt-4 text-sm text-muted">지도를 불러올 수 없어요.</p>;
  }

  return (
    <VenueMapCanvas
      key={`${latitude},${longitude}, ${placeName}`}
      appKey={appKey}
      latitude={latitude}
      longitude={longitude}
      placeName={placeName}
    />
  );
}

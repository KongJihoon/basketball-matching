import { useEffect, useId, useRef } from 'react';

type GameParticipationDialogProps = {
  action: 'join' | 'cancel';
  gameTitle: string;
  startTime: string;
  placeName: string;
  conditions: string;
  isPending: boolean;
  canConfirm: boolean;
  errorMessage?: string;
  onClose: () => void;
  onConfirm: () => void;
};

export function GameParticipationDialog({
  action,
  gameTitle,
  startTime,
  placeName,
  conditions,
  isPending,
  canConfirm,
  errorMessage,
  onClose,
  onConfirm,
}: GameParticipationDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const isCancel = action === 'cancel';

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    dialog.showModal();
    return () => dialog.close();
  }, []);

  function close() {
    if (isPending) return;

    dialogRef.current?.close();
    onClose();
  }

  const confirmColor = isCancel
    ? 'bg-red-700 enabled:hover:bg-red-800'
    : 'bg-brand-ink enabled:hover:bg-green-800';

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
      className="fixed inset-0 m-auto max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-md overflow-y-auto rounded-2xl border border-line bg-page p-0 text-foreground shadow-xl backdrop:bg-black/50"
    >
      <div className="p-5 sm:p-6">
        <h2 id={titleId} className="text-xl font-bold">
          {isCancel ? '경기 참가를 취소하시겠습니까?' : '이 경기에 참가하시겠습니까?'}
        </h2>

        <div className="mt-4 space-y-1 rounded-xl bg-surface p-4 text-sm">
          <p className="wrap-break-word font-semibold">{gameTitle}</p>
          <p className="text-muted">{startTime}</p>
          <p className="wrap-break-word text-muted">{placeName}</p>
          <p className="text-muted">{conditions}</p>
        </div>

        {errorMessage && (
          <p role="alert" className="mt-4 text-sm text-red-700">
            {errorMessage}
          </p>
        )}

        <div className="mt-5 grid grid-cols-2 gap-2">
          <button
            type="button"
            disabled={isPending}
            onClick={close}
            className="min-h-11 rounded-xl border border-line px-3 text-sm font-semibold enabled:hover:bg-surface disabled:opacity-50"
          >
            {isCancel ? '참가 유지' : '닫기'}
          </button>
          <button
            type="button"
            disabled={isPending || !canConfirm}
            onClick={onConfirm}
            className={`min-h-11 rounded-xl px-3 text-sm font-semibold text-white disabled:opacity-50 ${confirmColor}`}
          >
            {isPending ? '처리 중…' : isCancel ? '참가 취소' : '참가 신청'}
          </button>
        </div>
      </div>
    </dialog>
  );
}

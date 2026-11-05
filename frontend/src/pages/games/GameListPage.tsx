export function GameListPage() {
  return (
    <div>
      <section
        aria-labelledby="game-page-title"
        className="rounded-3xl bg-brand-soft px-6 py-10 sm:px-10 sm:py-14"
      >
        <p className="text-sm font-semibold text-brand-ink">Basketball Matching</p>

        <h1
          id="game-page-title"
          className="mt-4 text-xl leading-tight font-bold tracking-tight sm:text-4xl"
        >
          원하는 시간,
          <br />
          함께 뛸 사람
        </h1>
      </section>

      <section
        aria-labelledby="service-description"
        className="mt-8 rounded-2xl border border-line bg-surface p-6"
      >
        <h2 id="service-description" className="text-xl font-bold">
          함께 뛰는 즐거움, 농구 매칭
        </h2>

        <p className="mt-3 leading-7 text-muted">
          나에게 맞는 농구 경기를 찾고, 새로운 사람들과 코트에서 만나세요.
        </p>
      </section>
    </div>
  );
}

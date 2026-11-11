# RAGMe! frontend

React 18 + TypeScript + Vite. The approved design is `../docs/design/raga-prototype.html`; the migration notes and backend gaps are in `../docs/frontend-migration-plan.md`.

## Run

```bash
npm install
npm run dev      # http://localhost:5173, /api is proxied to the Spring Boot app on :8080
npm run lint
npm test         # answer-adapter checks (Node 22.18+)
npm run build    # type-checks, then writes the site into ../src/main/resources/static for Spring to serve
```

Build the frontend before `docker build`: the image packages whatever is in `src/main/resources/static`.

## Environment

Copy `.env.example` to `.env.local`:

- `VITE_API_BASE_URL`: leave empty for the dev proxy and for the Spring-served build.
- `VITE_API_KEY`: must match `APP_API_KEY` on the backend. It is visible in the browser bundle, so use it for demos only.

## Layout

- `src/api/`: every network call (`client.ts`, `documents.ts`), the answer adapter (`adapters.ts`) and the local stand-in for features the backend lacks (`local.ts`, marked `MOCK: needs backend support`).
- `src/store/store.tsx`: notebooks, sources, chat, notes and Studio state.
- `src/pages/`: landing (`#home`), notebook list (`#notebooks`), workspace (`#nb-<id>`).
- `src/lib/sound.ts`: all click sounds. One delegated listener plays the root `sounds/` asset (sliced and pitched per action). Choose a sound per control with `data-sound="open|close|toggle|confirm|success|none"`. The on/off preference is stored in `localStorage['ragme-sound']`.
- `src/lib/motion.tsx`: Motion setup (lazy-loaded features, `prefers-reduced-motion` respected) and the `Reveal` / `RevealGroup` / `RevealItem` scroll helpers. Small hover, press and entrance effects live in `app.css`.
- `src/styles/tokens.css`: all colours and fonts, for light and dark themes. `app.css` uses only these tokens.

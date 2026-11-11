# Frontend migration plan: RAGA prototype → React app

Status: **implemented** (2026-11-11). Decisions taken on the open questions in section 7:

| # | Decision | What was built |
|---|---|---|
| D1 | Use the inner frontend; remove the outer copy after checking it | The outer `ai-research-platform/frontend` was an older copy with no unique work, so it was removed. The outer CI now builds `ai-research-platform/frontend`. |
| D2 | Accept only TXT, MD, CSV, HTML, JSON | PDF, Word, Excel and other types appear as failed sources with a specific "how to fix" message and only a Remove button. Files over 1 MB and empty files are rejected the same way. HTML is converted to its visible text before upload. |
| D3 | Empty start, no sample notebooks | No sample data and no scripted answers. "Try it yourself" goes to `#notebooks`. |
| D4 | Keep the Website tab, clearly unavailable | The tab shows a notice and keeps its validation. A valid address explains that websites can't be added yet and offers "Use Paste text". Nothing is added. |
| D5 | Summary real; the other Studio tiles "coming soon" | Summary calls `POST /api/summarize` with the ticked sources' text. Study guide, FAQ and Key topics carry a "Soon" badge and show a toast. |
| D6 | Replace prototype wording | The "Show an error" chip and all "prototype" notes are removed. Landing copy now describes only what the product does today (file types, honest source on/off behaviour, where data is kept). |
| D7 | Plain error copy | `src/api/client.ts` has one message per failure kind: timeout, can't reach server, access settings, busy, answering service unavailable, invalid request, server error. Upload failures have their own messages. |

Two behaviours changed from the plan: removing a source only removes it from the notebook (G5), and the "partial answer" note now says "Part of this answer may come from documents that are switched off or not in this notebook". The prototype's "details are left out" wording would be false, because the backend can't leave them out (G3).

## 0. What I read

| Item | Location | Notes |
|---|---|---|
| Prototype (source of truth) | `E:\Projects\RAGA\design\RAGA AI Platform.html` | Not at `docs/design/raga-prototype.html`. Read in full (493 lines: CSS, templates, sample data, all handlers). |
| Visual reference | `E:\Projects\RAGA\design\RAGA.pdf` | Will be used during visual checks (step 8). |
| Active frontend | `ai-research-platform/ai-research-platform/frontend/` | React 18 + Vite 5 + **TypeScript**. Has `package-lock.json` and `node_modules`. Builds into `../src/main/resources/static` (Spring serves it). The README points here. |
| Duplicate frontend | `ai-research-platform/frontend/` | Near-identical stale copy (no lockfile, `outDir` points to `../ai-research-platform/src/main/resources/static`). See open decision D1. |
| Backend | `ai-research-platform/ai-research-platform/src/main/java/...` | 4 controllers, read with their DTOs, services, security and error handler. |

Proposal: copy the prototype and PDF to `docs/design/raga-prototype.html` and `docs/design/raga-prototype.pdf` in this project so the paths in your brief are true.

### Findings that shape the plan

1. **The old UI is one page** (`App.tsx`, 160 lines): "Add document" (title + text → `POST /api/webhooks/add-document`) and "Ask a question" (`POST /api/webhooks/ask`). API helper `src/api.ts` sends `X-API-Key` from `VITE_API_KEY`, base URL from `VITE_API_BASE_URL`; Vite proxies `/api` → `localhost:8080`. All of this is reused.
2. **`src/api.ts` always sets `Content-Type: application/json`**, which breaks multipart upload. The new client sets it only for JSON bodies.
3. **`npm run lint` is broken today.** `eslint.config.js` imports `eslint`, `@eslint/js`, `globals`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, none of which are installed. I will add them as devDependencies (no runtime dependencies are added).
4. **The Docker image does not build the frontend.** `Dockerfile` only runs Maven on `src/`, and picks up whatever `npm run build` last wrote into `src/main/resources/static`. Build order stays: `npm run build`, then `docker build`. No Docker change.
5. **Spring has no SPA fallback.** A path like `/notebooks/n1` would 404 on refresh in the Docker build. The prototype uses hash routes, so using them avoids any backend change (see section 1).
6. **CORS is already open** (`CorsConfig`: any origin, all methods, any header). No CORS change is needed.
7. Every `/api/**` call except `/api/health` needs the API key. The key is baked into the browser bundle (existing behaviour, documented in `.env.example`). Unchanged.

---

## 1. Routes

The prototype routes are `#home`, `#notebooks` and `#nb-<id>`. I will copy them exactly, using a ~15-line `useHashRoute` hook (no router dependency). Hash routing also works on refresh when Spring serves the build.

| Prototype hash | Your brief's path | Page |
|---|---|---|
| `#home` (also empty hash) | `/` | Landing website |
| `#notebooks` | `/notebooks` | Notebook list |
| `#nb-<id>` | `/notebooks/:id` | Workspace. An unknown id falls back to `#notebooks`, as in the prototype. |
| any other hash | | Falls back to `#home` |

Behaviour copied from the prototype router: scroll to top on every route change, close any dialog on route change, and default to the Chat tab when entering a notebook at ≤980px.

Landing links, matching the prototype: logo → `#home`. "Open RAGA", "Try RAGA" and "Start a notebook" → `#notebooks`. "Try it yourself" → `#nb-n1` (see D3). Nav and footer "Product", "How it works", "Who it's for" and "FAQ" scroll to `#product`, `#how`, `#uses` and `#faq` (instant scroll under reduced motion). "Create an account", "Privacy", "Terms", "Contact", "Share" and "Help" show the prototype's toasts word for word.

## 2. Component tree

```
src/
  main.tsx                     theme init from localStorage('raga-theme'), mount <App/>
  App.tsx                      useHashRoute → <Landing/> | <AppTop/> + <Notebooks/> | <Workspace/>; <ToastHost/>
  styles/
    tokens.css                 all custom properties (section 3), light + dark
    app.css                    one global stylesheet ported 1:1 from the prototype CSS, colours via tokens only
  api/
    client.ts                  fetch wrapper: base URL, X-API-Key, timeout (AbortController), error mapping
    documents.ts               REAL: uploadFile, addText, ask, searchPassages, summarize
    adapters.ts                RAGResponse + search hits → Answer {status, segments, citations, partial}
    local.ts                   MOCK: notebooks, source list, on/off, notes, studio results (localStorage)
  store/
    store.tsx                  one context + useReducer: notebooks, sources, chats, notes, outputs, undo
  components/
    Icon.tsx                   the prototype's 30 path icons (same `P` map, same 24px stroke)
    Mascot.tsx                 moods happy/think/wow/oops/shrug × colours violet/lime/pink/blue, useId for gradient ids
    Scribble.tsx               hero scribbles
    SpinBadge.tsx              rotating pink "ASK" badge
    Ribbon.tsx                 scrolling lime ribbon
    Toast.tsx                  #toast with optional action (Undo), 2.8 s / 6 s like the prototype
    Dialog.tsx                 scrim + focus trap + Escape + focus return; used by the modal and the drawer
    AppTop.tsx                 app header: logo, Notebooks, Help, theme, back to site, avatar
    ThemeToggle.tsx            sets data-theme on <html>, stores it in localStorage
  pages/
    Landing.tsx                hero, ribbon, tiles, product + mock, features bento, how (tilts), uses, lime trust band, FAQ <details>, CTA, footer
    Notebooks.tsx              heading, search, "New notebook" card, notebook cards, empty search state, NewNotebookDialog
    workspace/
      Workspace.tsx            header (back, title, Share, Add source), tabs3 (≤980px), the three panels
      SourcesPanel.tsx         source rows: checkbox, file-type badge, view, remove + undo, reading bar, failed + Try again/Remove, "N of M in use"
      ChatPanel.tsx            welcome, messages, loading steps, typewriter reveal, cite chips, refs, copy, save, none/err states, composer + suggestions
      StudioPanel.tsx          4 tiles, result cards (copy, save, remove), Saved notes
      SourceViewer.tsx         drawer, highlights and scrolls to the cited passage
      AddSourcesDialog.tsx     tabs Upload files (drag and drop) / Website / Paste text, prototype validation copy
```

About 25 files. No UI libraries, no router, no state library, no icon library.

## 3. Design tokens (`styles/tokens.css`)

Copied from the prototype `:root`. The prototype has more tokens than the brief lists (status colours, font stacks). The HTML wins, so they are all included.

**Brand (both themes)**
`--b-violet #5B3CFF` · `--b-violet-d #3A1FD1` · `--b-lime #B9FA3C` · `--b-navy #04045E` · `--b-pink #FF7BC8` · `--b-ink #0A0A14`

| Token | Light | Dark |
|---|---|---|
| `--bg` | #F2F2F9 | #0A0A10 |
| `--surface` | #FFFFFF | #14141D |
| `--surface-2` | #EAEAF5 | #1F1F2C |
| `--ink` | #0B0B22 | #F4F4FB |
| `--ink-2` | #575772 | #A8A8C2 |
| `--line` | #DEDEEC | #2C2C3E |
| `--accent` | #4B2FE8 | #A092FF |
| `--accent-soft` | #E6E0FF | #2A2458 |
| `--ok` / `--ok-soft` | #137A40 / #DCF4E6 | #5EE394 / #10301F |
| `--warn` / `--warn-soft` | #9A5B00 / #FFEBC7 | #FFB547 / #3A2A0A |
| `--bad` / `--bad-soft` | #C72A34 / #FFE0E2 | #FF7079 / #3C1519 |

**Fixed colours the prototype hard-codes in CSS.** These become named tokens so components hold no hex values:
`--on-lime #10130A` · `--on-pink #1A0A14` · `--band-dark #0C0C12` · `--foot-bg #08080D` · `--mock-bg #1A1A26` · `--mock-line #2C2C40` · `--mock-panel #0E0E18` · `--mock-item #1A1A28` · `--mock-chip #222234` · `--toast-bg #0A0A14` · `--scrim rgba(5,5,20,.55)`. The file-type badge pairs (pdf, docx, xlsx/csv, md/txt, html/link, json) and the mascot gradient palette (`COL`, face ink `#12122B`) also become tokens.

**Theme switching:** `prefers-color-scheme` by default (`:root:not([data-theme="light"])`). The toggle sets `data-theme` on `<html>` and stores `raga-theme` in localStorage. The overrides for `.big.dk em` and `.apptop .logo` (lime in dark mode) are copied.

**Fonts:** Anton (display, uppercase), Inter 400–800 (UI), Instrument Serif italic (`em`). They load from the same Google Fonts URL as the prototype, with the prototype's fallbacks: `'Anton','Impact',sans-serif`, `'Inter',system-ui,sans-serif` and `'Instrument Serif',Georgia,serif`. No new package. (`@fontsource` is the alternative if you need offline or self-hosted fonts. See D6.)

**Shapes:** pill buttons and chips 999px, cards 24px, notebook cards, tilts and modals 28px, `.wrap` max-width 1280px with a 16px gutter (safe-area aware).
**Breakpoints:** 1180px, 980px, 640px (rules copied as is).
**Motion:** `bob` 4.5 s, `spin` 20 s, `mq` 34 s ribbon, card hover lift, `pop` and `slide` for dialogs, `blink` caret, `pulse` dot, typewriter reveal. All of it is disabled by the prototype's `prefers-reduced-motion` rule, and the typewriter shows the full text at once in that case.

## 4. UI action → backend endpoint

"REAL" means an existing endpoint, unchanged. "MOCK" means local state in `api/local.ts`, marked `// MOCK: needs backend support`.

| UI action | Endpoint | Notes |
|---|---|---|
| Upload files (drop or browse) | **REAL** `POST /api/documents/upload` (multipart `file`) | Backend reads the file as UTF-8 text: only `.txt .md .csv .html .json` produce usable content. PDF/Word/Excel do not. See gap G1 and D2. Spring's default upload limit is 1 MB. |
| Source "reading" progress bar | REAL (request in flight) | Indeterminate bar while the upload request is pending. Most of the time is spent on the server, so a percentage would be misleading. |
| Failed source → Try again | **REAL** re-runs the upload | The `File` is kept in memory for the session. After a reload, "Try again" is not possible, so only "Remove" is shown. |
| Paste text → Add source | **REAL** `POST /api/documents` `{title, content, source}` | Title max 500 characters, content max 100,000 (backend validation). |
| Website → Add source | Not available (explained in the tab) | No URL ingestion endpoint. See G2 and D4. |
| Ask a question / suggested question | **REAL** `POST /api/documents/ask` `{question}` → `{answer, sourcesUsed[], documentsRetrieved}` | Question max 1000 characters. Timeout 60 s → the prototype's error bubble. |
| Citation passages, source chips | **REAL** `GET /api/documents/search?query=<q>&limit=3` | Same retrieval the ask endpoint uses (top 3), so it returns the exact passages behind the answer, with their source title. Called in parallel with ask. |
| Loading steps | Client | Steps advance on timers while the real request is pending. The answer is shown when it arrives. |
| Streaming answer | Client (presentation only) | No streaming endpoint. The full real answer is revealed word by word, like the prototype. See G6. |
| "I couldn't find that in your sources" | REAL plus adapter | Shown when `sourcesUsed` is empty, when the backend returns its "no documents" sentence, or when every cited source is unticked or not in this notebook (→ "switched off" wording). See G3. |
| "Some details are left out…" note | Adapter | Shown when some, but not all, cited sources are unticked. See G3 for what this does and does not guarantee. |
| Citation number / ref chip → source viewer | REAL passages from `/search` | The drawer shows the passages retrieved for that source and highlights the cited one. |
| Source eye icon → viewer | Pasted text: local copy. Uploaded text files: local copy (read with `File.text()` before upload). Otherwise MOCK. | No endpoint returns a document's text. See G4. |
| List sources | **MOCK** | No list endpoint. `GET /api/documents/stats` only returns a chunk count. |
| Tick / untick source, "N of M in use" | **MOCK** (client) | The backend cannot filter by source. See G3. |
| Remove source + Undo | **MOCK** | No delete endpoint. The document stays in the knowledge base and can still be cited. See G5. |
| Notebooks: list, search, create | **MOCK** | No notebook concept. All documents share one collection. See G7. |
| Studio → Summary | **REAL** `POST /api/summarize` `{content}` | Content is the locally held text of the ticked sources, up to 100,000 characters. If no text is held locally (PDF etc.), the card shows the prototype's "not available" style message. See D5. |
| Studio → Study guide, FAQ, Key topics | Not built ("Soon" badge + toast) | No endpoint. See D5. |
| Copy answer / result / note | Client (clipboard) | Prototype toasts "Copied" and "Copy is blocked here…". |
| Save to notes, Saved notes, delete note | **MOCK** (localStorage) | No notes endpoint. |
| New chat | Client | Clears the chat in memory, as the prototype does. |
| Theme toggle, toasts, Share, Help, Privacy, Terms, Contact, Create an account | Client | Prototype toast copy. |
| (startup) backend reachable? | REAL `GET /api/health` | Optional. Only used to decide whether to show the error state early. |

`/api/webhooks/*` and `/api/research/{topic}` are not used. The webhooks duplicate `/api/documents/*` for n8n. Research answers from general knowledge, not from the user's sources, which would break the "no guessing" promise.

## 5. Backend gaps

Every MOCK lives behind `src/api/local.ts` with a `// MOCK: needs backend support` comment. The UI never claims a mocked feature works.

| # | Gap | Effect in the UI today | Smallest backend fix (not doing it unless you ask) |
|---|---|---|---|
| G1 | Upload reads raw bytes as UTF-8, so PDF/Word/Excel become unreadable content | Those files would upload "successfully" but answers would be garbage | Extract text in `uploadFile` (Apache Tika). |
| G2 | No website ingestion | Website tab cannot really add a page | `POST /api/documents/url` that fetches and extracts the page. |
| G3 | Ask searches **all** documents. There is no filter by source or notebook. | Unticking a source cannot stop it being used. The adapter can only hide answers whose cited sources are all off, and flag partial ones. | Accept an optional list of source titles in `AskQuestionRequest` and pass it as a Chroma `where` filter. |
| G4 | No endpoint returns a document's full text | The viewer can only show retrieved passages, or text the browser kept | `GET /api/documents/{title}/chunks` (Chroma `get` with `where`). |
| G5 | No delete | "Remove" only hides the source in this browser | `DELETE /api/documents?title=…` (Chroma delete with `where`). |
| G6 | No streaming | The answer appears word by word after it fully arrives | SSE endpoint. Low priority. |
| G7 | No notebooks, no source list, no notes | Notebooks, sources and notes live in this browser only (localStorage). They are lost on another device, and documents from other notebooks can be cited. | Persist notebooks and store `notebook_id` in chunk metadata. |
| G8 | No study guide, FAQ or topics generation | Those tiles are mocks | One `POST /api/studio/{kind}` that reuses `GeminiService`. |
| G9 | Answer text has no citation markers, and `sourcesUsed` is titles only | Citation numbers sit at the end of the answer, not after the exact sentence | Ask the model to tag sentences with `[n]` in the prompt. |

## 6. Old frontend files to remove

In `ai-research-platform/ai-research-platform/frontend/`:

| File | Action |
|---|---|
| `src/App.tsx` | Replace |
| `src/App.css`, `src/index.css` | Delete (replaced by `styles/tokens.css` and `styles/app.css`) |
| `src/api.ts` | Delete. Its logic moves into `src/api/client.ts`. |
| `src/assets/hero.png`, `src/assets/react.svg`, `src/assets/vite.svg` | Delete (unused Vite template assets) |
| `public/icons.svg` | Delete (unused template sprite) |
| `README.md` | Replace the Vite template text with a short run, build and env section |
| `index.html` | Edit: title "RAGA AI Platform", fonts preconnect and link, `theme-color` |
| `public/favicon.svg` | Keep for now (the prototype has none) |
| `src/main.tsx`, `src/vite-env.d.ts`, `vite.config.ts`, `tsconfig*`, `eslint.config.js`, `.env.example`, `.gitignore` | Keep. `main.tsx` imports change. |
| `../src/main/resources/static/*` | Regenerated by `npm run build` (`emptyOutDir`). Not edited by hand. |

`package.json`: no runtime dependencies added or removed. The devDependencies needed by the existing `eslint.config.js` are added so `npm run lint` works.

Outside the active frontend, only with your approval: the duplicate `ai-research-platform/frontend/` folder (D1).

## 7. Open decisions (please answer before I start coding)

- **D1. Duplicate frontend.** I'll replace `ai-research-platform/ai-research-platform/frontend` (the one with the lockfile, which the README uses). Should I delete the stale `ai-research-platform/frontend` copy, or leave it alone?
- **D2. PDF/Word/Excel uploads (G1).** Options: (a) send them anyway (they "upload" but answers are wrong); (b) **recommended:** mark them as failed with the prototype's failed-source pattern, saying RAGA can't read that file type yet, until the backend extracts text; (c) you approve a small backend change (Tika) and they work for real. The prototype's message for unsupported files lists PDF, Word and Excel as readable, so (b) needs one sentence of copy that is not in the prototype. I'll propose it before using it.
- **D3. Sample notebooks.** The prototype ships three sample notebooks (Company Handbook, Biology Revision, Market Research Q4) with scripted answers, and "Try it yourself" links to the first one. With a real backend their documents don't exist. Options: (a) **recommended:** start with no notebooks; "Try it yourself" goes to `#notebooks`; (b) keep them as a clearly labelled local demo with scripted answers. That would show mocked answers as if they worked, so I advise against it.
- **D4. Website tab (G2).** (a) **recommended:** keep the tab and its validation; on submit, add the source in the prototype's failed state with a short "can't add websites yet" line; (b) hide the tab. Either way it's new copy, or a change from the prototype.
- **D5. Studio.** Summary can be real through `/api/summarize` when the browser holds the source text. Study guide, FAQ and Key topics have no endpoint. (a) **recommended:** keep the tiles. Unsupported results use the prototype's own placeholder line style ("…not available yet") instead of fake content. (b) Generate them by sending fixed questions through `/api/documents/ask`. That is real, but only uses three passages, so results are thin.
- **D6. Prototype-only copy.** Some prototype text describes the prototype itself: the footer's "Design prototype with sample content.", the toast "Files stay in your browser in this prototype.", "Prototype note: files you add are not read…", "Summaries are written for the sample notebooks only in this prototype.", and the dashed **"Show an error"** chip, which forces a fake error. My recommendation: drop the "Show an error" chip and the prototype notes, and keep the footer line until you give me final copy. Confirm, or tell me to keep them word for word.
- **D7. Error copy.** The prototype has exactly one error message: "Something went wrong / RAGA took too long to answer. Your question is still here." I'll use it for timeouts. For other failures (backend down, bad API key, rate limit) I'd reuse the same heading with a neutral body such as "RAGA couldn't answer just now. Your question is still here." Approve, or give me wording.

## 8. Verification plan (after approval and build)

1. `npm run lint`, `npm run build` (`tsc -b && vite build`), then `docker build` in `ai-research-platform/ai-research-platform`.
2. Run Vite on :5173 next to the prototype HTML. Take screenshots at 1360px and 400px, in light and dark, for landing, notebooks and workspace, plus 375, 768 and 1024 to check for horizontal overflow. Compare with `RAGA.pdf`.
3. Click through every item in sections 4 and 5 of your brief, plus keyboard-only passes on dialogs and the drawer (focus trap, Escape, focus return).
4. With `docker compose up` (backend + ChromaDB): upload a `.txt` file, paste text, ask a question, open a citation, and check the passage.
5. Contrast check of text and token pairs in both themes. `.src.off` uses `opacity:.6`, which may fall below AA, and I'll report it if so.
6. Final report: what was replaced, what is REAL, what is MOCK (gaps G1–G9), and any differences from the prototype.

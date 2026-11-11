/**
 * The only place that talks to the network.
 * Base URL: empty in dev (Vite proxies /api to :8080) and in the Spring-served build (same origin).
 */
const base = import.meta.env.VITE_API_BASE_URL ?? ''
const apiKey = import.meta.env.VITE_API_KEY ?? ''

export type ApiErrorKind = 'timeout' | 'network' | 'auth' | 'rate' | 'ai' | 'invalid' | 'server' | 'aborted'

export class ApiError extends Error {
  readonly kind: ApiErrorKind
  constructor(kind: ApiErrorKind) {
    super(kind)
    this.kind = kind
  }
}

/** Plain-language copy for every failure. Never shows technical details to the user. */
export const ERROR_COPY: Record<ApiErrorKind, string> = {
  timeout: 'RAGMe! took too long to answer. Your question is still here.',
  network: 'RAGMe! can’t reach its server right now. Check your connection and try again.',
  auth: 'RAGMe! isn’t connected to its service correctly. Ask your administrator to check the access settings.',
  rate: 'RAGMe! is handling a lot of requests right now. Wait a moment and try again.',
  ai: 'The answering service is unavailable at the moment. Please try again in a few minutes.',
  invalid: 'RAGMe! couldn’t process that request. Check what you entered and try again.',
  server: 'Something went wrong on our side. Please try again.',
  aborted: 'Cancelled.',
}

export const errorKind = (e: unknown): ApiErrorKind => (e instanceof ApiError ? e.kind : 'server')

type Options = {
  method?: 'GET' | 'POST'
  json?: unknown
  form?: FormData
  timeoutMs?: number
  signal?: AbortSignal
}

export async function request<T>(path: string, { method = 'GET', json, form, timeoutMs = 60_000, signal }: Options = {}): Promise<T> {
  const headers = new Headers()
  if (apiKey) headers.set('X-API-Key', apiKey)
  if (json !== undefined) headers.set('Content-Type', 'application/json')

  const ctrl = new AbortController()
  let timedOut = false
  const timer = setTimeout(() => {
    timedOut = true
    ctrl.abort()
  }, timeoutMs)
  const onAbort = () => ctrl.abort()
  signal?.addEventListener('abort', onAbort)

  let res: Response
  try {
    res = await fetch(`${base}${path}`, {
      method,
      headers,
      body: form ?? (json !== undefined ? JSON.stringify(json) : undefined),
      signal: ctrl.signal,
    })
  } catch {
    throw new ApiError(timedOut ? 'timeout' : signal?.aborted ? 'aborted' : 'network')
  } finally {
    clearTimeout(timer)
    signal?.removeEventListener('abort', onAbort)
  }

  const text = await res.text().catch(() => '')
  if (!res.ok) throw new ApiError(classify(res.status, text))
  try {
    return JSON.parse(text) as T
  } catch {
    throw new ApiError('server')
  }
}

function classify(status: number, body: string): ApiErrorKind {
  let error = ''
  try {
    error = String((JSON.parse(body) as { error?: unknown }).error ?? '')
  } catch {
    /* not JSON */
  }
  // The backend passes the AI provider's own status through with this label.
  if (error === 'External service error') return status === 429 ? 'rate' : 'ai'
  if (status === 401 || status === 403) return 'auth'
  if (status === 429) return 'rate'
  if (status === 400 || status === 413 || status === 422) return 'invalid'
  // Dev proxy with the backend down answers 500/502/504 with an empty body; a 404 means no RAGMe! server at that address.
  if (!body.trim() || status === 404 || status === 502 || status === 503 || status === 504) return 'network'
  return 'server'
}

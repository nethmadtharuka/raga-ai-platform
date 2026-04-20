/**
 * API base: in dev, leave empty to use Vite proxy (/api -> localhost:8080).
 * For production static build served by Spring, same-origin "" works.
 */
const base = import.meta.env.VITE_API_BASE_URL ?? ''

const apiKey = import.meta.env.VITE_API_KEY ?? ''

export async function apiFetch(
  path: string,
  init: RequestInit = {}
): Promise<Response> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (apiKey) {
    headers.set('X-API-Key', apiKey)
  }
  return fetch(`${base}${path}`, { ...init, headers })
}

/** REAL backend endpoints (Spring Boot DocumentController / ResearchController). */
import { request } from './client'

export type RagResponse = { answer?: string; sourcesUsed?: string[]; documentsRetrieved?: number }
export type Passage = { title: string; content: string }

/** POST /api/documents/upload — the server reads the file as plain text. */
export function uploadFile(file: Blob, name: string) {
  const form = new FormData()
  form.append('file', file, name)
  return request<{ filename?: string; chunks_created?: number }>('/api/documents/upload', {
    method: 'POST',
    form,
    timeoutMs: 180_000, // long files are processed piece by piece before the server replies
  })
}

/** POST /api/documents — pasted text. */
export function addText(title: string, content: string) {
  return request<unknown[]>('/api/documents', {
    method: 'POST',
    json: { title, content, source: 'raga_ui' },
    timeoutMs: 180_000,
  })
}

/** POST /api/documents/ask */
export function ask(question: string, signal?: AbortSignal) {
  // The server may retry once and then try its fallback model before answering.
  return request<RagResponse>('/api/documents/ask', { method: 'POST', json: { question }, signal, timeoutMs: 90_000 })
}

/** GET /api/documents/search — same top-3 retrieval the ask endpoint uses, so these are the passages behind the answer. */
export async function searchPassages(query: string, signal?: AbortSignal): Promise<Passage[]> {
  const docs = await request<{ title?: string; content?: string }[]>(
    `/api/documents/search?query=${encodeURIComponent(query)}&limit=3`,
    { signal },
  )
  return docs.filter((d) => d.title && d.content).map((d) => ({ title: d.title!, content: d.content! }))
}

/** POST /api/summarize */
export async function summarize(content: string) {
  const r = await request<{ summary?: string }>('/api/summarize', { method: 'POST', json: { content }, timeoutMs: 120_000 })
  return (r.summary ?? '').trim()
}

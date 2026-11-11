/**
 * MOCK: needs backend support.
 * The backend has no notebooks, source list, source on/off, delete, notes or saved Studio results.
 * They are kept in this browser's localStorage until those endpoints exist
 * (see docs/frontend-migration-plan.md, "Backend gaps").
 */
import type { Note, Notebook, StudioOut } from '../types.ts'

export type LocalData = {
  notebooks: Notebook[]
  notes: Record<string, Note[]>
  outs: Record<string, StudioOut[]>
}

export const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36)

const KEY = 'raga-data-v1'
const empty = (): LocalData => ({ notebooks: [], notes: {}, outs: {} })

export function loadLocal(): LocalData {
  try {
    const d = JSON.parse(localStorage.getItem(KEY) ?? 'null') as LocalData | null
    if (!d || !Array.isArray(d.notebooks)) return empty()
    // Work that was in progress when the page closed cannot resume.
    for (const nb of d.notebooks)
      for (const s of nb.sources)
        if (s.status === 'reading') {
          s.status = 'failed'
          s.retryable = false
          s.error = 'Adding this file was interrupted. Remove it and add the file again.'
        }
    for (const k of Object.keys(d.outs ?? {})) d.outs[k] = d.outs[k].filter((o) => o.status !== 'loading')
    return { notebooks: d.notebooks, notes: d.notes ?? {}, outs: d.outs ?? {} }
  } catch {
    return empty()
  }
}

export function saveLocal(d: LocalData) {
  try {
    localStorage.setItem(KEY, JSON.stringify(d))
  } catch {
    // Storage full: keep the structure, drop the stored source text (it stays on the server).
    try {
      const slim = { ...d, notebooks: d.notebooks.map((nb) => ({ ...nb, sources: nb.sources.map((s) => ({ ...s, text: undefined })) })) }
      localStorage.setItem(KEY, JSON.stringify(slim))
    } catch {
      /* storage unavailable (private mode): the app still works for this visit */
    }
  }
}

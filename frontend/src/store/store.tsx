import { createContext, useContext, useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { addText, uploadFile } from '../api/documents'
import { ERROR_COPY, errorKind } from '../api/client'
import { loadLocal, saveLocal, uid, type LocalData } from '../api/local'
import type { ChatMsg, Note, Notebook, Source, SourceType, StudioOut } from '../types'
import { useToast } from '../components/Toast'
import { play } from '../lib/sound'

type State = LocalData & { chats: Record<string, ChatMsg[]> }
type Undo = { nbId: string; source: Source; index: number }

const COLORS = ['violet', 'lime', 'pink', 'white'] as const
export const MAX_FILE_BYTES = 1024 * 1024 // the server's default upload limit
const READABLE: Record<string, SourceType> = { txt: 'txt', md: 'md', markdown: 'md', csv: 'csv', html: 'html', htm: 'html', json: 'json' }

const pages = (chars: number) => {
  const n = Math.max(1, Math.round(chars / 3000))
  return `${n} page${n === 1 ? '' : 's'}`
}

/** Files waiting for "Try again". Kept in memory only, so retries work until the page is reloaded. */
const pending = new Map<string, { blob: Blob; name: string }>()

function rejectReason(ext: string): { type: SourceType; error: string } {
  if (ext === 'pdf') return { type: 'pdf', error: 'PDF files can’t be read yet. Copy the text into Paste text, or save it as a .txt file and add it again.' }
  if (ext === 'doc' || ext === 'docx') return { type: 'docx', error: 'Word files can’t be read yet. Copy the text into Paste text, or save it as a .txt file and add it again.' }
  if (ext === 'xls' || ext === 'xlsx') return { type: 'xlsx', error: 'Excel files can’t be read yet. Save the sheet as a CSV file and add it again.' }
  return { type: 'file', error: `RAGMe! can’t read ${ext ? '.' + ext : 'these'} files. Try a text, Markdown, CSV, HTML or JSON file.` }
}

/** Web pages are sent as their visible text, which is what people ask about. */
function htmlToText(html: string) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.querySelectorAll('script,style,noscript,template').forEach((n) => n.remove())
  const title = doc.title.trim()
  const body = (doc.body?.textContent ?? '').replace(/[ \t]+/g, ' ').replace(/\s*\n\s*/g, '\n').replace(/\n{2,}/g, '\n\n').trim()
  return [title, body].filter(Boolean).join('\n\n')
}

function useStoreValue() {
  const toast = useToast()
  const [s, set] = useState<State>(() => ({ ...loadLocal(), chats: {} }))
  const undo = useRef<Undo | null>(null)
  const ref = useRef(s)
  useLayoutEffect(() => {
    ref.current = s
  })

  useEffect(() => saveLocal({ notebooks: s.notebooks, notes: s.notes, outs: s.outs }), [s.notebooks, s.notes, s.outs])

  const actions = useMemo(() => {
    const patchNb = (nbId: string, f: (nb: Notebook) => Notebook) =>
      set((p) => ({ ...p, notebooks: p.notebooks.map((nb) => (nb.id === nbId ? f(nb) : nb)) }))
    const patchSource = (nbId: string, id: string, patch: Partial<Source>) =>
      patchNb(nbId, (nb) => ({ ...nb, sources: nb.sources.map((x) => (x.id === id ? { ...x, ...patch } : x)) }))
    const pushSource = (nbId: string, src: Source) =>
      patchNb(nbId, (nb) => ({ ...nb, updatedAt: Date.now(), sources: [...nb.sources, src] }))

    async function send(nbId: string, id: string) {
      const job = pending.get(id)
      if (!job) return
      patchSource(nbId, id, { status: 'reading', error: undefined })
      try {
        await uploadFile(job.blob, job.name)
        pending.delete(id)
        patchSource(nbId, id, { status: 'ready', on: true })
        play('success')
        toast(`${job.name} is ready`)
      } catch (e) {
        const kind = errorKind(e)
        patchSource(nbId, id, {
          status: 'failed',
          retryable: true,
          error: kind === 'invalid' ? 'RAGMe! couldn’t read this file. Check that it contains text and try again.' : ERROR_COPY[kind],
        })
      }
    }

    return {
      createNotebook(title: string) {
        const nb: Notebook = { id: uid(), title, color: COLORS[ref.current.notebooks.length % 4], updatedAt: Date.now(), sources: [] }
        set((p) => ({ ...p, notebooks: [...p.notebooks, nb] }))
        return nb.id
      },

      async addFiles(nbId: string, list: File[]) {
        const taken = new Set(ref.current.notebooks.find((n) => n.id === nbId)?.sources.map((x) => x.name))
        for (const f of list) {
          const ext = (f.name.includes('.') ? f.name.split('.').pop()! : '').toLowerCase()
          const type = READABLE[ext]
          const base: Source = { id: uid(), name: f.name, type: type ?? 'file', detail: '', status: 'failed', on: false, retryable: false }
          if (!type) {
            pushSource(nbId, { ...base, ...rejectReason(ext) })
            continue
          }
          if (taken.has(f.name)) {
            pushSource(nbId, { ...base, error: 'A source with this name is already in this notebook. Rename the file and add it again.' })
            continue
          }
          if (f.size > MAX_FILE_BYTES) {
            pushSource(nbId, { ...base, error: 'This file is larger than 1 MB. Split it into smaller files and add them again.' })
            continue
          }
          taken.add(f.name)
          let text = ''
          try {
            text = await f.text()
            if (type === 'html') text = htmlToText(text)
          } catch {
            pushSource(nbId, { ...base, error: 'RAGMe! couldn’t open this file. Check that it isn’t damaged and add it again.' })
            continue
          }
          if (!text.trim()) {
            pushSource(nbId, { ...base, error: 'This file is empty. Add a file that contains text.' })
            continue
          }
          const src: Source = { ...base, status: 'reading', on: true, detail: pages(text.length), text }
          pending.set(src.id, { blob: new Blob([text], { type: 'text/plain' }), name: f.name })
          pushSource(nbId, src)
          void send(nbId, src.id)
        }
      },

      async addPasted(nbId: string, title: string, text: string) {
        const name = `${title}.txt`
        const src: Source = { id: uid(), name, type: 'txt', detail: 'Pasted text', status: 'reading', on: true, text }
        pushSource(nbId, src)
        try {
          await addText(name, text)
          patchSource(nbId, src.id, { status: 'ready' })
          play('success')
          toast('Text added')
        } catch (e) {
          pending.set(src.id, { blob: new Blob([text], { type: 'text/plain' }), name })
          patchSource(nbId, src.id, { status: 'failed', retryable: true, error: ERROR_COPY[errorKind(e)] })
        }
      },

      retrySource(nbId: string, id: string) {
        if (pending.has(id)) void send(nbId, id)
        else patchSource(nbId, id, { retryable: false, error: 'This file is no longer available. Remove it and add the file again.' })
      },

      toggleSource(nbId: string, id: string) {
        patchNb(nbId, (nb) => ({ ...nb, sources: nb.sources.map((x) => (x.id === id ? { ...x, on: !x.on } : x)) }))
      },

      // MOCK: needs backend support — the document stays on the server; it is only removed from this notebook.
      removeSource(nbId: string, id: string) {
        const nb = ref.current.notebooks.find((n) => n.id === nbId)
        const index = nb?.sources.findIndex((x) => x.id === id) ?? -1
        if (!nb || index < 0) return
        undo.current = { nbId, source: nb.sources[index], index }
        patchNb(nbId, (n) => ({ ...n, sources: n.sources.filter((x) => x.id !== id) }))
        return nb.sources[index]
      },

      undoRemove() {
        const u = undo.current
        undo.current = null
        if (u) patchNb(u.nbId, (nb) => ({ ...nb, sources: [...nb.sources.slice(0, u.index), u.source, ...nb.sources.slice(u.index)] }))
      },

      pushMsgs: (nbId: string, msgs: ChatMsg[]) => set((p) => ({ ...p, chats: { ...p.chats, [nbId]: [...(p.chats[nbId] ?? []), ...msgs] } })),
      patchMsg: (nbId: string, id: string, patch: Partial<ChatMsg>) =>
        set((p) => ({ ...p, chats: { ...p.chats, [nbId]: (p.chats[nbId] ?? []).map((m) => (m.id === id ? ({ ...m, ...patch } as ChatMsg) : m)) } })),
      clearChat: (nbId: string) => set((p) => ({ ...p, chats: { ...p.chats, [nbId]: [] } })),

      addNote: (nbId: string, note: Omit<Note, 'id'>) =>
        set((p) => ({ ...p, notes: { ...p.notes, [nbId]: [{ id: uid(), ...note }, ...(p.notes[nbId] ?? [])] } })),
      removeNote: (nbId: string, id: string) => set((p) => ({ ...p, notes: { ...p.notes, [nbId]: (p.notes[nbId] ?? []).filter((n) => n.id !== id) } })),

      addOut: (nbId: string, out: StudioOut) => set((p) => ({ ...p, outs: { ...p.outs, [nbId]: [out, ...(p.outs[nbId] ?? [])] } })),
      patchOut: (nbId: string, id: string, patch: Partial<StudioOut>) =>
        set((p) => ({ ...p, outs: { ...p.outs, [nbId]: (p.outs[nbId] ?? []).map((o) => (o.id === id ? { ...o, ...patch } : o)) } })),
      removeOut: (nbId: string, id: string) => set((p) => ({ ...p, outs: { ...p.outs, [nbId]: (p.outs[nbId] ?? []).filter((o) => o.id !== id) } })),
    }
  }, [toast])

  return { ...s, ...actions }
}

type Store = ReturnType<typeof useStoreValue>
const Ctx = createContext<Store | null>(null)

export function StoreProvider({ children }: { children: ReactNode }) {
  return <Ctx.Provider value={useStoreValue()}>{children}</Ctx.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useStore() {
  const s = useContext(Ctx)
  if (!s) throw new Error('useStore outside StoreProvider')
  return s
}

export type SourceType = 'txt' | 'md' | 'csv' | 'html' | 'json' | 'pdf' | 'docx' | 'xlsx' | 'link' | 'file'
export type NotebookColor = 'violet' | 'lime' | 'pink' | 'white'

export type Source = {
  id: string
  /** Display name. Also the title the server stores, which is how answers point back to this source. */
  name: string
  type: SourceType
  detail: string
  status: 'reading' | 'ready' | 'failed'
  on: boolean
  error?: string
  /** True when "Try again" makes sense (a temporary failure, not an unsupported file). */
  retryable?: boolean
  /** The source's text, kept in this browser so it can be shown and summarised. */
  text?: string
}

export type Notebook = {
  id: string
  title: string
  color: NotebookColor
  updatedAt: number
  sources: Source[]
}

export type Note = { id: string; title: string; body: string }

export type StudioKind = 'summary' | 'guide' | 'faq' | 'topics'
export type StudioOut = {
  id: string
  kind: StudioKind
  status: 'loading' | 'done' | 'error'
  n: number
  text?: string
  error?: string
  shortened?: boolean
}

export type Ref = { sourceId: string; name: string; passage?: string }

export type Answer =
  | { kind: 'found'; paragraphs: string[]; refs: Ref[]; partial: boolean }
  | { kind: 'none'; switchedOff: boolean }

export type ChatMsg =
  | { id: string; role: 'user'; text: string }
  | { id: string; role: 'ai'; q: string; st: 'loading' | 'stream' | 'done' | 'err'; answer?: Answer; error?: string }

export const activeSources = (nb: Notebook) => nb.sources.filter((s) => s.status === 'ready' && s.on)

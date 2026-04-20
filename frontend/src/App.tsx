import { useCallback, useRef, useState, type FormEvent } from 'react'
import { apiFetch } from './api'
import './App.css'

type WebhookSuccess<T> = { success: boolean; result?: T; message?: string }

export default function App() {
  const [addLoading, setAddLoading] = useState(false)
  const [askLoading, setAskLoading] = useState(false)
  const [addMessage, setAddMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)
  const [askMessage, setAskMessage] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const answerRef = useRef<HTMLDivElement>(null)
  const sourcesRef = useRef<HTMLUListElement>(null)

  const handleAdd = useCallback(async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const title = (form.elements.namedItem('title') as HTMLInputElement).value
    const content = (form.elements.namedItem('content') as HTMLTextAreaElement).value
    setAddLoading(true)
    setAddMessage(null)
    try {
      const res = await apiFetch('/api/webhooks/add-document', {
        method: 'POST',
        body: JSON.stringify({
          title,
          content,
          metadata: { source: 'react_ui', timestamp: new Date().toISOString() },
        }),
      })
      const text = await res.text()
      if (!res.ok) {
        setAddMessage({ type: 'err', text: text || res.statusText })
        return
      }
      const data = JSON.parse(text) as WebhookSuccess<{ chunks_created?: number }>
      const chunks = data.result?.chunks_created
      setAddMessage({
        type: 'ok',
        text:
          chunks != null
            ? `Document added (${chunks} chunks).`
            : 'Document added.',
      })
      form.reset()
    } catch (err) {
      setAddMessage({
        type: 'err',
        text: err instanceof Error ? err.message : 'Request failed',
      })
    } finally {
      setAddLoading(false)
    }
  }, [])

  const handleAsk = useCallback(async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const q = (e.currentTarget.elements.namedItem('question') as HTMLTextAreaElement).value
    setAskLoading(true)
    setAskMessage(null)
    if (answerRef.current) answerRef.current.textContent = ''
    if (sourcesRef.current) sourcesRef.current.replaceChildren()

    try {
      const res = await apiFetch('/api/webhooks/ask', {
        method: 'POST',
        body: JSON.stringify({ question: q }),
      })
      const text = await res.text()
      if (!res.ok) {
        setAskMessage({ type: 'err', text: text || res.statusText })
        return
      }
      const data = JSON.parse(text) as WebhookSuccess<{
        answer?: string
        sourcesUsed?: string[]
        documentsRetrieved?: number
      }>
      const result = data.result ?? (data as unknown as { answer?: string })
      const answer = result?.answer ?? ''
      if (answerRef.current) {
        answerRef.current.textContent = answer || 'No answer returned.'
      }
      const sources = result?.sourcesUsed ?? []
      if (sourcesRef.current && sources.length > 0) {
        for (const s of sources) {
          const li = document.createElement('li')
          li.textContent = s
          sourcesRef.current.appendChild(li)
        }
      }
      setAskMessage({ type: 'ok', text: 'Done.' })
    } catch (err) {
      setAskMessage({
        type: 'err',
        text: err instanceof Error ? err.message : 'Request failed',
      })
    } finally {
      setAskLoading(false)
    }
  }, [])

  return (
    <div className="page">
      <header className="header">
        <h1>AI Research Platform</h1>
        <p className="tagline">RAG over your documents</p>
      </header>

      <section className="card">
        <h2>Add document</h2>
        <form onSubmit={handleAdd}>
          <label htmlFor="title">Title</label>
          <input id="title" name="title" required autoComplete="off" />

          <label htmlFor="content">Content</label>
          <textarea id="content" name="content" required rows={6} />

          <button type="submit" disabled={addLoading}>
            {addLoading ? 'Processing…' : 'Add document'}
          </button>
        </form>
        {addMessage && (
          <p className={addMessage.type === 'ok' ? 'msg ok' : 'msg err'}>{addMessage.text}</p>
        )}
      </section>

      <section className="card">
        <h2>Ask a question</h2>
        <form onSubmit={handleAsk}>
          <label htmlFor="question">Question</label>
          <textarea id="question" name="question" required rows={4} />

          <button type="submit" disabled={askLoading}>
            {askLoading ? 'Thinking…' : 'Ask'}
          </button>
        </form>
        <div className="answer-wrap">
          <div className="answer-label">Answer</div>
          <div ref={answerRef} className="answer" />
        </div>
        <div className="sources-wrap">
          <div className="answer-label">Sources</div>
          <ul ref={sourcesRef} className="sources" />
        </div>
        {askMessage && (
          <p className={askMessage.type === 'ok' ? 'msg ok' : 'msg err'}>{askMessage.text}</p>
        )}
      </section>
    </div>
  )
}

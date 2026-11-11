import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { answerText, toAnswer } from '../../api/adapters'
import { ERROR_COPY, errorKind } from '../../api/client'
import { ask, searchPassages } from '../../api/documents'
import { uid } from '../../api/local'
import { Mascot } from '../../components/Art'
import { Icon } from '../../components/Icon'
import { copyText, useToast } from '../../components/Toast'
import { play } from '../../lib/sound'
import { useStore } from '../../store/store'
import { activeSources, type Answer, type ChatMsg, type Notebook } from '../../types'
import type { View } from './Workspace'

type AiMsg = Extract<ChatMsg, { role: 'ai' }>
type Found = Extract<Answer, { kind: 'found' }>

const SUGGESTIONS = ['What is this document about?', 'Summarise the key points']
const STEPS = ['Reading your sources', 'Finding the right passages', 'Writing your answer']
const MAX_Q = 1000
const reducedMotion = () => matchMedia('(prefers-reduced-motion: reduce)').matches

export function ChatPanel({ nb, onAdd, onView }: { nb: Notebook; onAdd: () => void; onView: (v: View) => void }) {
  const store = useStore()
  const { pushMsgs, patchMsg, clearChat } = store
  const msgs = store.chats[nb.id] ?? []
  const toast = useToast()
  const [q, setQ] = useState('')
  const box = useRef<HTMLDivElement>(null)
  const ctrls = useRef(new Map<string, AbortController>())
  const latest = useRef(nb)
  useLayoutEffect(() => {
    latest.current = nb
  })

  const off = activeSources(nb).length === 0
  const busy = msgs.some((m) => m.role === 'ai' && (m.st === 'loading' || m.st === 'stream'))

  const stick = () => {
    const c = box.current
    if (c && c.scrollHeight - c.scrollTop - c.clientHeight < 160) c.scrollTop = c.scrollHeight
  }
  useEffect(() => {
    const c = box.current
    if (c) c.scrollTop = c.scrollHeight
  }, [msgs.length])

  async function run(id: string, question: string) {
    patchMsg(nb.id, id, { st: 'loading', error: undefined, answer: undefined })
    const ctrl = new AbortController()
    ctrls.current.set(id, ctrl)
    try {
      const [res, passages] = await Promise.all([ask(question, ctrl.signal), searchPassages(question, ctrl.signal).catch(() => [])])
      const answer = toAnswer(res, passages, latest.current.sources)
      if (answer.kind === 'found') play('success')
      patchMsg(nb.id, id, { st: answer.kind === 'found' && !reducedMotion() ? 'stream' : 'done', answer })
    } catch (e) {
      const kind = errorKind(e)
      if (kind !== 'aborted') patchMsg(nb.id, id, { st: 'err', error: ERROR_COPY[kind] })
    } finally {
      ctrls.current.delete(id)
    }
  }

  function submit(text: string) {
    const question = text.trim().slice(0, MAX_Q)
    if (!question || off) return
    const id = uid()
    pushMsgs(nb.id, [
      { id: uid(), role: 'user', text: question },
      { id, role: 'ai', q: question, st: 'loading' },
    ])
    void run(id, question)
  }

  const newChat = () => {
    ctrls.current.forEach((c) => c.abort())
    ctrls.current.clear()
    clearChat(nb.id)
  }

  return (
    <section className="card panel p-chat" aria-labelledby="chat-h">
      <div className="ph2">
        <h2 id="chat-h">Chat</h2>
        <button type="button" className="btn sm soft" onClick={newChat} data-sound="close">
          <Icon name="plus" size={14} /> New chat
        </button>
      </div>
      <div className="chat" ref={box} aria-live="polite" aria-busy={busy}>
        {msgs.length === 0 ? (
          <div className="welcome">
            <Mascot mood="wow" color="violet" size={104} />
            <h3>{nb.sources.length ? 'Ask anything about this notebook' : 'Add a source to get started'}</h3>
            <p>{nb.sources.length ? 'Pick a suggested question below or type your own.' : 'RAGMe! answers from the documents you add, and shows where each answer came from.'}</p>
          </div>
        ) : (
          msgs.map((m) =>
            m.role === 'user' ? (
              <div key={m.id} className="msg u">
                <div className="bub">{m.text}</div>
              </div>
            ) : (
              <AiBubble
                key={m.id}
                m={m}
                nb={nb}
                onAdd={onAdd}
                onView={onView}
                onRetry={() => void run(m.id, m.q)}
                onDone={() => patchMsg(nb.id, m.id, { st: 'done' })}
                onTick={stick}
                onCopy={() => m.answer && copyText(answerText(m.answer), toast)}
                onSave={() => {
                  if (m.answer?.kind !== 'found') return
                  store.addNote(nb.id, { title: m.q, body: m.answer.paragraphs.join('\n\n') })
                  toast('Saved to notes')
                }}
              />
            ),
          )
        )}
      </div>
      <form
        className="composer"
        onSubmit={(e) => {
          e.preventDefault()
          submit(q)
          setQ('')
        }}
      >
        <div className="sugg">
          {SUGGESTIONS.map((s) => (
            <button key={s} type="button" className="chip" disabled={off} onClick={() => submit(s)} data-sound="confirm">
              {s}
            </button>
          ))}
        </div>
        <div className="inputrow">
          <textarea
            rows={2}
            aria-label="Your question"
            maxLength={MAX_Q}
            disabled={off}
            placeholder={off ? 'Select at least one source to ask a question' : 'Ask about your documents…'}
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault()
                e.currentTarget.form?.requestSubmit()
              }
            }}
          />
          <button type="submit" className="btn lime" disabled={off} data-sound="confirm">
            Ask <Icon name="send" size={16} />
          </button>
        </div>
      </form>
    </section>
  )
}

type BubbleProps = {
  m: AiMsg
  nb: Notebook
  onAdd: () => void
  onView: (v: View) => void
  onRetry: () => void
  onDone: () => void
  onTick: () => void
  onCopy: () => void
  onSave: () => void
}

function AiBubble({ m, nb, onAdd, onView, onRetry, onDone, onTick, onCopy, onSave }: BubbleProps) {
  if (m.st === 'loading')
    return (
      <div className="msg">
        <div>
          <Mascot mood="think" color="violet" size={44} />
        </div>
        <div className="bub ai" key="loading">
          <LoadingSteps />
        </div>
      </div>
    )

  if (m.st === 'err')
    return (
      <div className="msg">
        <div>
          <Mascot mood="oops" color="pink" size={44} />
        </div>
        <div className="bub ai err" role="alert" key="err">
          <h3>
            <Icon name="alert" size={16} /> Something went wrong
          </h3>
          <p>{m.error}</p>
          <div className="acts">
            <button type="button" className="btn sm violet" onClick={onRetry}>
              <Icon name="refresh" size={14} /> Try again
            </button>
          </div>
        </div>
      </div>
    )

  const a = m.answer!
  if (a.kind === 'none') {
    const bad = nb.sources.filter((s) => s.status === 'failed')
    const n = activeSources(nb).length
    return (
      <div className="msg">
        <div>
          <Mascot mood="shrug" color="lime" size={44} />
        </div>
        <div className="bub ai warn" key="none">
          <h3>I couldn’t find that in your sources</h3>
          <p>
            {a.switchedOff
              ? 'The documents that cover this question are switched off. Turn them back on and ask again.'
              : `None of the ${n} selected source${n === 1 ? '' : 's'} mention this.`}
            {bad.length > 0 && ` ${bad.length === 1 ? 'One file' : 'Some files'} could not be read (${bad.map((b) => b.name).join(', ')}), and may contain the answer.`}
          </p>
          <div className="acts">
            <button type="button" className="btn sm lime" onClick={onAdd} data-sound="open">
              <Icon name="plus" size={14} /> Add a source
            </button>
          </div>
        </div>
      </div>
    )
  }

  const done = m.st === 'done'
  return (
    <div className="msg">
      <div>
        <Mascot mood="happy" color="violet" size={44} />
      </div>
      <div className="bub ai" key="answer">
        <AnswerText a={a} streaming={m.st === 'stream'} onDone={onDone} onTick={onTick} onCite={(i) => onView({ sourceId: a.refs[i].sourceId, passage: a.refs[i].passage })} />
        {done && (
          <>
            <div className="refs">
              {a.refs.map((r, i) => (
                <button key={i} type="button" className="ref" onClick={() => onView({ sourceId: r.sourceId, passage: r.passage })} data-sound="open">
                  <b>{i + 1}</b>
                  <span>{r.name}</span>
                </button>
              ))}
            </div>
            {a.partial && (
              <p className="muted ans-note" style={{ marginTop: 10 }}>
                Part of this answer may come from documents that are switched off or not in this notebook.
              </p>
            )}
            <div className="acts">
              <button type="button" className="btn sm soft" onClick={onCopy} data-sound="confirm">
                <Icon name="copy" size={14} /> Copy
              </button>
              <button type="button" className="btn sm soft" onClick={onSave} data-sound="confirm">
                <Icon name="note" size={14} /> Save to notes
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function LoadingSteps() {
  const [stage, setStage] = useState(0)
  useEffect(() => {
    const t = [setTimeout(() => setStage(1), 450), setTimeout(() => setStage(2), 950)]
    return () => t.forEach(clearTimeout)
  }, [])
  return (
    <ol className="steps" aria-label="Working on your answer">
      {STEPS.map((s, k) => (
        <li key={s} className={k < stage ? 'done' : k === stage ? 'cur' : ''}>
          {k < stage ? (
            <Icon name="check" size={14} />
          ) : k === stage ? (
            k === STEPS.length - 1 ? (
              <span className="typing" aria-hidden="true">
                <i />
                <i />
                <i />
              </span>
            ) : (
              <i className="dot" />
            )
          ) : (
            <span className="sp" />
          )}
          {s}
        </li>
      ))}
    </ol>
  )
}

/** Reveals the finished answer word by word (the server sends it in one piece). */
function AnswerText({ a, streaming, onDone, onTick, onCite }: { a: Found; streaming: boolean; onDone: () => void; onTick: () => void; onCite: (i: number) => void }) {
  const words = a.paragraphs.map((p) => p.split(/(?<=\s)/))
  const total = words.reduce((n, w) => n + w.length, 0)
  const [shown, setShown] = useState(streaming ? 0 : total)
  const cb = useRef({ onDone, onTick })
  useEffect(() => {
    cb.current = { onDone, onTick }
  })

  useEffect(() => {
    if (!streaming) return
    const step = Math.max(2, Math.ceil(total / 70)) // long answers finish in about 3 seconds
    let n = 0
    const iv = setInterval(() => {
      n = Math.min(total, n + step)
      setShown(n)
      cb.current.onTick()
      if (n >= total) {
        clearInterval(iv)
        cb.current.onDone()
      }
    }, 45)
    return () => clearInterval(iv)
  }, [streaming, total])

  const last = a.paragraphs.length - 1
  const limit = streaming ? shown : total
  const takes = words.map((_, i) => {
    const before = words.slice(0, i).reduce((n, w) => n + w.length, 0)
    return Math.max(0, Math.min(words[i].length, limit - before))
  })
  const caretAt = streaming ? Math.max(0, takes.findIndex((t, i) => t < words[i].length)) : -1
  return (
    <>
      {words.map((w, i) => {
        const take = takes[i]
        if (take === 0 && i > 0) return null
        const complete = !streaming || (i === last && take === w.length)
        return (
          <p key={i} className="ans">
            {w.slice(0, take).join('')}
            {i === last &&
              complete &&
              a.refs.map((_, k) => (
                <button key={k} type="button" className="cite" onClick={() => onCite(k)} aria-label={`Source ${k + 1}: ${a.refs[k].name}`}>
                  {k + 1}
                </button>
              ))}
            {streaming && i === caretAt && <span className="caret" />}
          </p>
        )
      })}
    </>
  )
}

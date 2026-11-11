import { toParagraphs } from '../../api/adapters'
import { ERROR_COPY, errorKind } from '../../api/client'
import { summarize } from '../../api/documents'
import { uid } from '../../api/local'
import { Icon, type IconName } from '../../components/Icon'
import { copyText, useToast } from '../../components/Toast'
import { play } from '../../lib/sound'
import { useStore } from '../../store/store'
import { activeSources, type Notebook, type StudioKind, type StudioOut } from '../../types'

const MAX_CHARS = 100_000 // the server's limit for one summary
const TILES: [StudioKind, string, IconName, string][] = [
  ['summary', 'Summary', 'spark', 'v'],
  ['guide', 'Study guide', 'book', 'l'],
  ['faq', 'FAQ', 'help', 'p'],
  ['topics', 'Key topics', 'layers', 'w'],
]
const SOON: Record<Exclude<StudioKind, 'summary'>, string> = {
  guide: 'Study guides are coming soon.',
  faq: 'FAQs are coming soon.',
  topics: 'Key topics are coming soon.',
}
const plural = (n: number) => `${n} source${n === 1 ? '' : 's'}`

export function StudioPanel({ nb }: { nb: Notebook }) {
  const store = useStore()
  const toast = useToast()
  const outs = store.outs[nb.id] ?? []
  const notes = store.notes[nb.id] ?? []

  async function runSummary(existing?: StudioOut) {
    const picked = activeSources(nb)
    if (!picked.length) {
      toast('Select at least one source first.')
      return
    }
    const withText = picked.filter((s) => s.text?.trim())
    const id = existing?.id ?? uid()
    const out: StudioOut = { id, kind: 'summary', status: 'loading', n: withText.length || picked.length }
    if (existing) store.patchOut(nb.id, id, { ...out, error: undefined, text: undefined })
    else store.addOut(nb.id, out)

    if (!withText.length) {
      store.patchOut(nb.id, id, {
        status: 'error',
        error: 'The text of the selected sources isn’t saved in this browser, so RAGMe! can’t summarise them here. Add the files again to summarise them.',
      })
      return
    }
    const all = withText.map((s) => `${s.name}\n\n${s.text!.trim()}`).join('\n\n---\n\n')
    try {
      const text = await summarize(all.slice(0, MAX_CHARS))
      if (!text) throw new Error('empty')
      store.patchOut(nb.id, id, { status: 'done', text, shortened: all.length > MAX_CHARS })
      play('success')
    } catch (e) {
      store.patchOut(nb.id, id, { status: 'error', error: ERROR_COPY[errorKind(e)] })
    }
  }

  return (
    <section className="card panel p-stu" aria-labelledby="stu-h">
      <div className="ph2">
        <h2 id="stu-h">Studio</h2>
      </div>
      <div className="pbody">
        <div className="studio">
          {TILES.map(([k, label, icon, c]) => (
            <button key={k} type="button" className={`stile ${c}`} onClick={() => (k === 'summary' ? void runSummary() : toast(SOON[k]))} data-sound={k === 'summary' ? 'confirm' : undefined}>
              <Icon name={icon} size={20} />
              {label}
              {k !== 'summary' && <span className="soon">Soon</span>}
            </button>
          ))}
        </div>

        {outs.map((o) =>
          o.status === 'loading' ? (
            <div key={o.id} className="out" aria-live="polite">
              <h3>Summary</h3>
              <div className="row g8 muted">
                <i className="dot" /> Writing from {plural(o.n)}…
              </div>
              <div className="sk" aria-hidden="true">
                <i />
                <i />
                <i />
              </div>
            </div>
          ) : o.status === 'error' ? (
            <div key={o.id} className="out bad" role="alert">
              <h3>
                Summary
                <button type="button" className="iconbtn sm" onClick={() => store.removeOut(nb.id, o.id)} aria-label="Remove" data-sound="close">
                  <Icon name="x" size={14} />
                </button>
              </h3>
              <p style={{ color: 'var(--bad)' }}>
                {o.error}
              </p>
              <div className="row g8">
                <button type="button" className="btn sm soft" onClick={() => void runSummary(o)}>
                  <Icon name="refresh" size={14} /> Try again
                </button>
              </div>
            </div>
          ) : (
            <div key={o.id} className="out">
              <h3>
                Summary
                <span className="row g8">
                  <button type="button" className="iconbtn sm" onClick={() => copyText(o.text ?? '', toast)} aria-label="Copy">
                    <Icon name="copy" size={14} />
                  </button>
                  <button
                    type="button"
                    className="iconbtn sm"
                    onClick={() => {
                      store.addNote(nb.id, { title: 'Summary', body: toParagraphs(o.text ?? '').join('\n\n') })
                      toast('Saved to notes')
                    }}
                    aria-label="Save to notes"
                    data-sound="confirm"
                  >
                    <Icon name="note" size={14} />
                  </button>
                  <button type="button" className="iconbtn sm" onClick={() => store.removeOut(nb.id, o.id)} aria-label="Remove" data-sound="close">
                    <Icon name="x" size={14} />
                  </button>
                </span>
              </h3>
              {toParagraphs(o.text ?? '').map((p, i) => (
                <p key={i}>{p}</p>
              ))}
              <span className="meta">
                Based on {plural(o.n)}
                {o.shortened ? ' · long sources were shortened to fit' : ''}
              </span>
            </div>
          ),
        )}

        <h3 className="shead">Saved notes</h3>
        {notes.length ? (
          notes.map((n) => (
            <div key={n.id} className="out">
              <h3>
                <span style={{ overflowWrap: 'anywhere' }}>{n.title}</span>
                <span className="row g8">
                  <button type="button" className="iconbtn sm" onClick={() => copyText(n.body, toast)} aria-label="Copy note">
                    <Icon name="copy" size={14} />
                  </button>
                  <button type="button" className="iconbtn sm" onClick={() => store.removeNote(nb.id, n.id)} aria-label="Delete note" data-sound="close">
                    <Icon name="trash" size={14} />
                  </button>
                </span>
              </h3>
              <p>{n.body}</p>
            </div>
          ))
        ) : (
          <p className="muted">Save an answer or summary and it will appear here.</p>
        )}
      </div>
    </section>
  )
}

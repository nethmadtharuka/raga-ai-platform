import { AnimatePresence } from 'motion/react'
import { useState, type CSSProperties } from 'react'
import { Mascot, type MascotColor, type Mood } from '../components/Art'
import { Dialog } from '../components/Dialog'
import { Icon } from '../components/Icon'
import { useStore } from '../store/store'
import type { NotebookColor } from '../types'

const MOODS: Mood[] = ['happy', 'wow', 'think', 'happy']
const MASCOT_ON: Record<NotebookColor, MascotColor> = { violet: 'lime', lime: 'violet', pink: 'blue', white: 'violet' }

function updated(ts: number) {
  const mins = (Date.now() - ts) / 60000
  if (mins < 2) return 'Just now'
  const day = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((day(new Date()) - day(new Date(ts))) / 86400000)
  return days <= 0 ? 'Today' : days === 1 ? 'Yesterday' : `${days} days ago`
}

export function Notebooks() {
  const { notebooks } = useStore()
  const [q, setQ] = useState('')
  const [creating, setCreating] = useState(false)
  const list = notebooks.filter((n) => !q || n.title.toLowerCase().includes(q.toLowerCase()))

  return (
    <div className="wrap page">
      <div className="ph">
        <div>
          <h1>Your notebooks</h1>
          <p>Each notebook holds the documents for one topic.</p>
        </div>
        {notebooks.length > 0 && (
          <label className="search">
            <Icon name="search" size={16} />
            <input type="search" placeholder="Search notebooks" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search notebooks" />
          </label>
        )}
      </div>

      <div className="nbgrid">
        <button type="button" className="nbc add" onClick={() => setCreating(true)} data-sound="open">
          <span className="tic">
            <Icon name="plus" size={22} />
          </span>
          <h3>New notebook</h3>
        </button>
        {list.map((n, i) => (
          <a key={n.id} className={`nbc ${n.color}`} href={`#nb-${n.id}`} data-sound="open" style={{ '--i': i + 1 } as CSSProperties}>
            <div>
              <h3>{n.title}</h3>
            </div>
            <small>
              {n.sources.length} source{n.sources.length === 1 ? '' : 's'} · {updated(n.updatedAt)}
            </small>
            <Mascot mood={MOODS[i % 4]} color={MASCOT_ON[n.color]} size={92} />
          </a>
        ))}
      </div>

      {notebooks.length === 0 && (
        <div className="empty">
          <Mascot mood="wow" color="violet" size={96} />
          <h2>Start your first notebook</h2>
          <p>A notebook keeps the documents for one topic together, so you can ask questions about them.</p>
          <button type="button" className="btn lime" onClick={() => setCreating(true)} data-sound="open">
            <Icon name="plus" size={16} /> New notebook
          </button>
        </div>
      )}
      {notebooks.length > 0 && !list.length && (
        <div className="empty">
          <Mascot mood="think" color="violet" size={96} />
          <h2>No notebooks match</h2>
          <p>Try a different word, or create a new notebook.</p>
          <button type="button" className="btn soft" onClick={() => setQ('')}>
            Clear search
          </button>
        </div>
      )}

      <AnimatePresence>{creating && <NewNotebookDialog onClose={() => setCreating(false)} />}</AnimatePresence>
    </div>
  )
}

function NewNotebookDialog({ onClose }: { onClose: () => void }) {
  const { createNotebook } = useStore()
  const [name, setName] = useState('')
  const [err, setErr] = useState('')
  const create = () => {
    const n = name.trim()
    if (!n) {
      setErr('Give your notebook a name.')
      return
    }
    const id = createNotebook(n)
    onClose()
    location.hash = `#nb-${id}`
  }
  return (
    <Dialog label="New notebook" onClose={onClose}>
      <h2>New notebook</h2>
      <form
        className="col g16"
        onSubmit={(e) => {
          e.preventDefault()
          create()
        }}
        noValidate
      >
        <label className="col g8">
          <span className="muted">Give it a name</span>
          <input
            className="fld"
            data-autofocus
            maxLength={40}
            placeholder="e.g. Tax documents 2026"
            value={name}
            aria-invalid={!!err}
            aria-describedby={err ? 'nb-err' : undefined}
            onChange={(e) => {
              setName(e.target.value)
              if (err) setErr('')
            }}
          />
        </label>
        {err && (
          <p id="nb-err" className="ferr" role="alert">
            {err}
          </p>
        )}
        <div className="row g8 end">
          <button type="button" className="btn soft" onClick={onClose} data-sound="close">
            Cancel
          </button>
          <button type="submit" className="btn lime" data-sound="confirm">
            Create notebook
          </button>
        </div>
      </form>
    </Dialog>
  )
}

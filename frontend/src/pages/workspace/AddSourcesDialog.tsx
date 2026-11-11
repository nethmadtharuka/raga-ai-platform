import { useRef, useState } from 'react'
import { Mascot } from '../../components/Art'
import { Dialog } from '../../components/Dialog'
import { Icon } from '../../components/Icon'
import { useStore } from '../../store/store'
import type { Notebook } from '../../types'

type Tab = 'files' | 'link' | 'text'
const MAX_TEXT = 100_000

export function AddSourcesDialog({ nb, onClose, onAdded }: { nb: Notebook; onClose: () => void; onAdded: () => void }) {
  const { addFiles, addPasted } = useStore()
  const [tab, setTab] = useState<Tab>('files')
  const [err, setErr] = useState('')
  const [over, setOver] = useState(false)
  const [link, setLink] = useState('')
  const [linkOk, setLinkOk] = useState(false)
  const [title, setTitle] = useState('')
  const [text, setText] = useState('')
  const file = useRef<HTMLInputElement>(null)

  const switchTab = (t: Tab) => {
    setTab(t)
    setErr('')
    setLinkOk(false)
  }
  const takeFiles = (list: FileList | null) => {
    if (!list?.length) return
    void addFiles(nb.id, Array.from(list))
    onAdded()
  }
  const submitLink = () => {
    const v = link.trim()
    if (!/^(https?:\/\/)?[^\s.]+\.[^\s]{2,}$/.test(v)) {
      setErr('Enter a web address like https://example.com/article.')
      return
    }
    setLinkOk(true)
  }
  const submitText = () => {
    const t = text.trim()
    const name = (title.trim() || 'Pasted text').slice(0, 60)
    if (t.length < 10) return setErr('Paste at least a sentence of text.')
    if (t.length > MAX_TEXT) return setErr('This text is too long. Paste up to 100,000 characters at a time.')
    if (nb.sources.some((s) => s.name === `${name}.txt`)) return setErr('A source with this title is already in this notebook. Choose a different title.')
    void addPasted(nb.id, name, t)
    onAdded()
  }
  const errId = err ? 'add-err' : undefined

  return (
    <Dialog label="Add sources" onClose={onClose}>
      <div className="row sb">
        <h2>Add sources</h2>
        <button type="button" className="iconbtn" onClick={onClose} aria-label="Close" data-sound="close">
          <Icon name="x" size={18} />
        </button>
      </div>
      <div className="tabs" role="tablist" aria-label="How to add" data-i={tab === 'files' ? 0 : tab === 'link' ? 1 : 2}>
        {(
          [
            ['files', 'Upload files'],
            ['link', 'Website'],
            ['text', 'Paste text'],
          ] as const
        ).map(([v, label]) => (
          <button key={v} type="button" role="tab" aria-selected={tab === v} className={tab === v ? 'on' : ''} onClick={() => switchTab(v)}>
            {label}
          </button>
        ))}
      </div>

      {tab === 'files' && (
        <>
          <button
            type="button"
            className={`drop${over ? ' over' : ''}`}
            data-sound="open"
            onClick={() => file.current?.click()}
            onDragOver={(e) => {
              e.preventDefault()
              setOver(true)
            }}
            onDragLeave={() => setOver(false)}
            onDrop={(e) => {
              e.preventDefault()
              setOver(false)
              takeFiles(e.dataTransfer.files)
            }}
          >
            <Mascot mood="wow" color="lime" size={72} />
            <span className="dh">Drag files here or click to browse</span>
            <span className="fmts">
              {['Text', 'Markdown', 'CSV', 'HTML', 'JSON'].map((x) => (
                <span key={x} className="pill">
                  {x}
                </span>
              ))}
            </span>
            <span className="muted">Up to 1 MB per file. PDF, Word and Excel support is coming soon.</span>
          </button>
          <input ref={file} type="file" multiple hidden onChange={(e) => takeFiles(e.target.files)} />
        </>
      )}

      {tab === 'link' && (
        <form
          className="col g16"
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            submitLink()
          }}
        >
          <div className="note">
            <Icon name="link" size={16} />
            <span>Adding websites isn’t available yet. For now, copy the text of the page and add it with Paste text.</span>
          </div>
          <label className="col g8">
            <span className="muted">Web address</span>
            <input
              className="fld"
              data-autofocus
              inputMode="url"
              placeholder="https://example.com/article"
              value={link}
              aria-invalid={!!err}
              aria-describedby={errId}
              onChange={(e) => {
                setLink(e.target.value)
                setErr('')
                setLinkOk(false)
              }}
            />
          </label>
          {err && (
            <p id="add-err" className="ferr" role="alert">
              {err}
            </p>
          )}
          {linkOk && (
            <p className="note" role="status">
              <Icon name="alert" size={16} />
              <span>RAGMe! can’t add this website yet. Open the page, copy its text and use Paste text instead.</span>
            </p>
          )}
          <div className="row g8 end">
            {linkOk ? (
              <button type="button" className="btn lime" onClick={() => switchTab('text')} data-sound="toggle">
                Use Paste text
              </button>
            ) : (
              <button type="submit" className="btn lime" data-sound="confirm">
                Add source
              </button>
            )}
          </div>
        </form>
      )}

      {tab === 'text' && (
        <form
          className="col g16"
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            submitText()
          }}
        >
          <label className="col g8">
            <span className="muted">Title</span>
            <input className="fld" data-autofocus maxLength={60} placeholder="Meeting notes" value={title} onChange={(e) => (setTitle(e.target.value), setErr(''))} />
          </label>
          <label className="col g8">
            <span className="muted">Text</span>
            <textarea
              className="fld"
              rows={5}
              placeholder="Paste your text here"
              value={text}
              aria-invalid={!!err}
              aria-describedby={errId}
              onChange={(e) => {
                setText(e.target.value)
                setErr('')
              }}
            />
          </label>
          {err && (
            <p id="add-err" className="ferr" role="alert">
              {err}
            </p>
          )}
          <div className="row g8 end">
            <button type="submit" className="btn lime" data-sound="confirm">
              Add source
            </button>
          </div>
        </form>
      )}
    </Dialog>
  )
}

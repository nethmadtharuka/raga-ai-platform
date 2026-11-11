import { useEffect, useRef } from 'react'
import { Dialog } from '../../components/Dialog'
import { Icon } from '../../components/Icon'
import type { Notebook } from '../../types'
import { FileType } from './SourcesPanel'
import type { View } from './Workspace'

const paras = (t: string) =>
  t
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean)

/** Drawer showing a source, with the passage behind an answer highlighted and scrolled into view. */
export function SourceViewer({ nb, view, onClose }: { nb: Notebook; view: View; onClose: () => void }) {
  const s = nb.sources.find((x) => x.id === view.sourceId)
  const hl = useRef<HTMLParagraphElement>(null)
  useEffect(() => hl.current?.scrollIntoView?.({ block: 'center' }), [])

  const passage = view.passage?.trim()
  const text = s?.text ?? ''
  const at = passage ? text.indexOf(passage) : -1

  // [paragraph, highlighted?]
  let blocks: [string, boolean][]
  if (passage && at >= 0)
    blocks = [...paras(text.slice(0, at)).map((p): [string, boolean] => [p, false]), [passage, true], ...paras(text.slice(at + passage.length)).map((p): [string, boolean] => [p, false])]
  else if (passage) blocks = [[passage, true], ...paras(text).map((p): [string, boolean] => [p, false])]
  else blocks = paras(text).map((p) => [p, false])

  return (
    <Dialog label={s ? s.name : 'Source'} onClose={onClose} variant="drawer">
      <div className="row sb g12" style={{ alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          {s && <FileType type={s.type} />}
          <h2 style={{ marginTop: 8 }}>{s ? s.name : 'Source removed'}</h2>
          {s?.detail && <span className="muted">{s.detail}</span>}
        </div>
        <button type="button" className="iconbtn" onClick={onClose} aria-label="Close" data-autofocus data-sound="close">
          <Icon name="x" size={18} />
        </button>
      </div>
      {!s ? (
        <p className="muted">This source is no longer in this notebook.</p>
      ) : (
        <>
          {passage && <p className="muted">The highlighted passage is the one RAGMe! used.</p>}
          {!text && <p className="muted">{passage ? 'The rest of this source isn’t saved in this browser.' : 'The text of this source isn’t saved in this browser.'}</p>}
          {blocks.map(([p, on], i) => (
            <p key={i} ref={on ? hl : undefined} className={`para${on ? ' hl' : ''}`}>
              {p}
            </p>
          ))}
        </>
      )}
    </Dialog>
  )
}

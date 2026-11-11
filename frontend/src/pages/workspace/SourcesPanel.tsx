import { Mascot } from '../../components/Art'
import { Icon } from '../../components/Icon'
import { useToast } from '../../components/Toast'
import { useStore } from '../../store/store'
import { activeSources, type Notebook, type Source, type SourceType } from '../../types'
import type { View } from './Workspace'

const TL: Record<SourceType, string> = { pdf: 'PDF', docx: 'Word', txt: 'Text', md: 'Notes', html: 'Web', csv: 'CSV', xlsx: 'Excel', json: 'Data', link: 'Link', file: 'File' }

export const FileType = ({ type }: { type: SourceType }) => <span className={`ft ${type}`}>{TL[type]}</span>

export function SourcesPanel({ nb, onAdd, onView }: { nb: Notebook; onAdd: () => void; onView: (v: View) => void }) {
  const ready = nb.sources.filter((s) => s.status === 'ready').length
  return (
    <section className="card panel p-src" aria-labelledby="src-h">
      <div className="ph2">
        <h2 id="src-h">Sources</h2>
        <span className="pill">
          {activeSources(nb).length} of {ready} in use
        </span>
      </div>
      <div className="pbody">
        {nb.sources.length === 0 ? (
          <div className="empty">
            <Mascot mood="wow" color="lime" size={84} />
            <h3>No sources yet</h3>
            <p>Add a document or some text to get started.</p>
            <button type="button" className="btn lime sm" onClick={onAdd} data-sound="open">
              <Icon name="plus" size={14} /> Add source
            </button>
          </div>
        ) : (
          <ul className="col g8">
            {nb.sources.map((s) => (
              <SourceRow key={s.id} nbId={nb.id} s={s} onView={onView} />
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

function SourceRow({ nbId, s, onView }: { nbId: string; s: Source; onView: (v: View) => void }) {
  const { toggleSource, removeSource, retrySource, undoRemove } = useStore()
  const toast = useToast()
  const ready = s.status === 'ready'
  const remove = () => {
    removeSource(nbId, s.id)
    toast(`Removed ${s.name}`, {
      label: 'Undo',
      run: () => {
        undoRemove()
        toast('Restored')
      },
    })
  }
  return (
    <li className={`src${s.status === 'failed' ? ' bad' : ''}${ready && !s.on ? ' off' : ''}`}>
      <div className="top">
        {ready && (
          <button type="button" className={`chk${s.on ? ' on' : ''}`} role="checkbox" aria-checked={s.on} aria-label={`Use ${s.name}`} onClick={() => toggleSource(nbId, s.id)}>
            <Icon name="check" size={14} />
          </button>
        )}
        <FileType type={s.type} />
        <div className="nm">
          {s.name}
          <small>{ready ? s.detail || 'Ready' : s.status === 'reading' ? 'Reading…' : 'Could not read'}</small>
        </div>
        {ready && (
          <>
            <button type="button" className="iconbtn sm" onClick={() => onView({ sourceId: s.id })} aria-label={`View ${s.name}`} data-sound="open">
              <Icon name="eye" size={15} />
            </button>
            <button type="button" className="iconbtn sm" onClick={remove} aria-label={`Remove ${s.name}`} data-sound="close">
              <Icon name="trash" size={15} />
            </button>
          </>
        )}
      </div>
      {s.status === 'reading' && (
        <div className="bar" role="progressbar" aria-label={`Reading ${s.name}`}>
          <i />
        </div>
      )}
      {s.status === 'failed' && (
        <>
          <p className="err">{s.error}</p>
          <div className="row g8 wrapx">
            {s.retryable && (
              <button type="button" className="btn sm soft" onClick={() => retrySource(nbId, s.id)}>
                <Icon name="refresh" size={14} /> Try again
              </button>
            )}
            <button type="button" className="btn sm soft" onClick={remove} data-sound="close">
              Remove
            </button>
          </div>
        </>
      )}
    </li>
  )
}

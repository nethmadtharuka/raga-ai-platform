import { AnimatePresence } from 'motion/react'
import { useState } from 'react'
import { Icon } from '../../components/Icon'
import { useToast } from '../../components/Toast'
import type { Notebook } from '../../types'
import { AddSourcesDialog } from './AddSourcesDialog'
import { ChatPanel } from './ChatPanel'
import { SourceViewer } from './SourceViewer'
import { SourcesPanel } from './SourcesPanel'
import { StudioPanel } from './StudioPanel'

type Tab = 'sources' | 'chat' | 'studio'
export type View = { sourceId: string; passage?: string }

export function Workspace({ nb }: { nb: Notebook }) {
  const toast = useToast()
  const [tab, setTab] = useState<Tab>(nb.sources.length ? 'chat' : 'sources')
  const [adding, setAdding] = useState(false)
  const [view, setView] = useState<View | null>(null)
  const openAdd = () => setAdding(true)

  return (
    <div className="wrap page">
      <div className="wsh">
        <a className="iconbtn" href="#notebooks" aria-label="All notebooks">
          <Icon name="back" size={16} />
        </a>
        <h1>{nb.title}</h1>
        <button type="button" className="btn sm soft" onClick={() => toast('Sharing is coming soon.')}>
          Share
        </button>
        <button type="button" className="btn sm lime" onClick={openAdd} data-sound="open">
          <Icon name="plus" size={14} /> Add source
        </button>
      </div>

      <div className="tabs3" role="tablist" aria-label="Notebook sections" data-i={tab === 'sources' ? 0 : tab === 'chat' ? 1 : 2}>
        {(
          [
            ['sources', 'Sources'],
            ['chat', 'Chat'],
            ['studio', 'Studio'],
          ] as const
        ).map(([v, label]) => (
          <button key={v} type="button" role="tab" aria-selected={tab === v} className={tab === v ? 'on' : ''} onClick={() => setTab(v)}>
            {label}
          </button>
        ))}
      </div>

      <div className="ws" data-tab={tab}>
        <SourcesPanel nb={nb} onAdd={openAdd} onView={setView} />
        <ChatPanel nb={nb} onAdd={openAdd} onView={setView} />
        <StudioPanel nb={nb} />
      </div>

      <AnimatePresence>
      {adding && (
        <AddSourcesDialog
          nb={nb}
          onClose={() => setAdding(false)}
          onAdded={() => {
            setAdding(false)
            setTab('sources')
          }}
        />
      )}
      </AnimatePresence>
      <AnimatePresence>{view && <SourceViewer nb={nb} view={view} onClose={() => setView(null)} />}</AnimatePresence>
    </div>
  )
}

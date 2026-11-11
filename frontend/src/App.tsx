import { AnimatePresence, m } from 'motion/react'
import { AppTop, useHash } from './components/Chrome'
import { ToastProvider } from './components/Toast'
import { MotionProvider } from './lib/motion'
import { Landing } from './pages/Landing'
import { Notebooks } from './pages/Notebooks'
import { Workspace } from './pages/workspace/Workspace'
import { StoreProvider, useStore } from './store/store'

const page = {
  initial: { opacity: 0, y: 14 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.35 } },
  exit: { opacity: 0, y: -8, transition: { duration: 0.18 } },
}

function Routes() {
  const hash = useHash()
  const { notebooks } = useStore()
  const nb = hash.startsWith('nb-') ? notebooks.find((n) => n.id === hash.slice(3)) : undefined
  const route = nb ? 'workspace' : hash === 'notebooks' || hash.startsWith('nb-') ? 'notebooks' : 'home'

  return (
    <>
      {route !== 'home' && <AppTop active />}
      <AnimatePresence mode="wait" initial={false} onExitComplete={() => window.scrollTo(0, 0)}>
        <m.div key={nb ? nb.id : route} {...page}>
          {route === 'home' ? <Landing /> : <main>{nb ? <Workspace nb={nb} /> : <Notebooks />}</main>}
        </m.div>
      </AnimatePresence>
    </>
  )
}

export default function App() {
  return (
    <MotionProvider>
      <ToastProvider>
        <StoreProvider>
          <Routes />
        </StoreProvider>
      </ToastProvider>
    </MotionProvider>
  )
}

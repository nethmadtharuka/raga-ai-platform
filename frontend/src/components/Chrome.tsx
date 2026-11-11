import { useState, useSyncExternalStore } from 'react'
import { isSoundOn, setSoundOn, subscribeSound } from '../lib/sound'
import { Icon } from './Icon'
import { useToast } from './Toast'

/* ---------- theme ---------- */
const media = () => window.matchMedia('(prefers-color-scheme: dark)')
const isDark = () => {
  const t = document.documentElement.getAttribute('data-theme')
  return t ? t === 'dark' : media().matches
}

export function ThemeToggle({ inverse = false }: { inverse?: boolean }) {
  const [dark, setDark] = useState(isDark)
  const toggle = () => {
    const next = isDark() ? 'light' : 'dark'
    document.documentElement.setAttribute('data-theme', next)
    try {
      localStorage.setItem('raga-theme', next)
    } catch {
      /* storage blocked: theme still applies for this visit */
    }
    setDark(next === 'dark')
  }
  return (
    <button type="button" className={`iconbtn${inverse ? ' inv' : ''}`} onClick={toggle} aria-label="Toggle light and dark theme" data-sound="toggle">
      <span key={dark ? 'sun' : 'moon'} className="swap">
        <Icon name={dark ? 'sun' : 'moon'} />
      </span>
    </button>
  )
}

/* ---------- sound effects on/off (remembered in localStorage) ---------- */
export function SoundToggle({ inverse = false }: { inverse?: boolean }) {
  const on = useSyncExternalStore(subscribeSound, isSoundOn)
  return (
    <button
      type="button"
      className={`iconbtn${inverse ? ' inv' : ''}`}
      onClick={() => setSoundOn(!on)}
      aria-label="Sound effects"
      aria-pressed={on}
      title={on ? 'Sound effects on' : 'Sound effects off'}
      data-sound="none"
    >
      <span key={on ? 'on' : 'off'} className="swap">
        <Icon name={on ? 'sound' : 'mute'} />
      </span>
    </button>
  )
}

/* ---------- hash routing, like the prototype: #home, #notebooks, #nb-<id> ---------- */
const subscribe = (cb: () => void) => {
  window.addEventListener('hashchange', cb)
  return () => window.removeEventListener('hashchange', cb)
}
// eslint-disable-next-line react-refresh/only-export-components
export const useHash = () => useSyncExternalStore(subscribe, () => location.hash.slice(1) || 'home')

/* ---------- app header ---------- */
export function AppTop({ active }: { active: boolean }) {
  const toast = useToast()
  return (
    <header className="apptop">
      <div className="wrap">
        <a className="logo" href="#home" aria-label="RAGMe! website">
          RAGMe!
        </a>
        <nav aria-label="App">
          <a href="#notebooks" className={active ? 'on' : ''} aria-current={active ? 'page' : undefined}>
            Notebooks
          </a>
        </nav>
        <button type="button" className="btn sm soft hide-s" onClick={() => toast('The help center is coming soon.')}>
          <Icon name="help" size={16} /> Help
        </button>
        <SoundToggle />
        <ThemeToggle />
        <a className="iconbtn" href="#home" aria-label="Back to website">
          <Icon name="back" size={16} />
        </a>
        <span className="avatar" aria-hidden="true">
          A
        </span>
      </div>
    </header>
  )
}

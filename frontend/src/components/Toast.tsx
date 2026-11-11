import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

type Action = { label: string; run: () => void }
type ToastFn = (message: string, action?: Action) => void

const Ctx = createContext<ToastFn>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [t, setT] = useState<{ message: string; action?: Action; on: boolean }>({ message: '', on: false })
  const timer = useRef<number | undefined>(undefined)

  const show = useCallback<ToastFn>((message, action) => {
    window.clearTimeout(timer.current)
    setT({ message, action, on: true })
    timer.current = window.setTimeout(() => setT((p) => ({ ...p, on: false })), action ? 6000 : 2800)
  }, [])

  return (
    <Ctx.Provider value={show}>
      {children}
      <div className={`toast${t.on ? ' on' : ''}`} role="status" aria-live="polite">
        {t.on && <span>{t.message}</span>}
        {t.on && t.action && (
          <button
            type="button"
            onClick={() => {
              setT((p) => ({ ...p, on: false }))
              t.action!.run()
            }}
          >
            {t.action.label}
          </button>
        )}
      </div>
    </Ctx.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useToast = () => useContext(Ctx)

/** Clipboard copy with the prototype's feedback. */
// eslint-disable-next-line react-refresh/only-export-components
export function copyText(text: string, toast: ToastFn) {
  const fail = () => toast('Copy is blocked here. Select the text and copy it manually.')
  try {
    navigator.clipboard.writeText(text).then(() => toast('Copied'), fail)
  } catch {
    fail()
  }
}

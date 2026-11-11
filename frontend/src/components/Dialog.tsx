import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { m } from 'motion/react'
import { play } from '../lib/sound'

const FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]),textarea:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex="-1"])'

/** Modal or drawer: scrim, focus trap, Escape to close, focus returns to the opener. */
export function Dialog({ label, onClose, variant = 'modal', children }: { label: string; onClose: () => void; variant?: 'modal' | 'drawer'; children: ReactNode }) {
  const box = useRef<HTMLDivElement>(null)
  const close = useRef(onClose)
  useEffect(() => {
    close.current = onClose
  })

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const el = box.current!
    const first = el.querySelector<HTMLElement>('[autofocus],[data-autofocus]') ?? el.querySelector<HTMLElement>(FOCUSABLE)
    ;(first ?? el).focus()

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault()
        play('close')
        close.current()
        return
      }
      if (e.key !== 'Tab') return
      const items = Array.from(el.querySelectorAll<HTMLElement>(FOCUSABLE)).filter((n) => n.offsetParent !== null || n === document.activeElement)
      if (!items.length) return
      const a = items[0]
      const z = items[items.length - 1]
      if (e.shiftKey && (document.activeElement === a || document.activeElement === el)) {
        e.preventDefault()
        z.focus()
      } else if (!e.shiftKey && document.activeElement === z) {
        e.preventDefault()
        a.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      opener?.focus?.()
    }
  }, [])

  const Tag = variant === 'drawer' ? m.aside : m.div
  // The modal is centred with x/y here (not in CSS) so the motion transform and the centring don't fight.
  const motionProps =
    variant === 'drawer'
      ? { initial: { x: 48, opacity: 0 }, animate: { x: 0, opacity: 1 }, exit: { x: 48, opacity: 0, transition: { duration: 0.2 } } }
      : {
          style: { x: '-50%' },
          initial: { y: '-46%', scale: 0.96, opacity: 0 },
          animate: { y: '-50%', scale: 1, opacity: 1 },
          exit: { y: '-48%', scale: 0.97, opacity: 0, transition: { duration: 0.16 } },
        }
  return createPortal(
    <>
      <m.div
        className="scrim"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1, transition: { duration: 0.2 } }}
        exit={{ opacity: 0, transition: { duration: 0.18 } }}
        onClick={() => {
          play('close')
          close.current()
        }}
      />
      <Tag ref={box} className={variant} role="dialog" aria-modal="true" aria-label={label} tabIndex={-1} transition={{ duration: 0.3 }} {...motionProps}>
        {children}
      </Tag>
    </>,
    document.body,
  )
}

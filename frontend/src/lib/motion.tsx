/**
 * Shared motion setup: the light feature bundle (LazyMotion + domAnimation), reduced-motion support,
 * and a few reveal helpers so every page uses the same timing.
 * Reveal wrappers animate a wrapper element, never the card itself, so cards keep their CSS hover transforms.
 */
import { LazyMotion, MotionConfig, m, type Variants } from 'motion/react'
import type { ReactNode } from 'react'

const loadFeatures = () => import('./motionFeatures').then((r) => r.default)

export const EASE = [0.22, 1, 0.36, 1] as const

export function MotionProvider({ children }: { children: ReactNode }) {
  return (
    <LazyMotion features={loadFeatures} strict>
      <MotionConfig reducedMotion="user" transition={{ duration: 0.45, ease: EASE }}>
        {children}
      </MotionConfig>
    </LazyMotion>
  )
}

const group: Variants = { hidden: {}, show: { transition: { staggerChildren: 0.07 } } }
// eslint-disable-next-line react-refresh/only-export-components
export const revealItem: Variants = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { duration: 0.55, ease: EASE } },
}

type Props = { children: ReactNode; className?: string; onMount?: boolean }

/** Fades up once when scrolled into view (or on mount). */
export function Reveal({ children, className, onMount }: Props) {
  return (
    <m.div
      className={className}
      variants={revealItem}
      initial="hidden"
      {...(onMount ? { animate: 'show' } : { whileInView: 'show', viewport: { once: true, amount: 0.25 } })}
    >
      {children}
    </m.div>
  )
}

/** Container whose RevealItem children arrive one after another. */
export function RevealGroup({ children, className, onMount }: Props) {
  return (
    <m.div
      className={className}
      variants={group}
      initial="hidden"
      {...(onMount ? { animate: 'show' } : { whileInView: 'show', viewport: { once: true, amount: 0.15 } })}
    >
      {children}
    </m.div>
  )
}

export function RevealItem({ children, className = 'ri' }: { children: ReactNode; className?: string }) {
  return (
    <m.div className={className} variants={revealItem}>
      {children}
    </m.div>
  )
}

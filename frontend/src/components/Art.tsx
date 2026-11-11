/** Mascot, scribbles, rotating badge and ribbon, redrawn from the prototype's inline SVG. */
import { m } from 'motion/react'
import { useId, type ReactNode } from 'react'

/** useId() contains ':' which is awkward inside url(#…); strip it. */
const useSvgId = () => 'g' + useId().replace(/[^a-zA-Z0-9_-]/g, '')

export type Mood = 'happy' | 'think' | 'wow' | 'oops' | 'shrug'
export type MascotColor = 'violet' | 'lime' | 'pink' | 'blue'

const face = { fill: 'var(--m-face)' }
const line = { stroke: 'var(--m-face)' }
const shine = { fill: 'var(--white)' }

const FACES: Record<Mood, ReactNode> = {
  happy: (
    <>
      <ellipse cx="44" cy="58" rx="6.5" ry="9.5" style={face} />
      <ellipse cx="76" cy="58" rx="6.5" ry="9.5" style={face} />
      <circle cx="46" cy="54" r="2.4" style={shine} />
      <circle cx="78" cy="54" r="2.4" style={shine} />
      <path d="M48 76q12 12 24 0" fill="none" style={line} strokeWidth="4" strokeLinecap="round" />
    </>
  ),
  think: (
    <>
      <ellipse cx="44" cy="56" rx="6.5" ry="9.5" style={face} />
      <ellipse cx="76" cy="56" rx="6.5" ry="9.5" style={face} />
      <circle cx="46" cy="50" r="2.6" style={shine} />
      <circle cx="78" cy="50" r="2.6" style={shine} />
      <ellipse cx="62" cy="80" rx="5" ry="6" style={face} />
    </>
  ),
  wow: (
    <>
      <ellipse cx="44" cy="56" rx="8" ry="11" style={face} />
      <ellipse cx="76" cy="56" rx="8" ry="11" style={face} />
      <circle cx="46" cy="52" r="3" style={shine} />
      <circle cx="78" cy="52" r="3" style={shine} />
      <ellipse cx="60" cy="82" rx="8" ry="7" style={face} />
    </>
  ),
  oops: (
    <>
      <path d="M37 51l13 13M50 51L37 64M70 51l13 13M83 51L70 64" style={line} strokeWidth="4.5" strokeLinecap="round" />
      <path d="M46 84q5-8 10 0t10 0t10 0" fill="none" style={line} strokeWidth="4" strokeLinecap="round" />
    </>
  ),
  shrug: (
    <>
      <path d="M37 58h14M69 58h14" style={line} strokeWidth="4.5" strokeLinecap="round" />
      <path d="M50 80h20" style={line} strokeWidth="4" strokeLinecap="round" />
    </>
  ),
}

export function Mascot({ mood = 'happy', color = 'violet', size = 120 }: { mood?: Mood; color?: MascotColor; size?: number }) {
  const id = useSvgId()
  const c = (n: number) => `var(--m-${color}-${n})`
  return (
    <svg className="mascot" width={size} height={size} viewBox="0 0 120 120" aria-hidden="true">
      <defs>
        <radialGradient id={id} cx=".34" cy=".26" r=".9">
          <stop offset="0" style={{ stopColor: c(1) }} />
          <stop offset=".55" style={{ stopColor: c(2) }} />
          <stop offset="1" style={{ stopColor: c(3) }} />
        </radialGradient>
      </defs>
      <ellipse cx="60" cy="113" rx="34" ry="5" style={{ fill: 'var(--b-ink)' }} opacity=".2" />
      <ellipse cx="42" cy="108" rx="12" ry="6" style={{ fill: c(3) }} />
      <ellipse cx="78" cy="108" rx="12" ry="6" style={{ fill: c(3) }} />
      <path d="M60 7C93 7 113 29 113 61c0 31-20 49-53 49S7 92 7 61C7 29 27 7 60 7z" fill={`url(#${id})`} />
      <ellipse cx="40" cy="28" rx="17" ry="8.5" style={shine} opacity=".38" transform="rotate(-24 40 28)" />
      {FACES[mood]}
    </svg>
  )
}

export function Scribble({ color, width = 16, d = 'M8 90C40 20 60 150 100 70S160 10 190 80 230 140 262 60', draw = false }: { color: string; width?: number; d?: string; draw?: boolean }) {
  return (
    <svg className="scrib" viewBox="0 0 270 160" fill="none" style={{ stroke: color }} strokeWidth={width} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {draw ? (
        <m.path d={d} initial={{ pathLength: 0, opacity: 0 }} animate={{ pathLength: 1, opacity: 1 }} transition={{ delay: 0.35, duration: 1.3, ease: 'easeInOut' }} />
      ) : (
        <path d={d} />
      )}
    </svg>
  )
}

export function SpinBadge() {
  const id = useSvgId()
  return (
    <svg className="badge-spin" viewBox="0 0 160 160" aria-hidden="true">
      <circle cx="80" cy="80" r="78" style={{ fill: 'var(--b-pink)' }} />
      <defs>
        <path id={id} d="M80 80m-52 0a52 52 0 1 1 104 0a52 52 0 1 1-104 0" />
      </defs>
      <text fontSize="15" fontWeight="700" letterSpacing="1.8" style={{ fill: 'var(--on-pink)' }}>
        <textPath href={`#${id}`}>ASK YOUR DOCUMENTS  -   GET REAL ANS</textPath>
      </text>
      <text x="80" y="92" textAnchor="middle" fontFamily="Anton, Impact, sans-serif" fontSize="30" style={{ fill: 'var(--on-pink)' }}>
        ASK
      </text>
    </svg>
  )
}

const RIB = ['Upload', 'Ask', 'Understand', 'Check the source', 'Summarise', 'Study', 'Decide']

export function Ribbon() {
  const items = [0, 1, 2, 3].flatMap((k) =>
    RIB.map((t) => (
      <span key={`${k}-${t}`} style={{ display: 'contents' }}>
        <span>{t}</span>
        <i>✱</i>
      </span>
    )),
  )
  return (
    <div className="ribbon" aria-hidden="true">
      <div className="rib-track">{items}</div>
    </div>
  )
}

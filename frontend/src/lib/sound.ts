/**
 * Central sound effects. One provided asset (sounds/mixkit-quick-win-video-game-notification-269.wav,
 * two short notes between ~70 and ~475 ms) is sliced and pitched into variants with the Web Audio API.
 *
 * - The audio context is created on the first user gesture, as browsers' autoplay rules require.
 * - The file is fetched and decoded once, lazily, so it never delays page load.
 * - A new sound stops the one still playing, and the same sound is throttled, so nothing piles up.
 * - The ON/OFF preference is stored in localStorage ('ragme-sound').
 */
import assetUrl from '../../../sounds/mixkit-quick-win-video-game-notification-269.wav?url'

export type Sfx = 'click' | 'toggle' | 'open' | 'close' | 'confirm' | 'success'

// [offset s, duration s, playback rate, volume]
const SPEC: Record<Sfx, [number, number, number, number]> = {
  click: [0.072, 0.05, 1.6, 0.16],
  toggle: [0.072, 0.045, 2.1, 0.14],
  open: [0.072, 0.11, 1.25, 0.2],
  close: [0.148, 0.09, 0.85, 0.17],
  confirm: [0.072, 0.17, 1.05, 0.22],
  success: [0.07, 0.42, 1, 0.26],
}
const THROTTLE_MS = 70
const KEY = 'ragme-sound'

let ctx: AudioContext | null = null
let buffer: AudioBuffer | null = null
let loading: Promise<void> | null = null
let voice: AudioBufferSourceNode | null = null
const lastAt: Partial<Record<Sfx, number>> = {}
const listeners = new Set<() => void>()

let enabled = (() => {
  try {
    return localStorage.getItem(KEY) !== 'off'
  } catch {
    return true
  }
})()

function load() {
  if (!ctx) {
    const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AC) return Promise.resolve()
    ctx = new AC()
  }
  loading ??= fetch(assetUrl)
    .then((r) => r.arrayBuffer())
    .then((data) => ctx!.decodeAudioData(data))
    .then((b) => {
      buffer = b
    })
    .catch(() => {
      loading = null // try again on a later gesture
    })
  return loading
}

export function play(name: Sfx) {
  if (!enabled || typeof window === 'undefined') return
  const now = performance.now()
  if (now - (lastAt[name] ?? 0) < THROTTLE_MS) return
  lastAt[name] = now
  if (buffer && ctx) start(name)
  // Still decoding (first gesture): play only if it is ready quickly, never late.
  else void load().then(() => buffer && performance.now() - now < 400 && start(name))
}

function start(name: Sfx) {
  if (!ctx || !buffer) return
  if (ctx.state === 'suspended') void ctx.resume()
  const [offset, dur, rate, vol] = SPEC[name]
  try {
    voice?.stop()
  } catch {
    /* already ended */
  }
  const src = ctx.createBufferSource()
  src.buffer = buffer
  src.playbackRate.value = rate
  const gain = ctx.createGain()
  const t = ctx.currentTime
  const real = dur / rate
  gain.gain.setValueAtTime(vol, t)
  gain.gain.linearRampToValueAtTime(0.0001, t + real) // fade out: no click at the cut
  src.connect(gain).connect(ctx.destination)
  src.start(t, offset, dur)
  src.onended = () => {
    if (voice === src) voice = null
  }
  voice = src
}

export const isSoundOn = () => enabled

export function setSoundOn(on: boolean) {
  enabled = on
  try {
    localStorage.setItem(KEY, on ? 'on' : 'off')
  } catch {
    /* storage blocked: applies to this visit */
  }
  listeners.forEach((l) => l())
  if (on) {
    void load()
    play('toggle')
  }
}

export function subscribeSound(cb: () => void) {
  listeners.add(cb)
  return () => listeners.delete(cb)
}

const CONTROL = 'button, a[href], summary, [role="tab"], [role="checkbox"]'

/**
 * One delegated listener gives every control its sound, so components hold no audio code.
 * A control can choose its sound with data-sound="open|close|toggle|confirm|success|none".
 */
export function installSounds() {
  const warm = () => {
    if (enabled) void load()
  }
  document.addEventListener('pointerdown', warm, { capture: true, once: true })
  document.addEventListener('keydown', warm, { capture: true, once: true })
  document.addEventListener(
    'click',
    (e) => {
      const el = (e.target as Element | null)?.closest?.(CONTROL)
      if (!el || el.closest('[data-sound="none"]')) return
      const chosen = el.closest('[data-sound]')?.getAttribute('data-sound') as Sfx | null
      const role = el.getAttribute('role')
      play(chosen ?? (role === 'tab' || role === 'checkbox' ? 'toggle' : 'click'))
    },
    true,
  )
}

/** Turns backend answers into the shapes the chat shows. Pure: no network, no React. */
import type { Answer, Ref, Source } from '../types.ts'
import type { Passage, RagResponse } from './documents.ts'

// ponytail: phrase match for "the documents don't cover this"; replace with a backend flag if one is added.
const NOT_FOUND =
  /\b(context|documents?|sources?|text)( provided)?\s+(does not|doesn't|do not|don't)\s+(contain|mention|include|provide|say|specify|have)\b|not enough information|\bno information (about|on|regarding)\b|\b(cannot|can't|unable to) (answer|determine)\b|not (mentioned|covered) in the (provided )?(context|documents?|sources?)/i

/**
 * The server searches every document it has (it cannot filter), so the result is checked against
 * this notebook's ticked sources:
 *  - no cited source is ticked here -> "couldn't find" (with "switched off" wording when some are unticked)
 *  - some cited sources are outside the selection -> answer shown with a note
 */
export function toAnswer(res: RagResponse, passages: Passage[], sources: Source[]): Answer {
  const answer = (res.answer ?? '').trim()
  const titles = [...new Set(res.sourcesUsed ?? [])]
  const byName = new Map(sources.map((s) => [s.name, s]))
  const isOn = (t: string) => {
    const s = byName.get(t)
    return !!s && s.status === 'ready' && s.on
  }
  const onTitles = titles.filter(isOn)
  const offHere = titles.some((t) => byName.has(t) && !isOn(t))

  if (!answer || !titles.length || !onTitles.length || NOT_FOUND.test(answer.slice(0, 400))) {
    return { kind: 'none', switchedOff: offHere && !onTitles.length }
  }

  const refs: Ref[] = []
  for (const p of passages) {
    if (!isOn(p.title)) continue
    const s = byName.get(p.title)!
    if (!refs.some((r) => r.sourceId === s.id && r.passage === p.content)) refs.push({ sourceId: s.id, name: s.name, passage: p.content })
  }
  for (const t of onTitles) {
    const s = byName.get(t)!
    if (!refs.some((r) => r.sourceId === s.id)) refs.push({ sourceId: s.id, name: s.name })
  }

  return { kind: 'found', paragraphs: toParagraphs(answer), refs, partial: onTitles.length < titles.length }
}

/** Plain paragraphs from the model's light markdown (bold markers and bullets). */
export function toParagraphs(text: string): string[] {
  return text
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^[ \t]*[*-][ \t]+/gm, '• ')
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean)
}

export const answerText = (a: Answer) =>
  a.kind === 'found' ? a.paragraphs.join('\n\n') + ' ' + a.refs.map((_, i) => `[${i + 1}]`).join('') : ''

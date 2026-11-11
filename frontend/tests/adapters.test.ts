// Run: npm test  (Node 22.18+ runs TypeScript directly)
import assert from 'node:assert/strict'
import { toAnswer, toParagraphs } from '../src/api/adapters.ts'
import type { Source } from '../src/types.ts'

const src = (id: string, name: string, on = true): Source => ({ id, name, type: 'txt', detail: '', status: 'ready', on })
const sources = [src('a', 'Leave.txt'), src('b', 'Pay.md'), src('c', 'Old.txt', false)]
const passages = [
  { title: 'Leave.txt', content: '14 days of leave.' },
  { title: 'Pay.md', content: 'Paid on the 25th.' },
  { title: 'Leave.txt', content: '14 days of leave.' },
]

// found: refs keep passages, de-duplicated, only for ticked sources
let a = toAnswer({ answer: 'You get 14 days.', sourcesUsed: ['Leave.txt', 'Pay.md', 'Leave.txt'] }, passages, sources)
assert.equal(a.kind, 'found')
if (a.kind === 'found') {
  assert.deepEqual(a.refs.map((r) => r.sourceId), ['a', 'b'])
  assert.equal(a.refs[0].passage, '14 days of leave.')
  assert.equal(a.partial, false)
}

// partial: one cited source is switched off
a = toAnswer({ answer: 'You get 14 days.', sourcesUsed: ['Leave.txt', 'Old.txt'] }, passages, sources)
assert.ok(a.kind === 'found' && a.partial)

// every cited source switched off -> none, "switched off" wording
a = toAnswer({ answer: 'Yes.', sourcesUsed: ['Old.txt'] }, [], sources)
assert.deepEqual(a, { kind: 'none', switchedOff: true })

// cited sources belong to another notebook -> none, plain wording
a = toAnswer({ answer: 'Yes.', sourcesUsed: ['Elsewhere.pdf'] }, [], sources)
assert.deepEqual(a, { kind: 'none', switchedOff: false })

// no documents on the server
a = toAnswer({ answer: "I don't have any documents to reference.", sourcesUsed: [] }, [], sources)
assert.equal(a.kind, 'none')

// the model says the material doesn't cover it
a = toAnswer({ answer: 'The provided context does not contain information about parental leave.', sourcesUsed: ['Leave.txt'] }, passages, sources)
assert.equal(a.kind, 'none')

// ordinary negative sentences are still answers
a = toAnswer({ answer: 'Employees do not have to provide documents for one day off.', sourcesUsed: ['Leave.txt'] }, passages, sources)
assert.equal(a.kind, 'found')

assert.deepEqual(toParagraphs('**Hi** there\n\n* one\n* two'), ['Hi there', '• one\n• two'])
console.log('adapters: all checks passed')

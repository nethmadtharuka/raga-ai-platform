import { act, useState, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

// Lets React's act() work outside a test-library wrapper.
;(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

let shouldThrow = true
function Flaky() {
  if (shouldThrow) throw new Error('boom')
  return <p>page content</p>
}

let host: HTMLDivElement
let root: Root
const render = (ui: ReactNode) => act(() => root.render(ui))
const text = () => host.textContent ?? ''
const button = (label: string) => Array.from(host.querySelectorAll('button,a')).find((b) => b.textContent?.trim() === label) as HTMLElement

beforeEach(() => {
  shouldThrow = true
  host = document.createElement('div')
  document.body.append(host)
  root = createRoot(host)
  vi.spyOn(console, 'error').mockImplementation(() => {}) // React logs caught errors; keep test output clean
})
afterEach(() => {
  act(() => root.unmount())
  host.remove()
  vi.restoreAllMocks()
})

describe('ErrorBoundary', () => {
  it('renders children when nothing fails', () => {
    shouldThrow = false
    render(<ErrorBoundary><Flaky /></ErrorBoundary>)
    expect(text()).toContain('page content')
    expect(host.querySelector('[role="alert"]')).toBeNull()
  })

  it('shows the recovery screen instead of a blank page when a child throws', () => {
    render(<ErrorBoundary><Flaky /></ErrorBoundary>)
    expect(host.querySelector('[role="alert"]')).not.toBeNull()
    expect(text()).toContain('Something went wrong on this page')
    expect(button('Try again')).toBeTruthy()
    expect(button('Back to notebooks').getAttribute('href')).toBe('#notebooks')
  })

  it('"Try again" renders the page again once the problem is gone', () => {
    render(<ErrorBoundary><Flaky /></ErrorBoundary>)
    shouldThrow = false
    act(() => button('Try again').click())
    expect(text()).toContain('page content')
  })

  it('"Try again" shows the recovery screen again if the error persists', () => {
    render(<ErrorBoundary><Flaky /></ErrorBoundary>)
    act(() => button('Try again').click())
    expect(text()).toContain('Something went wrong on this page')
  })

  it('clears the error when the route (resetKey) changes', () => {
    function Harness() {
      const [key, setKey] = useState('nb-1')
      return (
        <>
          <button onClick={() => setKey('notebooks')}>navigate</button>
          <ErrorBoundary resetKey={key}><Flaky /></ErrorBoundary>
        </>
      )
    }
    render(<Harness />)
    expect(text()).toContain('Something went wrong on this page')
    shouldThrow = false
    act(() => button('navigate').click())
    expect(text()).toContain('page content')
  })

  it('logs the crash for debugging', () => {
    render(<ErrorBoundary><Flaky /></ErrorBoundary>)
    expect(console.error).toHaveBeenCalledWith('RAGMe! view crashed:', expect.any(Error), expect.anything())
  })
})

import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Mascot } from './Art'

type Props = {
  children: ReactNode
  /** When this changes (e.g. the route), a shown error is cleared and the page renders again. */
  resetKey?: string
}
type State = { error: Error | null }

/** Catches rendering errors so one broken view shows a recovery screen instead of a blank page. */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('RAGMe! view crashed:', error, info.componentStack)
  }

  componentDidUpdate(prev: Props) {
    if (this.state.error && prev.resetKey !== this.props.resetKey) this.setState({ error: null })
  }

  retry = () => this.setState({ error: null })

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div className="wrap page">
        <div className="empty" role="alert">
          <Mascot mood="oops" color="pink" size={96} />
          <h2>Something went wrong on this page</h2>
          <p>Your notebooks and sources are safe. Try again, or go back to your notebooks.</p>
          <div className="row g8 wrapx center">
            <button type="button" className="btn lime" onClick={this.retry}>
              Try again
            </button>
            <a className="btn soft" href="#notebooks" onClick={this.retry}>
              Back to notebooks
            </a>
          </div>
        </div>
      </div>
    )
  }
}

import { Component, type ErrorInfo, type ReactNode } from "react";

/**
 * Keeps a rendering error inside one part of the page. Without it React unmounts the whole app, so one
 * unexpected table would blank every page. `fallback` gets a reset that renders the children again.
 */
export class ErrorBoundary extends Component<
  { fallback: (reset: () => void) => ReactNode; children: ReactNode },
  { failed: boolean }
> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error(error, info.componentStack);
  }

  reset = () => this.setState({ failed: false });

  render() {
    return this.state.failed ? this.props.fallback(this.reset) : this.props.children;
  }
}

"use client";

import React, { Component, ReactNode } from "react";

interface ErrorBoundaryProps {
    children: ReactNode;
    fallback?: ReactNode;
    onError?: (error: Error, errorInfo: React.ErrorInfo) => void;
}

interface ErrorBoundaryState {
    hasError: boolean;
    error: Error | null;
}

/**
 * React Error Boundary component for catching and handling runtime errors.
 * Wraps components to prevent entire app crashes from uncaught exceptions.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): ErrorBoundaryState {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
        console.error("[ErrorBoundary] Caught error:", error, errorInfo);
        this.props.onError?.(error, errorInfo);
    }

    handleReset = () => {
        this.setState({ hasError: false, error: null });
    };

    render() {
        if (this.state.hasError) {
            // Custom fallback if provided
            if (this.props.fallback) {
                return this.props.fallback;
            }

            // Default error UI
            return (
                <div className="px-6 py-10 sm:px-12 lg:px-16">
                    <div className="mx-auto max-w-3xl rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6">
                        <h2 className="text-xl font-semibold text-white">Something went wrong</h2>
                        <p className="mt-2 text-sm text-white/80">
                            {this.state.error?.message || "An unexpected error occurred"}
                        </p>
                        <div className="mt-4 flex gap-3">
                            <button
                                onClick={this.handleReset}
                                className="rounded-lg bg-white/10 px-4 py-2 text-sm font-medium text-white hover:bg-white/20 transition-colors"
                            >
                                Try Again
                            </button>
                            <button
                                onClick={() => window.location.reload()}
                                className="rounded-lg border border-white/20 px-4 py-2 text-sm text-white/70 hover:bg-white/5 transition-colors"
                            >
                                Reload Page
                            </button>
                        </div>
                        {process.env.NODE_ENV === "development" && this.state.error?.stack && (
                            <pre className="mt-4 max-h-48 overflow-auto rounded-lg bg-black/30 p-3 text-xs text-rose-300/80">
                                {this.state.error.stack}
                            </pre>
                        )}
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

/**
 * HOC to wrap functional components with error boundary
 */
export function withErrorBoundary<P extends object>(
    WrappedComponent: React.ComponentType<P>,
    fallback?: ReactNode
) {
    return function WithErrorBoundary(props: P) {
        return (
            <ErrorBoundary fallback={fallback}>
                <WrappedComponent {...props} />
            </ErrorBoundary>
        );
    };
}

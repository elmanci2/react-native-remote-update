/* eslint-disable prettier/prettier */
import React, { type ReactNode } from 'react';
import { NativeModules } from 'react-native';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  dev?: boolean;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

const RemoteUpdateModule = NativeModules.RemoteUpdate;
const INITIALIZATION_TIME_LIMIT = 5000;

class RemoteUpdateProvider extends React.Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  private appStartTime: number;

  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
    this.appStartTime = Date.now();
  }

  static getDerivedStateFromError(_: Error): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    console.error('Error caught in RemoteUpdateProvider:', error, errorInfo);

    const currentTime = Date.now();
    const timeSinceAppStart = currentTime - this.appStartTime;

    if (timeSinceAppStart <= INITIALIZATION_TIME_LIMIT && !this.props.dev) {
      RemoteUpdateModule.getBackupBundles(
        (err: string | null, result: string[]) => {
          if (err) {
            console.error('Failed to get backup bundles:', err);
          } else if (result.length > 0) {
            RemoteUpdateModule.deleteBundle(
              result[0]?.replace('.bundle', ''),
              (deleteError: string | null) => {
                if (deleteError) {
                  console.error('Failed to delete bundle:', deleteError);
                }
              }
            );
          }
        }
      );
    }
  }

  render() {
    const currentTime = Date.now();
    const timeSinceAppStart = currentTime - this.appStartTime;

    if (this.state.hasError) {
      if (timeSinceAppStart <= INITIALIZATION_TIME_LIMIT && !this.props.dev) {
        return this.props.fallback || null;
      }
      return null;
    }

    return this.props.children;
  }
}

export { RemoteUpdateProvider };

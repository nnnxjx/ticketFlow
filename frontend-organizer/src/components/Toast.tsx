import { useEffect } from "react";

export function Toast({ message, dismiss }: { message: string; dismiss: () => void }) {
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(dismiss, 4000);
    return () => window.clearTimeout(timer);
  }, [message]);
  if (!message) return null;
  return <div className="toast" role="status" aria-live="polite">{message}<button aria-label="关闭提示" onClick={dismiss}>×</button></div>;
}

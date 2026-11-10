import { EventEditorPanel } from "./EventEditorPanel";
import { SessionEditorPanel } from "./SessionEditorPanel";

export function ShowsPanel() {
  return (
    <div className="two-col">
      <EventEditorPanel />
      <SessionEditorPanel />
    </div>
  );
}

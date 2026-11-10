import React from "react";
import ReactDOM from "react-dom/client";
import { OrganizerProvider } from "./context";
import { OrganizerLayout } from "./components/OrganizerLayout";
import "./style.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <OrganizerProvider>
      <OrganizerLayout />
    </OrganizerProvider>
  </React.StrictMode>,
);

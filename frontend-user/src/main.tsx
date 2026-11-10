import React from "react";
import ReactDOM from "react-dom/client";
import { UserProvider } from "./context";
import { UserLayout } from "./components/UserLayout";
import "./style.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <UserProvider>
      <UserLayout />
    </UserProvider>
  </React.StrictMode>,
);

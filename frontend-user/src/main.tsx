import React from "react";
import ReactDOM from "react-dom/client";
import { UserProvider } from "./context";
import { UserLayout } from "./components/UserLayout";
import { BrowserRouter } from "react-router";
import "./style.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <UserProvider>
        <UserLayout />
      </UserProvider>
    </BrowserRouter>
  </React.StrictMode>,
);

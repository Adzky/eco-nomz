import React from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
import { LogoCropper } from "./LogoCropper";

const rootElement = document.getElementById("root");
if (!rootElement) {
  throw new Error("The logo crop application root is missing.");
}

const requestedLanguage =
  rootElement.dataset.language ?? new URLSearchParams(window.location.search).get("lang");
const language = requestedLanguage === "en" ? "en" : "id";
const restaurantIdValue = rootElement.dataset.restaurantId;
const restaurantId = restaurantIdValue ? Number(restaurantIdValue) : null;
if (restaurantIdValue && !Number.isSafeInteger(restaurantId)) {
  throw new Error("The restaurant logo page has an invalid restaurant ID.");
}
const csrfToken = document.querySelector<HTMLMetaElement>('meta[name="_csrf"]')?.content ?? null;
const csrfHeaderName = document.querySelector<HTMLMetaElement>('meta[name="_csrf_header"]')?.content ?? null;

createRoot(rootElement).render(
  <React.StrictMode>
    <LogoCropper
      language={language}
      restaurantId={restaurantId}
      logoUrl={rootElement.dataset.logoUrl ?? null}
      csrfToken={csrfToken}
      csrfHeaderName={csrfHeaderName}
    />
  </React.StrictMode>,
);

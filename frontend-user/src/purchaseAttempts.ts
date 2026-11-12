import type { Id } from "./types";

export type PurchaseAttempt = {
  requestId: string;
  tierId: Id;
  tierName: string;
  quantity: number;
  createdAt: number;
};

function storageKey(token: string) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return `ticketflow.purchaseAttempts.${payload.sub}`;
  } catch {
    return "ticketflow.purchaseAttempts.unknown";
  }
}

export function readAttempts(token: string): PurchaseAttempt[] {
  if (!token) return [];
  try {
    const value = JSON.parse(localStorage.getItem(storageKey(token)) || "[]");
    return Array.isArray(value) ? value.filter((x) => typeof x.requestId === "string" && typeof x.tierId === "string") : [];
  } catch {
    return [];
  }
}

export function writeAttempts(token: string, attempts: PurchaseAttempt[]) {
  if (token) localStorage.setItem(storageKey(token), JSON.stringify(attempts));
}

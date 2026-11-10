import type { SessionForm } from "./types";

export const money = (x: number) =>
  `¥${Number(x).toLocaleString("zh-CN", { minimumFractionDigits: 2 })}`;

export const local = (x: string) =>
  new Date(`${x}Z`).toLocaleString("zh-CN", { hour12: false });

export const toUtc = (x: string) => new Date(x).toISOString().slice(0, 19);

export const toInput = (x: string) => {
  const d = new Date(`${x}Z`);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};

export const future = (days: number) => {
  const d = new Date(Date.now() + days * 86400000);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};

export const status: Record<string, string> = {
  DRAFT: "草稿",
  PUBLISHED: "已发布",
  ON_SALE: "售卖中",
  STOPPED: "已停售",
  OFF_SHELF: "已下架",
  PENDING_PAYMENT: "待支付",
  PAID: "已支付",
  TICKETED: "已出票",
  CANCELLED: "已取消",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
};

export const emptySession = (): SessionForm => ({
  venueId: "",
  saleMode: "GENERAL",
  saleStart: future(1),
  saleEnd: future(3),
  startTime: future(4),
  endTime: future(4.15),
  checkinStart: future(3.95),
  checkinEnd: future(4.1),
});

export const price = (n: number) =>
  `¥${Number(n).toLocaleString("zh-CN", { minimumFractionDigits: 2 })}`;

export const when = (s: string) =>
  new Date(s.endsWith("Z") ? s : `${s}Z`).toLocaleString("zh-CN", {
    hour12: false,
  });

export const labels: Record<string, string> = {
  PENDING_PAYMENT: "待支付",
  PAID: "已支付",
  TICKETED: "已出票",
  CANCELLED: "已取消",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
  SOLD_OUT: "票已售罄",
  LIMIT_EXCEEDED: "超出个人限购",
  SEAT_UNAVAILABLE: "座位已被占用",
  ORDER_EXPIRED: "订单已过期",
};

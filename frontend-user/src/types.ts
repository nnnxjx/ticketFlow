export type Id = string;

export type Show = {
  id: Id;
  title: string;
  category: string | null;
  description: string | null;
};

export type Session = {
  id: Id;
  venueId: Id;
  saleMode: "GENERAL" | "ASSIGNED";
  startTime: string;
  saleStart: string;
};

export type Tier = {
  id: Id;
  name: string;
  price: number;
  availableStock: number;
  perUserLimit: number;
};

export type Seat = {
  id: Id;
  area: string;
  rowNo: string;
  seatNo: string;
  price: number;
  status: string;
  version: string;
};

export type Order = {
  id: Id;
  sessionId: Id;
  saleMode: string;
  status: string;
  amount: number;
  quantity: number;
  expireAt: string;
  createdAt: string;
  items: { id: Id; name: string; price: number; sessionSeatId: Id | null }[];
};

export type Ticket = { id: Id; status: string; token: string };

export type AsyncResult = {
  requestId: string;
  status: string;
  orderId: Id | null;
  failureCode: string | null;
};

export type Envelope<T> = { code: string; message: string; data: T };

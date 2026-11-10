export type Id = string;

export type Organizer = { id: Id; name: string; status: string };

export type Show = {
  id: Id;
  title: string;
  category: string | null;
  description: string | null;
  status: string;
};

export type Venue = { id: Id; name: string; address: string | null };

export type VenueSeat = { id: Id; area: string; rowNo: string; seatNo: string };

export type Session = {
  id: Id;
  eventId: Id;
  venueId: Id;
  saleMode: "GENERAL" | "ASSIGNED";
  saleStart: string;
  saleEnd: string;
  startTime: string;
  endTime: string;
  checkinStart: string;
  checkinEnd: string;
  status: string;
};

export type Tier = {
  id: Id;
  name: string;
  price: number;
  totalStock: number;
  perUserLimit: number;
  saleStart: string;
  saleEnd: string;
  status: string;
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
  quantity: number;
  amount: number;
  status: string;
  createdAt: string;
};

export type Envelope<T> = { code: string; message: string; data: T };

export type SessionForm = {
  venueId: Id | "";
  saleMode: "GENERAL" | "ASSIGNED";
  saleStart: string;
  saleEnd: string;
  startTime: string;
  endTime: string;
  checkinStart: string;
  checkinEnd: string;
};

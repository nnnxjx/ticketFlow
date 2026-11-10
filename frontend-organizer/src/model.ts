import React, { useEffect, useState } from "react";
import type {
  Id,
  Organizer,
  Show,
  Venue,
  VenueSeat,
  Session,
  Tier,
  Seat,
  Order,
  SessionForm,
} from "./types";
import {
  local,
  toUtc,
  toInput,
  emptySession,
} from "./format";
import { key, ApiError, api } from "./api";
export type {
  Id,
  Organizer,
  Show,
  Venue,
  VenueSeat,
  Session,
  Tier,
  Seat,
  Order,
  Envelope,
  SessionForm,
} from "./types";
export {
  money,
  local,
  toUtc,
  toInput,
  future,
  status,
  emptySession,
} from "./format";
export { key, ApiError, api } from "./api";

export function useOrganizerModel() {
  const [token, setToken] = useState(localStorage.getItem(key) || "");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [register, setRegister] = useState(false);
  const [section, setSection] = useState<
    "overview" | "shows" | "venues" | "orders" | "checkin"
  >("overview");
  const [organizer, setOrganizer] = useState<Organizer | null>(null);
  const [orgName, setOrgName] = useState("");
  const [shows, setShows] = useState<Show[]>([]);
  const [show, setShow] = useState<Show | null>(null);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");
  const [venues, setVenues] = useState<Venue[]>([]);
  const [venue, setVenue] = useState<Venue | null>(null);
  const [venueName, setVenueName] = useState("");
  const [address, setAddress] = useState("");
  const [venueSeats, setVenueSeats] = useState<VenueSeat[]>([]);
  const [area, setArea] = useState("A");
  const [rowNo, setRowNo] = useState("1");
  const [seatNo, setSeatNo] = useState("1");
  const [sessions, setSessions] = useState<Session[]>([]);
  const [session, setSession] = useState<Session | null>(null);
  const [sessionForm, setSessionForm] = useState<SessionForm>(emptySession());
  const [tiers, setTiers] = useState<Tier[]>([]);
  const [editingTier, setEditingTier] = useState<Tier | null>(null);
  const [tierName, setTierName] = useState("");
  const [tierPrice, setTierPrice] = useState("");
  const [tierStock, setTierStock] = useState("");
  const [tierLimit, setTierLimit] = useState("4");
  const [sessionSeats, setSessionSeats] = useState<Seat[]>([]);
  const [defaultPrice, setDefaultPrice] = useState("");
  const [orders, setOrders] = useState<Order[]>([]);
  const [orderPage, setOrderPage] = useState(1);
  const [orderTotal, setOrderTotal] = useState(0);
  const [orderEvent, setOrderEvent] = useState("");
  const [checkinToken, setCheckinToken] = useState("");
  const [checkinResult, setCheckinResult] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  useEffect(() => {
    if (!token) return;
    let active = true;
    const load = async () => {
      try {
        const o = await api<Organizer>("/organizers/me");
        if (!active) return;
        setOrganizer(o);
        const [s, v, p] = await Promise.all([
          api<Show[]>("/organizer/events"),
          api<Venue[]>("/organizer/venues"),
          api<{ orders: Order[]; total: string }>(
            "/organizer/orders?page=1&size=20",
          ),
        ]);
        if (active) {
          setShows(s);
          setVenues(v);
          setOrders(p.orders);
          setOrderTotal(Number(p.total));
        }
      } catch (e) {
        if (!active) return;
        if ((e as ApiError).code === "ORGANIZER_NOT_FOUND") setOrganizer(null);
        else setNotice((e as Error).message);
      }
    };
    void load();
    return () => {
      active = false;
    };
  }, [token]);
  async function run(task: () => Promise<void>) {
    setBusy(true);
    setNotice("");
    try {
      await task();
    } catch (e) {
      setNotice((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  async function auth(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      if (register) {
        await api("/auth/register", "POST", { phone, password });
        setRegister(false);
        setNotice("注册成功，请登录");
      } else {
        const x = await api<{ accessToken: string }>("/auth/login", "POST", {
          phone,
          password,
        });
        localStorage.setItem(key, x.accessToken);
        setToken(x.accessToken);
        setPassword("");
      }
    });
  }
  async function createOrganizer(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      setOrganizer(
        await api<Organizer>("/organizers", "POST", { name: orgName }),
      );
      setNotice("主办方已创建");
    });
  }
  async function reloadShows() {
    setShows(await api<Show[]>("/organizer/events"));
  }
  async function reloadVenues() {
    setVenues(await api<Venue[]>("/organizer/venues"));
  }
  async function selectShow(x: Show) {
    await run(async () => {
      setShow(x);
      setTitle(x.title);
      setCategory(x.category || "");
      setDescription(x.description || "");
      setSessions(await api<Session[]>(`/organizer/events/${x.id}/sessions`));
      setSession(null);
    });
  }
  async function eventSubmit(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      const body = { title, category, description };
      const x = await api<Show>(
        show ? `/organizer/events/${show.id}` : "/organizer/events",
        show ? "PUT" : "POST",
        body,
      );
      await reloadShows();
      await selectShow(x);
      setNotice(show ? "演出已更新" : "演出已创建");
    });
  }
  async function eventAction(action: "publish" | "off-shelf" | "delete") {
    if (!show) return;
    await run(async () => {
      if (action === "delete") {
        await api(`/organizer/events/${show.id}`, "DELETE");
        setShow(null);
      } else
        setShow(
          await api<Show>(`/organizer/events/${show.id}/${action}`, "POST"),
        );
      await reloadShows();
    });
  }
  async function venueSubmit(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      const x = await api<Venue>(
        venue ? `/organizer/venues/${venue.id}` : "/organizer/venues",
        venue ? "PUT" : "POST",
        { name: venueName, address },
      );
      setVenue(x);
      await reloadVenues();
      setNotice("场馆已保存");
    });
  }
  async function selectVenue(x: Venue) {
    await run(async () => {
      setVenue(x);
      setVenueName(x.name);
      setAddress(x.address || "");
      setVenueSeats(await api<VenueSeat[]>(`/organizer/venues/${x.id}/seats`));
    });
  }
  async function addVenueSeat(e: React.FormEvent) {
    e.preventDefault();
    if (!venue) return;
    await run(async () => {
      await api(`/organizer/venues/${venue.id}/seats`, "POST", {
        area,
        rowNo,
        seatNo,
      });
      setVenueSeats(
        await api<VenueSeat[]>(`/organizer/venues/${venue.id}/seats`),
      );
      setSeatNo(String(Number(seatNo) + 1 || 1));
    });
  }
  async function selectSession(x: Session) {
    await run(async () => {
      setSession(x);
      setEditingTier(null);
      setSessionForm({
        venueId: x.venueId,
        saleMode: x.saleMode,
        saleStart: toInput(x.saleStart),
        saleEnd: toInput(x.saleEnd),
        startTime: toInput(x.startTime),
        endTime: toInput(x.endTime),
        checkinStart: toInput(x.checkinStart),
        checkinEnd: toInput(x.checkinEnd),
      });
      if (x.saleMode === "GENERAL")
        setTiers(await api<Tier[]>(`/organizer/sessions/${x.id}/tiers`));
      else
        setSessionSeats(await api<Seat[]>(`/organizer/sessions/${x.id}/seats`));
    });
  }
  async function reloadSessions(eventId: Id) {
    setSessions(await api<Session[]>(`/organizer/events/${eventId}/sessions`));
  }
  async function saveSession(e: React.FormEvent) {
    e.preventDefault();
    if (!show || !sessionForm.venueId) return;
    await run(async () => {
      const body = {
        ...sessionForm,
        saleStart: toUtc(sessionForm.saleStart),
        saleEnd: toUtc(sessionForm.saleEnd),
        startTime: toUtc(sessionForm.startTime),
        endTime: toUtc(sessionForm.endTime),
        checkinStart: toUtc(sessionForm.checkinStart),
        checkinEnd: toUtc(sessionForm.checkinEnd),
      };
      const x = await api<Session>(
        session
          ? `/organizer/sessions/${session.id}`
          : `/organizer/events/${show.id}/sessions`,
        session ? "PUT" : "POST",
        body,
      );
      await reloadSessions(show.id);
      await selectSession(x);
      setNotice("场次已保存");
    });
  }
  async function sessionAction(action: "activate" | "stop" | "delete") {
    if (!session || !show) return;
    await run(async () => {
      if (action === "delete") {
        await api(`/organizer/sessions/${session.id}`, "DELETE");
        setSession(null);
      } else
        setSession(
          await api<Session>(
            `/organizer/sessions/${session.id}/${action}`,
            "POST",
          ),
        );
      await reloadSessions(show.id);
    });
  }
  function editTier(x: Tier) {
    if (x.status !== "DRAFT" || session?.status !== "DRAFT") return;
    setEditingTier(x);
    setTierName(x.name);
    setTierPrice(String(x.price));
    setTierStock(String(x.totalStock));
    setTierLimit(String(x.perUserLimit));
  }
  async function addTier(e: React.FormEvent) {
    e.preventDefault();
    if (!session) return;
    if (editingTier && (editingTier.status !== "DRAFT" || session.status !== "DRAFT")) return;
    await run(async () => {
      await api(
        editingTier
          ? `/organizer/tiers/${editingTier.id}`
          : `/organizer/sessions/${session.id}/tiers`,
        editingTier ? "PUT" : "POST",
        {
          name: tierName,
          price: Number(tierPrice),
          totalStock: Number(tierStock),
          perUserLimit: Number(tierLimit),
          saleStart: editingTier?.saleStart || session.saleStart,
          saleEnd: editingTier?.saleEnd || session.saleEnd,
        },
      );
      setTiers(await api<Tier[]>(`/organizer/sessions/${session.id}/tiers`));
      setEditingTier(null);
      setTierName("");
      setTierPrice("");
      setTierStock("");
    });
  }
  async function tierAction(id: Id, action: "activate" | "stop" | "delete") {
    if (!session) return;
    await run(async () => {
      await api(
        `/organizer/tiers/${id}${action === "delete" ? "" : `/${action}`}`,
        action === "delete" ? "DELETE" : "POST",
      );
      setTiers(await api<Tier[]>(`/organizer/sessions/${session.id}/tiers`));
    });
  }
  async function initSeats() {
    if (!session) return;
    await run(async () => {
      await api(`/organizer/sessions/${session.id}/seats/init`, "POST", {
        price: Number(defaultPrice),
      });
      setSessionSeats(
        await api<Seat[]>(`/organizer/sessions/${session.id}/seats`),
      );
    });
  }
  async function changeSeatPrice(x: Seat) {
    if (!session) return;
    const value = window.prompt(
      `${x.area}区 ${x.rowNo}排 ${x.seatNo}座的新价格`,
      String(x.price),
    );
    if (value === null) return;
    await run(async () => {
      await api(`/organizer/seats/${x.id}/price`, "PATCH", {
        price: Number(value),
      });
      setSessionSeats(
        await api<Seat[]>(`/organizer/sessions/${session.id}/seats`),
      );
    });
  }
  async function loadOrders(page = orderPage) {
    const query = new URLSearchParams({ page: String(page), size: "20" });
    if (orderEvent) query.set("eventId", orderEvent);
    const x = await api<{ orders: Order[]; total: string }>(
      `/organizer/orders?${query}`,
    );
    setOrders(x.orders);
    setOrderTotal(Number(x.total));
    setOrderPage(page);
  }
  async function checkin(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      const x = await api<{ ticketId: Id; checkedAt: string }>(
        "/organizer/checkins",
        "POST",
        { token: checkinToken.trim() },
      );
      setCheckinResult(`票券 #${x.ticketId} 检票成功 · ${local(x.checkedAt)}`);
      setCheckinToken("");
    });
  }

  return {
    token,
    setToken,
    phone,
    setPhone,
    password,
    setPassword,
    register,
    setRegister,
    section,
    setSection,
    organizer,
    setOrganizer,
    orgName,
    setOrgName,
    shows,
    setShows,
    show,
    setShow,
    title,
    setTitle,
    category,
    setCategory,
    description,
    setDescription,
    venues,
    setVenues,
    venue,
    setVenue,
    venueName,
    setVenueName,
    address,
    setAddress,
    venueSeats,
    setVenueSeats,
    area,
    setArea,
    rowNo,
    setRowNo,
    seatNo,
    setSeatNo,
    sessions,
    setSessions,
    session,
    setSession,
    sessionForm,
    setSessionForm,
    tiers,
    setTiers,
    editingTier,
    setEditingTier,
    tierName,
    setTierName,
    tierPrice,
    setTierPrice,
    tierStock,
    setTierStock,
    tierLimit,
    setTierLimit,
    sessionSeats,
    setSessionSeats,
    defaultPrice,
    setDefaultPrice,
    orders,
    setOrders,
    orderPage,
    setOrderPage,
    orderTotal,
    setOrderTotal,
    orderEvent,
    setOrderEvent,
    checkinToken,
    setCheckinToken,
    checkinResult,
    setCheckinResult,
    busy,
    setBusy,
    notice,
    setNotice,
    run,
    auth,
    createOrganizer,
    reloadShows,
    reloadVenues,
    selectShow,
    eventSubmit,
    eventAction,
    venueSubmit,
    selectVenue,
    addVenueSeat,
    selectSession,
    reloadSessions,
    saveSession,
    sessionAction,
    editTier,
    addTier,
    tierAction,
    initSeats,
    changeSeatPrice,
    loadOrders,
    checkin,
  };
}

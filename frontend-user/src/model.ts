import React, { useEffect, useRef, useState } from "react";
import type {
  Id,
  Show,
  Session,
  Tier,
  Seat,
  Order,
  Ticket,
  AsyncResult,
} from "./types";
import { labels } from "./format";
import { tokenKey, api } from "./api";
import { navigate } from "./router";
export type {
  Id,
  Show,
  Session,
  Tier,
  Seat,
  Order,
  Ticket,
  AsyncResult,
  Envelope,
} from "./types";
export { price, when, labels } from "./format";
export { tokenKey, api } from "./api";

export function useUserModel() {
  const [tab, setTabState] = useState<"discover" | "orders">("discover");
  function setTab(value: "discover" | "orders") {
    setTabState(value);
    navigate(value === "orders" ? "/orders" : "/");
  }
  const [token, setToken] = useState(localStorage.getItem(tokenKey) || "");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [registering, setRegistering] = useState(false);
  const [shows, setShows] = useState<Show[]>([]);
  const [show, setShow] = useState<Show | null>(null);
  const [sessions, setSessions] = useState<Session[]>([]);
  const sessionCache = useRef(new Map<Id, Session[]>());
  const [session, setSession] = useState<Session | null>(null);
  const [tiers, setTiers] = useState<Tier[]>([]);
  const [tierId, setTierId] = useState<Id | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [seats, setSeats] = useState<Seat[]>([]);
  const [chosen, setChosen] = useState<Id[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [order, setOrder] = useState<Order | null>(null);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [pending, setPending] = useState<AsyncResult | null>(null);
  const [pendingTier, setPendingTier] = useState<{
    id: Id;
    quantity: number;
  } | null>(null);
  const [assignedAttempt, setAssignedAttempt] = useState<{
    requestId: string;
    sessionId: Id;
    seatIds: Id[];
  } | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    let active = true;
    api<Show[]>("/events")
      .then((x) => {
        if (active) setShows(x);
      })
      .catch((e) => {
        if (active) setNotice(e.message);
      });
    return () => {
      active = false;
    };
  }, []);
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, []);
  useEffect(() => {
    if (!session || session.saleMode !== "ASSIGNED") return;
    let active = true;
    const stream = new EventSource(
      `/api/v1/sessions/${session.id}/seats/stream`,
    );
    const refresh = async () => {
      try {
        const latest = await api<Seat[]>(`/sessions/${session.id}/seats`);
        if (!active) return;
        // 断线重连会重新拉快照；版本号防止较慢的旧请求覆盖新状态。
        setSeats((previous) =>
          latest.map((item) => {
            const old = previous.find((seat) => seat.id === item.id);
            return old && BigInt(old.version) > BigInt(item.version)
              ? old
              : item;
          }),
        );
        setChosen((previous) =>
          previous.filter((id) =>
            latest.some((seat) => seat.id === id && seat.status === "FREE"),
          ),
        );
      } catch (e) {
        if (active) setNotice((e as Error).message);
      }
    };
    stream.onopen = () => {
      void refresh();
    };
    stream.onmessage = () => {
      void refresh();
    };
    return () => {
      active = false;
      stream.close();
    };
  }, [session?.id, session?.saleMode]);
  useEffect(() => {
    if (pending?.status !== "PROCESSING") return;
    let active = true;
    const check = async () => {
      try {
        const x = await api<AsyncResult>(
          `/orders/general/async/${encodeURIComponent(pending.requestId)}`,
        );
        if (!active) return;
        setPending(x);
        if (x.status === "ORDERED" && x.orderId) {
          await openOrder(x.orderId);
          await listOrders(1);
          setTab("orders");
          setNotice("订单已创建，请及时支付");
        } else if (x.status === "REJECTED")
          setNotice(labels[x.failureCode || ""] || x.failureCode || "下单失败");
      } catch (e) {
        if (active) setNotice((e as Error).message);
      }
    };
    void check();
    const id = window.setInterval(check, 2500);
    return () => {
      active = false;
      window.clearInterval(id);
    };
  }, [pending?.requestId, pending?.status]);
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
      if (registering) {
        await api("/auth/register", "POST", { phone, password });
        setRegistering(false);
        setNotice("注册成功，请登录");
      } else {
        const x = await api<{ accessToken: string }>("/auth/login", "POST", {
          phone,
          password,
        });
        localStorage.setItem(tokenKey, x.accessToken);
        setToken(x.accessToken);
        setPassword("");
        setNotice("登录成功");
      }
    });
  }
  async function selectShow(x: Show) {
    await run(async () => {
      let next = sessionCache.current.get(x.id);
      if (!next) {
        next = await api<Session[]>(`/events/${x.id}/sessions`);
        sessionCache.current.set(x.id, next);
      }
      setSessions(next);
      setShow(x);
      setSession(null);
    });
  }
  async function selectSession(x: Session) {
    await run(async () => {
      if (x.saleMode === "GENERAL") {
        const list = await api<Tier[]>(`/sessions/${x.id}/tiers`);
        setTiers(list);
        setTierId(list[0]?.id || null);
      } else {
        setSeats(await api<Seat[]>(`/sessions/${x.id}/seats`));
        setChosen([]);
      }
      setSession(x);
    });
  }
  async function refreshSeats() {
    if (session)
      await run(async () => {
        setSeats(await api<Seat[]>(`/sessions/${session.id}/seats`));
        setChosen([]);
      });
  }
  async function listOrders(n = page) {
    const x = await api<{ total: string; orders: Order[] }>(
      `/orders?page=${n}&size=10`,
    );
    setOrders(x.orders);
    setTotal(Number(x.total));
    setPage(n);
  }
  async function openOrder(id: Id) {
    const x = await api<Order>(`/orders/${id}`);
    setOrder(x);
    setTickets(
      x.status === "TICKETED"
        ? await api<Ticket[]>(`/orders/${id}/tickets`)
        : [],
    );
  }
  // 异步建单期间保留 requestId，用户重发时仍查询同一次 Redis 预占。
  async function buyGeneral(retry = false) {
    if (!token) {
      setNotice("请先登录");
      return;
    }
    const id = retry ? pendingTier?.id : tierId;
    const count = retry ? pendingTier?.quantity : quantity;
    if (!id || !count) return;
    await run(async () => {
      const x = await api<AsyncResult>("/orders/general", "POST", {
        requestId: retry && pending ? pending.requestId : crypto.randomUUID(),
        tierId: id,
        quantity: count,
      });
      setPending(x);
      setPendingTier({ id, quantity: count });
      if (x.status === "ORDERED" && x.orderId) {
        await openOrder(x.orderId);
        setTab("orders");
      } else
        setNotice(
          x.status === "REJECTED"
            ? labels[x.failureCode || ""] || x.failureCode || "下单失败"
            : "正在创建订单，请稍候",
        );
    });
  }
  // 网络失败后同一组选座沿用 requestId，避免后端已锁座而前端又生成一笔新订单。
  async function buyAssigned() {
    if (!token) {
      setNotice("请先登录");
      return;
    }
    if (!session || !chosen.length) return;
    const seatIds = [...chosen].sort();
    const sameAttempt =
      assignedAttempt?.sessionId === session.id &&
      assignedAttempt.seatIds.join(",") === seatIds.join(",");
    const requestId = sameAttempt
      ? assignedAttempt.requestId
      : crypto.randomUUID();
    setAssignedAttempt({ requestId, sessionId: session.id, seatIds });
    await run(async () => {
      const x = await api<Order>("/orders/assigned", "POST", {
        requestId,
        sessionId: session.id,
        seatIds,
      });
      setAssignedAttempt(null);
      setOrder(x);
      setTab("orders");
      setChosen([]);
      await listOrders(1);
      setNotice("座位锁定成功，请及时支付");
    });
  }
  async function act(action: "pay" | "cancel" | "refund") {
    if (!order) return;
    await run(async () => {
      await api(`/orders/${order.id}/${action}`, "POST");
      await openOrder(order.id);
      await listOrders();
      setNotice(
        action === "pay"
          ? "模拟支付成功，电子票正在生成"
          : action === "cancel"
            ? "订单已取消"
            : "退款申请已提交",
      );
    });
  }
  const currentTier = tiers.find((x) => x.id === tierId);
  const chosenPrice = seats
    .filter((x) => chosen.includes(x.id))
    .reduce((sum, x) => sum + Number(x.price), 0);
  const remaining =
    order?.status === "PENDING_PAYMENT"
      ? Math.max(
          0,
          Math.ceil((new Date(`${order.expireAt}Z`).getTime() - now) / 1000),
        )
      : 0;

  return {
    tab,
    setTab,
    token,
    setToken,
    phone,
    setPhone,
    password,
    setPassword,
    registering,
    setRegistering,
    shows,
    setShows,
    show,
    setShow,
    sessions,
    setSessions,
    session,
    setSession,
    tiers,
    setTiers,
    tierId,
    setTierId,
    quantity,
    setQuantity,
    seats,
    setSeats,
    chosen,
    setChosen,
    orders,
    setOrders,
    page,
    setPage,
    total,
    setTotal,
    order,
    setOrder,
    tickets,
    setTickets,
    pending,
    setPending,
    pendingTier,
    setPendingTier,
    assignedAttempt,
    setAssignedAttempt,
    busy,
    setBusy,
    notice,
    setNotice,
    now,
    setNow,
    run,
    auth,
    selectShow,
    selectSession,
    refreshSeats,
    listOrders,
    openOrder,
    buyGeneral,
    buyAssigned,
    act,
    currentTier,
    chosenPrice,
    remaining,
  };
}

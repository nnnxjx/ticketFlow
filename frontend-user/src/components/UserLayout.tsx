import { useUserContext } from "../context";
import { tokenKey } from "../model";
import { AuthPanel } from "./AuthPanel";
import { DiscoverPage } from "./DiscoverPage";
import { OrdersPage } from "./OrdersPage";
import { Toast } from "./Toast";
import { navigate, usePath } from "../router";
import { useEffect } from "react";

export function UserLayout() {
  const {
    setTab,
    token,
    setToken,
    notice,
    setNotice,
    setOrder,
    setOrders,
    run,
    listOrders,
  } = useUserContext();
  const path = usePath();
  const eventId = /^\/events\/([^/]+)$/.exec(path)?.[1];
  const ordersRoute = path === "/orders";

  useEffect(() => {
    if (ordersRoute && token) void run(() => listOrders(1));
  }, [ordersRoute, token]);

  function logout() {
    localStorage.removeItem(tokenKey);
    setToken("");
    setOrder(null);
    setOrders([]);
  }

  return (
    <div className="shell">
      <header>
        <div className="brand">
          <span>T</span> TicketFlow <small>发现下一场精彩</small>
        </div>
        <nav>
          <button
            className={!ordersRoute ? "selected" : ""}
            onClick={() => setTab("discover")}
          >
            发现演出
          </button>
          <button
            className={ordersRoute ? "selected" : ""}
            onClick={() => setTab("orders")}
          >
            我的订单
          </button>
        </nav>
        {token && (
          <button className="link" onClick={logout}>
            退出登录
          </button>
        )}
      </header>
      <Toast message={notice} dismiss={() => setNotice("")} />
      <main>
        {!token && <AuthPanel />}
        {ordersRoute ? <OrdersPage /> : <DiscoverPage eventId={eventId} onOpen={(id) => navigate(`/events/${id}`)} onBack={() => navigate("/")} />}
      </main>
      <footer>TicketFlow · 本地模拟购票平台</footer>
    </div>
  );
}

import { useUserContext } from "../context";
import { tokenKey } from "../model";
import { AuthPanel } from "./AuthPanel";
import { DiscoverPage } from "./DiscoverPage";
import { OrdersPage } from "./OrdersPage";
import { Toast } from "./Toast";
import { Link, Route, Routes, useLocation } from "react-router";
import { useEffect } from "react";

export function UserLayout() {
  const {
    token,
    setToken,
    notice,
    setNotice,
    setOrder,
    setOrders,
    run,
    listOrders,
  } = useUserContext();
  const location = useLocation();
  const ordersRoute = location.pathname === "/orders";

  useEffect(() => { window.scrollTo(0, 0); }, [location.pathname]);

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
          <Link
            className={!ordersRoute ? "selected" : ""}
            to="/"
          >
            发现演出
          </Link>
          <Link
            className={ordersRoute ? "selected" : ""}
            to="/orders"
          >
            我的订单
          </Link>
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
        <Routes>
          <Route path="/" element={<DiscoverPage />} />
          <Route path="/events/:eventId" element={<DiscoverPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="*" element={<div className="empty">页面不存在。<Link to="/">返回演出列表</Link></div>} />
        </Routes>
      </main>
      <footer>TicketFlow · 本地模拟购票平台</footer>
    </div>
  );
}

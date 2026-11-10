import { useUserContext } from "../context";
import { price, when, labels, api } from "../model";
import { QrTicket } from "./QrTicket";
import type { Ticket } from "../model";

export function OrdersPage() {
  const {
    token,
    orders,
    page,
    total,
    order,
    tickets,
    setTickets,
    busy,
    setNotice,
    run,
    listOrders,
    openOrder,
    act,
    remaining,
  } = useUserContext();
  return (
    <section className="section">
      <div className="section-title">
        <div>
          <span className="eyebrow">YOUR ORDERS</span>
          <h2>我的订单</h2>
        </div>
        <button
          className="secondary"
          onClick={() =>
            token ? void run(() => listOrders(1)) : setNotice("请先登录")
          }
        >
          刷新
        </button>
      </div>
      {!token ? (
        <div className="empty">登录后查看订单。</div>
      ) : (
        <div className="order-layout">
          <div>
            <div className="order-list">
              {orders.map((x) => (
                <button
                  key={x.id}
                  className={`row ${order?.id === x.id ? "chosen" : ""}`}
                  onClick={() => void run(() => openOrder(x.id))}
                >
                  <span>
                    <b>订单 #{x.id}</b>
                    <small>
                      {when(x.createdAt)} · {x.quantity} 张
                    </small>
                  </span>
                  <span>
                    <b>{price(x.amount)}</b>
                    <small>{labels[x.status] || x.status}</small>
                  </span>
                </button>
              ))}
              {!orders.length && (
                <div className="empty">还没有订单，去挑一场演出吧。</div>
              )}
            </div>
            <div className="pager">
              <button
                disabled={page <= 1}
                onClick={() => void run(() => listOrders(page - 1))}
              >
                上一页
              </button>
              {page} / {Math.max(1, Math.ceil(total / 10))}
              <button
                disabled={page * 10 >= total}
                onClick={() => void run(() => listOrders(page + 1))}
              >
                下一页
              </button>
            </div>
          </div>
          <div className="order-detail">
            {!order ? (
              <div className="empty">选择一笔订单查看详情。</div>
            ) : (
              <>
                <span className="eyebrow">ORDER #{order.id}</span>
                <h3>{labels[order.status] || order.status}</h3>
                <p>
                  场次 #{order.sessionId} · {order.quantity} 张
                </p>
                {order.status === "PENDING_PAYMENT" && (
                  <div className="countdown">
                    支付剩余{" "}
                    <strong>
                      {String(Math.floor(remaining / 60)).padStart(2, "0")}:
                      {String(remaining % 60).padStart(2, "0")}
                    </strong>
                  </div>
                )}
                <div className="items">
                  {order.items.map((x) => (
                    <div key={x.id}>
                      <span>{x.name}</span>
                      <b>{price(x.price)}</b>
                    </div>
                  ))}
                </div>
                <div className="total">
                  合计 <strong>{price(order.amount)}</strong>
                </div>
                <div className="actions">
                  {order.status === "PENDING_PAYMENT" && (
                    <>
                      <button
                        className="primary"
                        disabled={busy || remaining === 0}
                        onClick={() => void act("pay")}
                      >
                        模拟支付
                      </button>
                      <button
                        className="secondary"
                        disabled={busy}
                        onClick={() => void act("cancel")}
                      >
                        取消订单
                      </button>
                    </>
                  )}
                  {["PAID", "TICKETED"].includes(order.status) && (
                    <button
                      className="secondary"
                      disabled={busy}
                      onClick={() => void act("refund")}
                    >
                      申请整单退款
                    </button>
                  )}
                  {order.status === "TICKETED" && (
                    <button
                      className="secondary"
                      onClick={() =>
                        void run(async () =>
                          setTickets(
                            await api<Ticket[]>(`/orders/${order.id}/tickets`),
                          ),
                        )
                      }
                    >
                      刷新电子票
                    </button>
                  )}
                </div>
                {tickets.map((x) => (
                  <QrTicket key={x.id} ticket={x} />
                ))}
              </>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

import { useOrganizerContext } from "../context";
import { money, local, status } from "../model";

export function OrganizerOrdersPanel() {
  const {
    shows,
    orders,
    orderPage,
    orderTotal,
    orderEvent,
    setOrderEvent,
    run,
    loadOrders,
  } = useOrganizerContext();
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>订单查询</h2>
        <div className="toolbar">
          <select
            value={orderEvent}
            onChange={(e) => setOrderEvent(e.target.value)}
          >
            <option value="">全部演出</option>
            {shows.map((x) => (
              <option key={x.id} value={x.id}>
                {x.title}
              </option>
            ))}
          </select>
          <button onClick={() => void run(() => loadOrders(1))}>查询</button>
        </div>
      </div>
      <p className="muted">
        共 {orderTotal} 笔订单。列表仅显示当前主办方名下的场次。
      </p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>订单</th>
              <th>场次</th>
              <th>数量</th>
              <th>金额</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((x) => (
              <tr key={x.id}>
                <td>#{x.id}</td>
                <td>#{x.sessionId}</td>
                <td>{x.quantity}</td>
                <td>{money(x.amount)}</td>
                <td>{status[x.status] || x.status}</td>
                <td>{local(x.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!orders.length && <div className="empty">暂无订单</div>}
      <div className="pager">
        <button
          disabled={orderPage <= 1}
          onClick={() => void run(() => loadOrders(orderPage - 1))}
        >
          上一页
        </button>
        {orderPage} / {Math.max(1, Math.ceil(orderTotal / 20))}
        <button
          disabled={orderPage * 20 >= orderTotal}
          onClick={() => void run(() => loadOrders(orderPage + 1))}
        >
          下一页
        </button>
      </div>
    </section>
  );
}

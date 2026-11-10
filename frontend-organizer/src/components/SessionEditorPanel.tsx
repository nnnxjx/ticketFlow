import { useOrganizerContext } from "../context";
import { money, local, status, emptySession } from "../model";

export function SessionEditorPanel() {
  const {
    show,
    venues,
    sessions,
    session,
    setSession,
    sessionForm,
    setSessionForm,
    tiers,
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
    defaultPrice,
    setDefaultPrice,
    busy,
    selectSession,
    saveSession,
    sessionAction,
    editTier,
    addTier,
    tierAction,
    initSeats,
    changeSeatPrice,
  } = useOrganizerContext();
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>场次与商品</h2>
        {show && (
          <button
            onClick={() => {
              setSession(null);
              setSessionForm(emptySession());
            }}
          >
            ＋ 新建场次
          </button>
        )}
      </div>
      {!show ? (
        <div className="empty">先选择演出。</div>
      ) : (
        <>
          {sessions.map((x) => (
            <button
              key={x.id}
              className={`list-row ${session?.id === x.id ? "active" : ""}`}
              onClick={() => void selectSession(x)}
            >
              <strong>
                {local(x.startTime)} ·{" "}
                {x.saleMode === "GENERAL" ? "票档" : "选座"}
              </strong>
              <small>{status[x.status] || x.status}</small>
            </button>
          ))}
          <form className="form" onSubmit={saveSession}>
            <h3>{session ? "编辑场次" : "新建场次"}</h3>
            <div className="form-grid">
              <label>
                场馆
                <select
                  value={sessionForm.venueId}
                  onChange={(e) =>
                    setSessionForm({
                      ...sessionForm,
                      venueId: e.target.value,
                    })
                  }
                  required
                >
                  <option value="">选择场馆</option>
                  {venues.map((x) => (
                    <option key={x.id} value={x.id}>
                      {x.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                售票方式
                <select
                  value={sessionForm.saleMode}
                  onChange={(e) =>
                    setSessionForm({
                      ...sessionForm,
                      saleMode: e.target.value as "GENERAL" | "ASSIGNED",
                    })
                  }
                >
                  <option value="GENERAL">按票档</option>
                  <option value="ASSIGNED">指定座位</option>
                </select>
              </label>
              {(
                [
                  "saleStart",
                  "saleEnd",
                  "startTime",
                  "endTime",
                  "checkinStart",
                  "checkinEnd",
                ] as const
              ).map((field, index) => (
                <label key={field}>
                  {
                    [
                      "开售",
                      "停售",
                      "演出开始",
                      "演出结束",
                      "检票开始",
                      "检票结束",
                    ][index]
                  }
                  <input
                    type="datetime-local"
                    value={sessionForm[field]}
                    onChange={(e) =>
                      setSessionForm({
                        ...sessionForm,
                        [field]: e.target.value,
                      })
                    }
                    required
                  />
                </label>
              ))}
            </div>
            <button className="primary" disabled={busy}>
              保存场次
            </button>
          </form>
          {session && (
            <>
              <div className="button-row">
                <button
                  onClick={() => void sessionAction("activate")}
                  disabled={busy}
                >
                  启用场次
                </button>
                <button
                  onClick={() => void sessionAction("stop")}
                  disabled={busy}
                >
                  停售
                </button>
                <button
                  onClick={() => void sessionAction("delete")}
                  disabled={busy}
                >
                  删除草稿
                </button>
              </div>
              {session.saleMode === "GENERAL" ? (
                <div className="subpanel">
                  <h3>票档配置</h3>
                  {tiers.map((x) => (
                    <div key={x.id} className="data-row">
                      <span>
                        <b>{x.name}</b>
                        <small>
                          {money(x.price)} · 总量 {x.totalStock} · 限购{" "}
                          {x.perUserLimit} · {status[x.status] || x.status}
                        </small>
                      </span>
                      <span>
                        {x.status === "DRAFT" && session.status === "DRAFT" && (
                          <button disabled={busy} onClick={() => editTier(x)}>编辑</button>
                        )}
                        {(x.status === "DRAFT" && session.status === "DRAFT" || x.status === "STOPPED") && (
                          <button disabled={busy} onClick={() => void tierAction(x.id, "activate")}>启用</button>
                        )}
                        {x.status === "ON_SALE" && (
                          <button disabled={busy} onClick={() => void tierAction(x.id, "stop")}>停售</button>
                        )}
                        {x.status === "DRAFT" && session.status === "DRAFT" && (
                          <button disabled={busy} onClick={() => void tierAction(x.id, "delete")}>删除</button>
                        )}
                      </span>
                    </div>
                  ))}
                  <form className="inline-form" onSubmit={addTier}>
                    <input
                      value={tierName}
                      onChange={(e) => setTierName(e.target.value)}
                      placeholder="票档名称"
                      required
                    />
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={tierPrice}
                      onChange={(e) => setTierPrice(e.target.value)}
                      placeholder="价格"
                      required
                    />
                    <input
                      type="number"
                      min="0"
                      value={tierStock}
                      onChange={(e) => setTierStock(e.target.value)}
                      placeholder="总量"
                      required
                    />
                    <input
                      type="number"
                      min="1"
                      value={tierLimit}
                      onChange={(e) => setTierLimit(e.target.value)}
                      placeholder="限购"
                      required
                    />
                    <button className="primary" disabled={busy}>
                      {editingTier ? "保存票档" : "添加票档"}
                    </button>
                    {editingTier && (
                      <button
                        type="button"
                        onClick={() => {
                          setEditingTier(null);
                          setTierName("");
                          setTierPrice("");
                          setTierStock("");
                        }}
                      >
                        取消编辑
                      </button>
                    )}
                  </form>
                </div>
              ) : (
                <div className="subpanel">
                  <h3>场次座位</h3>
                  <div className="inline-form">
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={defaultPrice}
                      onChange={(e) => setDefaultPrice(e.target.value)}
                      placeholder="默认价格"
                    />
                    <button
                      className="primary"
                      disabled={busy || !defaultPrice}
                      onClick={() => void initSeats()}
                    >
                      按场馆模板初始化
                    </button>
                  </div>
                  <div className="chip-list">
                    {sessionSeats.map((x) => (
                      <button
                        key={x.id}
                        onClick={() => void changeSeatPrice(x)}
                      >
                        {x.area}区 {x.rowNo}排{x.seatNo}座 · {money(x.price)} ·{" "}
                        {x.status}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}
    </section>
  );
}

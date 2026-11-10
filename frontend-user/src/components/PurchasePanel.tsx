import { useUserContext } from "../context";
import { price, when } from "../model";

export function PurchasePanel() {
  const {
    session,
    tiers,
    tierId,
    setTierId,
    quantity,
    setQuantity,
    seats,
    chosen,
    setChosen,
    pending,
    busy,
    refreshSeats,
    buyGeneral,
    buyAssigned,
    currentTier,
    chosenPrice,
  } = useUserContext();
  if (!session) return null;
  return (
    <div className="purchase">
      <div className="section-title">
        <div>
          <span className="eyebrow">CHOOSE YOUR PLACE</span>
          <h3>{session.saleMode === "GENERAL" ? "选择票档" : "选择座位"}</h3>
        </div>
        <span>开售 {when(session.saleStart)}</span>
      </div>
      {session.saleMode === "GENERAL" ? (
        <>
          <div className="tier-list">
            {tiers.map((x) => (
              <button
                key={x.id}
                className={`row ${tierId === x.id ? "chosen" : ""}`}
                onClick={() => setTierId(x.id)}
              >
                <span>
                  <b>{x.name}</b>
                  <small>
                    剩余 {x.availableStock} · 每人限购 {x.perUserLimit}
                  </small>
                </span>
                <strong>{price(x.price)}</strong>
              </button>
            ))}
          </div>
          <div className="buy">
            <label>
              数量{" "}
              <input
                type="number"
                min={1}
                value={quantity}
                onChange={(e) =>
                  setQuantity(Math.max(1, Number(e.target.value)))
                }
              />
            </label>
            <strong>{price(Number(currentTier?.price || 0) * quantity)}</strong>
            <button
              className="primary"
              disabled={busy || !tierId || pending?.status === "PROCESSING"}
              onClick={() => void buyGeneral()}
            >
              立即抢票
            </button>
          </div>
          {pending?.status === "PROCESSING" && (
            <div className="pending">
              正在创建订单 · {pending.requestId}
              <button onClick={() => void buyGeneral(true)}>
                重新发送同一请求
              </button>
            </div>
          )}
        </>
      ) : (
        <>
          <div className="legend">
            ● 可选　 <span>● 已选</span>　 ● 已售 / 锁定{" "}
            <button onClick={() => void refreshSeats()}>刷新座位</button>
          </div>
          <div className="seat-map">
            <div className="stage">舞台 · STAGE</div>
            {[...new Set(seats.map((x) => x.area))].map((area) => (
              <div className="area" key={area}>
                <b>{area} 区</b>
                <div>
                  {seats
                    .filter((x) => x.area === area)
                    .map((x) => (
                      <button
                        key={x.id}
                        className={`seat ${chosen.includes(x.id) ? "picked" : ""}`}
                        disabled={x.status !== "FREE"}
                        title={`${x.rowNo}排${x.seatNo}座 ${price(x.price)}`}
                        onClick={() =>
                          setChosen((old) =>
                            old.includes(x.id)
                              ? old.filter((id) => id !== x.id)
                              : old.length < 8
                                ? [...old, x.id]
                                : old,
                          )
                        }
                      >
                        {x.rowNo}-{x.seatNo}
                      </button>
                    ))}
                </div>
              </div>
            ))}
          </div>
          <div className="buy">
            <span>已选 {chosen.length} / 8 座</span>
            <strong>{price(chosenPrice)}</strong>
            <button
              className="primary"
              disabled={busy || !chosen.length}
              onClick={() => void buyAssigned()}
            >
              确认选座
            </button>
          </div>
        </>
      )}
    </div>
  );
}

import { useOrganizerContext } from "../context";

export function VenuesPanel() {
  const {
    venues,
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
    busy,
    venueSubmit,
    selectVenue,
    addVenueSeat,
  } = useOrganizerContext();
  return (
    <div className="two-col">
      <section className="panel">
        <div className="panel-head">
          <h2>场馆列表</h2>
          <button
            onClick={() => {
              setVenue(null);
              setVenueName("");
              setAddress("");
              setVenueSeats([]);
            }}
          >
            ＋ 新建
          </button>
        </div>
        {venues.map((x) => (
          <button
            key={x.id}
            className={`list-row ${venue?.id === x.id ? "active" : ""}`}
            onClick={() => void selectVenue(x)}
          >
            <strong>{x.name}</strong>
            <small>{x.address || "未填写地址"}</small>
          </button>
        ))}
        <form className="form" onSubmit={venueSubmit}>
          <h3>{venue ? "编辑场馆" : "新建场馆"}</h3>
          <label>
            名称
            <input
              value={venueName}
              onChange={(e) => setVenueName(e.target.value)}
              required
            />
          </label>
          <label>
            地址
            <input
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </label>
          <button className="primary" disabled={busy}>
            保存场馆
          </button>
        </form>
      </section>
      <section className="panel">
        <h2>物理座位模板</h2>
        {!venue ? (
          <div className="empty">先选择或创建场馆。</div>
        ) : (
          <>
            <p className="muted">
              {venue.name} · {venueSeats.length} 个座位
            </p>
            <form className="inline-form" onSubmit={addVenueSeat}>
              <input
                value={area}
                onChange={(e) => setArea(e.target.value)}
                placeholder="区域"
                required
              />
              <input
                value={rowNo}
                onChange={(e) => setRowNo(e.target.value)}
                placeholder="排号"
                required
              />
              <input
                value={seatNo}
                onChange={(e) => setSeatNo(e.target.value)}
                placeholder="座号"
                required
              />
              <button className="primary" disabled={busy}>
                添加座位
              </button>
            </form>
            <div className="chip-list">
              {venueSeats.map((x) => (
                <span key={x.id}>
                  {x.area}区 · {x.rowNo}排{x.seatNo}座
                </span>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  );
}

import { useOrganizerContext } from "../context";

export function CheckinPanel() {
  const { checkinToken, setCheckinToken, checkinResult, busy, checkin } =
    useOrganizerContext();
  return (
    <section className="panel checkin">
      <span className="eyebrow">AT THE DOOR</span>
      <h2>现场检票</h2>
      <p>
        扫描器输入或粘贴电子票令牌后确认。只有本主办方演出且在检票时间内的未使用票能通过。
      </p>
      <form onSubmit={checkin}>
        <label>
          电子票令牌
          <input
            autoFocus
            value={checkinToken}
            onChange={(e) => setCheckinToken(e.target.value)}
            placeholder="扫描或粘贴令牌"
            required
          />
        </label>
        <button className="primary" disabled={busy}>
          确认检票
        </button>
      </form>
      {checkinResult && <div className="success">✓ {checkinResult}</div>}
    </section>
  );
}

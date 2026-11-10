import { useOrganizerContext } from "../context";

export function OverviewPanel() {
  const { setSection, shows, venues, orderTotal } = useOrganizerContext();
  return (
    <>
      <div className="metrics">
        <div>
          <span>演出总数</span>
          <strong>{shows.length}</strong>
          <small>含草稿与已发布</small>
        </div>
        <div>
          <span>场馆总数</span>
          <strong>{venues.length}</strong>
          <small>可用于场次配置</small>
        </div>
        <div>
          <span>订单数量</span>
          <strong>{orderTotal || "—"}</strong>
          <small>打开订单页刷新统计</small>
        </div>
      </div>
      <section className="panel">
        <span className="eyebrow">QUICK START</span>
        <h2>从场馆到开场</h2>
        <div className="steps">
          <div>
            01 <strong>建立场馆与物理座位</strong>
          </div>
          <div>
            02 <strong>创建演出和场次</strong>
          </div>
          <div>
            03 <strong>配置票档或场次座位</strong>
          </div>
          <div>
            04 <strong>启用场次并发布演出</strong>
          </div>
        </div>
        <button className="primary" onClick={() => setSection("shows")}>
          开始管理演出 →
        </button>
      </section>
    </>
  );
}

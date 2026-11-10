import { useOrganizerContext } from "../context";
import { key } from "../model";
import { LoginPage } from "./LoginPage";
import { OnboardingPanel } from "./OnboardingPanel";
import { OverviewPanel } from "./OverviewPanel";
import { VenuesPanel } from "./VenuesPanel";
import { ShowsPanel } from "./ShowsPanel";
import { OrganizerOrdersPanel } from "./OrganizerOrdersPanel";
import { CheckinPanel } from "./CheckinPanel";
import { Toast } from "./Toast";

const sections = [
  ["overview", "总览", "◫"],
  ["shows", "演出与场次", "▣"],
  ["venues", "场馆与座位", "⌑"],
  ["orders", "订单查询", "▤"],
  ["checkin", "现场检票", "◇"],
] as const;

const titles = {
  overview: "工作台总览",
  shows: "演出与场次",
  venues: "场馆与座位",
  orders: "订单查询",
  checkin: "现场检票",
};

export function OrganizerLayout() {
  const {
    token,
    setToken,
    section,
    setSection,
    organizer,
    setOrganizer,
    notice,
    setNotice,
    run,
    loadOrders,
  } = useOrganizerContext();
  if (!token) return <LoginPage />;

  function logout() {
    localStorage.removeItem(key);
    setToken("");
    setOrganizer(null);
  }

  return (
    <div className="dashboard">
      <aside>
        <div className="side-brand">
          <span>T</span>
          <div>
            TicketFlow<small>ORGANIZER STUDIO</small>
          </div>
        </div>
        <nav>
          {sections.map(([id, label, icon]) => (
            <button
              key={id}
              className={section === id ? "active" : ""}
              onClick={() => {
                setSection(id);
                if (id === "orders") void run(() => loadOrders(1));
              }}
            >
              {icon}
              <span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="side-footer">
          <strong>{organizer?.name || "尚未创建主办方"}</strong>
          <button onClick={logout}>退出登录</button>
        </div>
      </aside>
      <div className="workspace">
        <header>
          <div>
            <span className="eyebrow">TICKETFLOW / ORGANIZER</span>
            <h1>{titles[section]}</h1>
          </div>
          <span className="header-pill">● 本地演示环境</span>
        </header>
        <Toast message={notice} dismiss={() => setNotice("")} />
        <main>
          {!organizer ? (
            <OnboardingPanel />
          ) : (
            <>
              {section === "overview" && <OverviewPanel />}
              {section === "venues" && <VenuesPanel />}
              {section === "shows" && <ShowsPanel />}
              {section === "orders" && <OrganizerOrdersPanel />}
              {section === "checkin" && <CheckinPanel />}
            </>
          )}
        </main>
      </div>
    </div>
  );
}

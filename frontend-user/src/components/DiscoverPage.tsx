import { PurchasePanel } from "./PurchasePanel";
import { useUserContext } from "../context";
import { when } from "../model";
import { api } from "../api";
import { useEffect, useState } from "react";
import type { Show } from "../types";
import { Link, useParams } from "react-router";

export function DiscoverPage() {
  const { eventId } = useParams();
  const {
    shows,
    show,
    sessions,
    session,
    selectShow,
    selectSession,
    setNotice,
  } = useUserContext();
  const [loadError, setLoadError] = useState("");
  useEffect(() => {
    if (!eventId || show?.id === eventId) return;
    let active = true;
    setLoadError("");
    const cached = shows.find((x) => x.id === eventId);
    if (cached) void selectShow(cached);
    else void api<Show>(`/events/${encodeURIComponent(eventId)}`)
      .then((x) => { if (active) return selectShow(x); })
      .catch((e) => { if (active) { setLoadError((e as Error).message); setNotice((e as Error).message); } });
    return () => { active = false; };
  }, [eventId]);
  return (
    <>
      {!eventId && <>
      <section className="hero">
        <span className="eyebrow">LIVE MOMENTS</span>
        <h1>
          好戏即将
          <br />
          <em>开场。</em>
        </h1>
        <p>从音乐节到剧场，挑一场属于你的现场。</p>
        <button
          onClick={() =>
            document
              .getElementById("lineup")
              ?.scrollIntoView({ behavior: "smooth" })
          }
        >
          探索演出 ↗
        </button>
        <div className="orbit">
          LIVE
          <br />
          NOW
        </div>
      </section>
      <section id="lineup" className="section">
        <div className="section-title">
          <div>
            <span className="eyebrow">THE LINEUP</span>
            <h2>正在上演</h2>
          </div>
          <span>{shows.length} 场演出</span>
        </div>
        <div className="show-grid">
          {shows.map((x, i) => (
            <Link
              key={x.id}
              className={`show-card tone-${i % 4}`}
              to={`/events/${x.id}`}
            >
              <div className="art">
                0{i + 1}
                <span>✦</span>
              </div>
              <div>
                <small>{x.category || "现场演出"}</small>
                <h3>{x.title}</h3>
                <span>查看场次与门票 ↗</span>
              </div>
            </Link>
          ))}
        </div>
        {!shows.length && <div className="empty">目前没有已发布的演出。</div>}
      </section>
      </>}
      {eventId && show?.id === eventId && (
        <section className="section detail">
          <div className="section-title">
            <div>
              <span className="eyebrow">EVENT DETAILS</span>
              <h2>{show.title}</h2>
              <p>{show.description || "精彩演出即将开始。"}</p>
            </div>
            <Link className="secondary" to="/">
              返回演出列表
            </Link>
          </div>
          {sessions.map((x) => (
            <button
              key={x.id}
              className={`row ${session?.id === x.id ? "chosen" : ""}`}
              onClick={() => void selectSession(x)}
            >
              <span>
                <b>{when(x.startTime)}</b>
                <small>
                  场馆 #{x.venueId} ·{" "}
                  {x.saleMode === "GENERAL" ? "票档购票" : "在线选座"}
                </small>
              </span>
              <span>查看 →</span>
            </button>
          ))}
          {!sessions.length && <div className="empty">暂无可售场次。</div>}
          {session && <PurchasePanel />}
        </section>
      )}
      {eventId && show?.id !== eventId && <div className="empty">{loadError || "正在加载演出详情…"}</div>}
    </>
  );
}

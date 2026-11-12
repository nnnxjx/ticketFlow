import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useUserContext } from "../context";
import { api, labels, type AsyncResult } from "../model";

export function OrderProcessingPage() {
  const { requestId = "" } = useParams();
  const navigate = useNavigate();
  const { token, pending, setPending, activeAttempt, setActiveAttempt, purchaseAttempts,
    saveAttempt, removeAttempts, openOrder, listOrders, setNotice } = useUserContext();
  const attemptRef = useRef<{ requestId: string; value: typeof activeAttempt } | null>(null);
  const attempt = (activeAttempt?.requestId === requestId ? activeAttempt
    : purchaseAttempts.find((x) => x.requestId === requestId))
    || (attemptRef.current?.requestId === requestId ? attemptRef.current.value : null);
  if (attempt) attemptRef.current = { requestId, value: attempt };
  const statusRef = useRef(pending?.requestId === requestId ? pending.status : "PROCESSING");
  const [result, setResult] = useState<AsyncResult | null>(null);
  const [slow, setSlow] = useState(false);
  const [continued, setContinued] = useState(false);
  const [retryAvailable, setRetryAvailable] = useState(false);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    setSlow(false);
    setContinued(false);
    setRetryAvailable(false);
    const timer = window.setTimeout(() => setSlow(true), 5000);
    const retryTimer = window.setTimeout(() => setRetryAvailable(true), 30000);
    return () => { window.clearTimeout(timer); window.clearTimeout(retryTimer); };
  }, [requestId]);

  useEffect(() => {
    if (!token || !requestId || !attempt || (slow && !continued)) return;
    let active = true;
    let finished = false;
    let timer: number | undefined;
    let inFlight = false;
    const check = async () => {
      if (!active || inFlight) return;
      inFlight = true;
      setChecking(true);
      try {
        const next = await api<AsyncResult>(`/orders/general/async/${encodeURIComponent(requestId)}`);
        if (!active) return;
        setResult(next);
        setPending(next);
        statusRef.current = next.status;
        if (next.status === "ORDERED" && next.orderId) {
          finished = true;
          removeAttempts([requestId]);
          setActiveAttempt(null);
          await openOrder(next.orderId);
          await listOrders(1);
          navigate("/orders");
          setNotice("订单已创建，请及时支付");
          return;
        }
        if (next.status !== "PROCESSING") {
          finished = true;
          removeAttempts([requestId]);
          setActiveAttempt(null);
          return;
        }
      } catch (error) {
        if (active) setNotice((error as Error).message);
      } finally {
        inFlight = false;
        if (active) {
          setChecking(false);
          if (!finished) timer = window.setTimeout(check, 1000);
        }
      }
    };
    void check();
    return () => { active = false; window.clearTimeout(timer); };
  }, [requestId, token, attempt?.requestId, slow, continued]);

  useEffect(() => {
    const beforeUnload = () => {
      if (statusRef.current === "PROCESSING" && attemptRef.current?.value) saveAttempt(attemptRef.current.value);
    };
    window.addEventListener("beforeunload", beforeUnload);
    return () => {
      window.removeEventListener("beforeunload", beforeUnload);
      // StrictMode 的开发期重复挂载不会离开当前地址，因此不写入离开记录。
      if (window.location.pathname !== `/orders/processing/${encodeURIComponent(requestId)}`
          && statusRef.current === "PROCESSING" && attemptRef.current?.value) saveAttempt(attemptRef.current.value);
    };
  }, [requestId, token]);

  async function retry() {
    if (!attempt) return;
    try {
      const next = await api<AsyncResult>("/orders/general", "POST", {
        requestId, tierId: attempt.tierId, quantity: attempt.quantity,
      });
      setPending(next);
      setResult(next);
      statusRef.current = next.status;
      if (next.status === "ORDERED" && next.orderId) {
        removeAttempts([requestId]);
        setActiveAttempt(null);
        await openOrder(next.orderId);
        await listOrders(1);
        navigate("/orders");
      } else if (next.status !== "PROCESSING") {
        removeAttempts([requestId]);
        setActiveAttempt(null);
      } else setContinued(true);
    } catch (error) { setNotice((error as Error).message); }
  }

  if (!token) return <section className="section"><div className="empty">请先登录后查看下单结果。</div></section>;
  if (!attempt) return <section className="section"><div className="empty">未找到这次购票请求。<Link to="/orders">查看我的订单</Link></div></section>;
  const status = result?.status || (pending?.requestId === requestId ? pending.status : "PROCESSING");
  return <section className="section processing-page">
    <span className="eyebrow">PURCHASE REQUEST</span>
    <h2>{status === "PROCESSING" ? "正在生成订单" : status === "REJECTED" ? "下单失败" : "暂未查到结果"}</h2>
    <p>{attempt.tierName} · {attempt.quantity} 张</p>
    <small>请求编号：{requestId}</small>
    {status === "PROCESSING" ? <>
      <p>{slow ? "处理时间较长，你可以继续等待或去逛演出。" : "正在确认购票结果，请稍候…"}</p>
      {slow && <div className="button-row">
        {!continued && <button className="secondary" onClick={() => setContinued(true)}>继续等待</button>}
        <Link className="secondary" to="/">去逛演出</Link>
      </div>}
      {retryAvailable && <button className="secondary" disabled={checking} onClick={() => void retry()}>重新发送同一请求</button>}
      <small>页面正在查询结果；离开后将停止查询。</small>
    </> : <>
      <p>{status === "REJECTED" ? labels[result?.failureCode || ""] || result?.failureCode || "下单失败" : "当前查不到这次请求的结果。"}</p>
      <Link className="secondary" to="/">返回演出列表</Link>
    </>}
  </section>;
}

import { useEffect, useRef } from "react";
import QRCode from "qrcode";
import type { Ticket } from "../model";

export function QrTicket({ ticket }: { ticket: Ticket }) {
  const canvas = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    if (canvas.current && ticket.token)
      void QRCode.toCanvas(canvas.current, ticket.token, {
        width: 168,
        margin: 2,
      });
  }, [ticket.token]);
  return (
    <div className="ticket">
      <canvas ref={canvas} />
      <div>
        <b>电子票 #{ticket.id}</b>
        <p>{ticket.status === "UNUSED" ? "待检票" : "已检票"}</p>
        <small>入场时出示二维码，请勿转发。</small>
      </div>
    </div>
  );
}

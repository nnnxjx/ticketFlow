import type { Envelope } from "./types";
import { labels } from "./format";

export const tokenKey = "ticketflow.user.jwt";

export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const token = localStorage.getItem(tokenKey);
  const response = await fetch(`/api/v1${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: token } : {}),
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let result: Envelope<T>;
  try {
    result = (await response.json()) as Envelope<T>;
  } catch {
    throw new Error("服务暂不可用，请稍后重试");
  }
  if (!response.ok || result.code !== "OK")
    throw new Error(
      labels[result.code] || result.message || `请求失败（${response.status}）`,
    );
  return result.data;
}

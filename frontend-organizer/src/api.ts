import type { Envelope } from "./types";

export const key = "ticketflow.organizer.jwt";

export class ApiError extends Error {
  constructor(
    message: string,
    public code: string,
  ) {
    super(message);
  }
}

export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const token = localStorage.getItem(key);
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
    throw new ApiError("服务暂不可用，请稍后重试", "NETWORK");
  }
  if (!response.ok || result.code !== "OK")
    throw new ApiError(result.message || "请求失败", result.code);
  return result.data;
}

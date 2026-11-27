"use client";

const TOKEN_KEY = "bw_token";

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage unavailable */
  }
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export const SERVER_DOWN = "Can't reach the Bodywise server. Is the backend running on port 8080?";

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export async function api<T = void>(path: string, method: Method = "GET", body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let res: Response;
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, SERVER_DOWN);
  }

  if (res.status === 401 && !path.startsWith("/auth")) {
    setToken(null);
    // Hard redirect on purpose: this runs outside React, and a full reload clears all cached state.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/login";
    throw new ApiError(401, "Please sign in again");
  }
  if (!res.ok) {
    let msg: string | null = null;
    try {
      const data = await res.json();
      if (data?.message) msg = data.message;
    } catch {
      /* not JSON: usually the Next.js proxy failing to reach the backend */
    }
    if (!msg) msg = res.status >= 500 ? SERVER_DOWN : `Request failed (${res.status})`;
    throw new ApiError(res.status, msg);
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

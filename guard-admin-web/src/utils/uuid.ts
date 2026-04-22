// src/utils/uuid.ts
// HC-03/HC-04：生成 Request-Id / Trace-Id

function fallbackUuid(): string {
  // 简易 RFC4122-like，仅用于客户端本地标识
  const hex = '0123456789abcdef'
  let s = ''
  for (let i = 0; i < 32; i++) {
    s += hex[Math.floor(Math.random() * 16)]
  }
  return `${s.slice(0, 8)}-${s.slice(8, 12)}-4${s.slice(13, 16)}-8${s.slice(17, 20)}-${s.slice(20, 32)}`
}

export function uuid(): string {
  try {
    const g = globalThis as unknown as { crypto?: Crypto }
    if (g.crypto?.randomUUID) return g.crypto.randomUUID()
  } catch {
    /* noop */
  }
  return fallbackUuid()
}

export function shortId(prefix = ''): string {
  return `${prefix}${uuid().replace(/-/g, '').slice(0, 24)}`
}

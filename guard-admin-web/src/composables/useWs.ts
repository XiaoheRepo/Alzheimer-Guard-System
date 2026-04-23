// src/composables/useWs.ts
// 定向 WebSocket 订阅（HC-05）
import { onScopeDispose, ref } from 'vue'
import { getWsTicket } from '@/api/admin'

type Handler = (data: unknown, topic: string) => void

interface WsFrame {
  type: 'subscribe' | 'unsubscribe' | 'ping' | 'pong' | 'message' | 'error'
  topic?: string
  data?: unknown
}

export function useWs() {
  let ws: WebSocket | null = null
  const connected = ref(false)
  const subs = new Map<string, Set<Handler>>()
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = 1000
  let stopped = false

  async function connect() {
    if (stopped) return
    try {
      const { ws_ticket } = await getWsTicket()
      const wsBase = import.meta.env.VITE_WS_BASE_URL
      if (!wsBase) return
      ws = new WebSocket(`${wsBase}?ticket=${encodeURIComponent(ws_ticket)}`)
      ws.addEventListener('open', () => {
        connected.value = true
        reconnectDelay = 1000
        // 重连后恢复订阅
        subs.forEach((_, topic) => send({ type: 'subscribe', topic }))
        heartbeatTimer = setInterval(() => send({ type: 'ping' }), 30_000)
      })
      ws.addEventListener('message', (ev) => {
        try {
          const frame = JSON.parse(ev.data) as WsFrame
          if (frame.type === 'pong') return
          if (frame.topic && subs.has(frame.topic)) {
            subs.get(frame.topic)?.forEach((h) => h(frame.data, frame.topic!))
          }
        } catch {
          /* ignore */
        }
      })
      ws.addEventListener('close', () => {
        connected.value = false
        cleanupTimers()
        scheduleReconnect()
      })
      ws.addEventListener('error', () => {
        ws?.close()
      })
    } catch {
      scheduleReconnect()
    }
  }

  function send(frame: WsFrame) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(frame))
    }
  }

  function scheduleReconnect() {
    if (stopped) return
    reconnectTimer = setTimeout(() => {
      reconnectDelay = Math.min(reconnectDelay * 2, 30_000)
      void connect()
    }, reconnectDelay)
  }

  function cleanupTimers() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function subscribe(topic: string, handler: Handler) {
    if (!subs.has(topic)) {
      subs.set(topic, new Set())
      send({ type: 'subscribe', topic })
    }
    subs.get(topic)!.add(handler)
  }

  function unsubscribe(topic: string, handler?: Handler) {
    const set = subs.get(topic)
    if (!set) return
    if (handler) set.delete(handler)
    if (!handler || set.size === 0) {
      subs.delete(topic)
      send({ type: 'unsubscribe', topic })
    }
  }

  function close() {
    stopped = true
    cleanupTimers()
    subs.forEach((_, topic) => send({ type: 'unsubscribe', topic }))
    subs.clear()
    ws?.close()
    ws = null
  }

  onScopeDispose(() => {
    close()
  })

  return { connect, subscribe, unsubscribe, close, connected }
}

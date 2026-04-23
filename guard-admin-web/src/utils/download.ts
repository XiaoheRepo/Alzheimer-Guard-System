// src/utils/download.ts
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  // 不 append 到 body，直接 click，避免部分浏览器因 DOM 变动触发两次下载
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

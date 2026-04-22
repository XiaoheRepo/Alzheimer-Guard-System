// src/directives/index.ts
import type { App } from 'vue'
import { vPermission } from './permission'

export function registerDirectives(app: App): void {
  app.directive('permission', vPermission)
}

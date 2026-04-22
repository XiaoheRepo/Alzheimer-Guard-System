// src/styles/antdv.ts
// AntDV 主题 Token（对齐 WAHB §4）
import { theme, type ThemeConfig } from 'ant-design-vue'

const commonToken = {
  colorPrimary: '#F97316',
  colorLink: '#0EA5E9',
  colorSuccess: '#22C55E',
  colorWarning: '#F59E0B',
  colorError: '#EF4444',
  colorInfo: '#0EA5E9',
  borderRadius: 8,
  wireframe: false,
}

export const lightTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    ...commonToken,
    colorBgLayout: '#F5F7FA',
    colorBgContainer: '#FFFFFF',
  },
}

export const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    ...commonToken,
    colorBgLayout: '#141414',
    colorBgContainer: '#1F1F1F',
    colorBgElevated: '#262626',
  },
}

export const echartsPalette = [
  '#F97316',
  '#0EA5E9',
  '#22C55E',
  '#A855F7',
  '#F59E0B',
  '#64748B',
]

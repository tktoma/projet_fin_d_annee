// ============================================================================
// front/src/__tests__/setup.js
// ============================================================================
// Fichier chargé avant chaque suite de tests.
// Configure @testing-library/jest-dom et les mocks globaux nécessaires.
// ============================================================================

import '@testing-library/jest-dom'
import { vi, afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// Nettoyage automatique du DOM après chaque test
afterEach(() => {
    cleanup()
})

// ── Mock global de window.matchMedia ──────────────────────────────────────────
// Certains composants utilisent des media queries (responsive)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
})

// ── Mock global de IntersectionObserver ──────────────────────────────────────
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
}))

// ── Mock global de ResizeObserver ─────────────────────────────────────────────
global.ResizeObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
}))

// ── Mock de window.scrollTo ───────────────────────────────────────────────────
window.scrollTo = vi.fn()

// ── Mock de localStorage ──────────────────────────────────────────────────────
const localStorageMock = (() => {
    let store = {}
    return {
        getItem: (key) => store[key] ?? null,
        setItem: (key, value) => { store[key] = String(value) },
        removeItem: (key) => { delete store[key] },
        clear: () => { store = {} },
        get length() { return Object.keys(store).length },
        key: (i) => Object.keys(store)[i] ?? null,
    }
})()
Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
    writable: true,
})

// ── Supprime les warnings React act() dans les tests async ────────────────────
const originalError = console.error
beforeAll(() => {
    console.error = (...args) => {
        if (
            args[0]?.includes?.('Warning: An update to') ||
            args[0]?.includes?.('act(')
        ) return
        originalError(...args)
    }
})
afterAll(() => {
    console.error = originalError
})
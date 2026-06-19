// ============================================================================
// front/vite.config.test.js  — Configuration Vitest
// ============================================================================
// À fusionner dans vite.config.js existant, ou utiliser comme fichier séparé
// avec : npx vitest --config vite.config.test.js
// ============================================================================

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    test: {
        // Environnement DOM simulé (jsdom)
        environment: 'jsdom',

        // Fichier de setup global (mocks, matchers, etc.)
        setupFiles: ['./src/__tests__/setup.js'],

        // Inclure tous les fichiers de test
        include: ['src/**/*.{test,spec}.{js,jsx}'],

        // Couverture de code
        coverage: {
            provider: 'v8',
            reporter: ['text', 'html', 'lcov'],
            include: ['src/**/*.{js,jsx}'],
            exclude: [
                'src/main.jsx',
                'src/__tests__/**',
                'src/**/*.test.*',
                'src/assets/**',
            ],
            // Seuils minimaux recommandés
            thresholds: {
                statements: 70,
                branches: 65,
                functions: 70,
                lines: 70,
            },
        },

        // Timeout par test (ms)
        testTimeout: 10_000,

        // Afficher chaque test individuellement
        reporter: 'verbose',
    },
})
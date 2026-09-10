import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// The Android WebView loads index.html from file:///android_asset/www/.
// Keep Vite-generated asset URLs relative so JS/CSS resolve from that directory.
export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    watch: {
      ignored: ['**/build/**', '**/.gradle/**', '**/debug.keystore*'],
    },
  },
});

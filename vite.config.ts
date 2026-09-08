import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// The production bundle is also embedded into the Android APK WebView.
// A relative base keeps Vite asset URLs valid from file:///android_asset/web/.
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

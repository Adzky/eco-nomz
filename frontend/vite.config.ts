import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

const outputDirectory = fileURLToPath(
  new URL("../src/main/resources/static/owner/logo-crop", import.meta.url),
);

export default defineConfig({
  base: "/owner/logo-crop/",
  plugins: [react()],
  server: {
    fs: {
      allow: [".."],
    },
  },
  build: {
    outDir: outputDirectory,
    emptyOutDir: true,
    cssCodeSplit: false,
    rollupOptions: {
      output: {
        entryFileNames: "app.js",
        chunkFileNames: "chunks/[name].js",
        assetFileNames: "assets/[name][extname]",
      },
    },
  },
  test: {
    include: ["src/**/*.test.ts"],
  },
});

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/map-config.json": "http://localhost:3000",
      "/api/v1/statistics": "http://localhost:8081",
      "/api/v1/parking": "http://localhost:8082",
    },
  },
});

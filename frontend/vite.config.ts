import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/map-config.json": "http://localhost:3000",
      "/api/v1/statistics": "http://localhost:8080",
      "/api/v1/parking": "http://localhost:8080",
      "/api/v1/air-quality": "http://localhost:8080",
      "/api/v1/beaches": "http://localhost:8080",
      "/api/v1/nature": "http://localhost:8080",
      "/api/v1/pharmacies": "http://localhost:8080",
    },
  },
});

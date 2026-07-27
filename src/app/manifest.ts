import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Teren",
    short_name: "Teren",
    description: "Delovni nalogi za montažo sledilne opreme",
    start_url: "/",
    display: "standalone",
    background_color: "#f9fafb",
    theme_color: "#2563eb",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
  };
}

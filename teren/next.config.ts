import type { NextConfig } from "next";

// Varnostne glave za vse odgovore. CSP je namerno omejen na direktive, ki ne zahtevajo nonce-ov za
// Next.js skripte: prepoved vstavljanja v okvir (clickjacking), base/form/object omejitve.
const securityHeaders = [
  {
    key: "Content-Security-Policy",
    value: "frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'",
  },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  // Velja samo prek HTTPS (Cloudflare tunel); brskalnik nato strani ne odpre več prek HTTP.
  { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains" },
  // Kamera za skeniranje kod in fotografiranje; ostalo ni potrebno.
  { key: "Permissions-Policy", value: "camera=(self), microphone=(), geolocation=(), payment=()" },
];

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  experimental: {
    serverActions: {
      bodySizeLimit: "50mb",
    },
    proxyClientMaxBodySize: "50mb",
  },
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;

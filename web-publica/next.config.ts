import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // "standalone" genera un servidor mínimo para la imagen de Docker
  output: "standalone",
  // No anunciar la tecnología del servidor en los headers
  poweredByHeader: false,
};

export default nextConfig;

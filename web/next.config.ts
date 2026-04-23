import type { NextConfig } from "next"

const nextConfig: NextConfig = {
  async rewrites() {
    return {
      // beforeFiles runs before Next.js checks its own file-system routes.
      // This is required for /api/* because Next.js reserves that prefix and
      // would return its own 404 before any rewrite could fire.
      // All auth API calls use /api/v1/auth/* directly (in auth.ts) so they
      // are also covered here — no afterFiles needed.
      beforeFiles: [
        {
          source: "/api/:path*",
          destination: "http://localhost:9741/api/:path*",
        },
      ],
    }
  },
}

export default nextConfig

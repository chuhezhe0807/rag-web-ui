/** @type {import('next').NextConfig} */
module.exports = {
  output: "standalone",
  experimental: {
    // This is needed for standalone output to work correctly
    outputFileTracingRoot: undefined,
    outputStandalone: true,
    skipMiddlewareUrlNormalize: true,
    skipTrailingSlashRedirect: true,
  },
  rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `http://localhost:8088/api/:path*`,
        basePath: false
      },
    ];
  },
};

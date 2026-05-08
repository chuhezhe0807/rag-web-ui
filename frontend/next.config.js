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
    // dev server 把下面两组路径转发给 rag-gateway (127.0.0.1:8080)，让浏览器
    // 看到的都是 same-origin (http://localhost:3000)，避开 CORS。
    //   /api/**           → /api/ai/auth|chat|documents|api-keys/**（Python + Java 混用）
    //   /knowledge-base/**→ 纯 Java rag-knowledge-service（US-007 之后 KB 域只剩 Java）
    const gateway = "http://127.0.0.1:8080";
    return [
      { source: "/api/:path*", destination: `${gateway}/api/:path*`, basePath: false },
      { source: "/knowledge-base/:path*", destination: `${gateway}/knowledge-base/:path*`, basePath: false },
      { source: "/knowledge-base", destination: `${gateway}/knowledge-base`, basePath: false },
    ];
  },
};

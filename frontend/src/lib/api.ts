interface FetchOptions extends Omit<RequestInit, 'body' | 'headers'> {
  data?: any;
  headers?: Record<string, string>;
}

/**
 * 带/的路径是根相对路径，基于当前域名 / 端口的根目录拼接，不受页面路径层级影响；
 * 不带/的路径是页面相对路径，基于当前页面的 URL 路径拼接，易受页面层级影响导致请求错误；
 */
export const BASE_URL = "/api/ai"

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function fetchApi(url: string, options: FetchOptions = {}) {
  const { data, headers: customHeaders = {}, ...restOptions } = options;

  // Get token from localStorage
  let token = '';
  if (typeof window !== 'undefined') {
    token = localStorage.getItem('token') || '';
  }

  const headers: Record<string, string> = {
    ...(token && { Authorization: `Bearer ${token}` }),
    ...customHeaders,
  };

  // If no content type is specified and we have data, default to JSON
  if (!headers['Content-Type'] && data && !(data instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const config: RequestInit = {
    ...restOptions,
    headers,
  };

  // Handle body based on Content-Type
  if (data) {
    if (data instanceof FormData) {
      config.body = data;
    } else if (headers['Content-Type'] === 'application/json') {
      config.body = JSON.stringify(data);
    } else if (headers['Content-Type'] === 'application/x-www-form-urlencoded') {
      config.body = typeof data === 'string' ? data : new URLSearchParams(data).toString();
    } else {
      config.body = data;
    }
  }

  try {
    const response = await fetch(`${BASE_URL}${url}`, config);
    const data = await response.json();

    if (response.status === 401 || data.code === 401) {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
      throw new ApiError(401, 'Unauthorized - Please log in again');
    }

    if (!response.ok || data.code !== 200) {
      throw new ApiError(
        response.status,
        data.message || data.detail || 'An error occurred'
      );
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(500, 'Network error or server is unreachable');
  }
}

// Helper methods for common HTTP methods
export const api = {
  get: (url: string, options?: Omit<FetchOptions, 'method'>) =>
    fetchApi(url, { ...options, method: 'GET' }),

  post: (url: string, data?: any, options?: Omit<FetchOptions, 'method'>) =>
    fetchApi(url, { ...options, method: 'POST', data }),

  put: (url: string, data?: any, options?: Omit<FetchOptions, 'method'>) =>
    fetchApi(url, { ...options, method: 'PUT', data }),

  delete: (url: string, options?: Omit<FetchOptions, 'method'>) =>
    fetchApi(url, { ...options, method: 'DELETE' }),

  patch: (url: string, data?: any, options?: Omit<FetchOptions, 'method'>) =>
    fetchApi(url, { ...options, method: 'PATCH', data }),
};

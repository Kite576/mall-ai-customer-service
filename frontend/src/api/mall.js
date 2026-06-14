const API_BASE = 'http://localhost:8080/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  const result = await response.json()
  if (!response.ok || result.code !== 200) {
    throw new Error(result.message || '请求失败')
  }
  return result.data
}

export const mallApi = {
  products: params => {
    const query = new URLSearchParams()
    if (params?.keyword) query.set('keyword', params.keyword)
    if (params?.categoryId) query.set('categoryId', params.categoryId)
    const suffix = query.toString()
    return request(`/products${suffix ? `?${suffix}` : ''}`)
  },
  product: id => request(`/products/${id}`),
  categories: () => request('/categories'),
  cart: () => request('/cart'),
  addCart: payload => request('/cart/items', { method: 'POST', body: JSON.stringify(payload) }),
  updateCart: (id, payload) => request(`/cart/items/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteCart: id => request(`/cart/items/${id}`, { method: 'DELETE' }),
  clearCart: () => request('/cart', { method: 'DELETE' }),
  createOrder: payload => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  orders: status => request(`/orders${status ? `?status=${status}` : ''}`),
  payOrder: id => request(`/orders/${id}/pay`, { method: 'PUT' }),
  cancelOrder: id => request(`/orders/${id}/cancel`, { method: 'PUT' }),
  confirmOrder: id => request(`/orders/${id}/confirm`, { method: 'PUT' }),
  tracking: id => request(`/orders/${id}/tracking`),
  login: payload => request('/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  register: payload => request('/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  chat: payload => request('/chat/send', { method: 'POST', body: JSON.stringify(payload) }),
  clearChat: sessionId => request(`/chat/session/${sessionId}`, { method: 'DELETE' }),
  chatStream: async (payload, onChunk) => {
    const response = await fetch(`${API_BASE}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })

    if (!response.ok || !response.body) {
      throw new Error('智能客服暂时不可用')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let fullText = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const part of parts) {
        const chunk = parseSseChunk(part)
        if (chunk) {
          fullText += chunk
          onChunk(chunk)
        }
      }
    }

    if (buffer) {
      const chunk = parseSseChunk(buffer)
      if (chunk) {
        fullText += chunk
        onChunk(chunk)
      }
    }

    return fullText
  }
}

function parseSseChunk(part) {
  return part
    .split('\n')
    .filter(line => !line.startsWith(':'))
    .map(line => (line.startsWith('data:') ? line.slice(5).trimStart() : line))
    .join('\n')
    .trimEnd()
}

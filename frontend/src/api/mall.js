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
    return request(`/products?${query.toString()}`)
  },
  categories: () => request('/categories'),
  cart: () => request('/cart'),
  addCart: payload => request('/cart/items', { method: 'POST', body: JSON.stringify(payload) }),
  updateCart: (id, payload) => request(`/cart/items/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteCart: id => request(`/cart/items/${id}`, { method: 'DELETE' }),
  createOrder: payload => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  orders: status => request(`/orders${status ? `?status=${status}` : ''}`),
  payOrder: id => request(`/orders/${id}/pay`, { method: 'PUT' }),
  cancelOrder: id => request(`/orders/${id}/cancel`, { method: 'PUT' }),
  tracking: id => request(`/orders/${id}/tracking`),
  chat: payload => request('/chat/send', { method: 'POST', body: JSON.stringify(payload) })
}

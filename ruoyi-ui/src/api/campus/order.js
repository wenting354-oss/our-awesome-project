import request from '@/utils/request'

// 创建订单 (下单)
export function createOrder(data) {
  return request({
    url: '/campus/order/create',
    method: 'post',
    data: data
  })
}

// 查询我买到的订单
export function listMyOrders(query) {
  return request({
    url: '/campus/order/my',
    method: 'get',
    params: query
  })
}

// 查询我卖出的订单
export function listSoldOrders(query) {
  return request({
    url: '/campus/order/sold',
    method: 'get',
    params: query
  })
}

// 卖家发货
export function shipOrder(orderId) {
  return request({
    url: '/campus/order/ship/' + orderId,
    method: 'post'
  })
}

// 买家确认收货
export function confirmReceipt(orderId) {
  return request({
    url: '/campus/order/confirm/' + orderId,
    method: 'post'
  })
}

// 买家取消订单
export function cancelOrder(orderId) {
  return request({
    url: '/campus/order/cancel/' + orderId,
    method: 'post'
  })
}

// 评价订单
export function rateOrder(orderId, data) {
  return request({
    url: '/campus/order/rate/' + orderId,
    method: 'post',
    params: data
  })
}

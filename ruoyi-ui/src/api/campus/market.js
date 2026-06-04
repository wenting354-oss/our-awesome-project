import request from '@/utils/request'

// 查询二手商品列表（支持分页、搜索、分类筛选）
export function listProduct(query) {
  return request({
    url: '/campus/product/list',
    method: 'get',
    params: query
  })
}

// 查询商品详情
export function getProduct(productId) {
  return request({
    url: '/campus/product/' + productId,
    method: 'get'
  })
}

// 新增商品
export function addProduct(data) {
  return request({
    url: '/campus/product',
    method: 'post',
    data: data
  })
}

// 修改商品
export function updateProduct(data) {
  return request({
    url: '/campus/product',
    method: 'put',
    data: data
  })
}

// 删除商品
export function delProduct(productId) {
  return request({
    url: '/campus/product/' + productId,
    method: 'delete'
  })
}

// 获取我的商品列表
export function listMyProduct(query) {
  return request({
    url: '/campus/product/my-list',
    method: 'get',
    params: query
  })
}

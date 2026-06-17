<template>
  <div class="market-pro-container">
    <!-- 1. 弥散渐变 + 极客风全景 Banner -->
    <div class="hero-section">
      <div class="hero-orbs">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="text-gradient">校园闲置</span> 焕新宇宙
        </h1>
        <p class="hero-subtitle">在这里，发现属于你的校园宝藏好物</p>
        <div class="hero-stats">
          <div class="stat-item"><span class="num">100%</span>真实校友</div>
          <div class="stat-divider"></div>
          <div class="stat-item"><span class="num">0</span>手续费</div>
          <div class="stat-divider"></div>
          <div class="stat-item"><span class="num">极速</span>同城面交</div>
        </div>
      </div>
    </div>

    <!-- 2. 突破边界：悬浮玻璃拟态搜索舱 -->
    <div class="glass-search-capsule">
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="pro-search-form" @submit.native.prevent>
        <el-form-item class="search-item main-search">
          <el-input
            v-model="queryParams.title"
            placeholder="搜一搜闲置宝贝..."
            clearable
            prefix-icon="el-icon-search"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>

        <div class="divider-line"></div>

        <el-form-item class="search-item select-item">
          <el-select v-model="queryParams.category" placeholder="全部分类" clearable @change="handleQuery">
            <el-option label="全部分类" value="" />
            <el-option label="电子产品" value="1" />
            <el-option label="图书教材" value="2" />
            <el-option label="生活用品" value="3" />
            <el-option label="服装配饰" value="4" />
            <el-option label="其他" value="5" />
          </el-select>
        </el-form-item>

        <div class="divider-line"></div>

        <el-form-item class="search-item select-item">
          <el-select v-model="queryParams.sortBy" placeholder="智能排序" clearable @change="handleQuery">
            <el-option label="最新发布" value="createTime" />
            <el-option label="价格低到高" value="priceAsc" />
            <el-option label="价格高到低" value="priceDesc" />
          </el-select>
        </el-form-item>

        <el-button type="primary" class="capsule-btn search-btn" @click="handleQuery">
          <i class="el-icon-search"></i>
        </el-button>
        <el-button class="capsule-btn reset-btn" @click="resetQuery">
          <i class="el-icon-refresh-left"></i>
        </el-button>
      </el-form>
    </div>

    <!-- 3. 操作控制台 -->
    <div class="action-console">
      <div class="console-left">
        <span class="result-text" v-if="total > 0">共发现 <span class="highlight">{{ total }}</span> 件好物</span>
      </div>
      <div class="console-right">
        <button class="pro-btn magic-btn" @click="openRecommend">
          <i class="el-icon-magic-stick"></i> 猜你喜欢
        </button>
        <button class="pro-btn publish-btn" @click="handleAdd">
          <i class="el-icon-s-promotion"></i> 极速发布
        </button>
        <button class="pro-btn my-btn" @click="goToMyProducts">
          <i class="el-icon-user-solid"></i> 我的闲置
        </button>
      </div>
    </div>

    <!-- 4. 流畅级联入场商品网格 -->
    <div class="product-wrapper" v-loading="loading">
      <el-empty v-if="productList.length === 0 && !loading" description="空空如也，换个词搜搜看吧~" :image-size="200"></el-empty>

      <div class="pro-grid-container" v-if="productList.length > 0">
        <div
          v-for="(product, index) in productList"
          :key="product.productId || product.id"
          class="pro-product-card"
          :style="{ animationDelay: `${index * 0.05}s` }"
          @click="goToDetail(product.productId || product.id)"
        >
          <!-- 封面图 -->
          <div class="card-hero">
            <el-image
              :src="product.imageUrls ? product.imageUrls.split(',')[0] : require('@/assets/logo/logo.png')"
              class="hero-img"
              fit="cover"
              lazy
            >
              <div slot="error" class="error-slot"><i class="el-icon-picture-outline"></i></div>
            </el-image>
            <div class="hero-overlay">
              <span class="explore-btn">探 索</span>
            </div>
            <div v-if="product.status !== '0'" class="glass-badge">
              {{ product.status === '1' ? '已拍下' : '已下架' }}
            </div>
          </div>

          <!-- 信息区 -->
          <div class="card-body">
            <h3 class="title" :title="product.title">{{ product.title }}</h3>
            <p class="desc">{{ product.description | truncate(35) }}</p>

            <div class="tag-row">
              <span class="cat-tag">{{ getCategoryLabel(product.category) }}</span>
              <span class="time-tag"><i class="el-icon-time"></i> {{ product.createTime ? product.createTime.substring(0, 10) : '' }}</span>
            </div>

            <div class="footer-row">
              <div class="price-zone">
                <span class="currency">¥</span>
                <span class="amount">{{ product.price }}</span>
              </div>
              <div class="user-zone">
                <el-avatar :size="24" :src="formatAvatar(product.avatar)" icon="el-icon-user-solid" class="tiny-avatar"></el-avatar>
                <span class="user-name">{{ product.nickName || product.userName || '同学' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pro-pagination" v-show="total > 0">
      <pagination :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </div>

    <!-- 保持原有的猜你喜欢弹窗功能，但套用全新的类名或保持原样 -->
    <el-dialog :visible.sync="recommendOpen" width="1100px" append-to-body custom-class="premium-recommend-dialog" top="6vh">
      <div slot="title" class="premium-dialog-header">
        <div class="header-content">
          <div class="icon-box"><i class="el-icon-magic-stick"></i></div>
          <div class="text-box">
            <h3>AI 专属推荐</h3>
            <p>基于协同过滤算法，为你匹配心动好物</p>
          </div>
        </div>
      </div>
      <div v-loading="recommendLoading" class="premium-recommend-wrapper">
        <el-empty v-if="recommendList.length === 0 && !recommendLoading" description="暂无推荐数据" :image-size="200"></el-empty>
        <div class="pro-grid-container" v-else>
          <div v-for="(product, index) in recommendList" :key="'rec-' + (product.productId || product.id)" class="pro-product-card" :style="{ animationDelay: `${index * 0.05}s` }" @click="goToDetailFromRecommend(product.productId || product.id)">
            <div class="card-hero">
              <el-image :src="product.imageUrls ? product.imageUrls.split(',')[0] : require('@/assets/logo/logo.png')" fit="cover" class="hero-img" lazy></el-image>
              <div class="hero-overlay"><span class="explore-btn">去看看</span></div>
              <div v-if="product.status !== '0'" class="glass-badge">{{ product.status === '1' ? '已拍下' : '已下架' }}</div>
            </div>
            <div class="card-body">
              <h3 class="title">{{ product.title }}</h3>
              <div class="tag-row"><span class="cat-tag">{{ getCategoryLabel(product.category) }}</span></div>
              <div class="footer-row">
                <div class="price-zone"><span class="currency">¥</span><span class="amount">{{ product.price }}</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProduct, getRecommendProducts } from '@/api/campus/product'

export default {
  name: 'Market',
  filters: {
    truncate(value, length) {
      if (!value) return ''
      value = value.toString()
      return value.length > length ? value.substring(0, length) + '...' : value
    }
  },
  data() {
    return {
      loading: false,
      productList: [],
      recommendOpen: false,
      recommendLoading: false,
      recommendList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 12, title: '', category: '', sortBy: 'createTime', status: '0' },
      categoryMap: { '1': '电子产品', '2': '图书教材', '3': '生活用品', '4': '服装配饰', '5': '其他' }
    }
  },
  created() {
    this.getList()
    this.$bus.$on('refreshMarketList', () => { this.getList(); });
  },
  beforeDestroy() {
    this.$bus.$off('refreshMarketList');
  },
  methods: {
    formatAvatar(avatar) {
      if (!avatar) {
        // 如果没有头像，返回默认的系统头像
        return require('@/assets/images/profile.jpg');
      }
      if (avatar.startsWith('http')) {
        return avatar;
      }
      // 拼接后端请求前缀
      return process.env.VUE_APP_BASE_API + avatar;
    },
    getList() {
      this.loading = true
      const query = { ...this.queryParams }
      if (query.sortBy === 'priceAsc') { query.orderByColumn = 'price'; query.isAsc = 'asc'; delete query.sortBy; }
      else if (query.sortBy === 'priceDesc') { query.orderByColumn = 'price'; query.isAsc = 'desc'; delete query.sortBy; }
      else { query.orderByColumn = 'create_time'; query.isAsc = 'desc'; delete query.sortBy; }

      listProduct(query).then(response => {
        this.productList = response.rows || []; this.total = response.total || 0; this.loading = false;
      }).catch(() => { this.loading = false; })
    },
    openRecommend() {
      this.recommendOpen = true; this.recommendLoading = true;
      getRecommendProducts().then(response => {
        this.recommendList = response.rows || response.data || []; this.recommendLoading = false;
      }).catch(() => { this.recommendLoading = false; })
    },
    goToDetailFromRecommend(productId) { this.recommendOpen = false; this.goToDetail(productId); },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); },
    resetQuery() { this.queryParams = { pageNum: 1, pageSize: 12, title: '', category: '', sortBy: 'createTime', status: '0' }; this.getList(); },
    goToDetail(productId) { this.$router.push({ path: '/market-page/detail', query: { id: productId } }); },
    handleAdd() { this.$router.push({ path: '/market-page/add' }); },
    goToMyProducts() { this.$router.push({ path: '/market-page/my-products' }); },
    getCategoryLabel(categoryId) { return this.categoryMap[categoryId] || '其他'; }
  }
}
</script>

<style scoped lang="scss">
/* --- 基础页面布局 --- */
.market-pro-container {
  background-color: #f8fafc;
  min-height: calc(100vh - 84px);
  padding-bottom: 40px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* --- 1. 弥散渐变 Hero Section --- */
.hero-section {
  position: relative;
  height: 280px;
  background: #0f172a;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;

  .hero-orbs {
    position: absolute;
    top: 0; left: 0; width: 100%; height: 100%;
    overflow: hidden;
    z-index: 1;
    .orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.6; animation: orbFloat 10s infinite alternate; }
    .orb-1 { width: 300px; height: 300px; background: #6366f1; top: -50px; left: 10%; animation-delay: 0s; }
    .orb-2 { width: 400px; height: 400px; background: #a855f7; bottom: -100px; right: 5%; animation-delay: -3s; }
    .orb-3 { width: 200px; height: 200px; background: #ec4899; top: 20%; left: 50%; transform: translateX(-50%); animation-delay: -6s; }
  }

  .hero-content {
    position: relative;
    z-index: 2;
    color: #fff;
    margin-top: -30px; /* 为搜索舱留空间 */

    .hero-title {
      font-size: 42px;
      font-weight: 900;
      margin: 0 0 16px 0;
      letter-spacing: 2px;
      .text-gradient {
        background: linear-gradient(to right, #a78bfa, #f472b6);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    .hero-subtitle {
      font-size: 16px;
      color: #cbd5e1;
      margin: 0 0 24px 0;
      font-weight: 300;
    }

    .hero-stats {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 20px;
      color: #94a3b8;
      font-size: 14px;
      .num { color: #fff; font-weight: bold; margin-right: 6px; font-size: 16px; }
      .stat-divider { width: 4px; height: 4px; background: #475569; border-radius: 50%; }
    }
  }
}

@keyframes orbFloat {
  0% { transform: translateY(0) scale(1); }
  100% { transform: translateY(-30px) scale(1.1); }
}

/* --- 2. 悬浮玻璃拟态搜索舱 --- */
.glass-search-capsule {
  position: relative;
  z-index: 10;
  max-width: 900px;
  margin: -35px auto 30px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.6);
  border-radius: 100px;
  padding: 8px 12px;
  box-shadow: 0 20px 40px -15px rgba(0,0,0,0.1);

  .pro-search-form {
    display: flex;
    align-items: center;
    width: 100%;
    margin: 0;

    .divider-line { width: 1px; height: 24px; background: #e2e8f0; margin: 0 16px; }

    ::v-deep .el-form-item { margin: 0; }
    ::v-deep .el-input__inner { border: none; background: transparent; box-shadow: none; font-size: 15px; color: #334155; }
    ::v-deep .el-input__inner::placeholder { color: #94a3b8; }

    .main-search {
      flex-grow: 1;
      ::v-deep .el-input__inner { padding-left: 40px; }
      ::v-deep .el-input__prefix { left: 10px; color: #64748b; font-size: 18px; }
    }

    .select-item {
      width: 130px;
    }

    .capsule-btn {
      width: 44px; height: 44px; border-radius: 50%; padding: 0; display: flex; align-items: center; justify-content: center; font-size: 18px; margin-left: 8px; border: none; transition: all 0.3s;
    }
    .search-btn { background: #6366f1; color: white; box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4); &:hover { transform: scale(1.05); background: #4f46e5; } }
    .reset-btn { background: #f1f5f9; color: #64748b; &:hover { background: #e2e8f0; color: #0f172a; transform: rotate(90deg); } }
  }
}

/* --- 3. 操作控制台 --- */
.action-console {
  max-width: 1200px;
  margin: 0 auto 24px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .result-text { font-size: 15px; color: #64748b; .highlight { color: #6366f1; font-weight: bold; font-size: 18px; } }

  .console-right {
    display: flex; gap: 16px;
    .pro-btn {
      border: none; padding: 10px 20px; border-radius: 12px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      &:hover { transform: translateY(-2px); }
    }
    .magic-btn { background: linear-gradient(135deg, #a855f7, #ec4899); color: white; box-shadow: 0 10px 20px -5px rgba(236, 72, 153, 0.3); }
    .publish-btn { background: #0f172a; color: white; box-shadow: 0 10px 20px -5px rgba(15, 23, 42, 0.3); }
    .my-btn { background: #fff; color: #475569; border: 1px solid #e2e8f0; &:hover { border-color: #cbd5e1; color: #0f172a; } }
  }
}

/* --- 4. 流畅级联入场商品网格 --- */
.product-wrapper { max-width: 1200px; margin: 0 auto; padding: 0 24px; }

.pro-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.pro-product-card {
  background: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px -1px rgba(0, 0, 0, 0.02);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: slideUpFade 0.6s both;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.02);
    border-color: #e2e8f0;

    .hero-img { transform: scale(1.05); }
    .hero-overlay { opacity: 1; }
  }

  .card-hero {
    position: relative; width: 100%; height: 240px; overflow: hidden; background: #f8fafc;
    .hero-img { width: 100%; height: 100%; transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
    .error-slot { display: flex; align-items: center; justify-content: center; height: 100%; font-size: 32px; color: #cbd5e1; }

    .hero-overlay {
      position: absolute; inset: 0; background: rgba(15, 23, 42, 0.2); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; opacity: 0; transition: all 0.3s ease;
      .explore-btn { background: rgba(255,255,255,0.9); color: #0f172a; padding: 10px 24px; border-radius: 30px; font-weight: bold; font-size: 14px; letter-spacing: 1px; }
    }

    .glass-badge {
      position: absolute; top: 12px; left: 12px; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); color: #fff; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 500;
    }
  }

  .card-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    flex-grow: 1;

    .title { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; color: #1e293b; line-height: 1.4; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; height: 44px; }
    .desc { margin: 0 0 16px 0; font-size: 13px; color: #64748b; line-height: 1.5; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 1; overflow: hidden; }

    .tag-row {
      display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
      .cat-tag { background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 8px; font-size: 12px; font-weight: 500; }
      .time-tag { font-size: 12px; color: #94a3b8; }
    }

    .footer-row {
      display: flex; justify-content: space-between; align-items: flex-end; margin-top: auto; border-top: 1px solid #f1f5f9; padding-top: 16px;

      .price-zone {
        color: #ef4444;
        .currency { font-size: 14px; font-weight: bold; margin-right: 2px; }
        .amount { font-size: 24px; font-weight: 900; letter-spacing: -0.5px; line-height: 1; }
      }

      .user-zone {
        display: flex; align-items: center; gap: 6px;
        .tiny-avatar { background: #e2e8f0; color: #64748b; }
        .user-name { font-size: 13px; color: #64748b; max-width: 70px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
      }
    }
  }
}

@keyframes slideUpFade {
  0% { opacity: 0; transform: translateY(40px); }
  100% { opacity: 1; transform: translateY(0); }
}

.pro-pagination { margin-top: 40px; display: flex; justify-content: center; }

/* 猜你喜欢弹窗样式保持高级感 */
::v-deep .premium-recommend-dialog {
  border-radius: 24px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  .el-dialog__header { padding: 0 !important; margin: 0 !important; }
  .el-dialog__body { padding: 32px; background-color: #f8fafc; }
  .el-dialog__headerbtn { top: 24px !important; right: 24px !important; z-index: 10; .el-dialog__close { color: #fff !important; font-size: 24px; } }
}
.premium-dialog-header {
  background: #0f172a; padding: 24px 32px;
  .header-content {
    display: flex; align-items: center; gap: 16px;
    .icon-box { width: 48px; height: 48px; background: linear-gradient(135deg, #a855f7, #ec4899); border-radius: 14px; display: flex; justify-content: center; align-items: center; i { font-size: 24px; color: #fff; } }
    .text-box { h3 { margin: 0 0 4px 0; font-size: 20px; color: #fff; } p { margin: 0; font-size: 13px; color: #94a3b8; } }
  }
}

@media (max-width: 768px) {
  .hero-section { height: 220px; .hero-title { font-size: 32px !important; } }
  .glass-search-capsule { border-radius: 20px; padding: 16px; margin: -20px 16px 20px; .pro-search-form { flex-direction: column; gap: 12px; .divider-line { display: none; } .search-item { width: 100%; } .main-search ::v-deep .el-input__inner { padding-left: 30px; } } }
  .action-console { flex-direction: column; gap: 16px; align-items: flex-start; .console-right { flex-wrap: wrap; } }
}
</style>

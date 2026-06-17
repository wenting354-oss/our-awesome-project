<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>

    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar @setLayout="setLayout"/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <settings ref="settingRef"/>

      <!-- 👇新增：智能助手悬浮球 -->
      <!-- 👇新增：智能助手悬浮球（加入 v-show 隐藏逻辑） -->
      <div class="ai-float-btn" v-show="!aiDrawerVisible" @click="aiDrawerVisible = true">
        <i class="el-icon-service"></i>
        <span>AI助手</span>
      </div>

      <!-- 👇新增：智能助手右侧抽屉弹窗 -->
      <el-drawer
        :visible.sync="aiDrawerVisible"
        direction="rtl"
        size="420px"
        :with-header="false"
        custom-class="ai-drawer"
        append-to-body>
        <!-- 直接复用你刚才写好的助手页面作为组件 -->
        <assistant-chat />
      </el-drawer>

    </div>
  </div>
</template>

<script>
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'
import variables from '@/assets/styles/variables.scss'

// 👇 引入你写好的智能助手页面作为局部组件
import AssistantChat from '@/views/assistant/index.vue'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Settings,
    Sidebar,
    TagsView,
    AssistantChat // 👈 注册组件
  },
  mixins: [ResizeMixin],
  data() {
    return {
      globalWs: null,
      aiDrawerVisible: false // 👈 控制AI助手抽屉的显示/隐藏
    }
  },
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView,
      fixedHeader: state => state.settings.fixedHeader,
      userId: state => state.user.id
    }),
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    variables() {
      return variables
    }
  },
  watch: {
    userId: {
      handler(val) {
        if (val) {
          this.initGlobalWebSocket();
        }
      },
      immediate: true
    }
  },
  destroyed() {
    if (this.globalWs) {
      this.globalWs.close();
    }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    },
    setLayout() {
      this.$refs.settingRef.openSetting()
    },
    initGlobalWebSocket() {
      if (this.globalWs) return;
      const wsUrl = `ws://localhost:8080/websocket/chat/${this.userId}`;
      this.globalWs = new WebSocket(wsUrl);

      this.globalWs.onmessage = (event) => {
        try {
          const res = JSON.parse(event.data);
          this.$bus.$emit('receiveGlobalMessage', res);

          if (this.$route.path !== '/private-chat/index' || this.$route.query.targetId != res.senderId) {
            this.$notify({
              title: '您收到了一条交易咨询新消息',
              message: res.content,
              position: 'bottom-right',
              type: 'info',
              duration: 4500,
              onClick: () => {
                this.$router.push({ path: '/private-chat/index', query: { targetId: res.senderId } });
              }
            });
          }
        } catch (e) {
          console.error("全局WebSocket收到非标准JSON消息:", event.data);
        }
      };

      this.globalWs.onclose = () => {
        this.globalWs = null;
        setTimeout(() => {
          if (this.userId) this.initGlobalWebSocket();
        }, 5000);
      };

      this.globalWs.onerror = (err) => {
        console.error("全局通讯长连接发生错误:", err);
      };
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mixin.scss";
@import "~@/assets/styles/variables.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.main-container:has(.fixed-header) {
  height: 100vh;
  overflow: hidden;
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{$base-sidebar-width});
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 54px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
}

/* 👇新增：高颜值悬浮球样式 */
.ai-float-btn {
  position: fixed;
  bottom: 80px;
  right: 40px;
  z-index: 9999;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #409EFF 0%, #1989fa 100%);
  border-radius: 50%;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ai-float-btn:hover {
  transform: translateY(-5px) scale(1.05);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.6);
}

.ai-float-btn i {
  font-size: 26px;
}

.ai-float-btn span {
  font-size: 12px;
  margin-top: 2px;
  font-weight: 500;
}

/* 👇新增：让抽屉里的聊天框填满且没有边距 */
::v-deep .ai-drawer .el-drawer__body {
  padding: 0 !important;
  overflow: hidden;
}
</style>

<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.png" alt="Logo" />
            <h1 class="site-title">AI应用生成</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://www.resky.top', target: '_blank' }, '关于作者'),
    title: '关于作者',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 64px;
  padding: 0 28px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.16);
  background:
    linear-gradient(90deg, rgba(7, 11, 19, 0.94), rgba(15, 23, 42, 0.92)),
    linear-gradient(180deg, rgba(34, 211, 238, 0.08), transparent);
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.22);
}

.header-left {
  height: 64px;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid rgba(34, 211, 238, 0.26);
  background: rgba(15, 23, 42, 0.78);
  box-shadow: 0 0 22px rgba(34, 211, 238, 0.16);
}

.site-title {
  color: var(--console-text);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0;
  text-shadow: 0 0 18px rgba(34, 211, 238, 0.24);
}

:deep(.ant-menu) {
  color: var(--console-text-soft);
  background: transparent !important;
}

:deep(.ant-menu-horizontal) {
  line-height: 64px;
}

:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-item),
:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-submenu) {
  color: var(--console-text-soft);
}

:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-item:hover),
:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-submenu:hover),
:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-item-selected) {
  color: var(--console-cyan);
}

:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-item-selected::after),
:deep(.ant-menu-light.ant-menu-horizontal > .ant-menu-item:hover::after) {
  border-bottom-color: var(--console-cyan);
  box-shadow: 0 0 14px rgba(34, 211, 238, 0.72);
}

.user-login-status {
  height: 64px;
  display: flex;
  align-items: center;
  color: var(--console-text-soft);
}

:deep(.ant-space) {
  color: var(--console-text-soft);
}
</style>

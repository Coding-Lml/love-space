import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/moments',
    name: 'moments',
    component: () => import('../views/Moments.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/moments/create',
    name: 'momentCreate',
    component: () => import('../views/MomentCreate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/diary',
    name: 'diary',
    component: () => import('../views/Diary.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/diary/write',
    name: 'diaryWrite',
    component: () => import('../views/DiaryWrite.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/anniversary',
    name: 'anniversary',
    component: () => import('../views/Anniversary.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next({ name: 'login' })
  } else if (to.name === 'login' && userStore.isLoggedIn) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router

import Vue from 'vue'
import VueRouter from 'vue-router'
import List from '../views/List.vue'
import Detail from '../views/Detail.vue'

Vue.use(VueRouter)

  const routes = [
  {
    path: '/',
    name: 'List',
    component: List
  },
  {
    path: '/test-relation/:id',
    name: 'Detail',
    component: Detail
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router

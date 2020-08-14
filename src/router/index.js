import Vue from 'vue'
import VueRouter from 'vue-router'
import List from '../views/List.vue'

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
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "about" */ '../views/Detail.vue')
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router

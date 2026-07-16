<template>
  <div id="english-app">
    <router-view />
    <FloatingRagChat :visible="showFloatingRag" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import FloatingRagChat from './components/FloatingRagChat.vue'

const route = useRoute()

const showFloatingRag = computed(() => {
  return route.name !== 'Login' && !route.meta?.admin
})
</script>
 
<style>
 * {
   margin: 0;
   padding: 0;
   box-sizing: border-box;
 }
 
 body {
   font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
   background: #f0f2f5;
   color: #333;
   min-height: 100vh;
 }
 
 #english-app {
   min-height: 100vh;
 }
 </style>

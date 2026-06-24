const CACHE_NAME = 'ohlordjesus-cache-v1';
const urlsToCache = [
  'css/modal.css',
  'css/reset.css',
  'css/style.css',
  'css/brands.css',
  'js/modal.js',
  'images/avatar.png',
  'images/avatar@2x.png',
  'images/icons/huifuben.svg',
  'images/icons/smdj.svg',
  'images/icons/shige.svg',
  'images/icons/generic-cloud.svg',
  'images/icons/generic-calendar.svg',
  'images/icons/generic-blog.svg',
  // 如果还有其他需要缓存的图标或图片，请在这里添加
  // 为 PWA 安装添加的图标
  'images/icon-192.png',
  'images/icon-512.png'
];

// 1. 安装 Service Worker 并缓存核心资源
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
  );
});

// 2. 拦截请求并从缓存中提供资源
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // 如果缓存中有匹配的资源，则返回它
        if (response) {
          return response;
        }
        // 否则，从网络请求
        return fetch(event.request);
      }
    )
  );
});

// 3. 激活 Service Worker 并清理旧缓存
self.addEventListener('activate', event => {
  const cacheWhitelist = [CACHE_NAME];
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheWhitelist.indexOf(cacheName) === -1) {
            // 如果缓存名不在白名单中，则删除它
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
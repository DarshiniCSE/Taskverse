/**
 * TaskVerse — Service Worker
 * Version: 1.0.0
 * 
 * SECURITY MANDATE:
 * - Cache ONLY safe static assets (CSS, JS, icons, fonts, offline fallback).
 * - NEVER cache authenticated API endpoints (/api/*).
 * - NEVER cache user credentials, passwords, session cookies, or CSRF tokens.
 * - API requests MUST always go directly to the network.
 */

const CACHE_NAME = 'taskverse-static-v1';

const STATIC_ASSETS = [
    './',
    './index.html',
    './css/style.css',
    './js/app.js',
    './js/fluid-background.js',
    './offline.html',
    './manifest.webmanifest',
    './manifest.json',
    './icons/icon-192.png',
    './icons/icon-512.png',
    './icons/icon-192-maskable.png',
    './icons/icon-512-maskable.png',
    './icons/apple-touch-icon.png',
    './icons/favicon.png',
    './icons/favicon-32.png'
];

// Install Event: Pre-cache core shell assets
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                // Pre-cache individual items safely so one missing asset doesn't break installation
                return Promise.allSettled(
                    STATIC_ASSETS.map((url) =>
                        cache.add(url).catch((err) => {
                            console.warn('[TaskVerse SW] Failed to pre-cache:', url, err);
                        })
                    )
                );
            })
            .then(() => self.skipWaiting())
    );
});

// Activate Event: Cleanup stale caches and claim clients immediately
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames
                        .filter((name) => name !== CACHE_NAME)
                        .map((name) => {
                            console.log('[TaskVerse SW] Deleting obsolete cache:', name);
                            return caches.delete(name);
                        })
                );
            })
            .then(() => self.clients.claim())
    );
});

// Fetch Event: Strict security routing
self.addEventListener('fetch', (event) => {
    const request = event.request;
    const url = new URL(request.url);

    // 1. NON-GET REQUESTS: Never intercept or cache mutations (POST, PUT, DELETE)
    if (request.method !== 'GET') {
        return; // Handled directly by native browser fetch
    }

    // 2. API REQUESTS: NEVER CACHE. Strict Network-Only.
    // Protects authentication, multi-tenant tasks, notes, user profiles, CSRF tokens.
    if (url.pathname.includes('/api/')) {
        event.respondWith(
            fetch(request).catch(() => {
                return new Response(
                    JSON.stringify({
                        success: false,
                        offline: true,
                        message: 'Network offline. Reconnect to sync with your TaskVerse cloud workspace.'
                    }),
                    {
                        status: 503,
                        statusText: 'Service Unavailable',
                        headers: {
                            'Content-Type': 'application/json; charset=UTF-8',
                            'Cache-Control': 'no-store, no-cache, must-revalidate'
                        }
                    }
                );
            })
        );
        return;
    }

    // 3. HTML NAVIGATION REQUESTS: Network-First with Offline Fallback
    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const copy = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
                    }
                    return networkResponse;
                })
                .catch(async () => {
                    const cache = await caches.open(CACHE_NAME);
                    // Try returning cached index.html, else return offline fallback page
                    const cachedNav = await cache.match('./index.html') || await cache.match('./');
                    if (cachedNav) {
                        return cachedNav;
                    }
                    const fallback = await cache.match('./offline.html');
                    return fallback || new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/plain' } });
                })
        );
        return;
    }

    // 4. STATIC ASSETS (CSS, JS, Icons, Images, Web Fonts): Stale-While-Revalidate
    event.respondWith(
        caches.match(request).then((cachedResponse) => {
            const fetchPromise = fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
                        const copy = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, copy));
                    }
                    return networkResponse;
                })
                .catch(() => {
                    // Network failed; cachedResponse is used if available
                });

            return cachedResponse || fetchPromise;
        })
    );
});

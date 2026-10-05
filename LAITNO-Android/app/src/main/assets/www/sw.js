/* LAITNO Android: assets are bundled in the APK, nothing to cache */
self.addEventListener('install',()=>self.skipWaiting());
self.addEventListener('activate',e=>e.waitUntil(self.clients.claim()));

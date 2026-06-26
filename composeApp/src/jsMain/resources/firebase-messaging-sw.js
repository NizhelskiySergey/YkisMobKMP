importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.2/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyD5ukrhK6g6xKlrn4Iv9zPQxB7ji_gACY4",
    authDomain: "ykis-mob.firebaseapp.com",
    databaseURL: "https://ykis-mob-default-rtdb.europe-west1.firebasedatabase.app",
    projectId: "ykis-mob",
    storageBucket: "ykis-mob.firebasestorage.app",
    messagingSenderId: "1062920014188",
    appId: "1:1062920014188:web:cd8ced095f943b9d088b49"
});

const messaging = firebase.messaging();

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(clients.claim()));

messaging.onBackgroundMessage((payload) => {
    console.log('[SW] Background message received:', payload);

    const title = payload.data?.title || payload.notification?.title || 'ЮКІС';
    const body = payload.data?.body || payload.notification?.body || 'Нове повідомлення';
    const chatId = payload.data?.chatId || payload.data?.chat_id;
    const iconUrl = '/ykis.png';

    const notificationOptions = {
        body: body,
        icon: iconUrl,
        badge: iconUrl,
        tag: chatId || 'ykis-msg',
        data: {
            chatId: chatId,
            url: self.location.origin + (chatId ? '/?chatId=' + chatId : '')
        }
    };

    return self.registration.showNotification(title, notificationOptions);
});

self.addEventListener('notificationclick', (event) => {
    event.notification.close();
    const chatId = event.notification.data.chatId;
    const urlToOpen = event.notification.data.url || self.location.origin;

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
            for (let client of windowClients) {
                if (client.url.startsWith(self.location.origin) && 'focus' in client) {
                    if (chatId) {
                        client.postMessage({ type: 'NAVIGATE_TO_CHAT', chatId: chatId });
                    }
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(urlToOpen);
            }
        })
    );
});

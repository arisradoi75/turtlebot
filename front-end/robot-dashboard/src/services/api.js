import axios from 'axios';

// URL-ul backend-ului tău Spring Boot
const API_URL = 'http://localhost:8080/api'; 

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor: Adaugă token-ul la fiecare cerere dacă suntem logați
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        // Nu trimitem token-ul pentru rutele de auth (login/register) pentru a evita erori 403 dacă token-ul e expirat
        if (token && !config.url.includes('/auth/login') && !config.url.includes('/auth/register')) {
            // Backend-ul așteaptă "Bearer " + token [cite: 111]
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;
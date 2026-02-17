import React, { useState } from 'react';
import api from '../services/api';
import { jwtDecode } from 'jwt-decode'; // Importăm decodorul
import { useNavigate, Link } from 'react-router-dom';

const Login = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            // Trimitem cererea conform clasei LoginRequest din Java [cite: 198]
            const response = await api.post('/v1/auth/login', {
                email,
                password
            });

            // Salvăm token-ul primit
            const { accessToken, refreshToken, name } = response.data;
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            localStorage.setItem('userName', name);

            // Decodăm token-ul pentru a extrage rolul
            const decodedToken = jwtDecode(accessToken);
            // Backend-ul adaugă rolul ca o listă de autorități. Extragem prima autoritate.
            const userRole = decodedToken.role[0].authority;
            localStorage.setItem('userRole', userRole);
            
            // Redirecționăm către Dashboard
            navigate('/dashboard');
        } catch (error) {
            let errorMessage = 'Autentificare eșuată!';
            if (error.response) {
                // Serverul a răspuns cu o eroare (ex: 401, 403)
                console.error('Server Response:', error.response.data);
                errorMessage += ` Detalii: ${error.response.data.message || 'Verifică email/parola.'}`;
            } else if (error.request) {
                // Cererea a fost făcută dar nu s-a primit răspuns (posibil CORS sau server oprit)
                console.error('No response received:', error.request);
                errorMessage = 'Eroare de rețea. Serverul nu răspunde. Verifică dacă backend-ul este pornit și configurația CORS.';
            } else {
                console.error('Error setting up request:', error.message);
                errorMessage = 'A apărut o eroare neașteptată la configurarea cererii.';
            }
            alert(errorMessage);
        }
    };

    return (
        <div style={{ padding: '2rem' }}>
            <h2>Login Robot Security</h2>
            <form onSubmit={handleLogin}>
                <div>
                    <label>Email:</label>
                    <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </div>
                <div>
                    <label>Parola:</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>
                <button type="submit">Logare</button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Nu ai cont? <Link to="/register">Înregistrează-te aici</Link>
            </p>
        </div>
    );
};

export default Login;
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

const Register = () => {
    const [name, setName] = useState('');
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        try {
            // Presupunem că endpoint-ul de register este /api/v1/auth/register
            await api.post('/v1/auth/register', {
                name,
                username,
                email,
                password,
            });

            alert('Înregistrare reușită! Vă puteți autentifica acum.');
            navigate('/'); // Redirecționare către pagina de login
        } catch (error) {
            let errorMessage = 'Înregistrarea a eșuat!';
            if (error.response) {
                // Serverul a răspuns cu o eroare (ex: 409 Conflict)
                console.error('Server Response:', error.response.data);
                errorMessage += ` Detalii: ${error.response.data.message || 'Email-ul poate fi deja folosit sau datele sunt invalide.'}`;
            } else if (error.request) {
                // Cererea a fost făcută dar nu s-a primit răspuns
                console.error('No response received:', error.request);
                errorMessage = 'Eroare de rețea. Serverul nu răspunde. Verifică dacă backend-ul este pornit și configurația CORS.';
            } else {
                console.error('Error setting up request:', error.message);
                errorMessage = 'A apărut o eroare neașteptată la înregistrare.';
            }
            alert(errorMessage);
        }
    };

    return (
        <div style={{ padding: '2rem' }}>
            <h2>Înregistrare Utilizator Nou</h2>
            <form onSubmit={handleRegister}>
                <div><label>Nume:</label><input type="text" value={name} onChange={(e) => setName(e.target.value)} required /></div>
                <div><label>Utilizator:</label><input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required /></div>
                <div><label>Email:</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
                <div><label>Parola:</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
                <button type="submit">Înregistrare</button>
            </form>
            <p style={{ marginTop: '1rem' }}>
                Aveți deja cont? <Link to="/">Autentificați-vă</Link>
            </p>
        </div>
    );
};

export default Register;
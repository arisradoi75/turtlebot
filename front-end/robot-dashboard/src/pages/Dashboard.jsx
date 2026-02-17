import React, { useEffect, useState, useRef } from 'react';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
    const [liveRobotData, setLiveRobotData] = useState(null); // Date primite LIVE prin WebSocket
    const [dbRobotData, setDbRobotData] = useState(null);     // Date încărcate din Baza de Date
    const [liveAlerts, setLiveAlerts] = useState([]);         // Alerte LIVE prin WebSocket
    const [dbAlert, setDbAlert] = useState(null);             // Ultima alertă din Baza de Date
    const stompClientRef = useRef(null); // Referință pentru clientul WebSocket
    // Inițializăm state-ul direct din localStorage pentru a evita re-randări inutile
    const [userInfo] = useState(() => {
        const name = localStorage.getItem('userName') || '';
        const role = localStorage.getItem('userRole') || '';
        return { name, role };
    });
    const navigate = useNavigate();

    // Verificăm dacă userul e logat la încărcare
    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            navigate('/');
        }
    }, [navigate]);

    // Încărcăm ultima stare cunoscută din baza de date la intrarea pe pagină
    useEffect(() => {
        const fetchLastKnownState = async () => {
            try {
                const response = await api.get('/robot/latest-telemetry');
                if (response.data) {
                    setDbRobotData(response.data);
                }
            } catch (error) {
                console.log("Nu există date anterioare sau backend-ul nu răspunde la GET /latest-telemetry");
            }

            // Fetch alerts (istoric)
            try {
                const alertsResponse = await api.get('/robot/latest-alert');
                if (alertsResponse.data) {
                    setDbAlert(alertsResponse.data);
                }
            } catch (error) {
                console.log("Nu există alerte anterioare sau backend-ul nu răspunde la GET /latest-alert");
            }
        };
        fetchLastKnownState();
    }, []);

    // Configurare WebSocket
    useEffect(() => {
        // Folosim import dinamic pentru a evita erorile "global is not defined" la pornire
        const connectWs = async () => {
            try {
                const SockJS = (await import('sockjs-client/dist/sockjs')).default;
                const Stomp = (await import('stompjs')).default;

                const socket = new SockJS('http://localhost:8080/ws-robot');
                const stompClient = Stomp.over(socket);
                stompClientRef.current = stompClient;

                stompClient.connect({}, (frame) => {
                    console.log('Conectat la WebSocket: ' + frame);

                    // Abonare la Telemetrie
                    stompClient.subscribe('/topic/telemetry', (message) => {
                        const data = JSON.parse(message.body);
                        setLiveRobotData(data);
                    });

                    // Abonare la Alerte
                    stompClient.subscribe('/topic/alerts', (message) => {
                        const alertData = JSON.parse(message.body);
                        setLiveAlerts((prev) => [alertData, ...prev]);
                    });
                });
            } catch (error) {
                console.error("Eroare la conectarea WebSocket:", error);
            }
        };

        connectWs();

        // Cleanup la ieșirea din pagină
        return () => {
            if (stompClientRef.current) stompClientRef.current.disconnect();
        };
    }, []);

    // Funcții pentru control ADMIN
    const sendCommand = async (command) => {
        try {
            // Apelăm endpoint-urile din AdminController [cite: 12, 13, 14]
            await api.post(`/admin/${command}`);
            alert(`Comanda ${command.toUpperCase()} trimisă!`);
        } catch (error) {
            console.error(error);
            alert('Eroare la trimiterea comenzii (Ești ADMIN?).');
        }
    };

    // Helper pentru formatarea datei (suportă și formatul array din Java)
    const formatTime = (ts) => {
        if (!ts) return '-';
        try {
            // Dacă backend-ul trimite data ca array: [2024, 2, 17, 10, 30, 0]
            if (Array.isArray(ts)) {
                const [year, month, day, hour, minute, second] = ts;
                // Luna în JS începe de la 0 (Ianuarie), în Java de la 1
                return new Date(year, month - 1, day, hour || 0, minute || 0, second || 0).toLocaleString('ro-RO');
            }
            // Dacă e string ISO
            return new Date(ts).toLocaleString('ro-RO');
        } catch (error) {
            console.error("Eroare formatare dată:", error);
            return 'Dată invalidă';
        }
    };

    return (
        <div style={{ padding: '20px' }}>
            <h1>Robot Security Dashboard</h1>
            {userInfo.name && (
                <h2>Bun venit, {userInfo.name}! (Rol: {userInfo.role})</h2>
            )}
            <button onClick={() => {
                localStorage.clear();
                navigate('/');
            }}>Logout</button>

            {/* ==== ZONA 1: LIVE & CONTROL (Orizontal) ==== */}
            <div style={{ display: 'flex', gap: '20px', marginTop: '20px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
                {/* ==== SECȚIUNE LIVE (WebSocket) ==== */}
                <div style={{ border: '1px solid #ccc', padding: '10px', width: '300px' }}>
                    <h2>Status Robot (Live)</h2>
                    {liveRobotData ? (
                        <ul>
                            <li><strong>Status:</strong> {liveRobotData.status}</li>
                            <li><strong>Baterie:</strong> {liveRobotData.batteryLevel}%</li>
                            <li><strong>Poziție:</strong> X: {liveRobotData.x}, Y: {liveRobotData.y}</li>
                            <li><strong>Ora:</strong> {formatTime(liveRobotData.timestamp)}</li>
                        </ul>
                    ) : (
                        <p style={{ color: 'gray' }}>🔴 Robot nedetectat</p>
                    )}
                </div>

                {/* ==== SECȚIUNE ALERTE LIVE ==== */}
                <div style={{ border: '1px solid red', padding: '10px', width: '300px' }}>
                    <h2>Alerte (Live)</h2>
                    {liveAlerts.length === 0 ? <p>Așteptare alerte...</p> : (
                        <ul>
                            {liveAlerts.map((alert, index) => (
                                <li key={index} style={{ marginBottom: '10px', borderBottom: '1px solid #eee' }}>
                                    <strong>{alert.alertType}</strong>: {alert.message} <br/>
                                    <small>{formatTime(alert.timestamp)}</small>
                                    {alert.snapshotBase64 && (
                                        <img src={`data:image/png;base64,${alert.snapshotBase64}`} alt="Snapshot" width="100" />
                                    )}
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                {/* Secțiunea Control Admin */}
                {userInfo.role === 'ADMIN' && (
                    <div style={{ border: '1px solid #ccc', padding: '10px' }}>
                        <h2>Panou Control (Admin)</h2>
                        <button onClick={() => sendCommand('start')} style={{ background: 'green', color: 'white', margin: '5px' }}>START</button>
                        <button onClick={() => sendCommand('stop')} style={{ background: 'red', color: 'white', margin: '5px' }}>STOP</button>
                        <button onClick={() => sendCommand('dock')} style={{ background: 'blue', color: 'white', margin: '5px' }}>DOCK</button>
                    </div>
                )}
            </div>

            {/* ==== ZONA 2: ISTORIC / DB (Orizontal) ==== */}
            <div style={{ marginTop: '20px', display: 'flex', gap: '20px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
                
                {/* ==== SECȚIUNE BAZĂ DE DATE (Ultima stare salvată) ==== */}
                <div style={{ border: '1px solid #007bff', padding: '10px', width: '300px' }}>
                    <h2>Istoric Telemetrie (DB)</h2>
                    {dbRobotData ? (
                        <ul>
                            <li><strong>Status:</strong> {dbRobotData.status}</li>
                            <li><strong>Baterie:</strong> {dbRobotData.batteryLevel}%</li>
                            <li><strong>Poziție:</strong> X: {dbRobotData.x}, Y: {dbRobotData.y}</li>
                            <li><strong>Data/Ora:</strong> {formatTime(dbRobotData.timestamp)}</li>
                        </ul>
                    ) : (
                        <p>Nu există date în istoric.</p>
                    )}
                </div>

                {/* ==== SECȚIUNE ULTIMA ALERTĂ (DB) ==== */}
                <div style={{ border: '1px solid #ff9800', padding: '10px', width: '300px' }}>
                    <h2>Ultima Alertă (DB)</h2>
                    {dbAlert ? (
                        <ul>
                            <li>
                                <strong>{dbAlert.alertType}</strong>: {dbAlert.message} <br/>
                                <small>{formatTime(dbAlert.timestamp)}</small>
                                {dbAlert.snapshotBase64 && (
                                    <img src={`data:image/png;base64,${dbAlert.snapshotBase64}`} alt="Snapshot" width="100" />
                                )}
                            </li>
                        </ul>
                    ) : (
                        <p>Nu există alerte în istoric.</p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
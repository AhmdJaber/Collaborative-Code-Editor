import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function AdminLogin() {
    const [name, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleHome = () => {
        navigate("/")
    }
    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            const response = await fetch('http://localhost:8080/auth/register/ADMIN', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, email, password }),
            });

            if (response.ok) {
                const authResponse = await response.json();
                alert("Registered successfully");
                localStorage.setItem('adminAccessToken', authResponse.access_token);
                localStorage.setItem('adminRefreshToken', authResponse.refresh_token);
                localStorage.setItem('adminName', name);
                localStorage.setItem('adminEmail', email);

                navigate('/admin');
            } else {
                console.error('Register failed');
            }
        } catch (error) {
            console.error('Error:', error);
        }
    };

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <div className="app-name">TwinCode</div>
                <div className="auth-buttons">
                    <button className="auth-button" onClick={handleHome}>Home</button>
                </div>
            </header>

            <main className="main-content">
                <div className="center-text">
                    <h1 className="welcome-text" style={{ fontFamily: "Times New Roman", color: '#353d4c' }}>
                        TwinCode
                    </h1>
                    <hr />

                    <div className="login-box-editor">
                        <h2>Admin Register</h2>
                        <form onSubmit={handleSubmit}>
                            <label>
                                <br />
                                <input
                                    type="text"
                                    value={name}
                                    className="modal_input"
                                    onChange={(e) => setUsername(e.target.value)}
                                    placeholder='Username'
                                    required
                                />
                            </label>
                            <label>
                                <br />
                                <input
                                    type="email"
                                    value={email}
                                    className="modal_input"
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder='Email'
                                    required
                                />
                            </label>
                            <label>
                                <br />
                                <input
                                    type="password"
                                    value={password}
                                    className="modal_input"
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder='Password'
                                    required
                                />
                            </label>
                            <br />
                            <button type="submit" className="auth-button">Register</button>
                        </form>

                    </div>
                </div>
            </main>
        </div>
    );
}

export default AdminLogin;
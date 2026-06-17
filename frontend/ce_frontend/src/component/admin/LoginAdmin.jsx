import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function AdminLogin() { 
    const [email, setEmail] = useState(''); 
    const [password, setPassword] = useState('');
    const navigate = useNavigate();

    const handleHome = () => {
        navigate("/")
    }
    const handleSubmit = async (e) => {
        e.preventDefault();

        try {
            const response = await fetch('http://localhost:8080/auth/authenticate/ADMIN', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                const authResponse = await response.json();
                localStorage.setItem('adminAccessToken', authResponse.access_token);
                localStorage.setItem('adminRefreshToken', authResponse.refresh_token);
                localStorage.setItem('adminEmail', email);
                localStorage.setItem('adminName', authResponse.client.name);
                
                navigate('/admin');
            } else {
                alert("Wrong username or password!")
                console.error('Login failed');
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
                        <h2>Admin Login</h2>
                        <form onSubmit={handleSubmit}>
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
                            <button type="submit" className="auth-button">Login</button>
                        </form>

                    </div>
                </div>
            </main>
        </div>
    );
}

export default AdminLogin;

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './EditorStyle.css';

function EditorLogin() { 
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleHome = () => {
        navigate("/");
    }
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        try {
            const response = await fetch('http://localhost:8080/auth/authenticate/EDITOR', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password }),
            });
            
            if (response.ok) {
                const authResponse = await response.json();
                
                localStorage.setItem('editorAccessToken', authResponse.access_token);
                localStorage.setItem('editorRefreshToken', authResponse.refresh_token);
                localStorage.setItem('email', email); 
                localStorage.setItem('name', authResponse.client.name); 
                
                
                navigate('/editor/projects');
            } else {
                setError("Wrong username or password!");
                console.error('Login failed');
            }
        } catch (error) {
            setError('Network Connection Error');
            console.error('Error:', error);
        }
    };

    return (
        <div className="welcome-page editor-auth-page editor-login-page">
            <header className="welcome-header editor-auth-header">
                <div className="app-name">TwinCode</div>
                <div className="auth-buttons">
                    <button className="auth-button" onClick={handleHome}>Home</button>
                </div>
            </header>

            <main className="main-content editor-auth-main">
                <section className="editor-login-shell">
                    <div className="editor-login-intro">
                        <span className="editor-register-eyebrow">Editor access</span>
                        <h1 className="editor-register-title">Sign in to your workspace</h1>
                    </div>

                    <div className="login-box-editor editor-register-card">
                        <div className="editor-register-card-header">
                            <h2>Login</h2>
                            <p>Use your email and password to continue.</p>
                        </div>

                        <form onSubmit={handleSubmit} className="editor-register-form">
                            <label className="editor-form-field">
                                <span>Email</span>
                                <input
                                    type="email"
                                    value={email}
                                    className="modal_input editor-input"
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="name@company.com"
                                    autoComplete="email"
                                    required
                                />
                            </label>

                            <label className="editor-form-field">
                                <span>Password</span>
                                <input
                                    type="password"
                                    value={password}
                                    className="modal_input editor-input"
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Enter your password"
                                    autoComplete="current-password"
                                    required
                                />
                            </label>

                            {error ? (
                                <div className="editor-register-error" role="alert">
                                    {error}
                                </div>
                            ) : null}

                            <button type="submit" className="auth-button editor-register-submit">
                                Login
                            </button>
                        </form>
                    </div>
                </section>
            </main>
        </div>
    );
}

export default EditorLogin;

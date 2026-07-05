import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../editor/EditorStyle.css';

function AdminLogin() {
    const [name, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleHome = () => {
        navigate("/")
    }
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

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
                setError('Registration failed');
                console.error('Register failed');
            }
        } catch (error) {
            setError('Network Connection Error');
            console.error('Error:', error);
        }
    };

    return (
        <div className="welcome-page editor-auth-page admin-register-page">
            <header className="welcome-header editor-auth-header">
                <div className="app-name">TwinCode</div>
                <div className="auth-buttons">
                    <button className="auth-button" onClick={handleHome}>Home</button>
                </div>
            </header>

            <main className="main-content editor-auth-main">
                <section className="editor-register-shell">
                    <div className="editor-register-copy">
                        <span className="editor-register-eyebrow">Admin access</span>
                        <h1 className="editor-register-title">Create your admin account</h1>
                        <p className="editor-register-description">
                            Set up admin access for TwinCode to manage users, oversee projects,
                            and keep the platform organized from a central workspace.
                        </p>

                        <div className="editor-register-benefits">
                            <div className="benefit-card">
                                <span className="benefit-icon">01</span>
                                <div>
                                    <strong>Platform oversight</strong>
                                    <p>Manage the collaborative environment from one account.</p>
                                </div>
                            </div>
                            <div className="benefit-card">
                                <span className="benefit-icon">02</span>
                                <div>
                                    <strong>Project control</strong>
                                    <p>Review and coordinate the editor-side workspace cleanly.</p>
                                </div>
                            </div>
                            <div className="benefit-card">
                                <span className="benefit-icon">03</span>
                                <div>
                                    <strong>Secure registration</strong>
                                    <p>Register with a verified identity and keep access organized.</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="login-box-editor editor-register-card">
                        <div className="editor-register-card-header">
                            <h2>Admin Register</h2>
                            <p>Create your administrative profile below.</p>
                        </div>

                        <form onSubmit={handleSubmit} className="editor-register-form">
                            <label className="editor-form-field">
                                <span>Username</span>
                                <input
                                    type="text"
                                    value={name}
                                    className="modal_input editor-input"
                                    onChange={(e) => setUsername(e.target.value)}
                                    placeholder="Enter your username"
                                    autoComplete="username"
                                    required
                                />
                            </label>

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
                                    placeholder="Create a secure password"
                                    autoComplete="new-password"
                                    required
                                />
                            </label>

                            <p className="editor-register-hint">
                                Admin access should use a strong, unique password.
                            </p>

                            {error ? (
                                <div className="editor-register-error" role="alert">
                                    {error}
                                </div>
                            ) : null}

                            <button type="submit" className="auth-button editor-register-submit">
                                Register
                            </button>
                        </form>
                    </div>
                </section>
            </main>
        </div>
    );
}

export default AdminLogin;

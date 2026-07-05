import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './EditorStyle.css';

function EditorLogin() {
    const [name, setUsername] = useState('');
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
            const response = await fetch('http://localhost:8080/auth/register/EDITOR', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, email, password }),
            });

            const authResponse = await response.json();

            if (response.ok) {
                alert("Registered successfully");

                localStorage.setItem('editorAccessToken', authResponse.access_token);
                localStorage.setItem('editorRefreshToken', authResponse.refresh_token);
                localStorage.setItem('email', email);
                localStorage.setItem('name', name);

                navigate('/editor/projects');
            } else {
                // backend error message
                setError(authResponse.error || "Registration failed");
                console.error('Register failed:', authResponse.error);
            }

        } catch (error) {
            setError("Network Connection Error");
            console.error('Network Error:', error);
        }
    };

    return (
        <div className="welcome-page editor-register-page">
            <header className="welcome-header editor-register-header">
                <div className="app-name">TwinCode</div>
                <div className="auth-buttons">
                    <button className="auth-button" onClick={handleHome}>Home</button>
                </div>
            </header>

            <main className="main-content editor-register-main">
                <section className="editor-register-shell">
                    <div className="editor-register-copy">
                        <span className="editor-register-eyebrow">Editor access</span>
                        <h1 className="editor-register-title">Create your editor account</h1>
                        <p className="editor-register-description">
                            Join TwinCode to collaborate in shared projects, manage code faster,
                            and move from idea to live editor without friction.
                        </p>

                        <div className="editor-register-benefits">
                            <div className="benefit-card">
                                <span className="benefit-icon">01</span>
                                <div>
                                    <strong>Collaborative workspace</strong>
                                    <p>Work inside a project space built for team editing.</p>
                                </div>
                            </div>
                            <div className="benefit-card">
                                <span className="benefit-icon">02</span>
                                <div>
                                    <strong>Fast onboarding</strong>
                                    <p>Create your account and start in a few quick steps.</p>
                                </div>
                            </div>
                            <div className="benefit-card">
                                <span className="benefit-icon">03</span>
                                <div>
                                    <strong>Secure sign-up</strong>
                                    <p>Your editor account stays tied to your email identity.</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="login-box-editor editor-register-card">
                        <div className="editor-register-card-header">
                            <h2>Sign Up</h2>
                            <p>Create your workspace profile below.</p>
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
                                Use a password you do not reuse elsewhere.
                            </p>

                            {error ? (
                                <div className="editor-register-error" role="alert">
                                    {error}
                                </div>
                            ) : null}

                            <button type="submit" className="auth-button editor-register-submit">
                                Sign Up
                            </button>
                        </form>
                    </div>
                </section>
            </main>
        </div>
    );
}

export default EditorLogin;

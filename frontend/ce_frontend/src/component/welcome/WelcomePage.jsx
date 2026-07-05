import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import TwinCodeBrand from '../common/TwinCodeBrand';
import './WelcomePage.css';

const WelcomePage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [role, setRole] = useState(null);

    const isLoggedIn = !!localStorage.getItem('email');

    useEffect(() => {
        const verifyAdminAuthority = async () => {
            const token = localStorage.getItem('editorAccessToken');
            if (!token) {
                setRole(null);
                return;
            }

            try {
                const response = await fetch('http://localhost:8080/auth/me', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    setRole(null);
                    return;
                }

                const currentUser = await response.json();
                setRole(currentUser.role || null);
            } catch (error) {
                setRole(null);
            }
        };

        verifyAdminAuthority();
    }, [location.key]);

    const handleLogin = () => {
        navigate("/editor/login");
    };

    const handleNameClick = () => {
        navigate('/editor/projects');
    };

    const capitalize = (str) => {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    };

    const handleLogout = () => {
        localStorage.removeItem('editorAccessToken');
        localStorage.removeItem('editorRefreshToken');
        localStorage.removeItem('email');
        localStorage.removeItem('name');
        localStorage.removeItem('editorRole');
        navigate('/');
    };

    const handleSignup = () => {
        navigate("/editor/register");
    };

    const isAdmin = role === 'ADMIN';

    const handlePublicRepos = () => {
        if (!localStorage.getItem('editorRefreshToken')) {
            navigate('/editor/login');
        } else {
            navigate('/repos');
        }
    };

    const handleStartCodeing = () => {
        if (!localStorage.getItem('editorRefreshToken')) {
            navigate('/editor/login');
        } else {
            navigate('/editor');
        }
    };

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <TwinCodeBrand fallbackLabel="guest" />

                {!isLoggedIn ? (
                    <div className="auth-buttons">
                        <button className="auth-button-name" onClick={handleLogin}>
                            Sign In
                        </button>
                            <button className="auth-button-name auth-button-name--sign-up" onClick={handleSignup}>
                            Sign Up
                        </button>
                    </div>
                ) : (
                    <div className="auth-buttons">
                        <button
                            className="auth-button-name"
                            onClick={handleNameClick}
                        >
                            {capitalize(localStorage.getItem('name'))}
                        </button>
                        <button
                            className="auth-button"
                            onClick={handleLogout}
                        >
                            Logout
                        </button>
                    </div>
                )}
            </header>

            <main className="main-content">
                <div className="center-text">
                    <h1
                        className="welcome-text"
                        style={{
                            fontFamily: "Times New Roman",
                            color: "#353d4c",
                        }}
                    >
                        TwinCode
                    </h1>

                    <h1
                        className="welcome-text"
                        style={{
                            fontFamily: "Times New Roman",
                            color: "#353d4c",
                        }}
                    >
                        Collaborative Code Editor
                    </h1>

                    <p
                        className="subtitle-text"
                        style={{ fontFamily: "Times New Roman" }}
                    >
                        Where you can write code collaboratively and utilize the
                        version control effectively
                    </p>

                    {isLoggedIn ? (
                        <>
                            <div className="start-coding">
                                <button
                                    className="start-button"
                                    onClick={handlePublicRepos}
                                >
                                    Public Repos
                                </button>
                            </div>

                            <div className="start-coding">
                                <button
                                    className="start-button-admin"
                                    onClick={handleStartCodeing}
                                >
                                    Start Coding
                                </button>
                            </div>

                            {isAdmin && (
                                <div className="start-coding">
                                    <button
                                        className="start-button admin-authority-button admin-authority-button--stacked"
                                        onClick={() => navigate('/admin')}
                                    >
                                        Adminstration
                                    </button>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="welcome-auth-buttons">
                            <button
                                className="auth-button-name"
                                onClick={handleLogin}
                            >
                                Sign In
                            </button>

                            {/* <button
                                className="auth-button auth-button-name--sign-up"
                                onClick={handleSignup}
                            >
                                Sign Up
                            </button> */}
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default WelcomePage;

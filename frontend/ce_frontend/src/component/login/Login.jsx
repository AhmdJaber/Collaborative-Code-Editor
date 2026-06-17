import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css'; // Assuming you have a separate CSS file for styling

const MainPage = () => {
    const navigate = useNavigate();
    const handleHome = () => {
        navigate("/")
    }
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
                    <div className="login-box">
                        <h1>Login as:</h1>
                        <br />

                        <form action="admin" method="GET">
                            <button type="submit" className="auth-button">Admin</button>
                        </form>
                        <br />

                        <form action="editor" method="GET">
                            <button type="submit" className="auth-button">Editor</button>
                        </form>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default MainPage;

import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import TwinCodeBrand from '../common/TwinCodeBrand';
import './LoginPage.css'; // Assuming you have a separate CSS file for styling

const MainPage = () => {
    const navigate = useNavigate();
    const handleHome = () => {
        navigate("/")
    }
    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <TwinCodeBrand fallbackLabel="editor" />
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
                        <h1>Editor access</h1>
                        <br />

                        <form action="/editor/login" method="GET">
                            <button type="submit" className="auth-button">Sign In</button>
                        </form>
                        <br />
                        <form action="/editor/register" method="GET">
                            <button type="submit" className="auth-button">Sign Up</button>
                        </form>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default MainPage;

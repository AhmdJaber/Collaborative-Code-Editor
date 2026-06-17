import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './WelcomePage.css';


const WelcomePage = () => {
    const navigate = useNavigate();
    const handleLogin = () =>{
        navigate("/login")
    }
    const handleNameClick = () =>{
        navigate('/editor/projects')
    }
    const capitalize = (str) => {
        if (!str) return '';
        return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
    };

    const handleLogout = () =>{
        localStorage.removeItem('editorAccessToken');
        localStorage.removeItem('email');
        localStorage.removeItem('name');
        navigate('/')
    }
    
    const handleSignup = () =>{
        navigate("/register")
    }
    const handlePublicRepos = () =>{
        if (!localStorage.getItem('editorRefreshToken')){
            navigate('/editor/login')
        } else {
            navigate('/repos'); 
        }
    }   
    const handleStartCodeing = () =>{
        if (!localStorage.getItem('editorRefreshToken')){
             navigate('/editor/login')
        } else {
            navigate('/editor'); 
        }
    
    }
    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <div className="app-name">TwinCode</div>
                {!localStorage.getItem('email') ? (
                    <div className="auth-buttons">
                        <button className="auth-button" onClick={handleLogin}>Log In</button>
                        <button className="auth-button" onClick={handleSignup}>Sign Up</button>
                    </div>
                ) : (
                    <div className="auth-buttons">
                        <button className="auth-button-name" onClick={handleNameClick}>{capitalize(localStorage.getItem('name'))}</button>
                        <button className="auth-button" onClick={handleLogout}>Logout</button>
                    </div>
                )}
            </header>

            <main className="main-content">
                <div className="center-text">
                    <h1 className="welcome-text" style={{ fontFamily: "Times New Roman", color: '#353d4c' }}>
                        TwinCode
                    </h1>
                    <h1 className="welcome-text" style={{ fontFamily: "Times New Roman", color: '#353d4c' }}>
                        Collaborative Code Editor
                    </h1>
                    <p className="subtitle-text" style={{ fontFamily: "Times New Roman" }}>
                        Where you can write code collaboratively and utilize the version control effectively
                    </p>
                    <div className="start-coding">
                        <span className="pip" style={{ background: '#353d4c'}}></span>
                        <button className="start-button" onClick={handlePublicRepos}>Public Repos</button>
                    </div>
                    <div className="start-coding">
                        <span className="pip"style={{ background: '#353d4c'}}></span>
                        <button className="start-button" onClick={() => navigate('/admin')}>Admin page</button>
                    </div>
                    <div className="start-coding">
                        <span className="pip"style={{ background: '#353d4c'}}></span>
                        <button className="start-button-admin" onClick={handleStartCodeing}>Start Coding</button>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default WelcomePage;

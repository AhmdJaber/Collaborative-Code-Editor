import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import TwinCodeBrand from '../common/TwinCodeBrand';

const MainPage = () => {
    const navigate = useNavigate();
    const handleHome = () => {
        navigate("/")
    }

    const handleEditorClieck = () =>{
        navigate("/editor/register"); 
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
                        <h1>Register as:</h1>
                        <br />

                            <button className="auth-button" onClick={handleEditorClieck}>Editor</button>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default MainPage;

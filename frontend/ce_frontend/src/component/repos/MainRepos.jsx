import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './ReposStyle.css';

const ReposMain = () => {
    const navigate = useNavigate();
    const [editors, setEditors] = useState([]);
    const [browse, setBrowse] = useState([]);
    const [repos, setRepos] = useState();
    const [currentSelected, setCurrentSelected] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('editorAccessToken');
        if (!token) {
            navigate("/editor/login");
        }
        fetchAllEditors();
        console.log(currentSelected); 
    }, [navigate]);

    const handleHome = () => {
        navigate('/')
    }

    const isTokenExpired = (token) => {
        const jwt = JSON.parse(atob(token.split('.')[1]));
        const currentTime = Date.now() / 1000;
        return jwt.exp < currentTime;
    };

    const refreshAccessToken = async () => {
        const refreshToken = localStorage.getItem('editorRefreshToken');
        if (!refreshToken) {
            throw new Error('No refresh token available');
        }

        const response = await fetch('http://localhost:8080/auth/refresh-token', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${refreshToken}`,
            },
        });

        if (response.ok) {
            console.log("New access token generated");
            const data = await response.json();
            localStorage.setItem('editorAccessToken', data.access_token);
            return data.access_token;
        } else {
            throw new Error('Failed to refresh access token');
        }
    };

    const fetchWithAuth = async (url, options = {}) => {
        let token = localStorage.getItem('editorAccessToken');

        if (isTokenExpired(token)) {
            try {
                token = await refreshAccessToken();
            } catch (error) {
                console.error('Unable to refresh token, logging out:', error);
                localStorage.removeItem('editorAccessToken');
                localStorage.removeItem('editorRefreshToken');
                navigate('/editor/login');
                return;
            }
        }

        const headers = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        };

        const response = await fetch(url, {
            ...options,
            headers: headers,
        });

        if (response.status === 401) {
            localStorage.removeItem('editorAccessToken');
            localStorage.removeItem('editorRefreshToken');
            navigate('/editor/login');
            return;
        }

        return response;
    };

    const handleLogout = async () => {
        const confirmLogout = window.confirm(`Are you sure you want to logout?`);
        if (confirmLogout) {
            try {
                const response = await fetchWithAuth('http://localhost:8080/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('editorAccessToken')}`,
                    }
                });

                if (response.ok) {
                    localStorage.removeItem('editorAccessToken');
                    navigate('/main');
                } else {
                    console.error('Failed to logout');
                }
            } catch (error) {
                console.error('Logout error:', error);
            }

        }

    };

    const fetchAllEditors = async () => {
        const token = localStorage.getItem('editorAccessToken');
        const response = await fetchWithAuth(`http://localhost:8080/editor/all-editors`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });

        const data = await response.json();
        setEditors(data);
    }

    const handleRepoClick = async (repo) => {
        const projectId = repo.id;
        const token = localStorage.getItem('editorAccessToken');
        await fetchWithAuth(`http://localhost:8080/editor/share_project_view_token/${projectId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });

        localStorage.removeItem('edit');
        const ownerId = repo.client.id;
        localStorage.setItem('project', projectId + "." + ownerId);
        navigate('/editor');
    }

    const handleEditorClieck = async (editor) => {
        const token = localStorage.getItem('editorAccessToken');
        const response = await fetchWithAuth(`http://localhost:8080/editor/get-public-projects/${editor.id}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });
        setCurrentSelected(editor); 
        const data = await response.json();
        setRepos(data);
    }

    const getEditorFromEmail = async (email) => {
        const token = localStorage.getItem('editorAccessToken');
        const response = await fetchWithAuth(`http://localhost:8080/editor/get-editor-by-data/${browse}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
        });

        if (!response.ok) {
            alert("No editors with email " + browse);
            return;
        }

        return await response.json();
    }

    const handleSubmitBrowse = async () => {
        const editor = await getEditorFromEmail(browse);
        if (editor.email == localStorage.getItem('email')) {
            alert("Can't browse your account");
            return;
        }
        setCurrentSelected(editor);
        handleEditorClieck(editor);
    }

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <div className="app-name">TwinCode</div>
                <div className="auth-buttons">
                    <button onClick={handleLogout} className="auth-button">Log out</button>
                    <button className="auth-button" onClick={handleHome}>Home</button>
                </div>
            </header>
            <br />
            <div className="center-text">
                <div className="comments_section-browse" style={{ marginLeft: '10px', width: '1495px' }}>
                    <h3 className="comments-heading">Browse editor</h3>
                    <input
                        type="text"
                        className="modal_input_browse"
                        onChange={(e) => setBrowse(e.target.value)}
                        placeholder="Write the editor email or name"
                    />
                    <div className="modal-buttons">
                        <button className="submit-button" onClick={handleSubmitBrowse} style={{ marginLeft: "0px", background: "#022e11" }}>browse</button>
                    </div>
                </div>

                <div className="comments_section" style={{ marginLeft: '10px', width: '1495px' }}>
                    <h3 className="comments-heading">All accounts</h3>
                    <div className="comments-buttons">
                        {editors && editors.length > 0 ? (
                            editors.map((editor, index) => (
                                editor.email != localStorage.getItem('email') && (
                                    <button key={index} onClick={() => handleEditorClieck(editor)} className="comment-button">
                                        <pre>
                                            <span className="label">Name   :</span> {editor.name} <br />
                                            <span className="label">Email  :</span> {editor.email} <br />
                                        </pre>
                                    </button>
                                )
                            ))
                        ) : (
                            <p>No editors yet</p>
                        )}
                    </div>
                </div>

                
                {repos && repos.length > 0 ? (
                    <div className="comments_section" style={{ marginLeft: '10px', width: '1495px' }}>
                        <h3 className="comments-heading">{currentSelected.name}'s public repos</h3>
                        <div className="comments-buttons">
                            {repos.map((repo, index) => (
                                <button key={index} onClick={() => handleRepoClick(repo)} className="comment-button">
                                    <pre>
                                        <span className="label">Name   :</span> {repo.name} <br />
                                    </pre>
                                </button>
                            ))}
                        </div>
                    </div>
                ) : (
                        currentSelected != '' && (
                            <div className="comments_section" style={{ marginLeft: '10px', width: '1495px' }}>
                                <h3 className="comments-heading">{currentSelected.name}'s public repos</h3>
                                <p>No public repos for <b>{currentSelected.name}</b></p>
                            </div>
                        )
                )}
            </div>
        </div>
    );
};

export default ReposMain;

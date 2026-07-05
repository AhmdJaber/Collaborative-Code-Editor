import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import TwinCodeBrand from '../common/TwinCodeBrand';
import './AdminStyle.css';
import './init'


const AdminMain = () => {
    const navigate = useNavigate();
    const [visibleInput, setVisibleInput] = useState(null);
    const [output, setOutput] = useState();
    const [editorId, setEditorId] = useState();
    const [projectId, setProjectId] = useState();
    const [isAdmin, setIsAdmin] = useState(null);


    useEffect(() => {
        const token = localStorage.getItem('editorAccessToken');
        if (!token) {
            navigate("/editor/login");
            return;
        }

        const verifyAdminAuthority = async () => {
            try {
                const response = await fetchWithAuth('http://localhost:8080/auth/me', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    },
                });

                if (!response) {
                    return;
                }

                if (!response.ok) {
                    navigate('/editor/projects');
                    return;
                }

                const currentUser = await response.json();
                if (currentUser.role !== 'ADMIN') {
                    navigate('/editor/projects');
                    return;
                }

                setIsAdmin(true);
            } catch (error) {
                console.error('Failed to verify admin authority:', error);
                navigate('/editor/projects');
            }
        };

        verifyAdminAuthority();
    }, [navigate]);

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
            if (data.client?.role) {
                localStorage.setItem('editorRole', data.client.role);
            }
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
                localStorage.removeItem('editorRole');
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
            // Handle unauthorized error properly
            localStorage.removeItem('editorAccessToken');
            localStorage.removeItem('editorRefreshToken');
            localStorage.removeItem('editorRole');
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
                    localStorage.removeItem('editorRefreshToken');
                    localStorage.removeItem('email');
                    localStorage.removeItem('name');
                    localStorage.removeItem('editorRole');
                    navigate('/editor/login');
                } else {
                    console.error('Failed to logout');
                }
            } catch (error) {
                console.error('Logout error:', error);
            }

        }

    };

    const handleAllEditors = async () => {
        const token = localStorage.getItem('editorAccessToken');
        try {
            const response = await fetchWithAuth(`http://localhost:8080/admin/get-editors`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            const clients = await response.json();
            if (clients.length > 0) {
                setOutput(getClients(clients));
            } else {
                setOutput("There are no editors");
            }
            console.log(clients);
        } catch (error) {
            console.error("Error while fetching editors: ", error);
        }
    }

    const getClients = (clients) => {
        let str = "";
        clients.map(client => {
            str += "Id: " + client.id + "\n" + "name: " + client.name + "\n" + "email: " + client.email + "\n" + "Role: " + client.role + "\n";
            str += "-------------------------------------------------------------\n\n"
        })

        return str;
    }

    const handleRemoveEditor = async () => {
        const confirm = window.confirm(`Are you sure that you want to remove the client ${editorId}`);
        if (!confirm) {
            return;
        }

        const token = localStorage.getItem('editorAccessToken');
        try {
            await fetchWithAuth(`http://localhost:8080/admin/remove-editor/${editorId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            setOutput("Client with Id : " + editorId + " deleted successfully");
        } catch (error) {
            console.error("Error while deleting editor: ", error);
        }
    }

    const handleAllEditorProjects = async () => {
        const token = localStorage.getItem('editorAccessToken');
        try {
            const response = await fetchWithAuth(`http://localhost:8080/admin/get-editor-projects/${editorId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            const data = await response.json();
            if (data.length > 0) {
                setOutput(getProjects(data));
            } else {
                setOutput("No projects for the editor " + editorId);
            }
        } catch (error) {
            console.error("Error while deleting editor: ", error);
        }
    }
    const getProjects = (projects) => {
        let str = "";
        projects.map(project => {
            str += "Id: " + project.id + "\n" + "name: " + project.name + "\n";
            str += "-------------------------------------------------------------\n\n"
        })

        return str;
    }

    const handleRemoveProject = async () => {
        if (!editorId || !projectId) {
            alert("Enter the editor Id and project Id");
            return;
        }

        const token = localStorage.getItem('editorAccessToken');
        try {
            await fetchWithAuth(`http://localhost:8080/admin/remvoe-project/${editorId}/${projectId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            setOutput("Project with Id : " + projectId + " and owner Id : " + editorId + " deleted successfully");
        } catch (error) {
            console.error("Error while deleting editor: ", error);
        }
    }

    const handleAllSharedWith = async () => {
        const token = localStorage.getItem('editorAccessToken');
        try {
            const response = await fetchWithAuth(`http://localhost:8080/admin/get-shared/${editorId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            const data = await response.json();
            if (data.length > 0) {
                setOutput(getProjects(data));
            } else {
                setOutput("There are no shared projects with the client " + editorId);
            }
        } catch (error) {
            console.error("Error while deleting editor: ", error);
        }
    }

    const handleRemoveSharedEditor = async () => {
        if (!editorId || !projectId) {
            alert("Enter the editor Id and project Id");
            return;
        }

        const token = localStorage.getItem('editorAccessToken');
        try {
            await fetchWithAuth(`http://localhost:8080/admin/remove-shared-project/${editorId}/${projectId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            setOutput("Project Share with project Id : " + projectId + " and shared editor Id : " + editorId + " deleted successfully");
        } catch (error) {
            console.error("Error while deleting editor: ", error);
        }
    }

    const toggleInput = (buttonName) => {
        setVisibleInput(visibleInput === buttonName ? null : buttonName);
    };

    if (isAdmin !== true) {
        return null;
    }

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <TwinCodeBrand fallbackLabel="admin" />
                <div className="auth-buttons">
                    <button onClick={handleLogout} className="auth-button">Log out</button>
                    <button className="auth-button" onClick={ () => navigate('/')}>Home</button>
                </div>
            </header>

            <div>
                <h1 className="welcome-text" style={{ fontFamily: "Times New Roman", color: '#353d4c', marginLeft: '45%'}}>
                    A d m i n
                </h1>
                <br />
                <div className="button-output-container">
                    <div className="button-container">
                        <button onClick={handleAllEditors} className="button-admin">All Editors</button>
                        <br />

                        <button onClick={() => toggleInput('removeEditor')} className="button-admin">Remove editor</button>
                        {visibleInput === 'removeEditor' && (
                            <>
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Editor ID"
                                    onChange={(e) => setEditorId(e.target.value)}
                                /><br />
                                <button className="submit-button" onClick={handleRemoveEditor}>Submit</button>
                            </>
                        )}
                        <br />

                        <button onClick={() => toggleInput('allEditorProjects')} className="button-admin">All editor projects</button>
                        {visibleInput === 'allEditorProjects' && (
                            <>
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Editor ID"
                                    onChange={(e) => setEditorId(e.target.value)}
                                /><br />
                                <button className="submit-button" onClick={handleAllEditorProjects}>Submit</button>
                            </>
                        )}
                        <br />

                        <button onClick={() => toggleInput('removeProject')} className="button-admin">Remove project from editor</button>
                        {visibleInput === 'removeProject' && (
                            <>
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Editor ID"
                                    onChange={(e) => setEditorId(e.target.value)}
                                />
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Project ID"
                                    onChange={(e) => setProjectId(e.target.value)}
                                /><br />
                                <button className="submit-button" onClick={handleRemoveProject}>Submit</button>
                            </>
                        )}
                        <br />

                        <button onClick={() => toggleInput('allSharedWith')} className="button-admin">All shared with</button>
                        {visibleInput === 'allSharedWith' && (
                            <>
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Editor ID"
                                    onChange={(e) => setEditorId(e.target.value)}
                                />
                                <button className="submit-button" onClick={handleAllSharedWith}>Submit</button>
                            </>
                        )}
                        <br />

                        <button onClick={() => toggleInput('removeSharedEditor')} className="button-admin">Remove shared editor</button>
                        {visibleInput === 'removeSharedEditor' && (
                            <>
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Shared editor ID"
                                    onChange={(e) => setEditorId(e.target.value)}
                                />
                                <input
                                    type="text"
                                    className="text-box"
                                    placeholder="Project ID"
                                    onChange={(e) => setProjectId(e.target.value)}
                                /><br />
                                <button className="submit-button" onClick={handleRemoveSharedEditor}>Submit</button>
                            </>
                        )}

                    </div>

                    <div className="output_admin">
                        <h3>Results:</h3>
                        <textarea
                            className="textarea_admin"
                            placeholder='Here is the results'
                            value={output}
                            readOnly
                        />
                    </div>
                </div>
            </div>
        </div>

    );
};

export default AdminMain;

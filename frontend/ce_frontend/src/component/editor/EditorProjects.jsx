import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import TwinCodeBrand from '../common/TwinCodeBrand';
import './EditorStyle.css';

/*
TODO:
    - If shared project recieved, do we need to reflect this change immediately to the client page? 
*/

const EditorProject = () => {
    const navigate = useNavigate();
    const [projects, setProjects] = useState([]);
    const [sharedEditProjects, setSharedEditProjects] = useState([]);
    const [sharedViewProjects, setSharedViewProjects] = useState([]);
    const [currentSelected, setCurrentSelected] = useState(null);
    const [isModalVisible, setIsModalVisibile] = useState(null);
    const [visibleProject, setVisibleProject] = useState("private");
    const [change, setChange] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('editorAccessToken');
        if (!token) {
            navigate("/editor/login");
        } else {
            fetchEditorProjects(token);
            fetchEditorSharedEditProjects(token);
            fetchEditorSharedViewProjects(token);
        }
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
        const token = localStorage.getItem('editorAccessToken');
        const confirmLogout = window.confirm(`Are you sure you want to logout?`);
        if (confirmLogout) {
            try {
                const response = await fetchWithAuth('http://localhost:8080/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    }
                });

                if (response.ok) {
                    localStorage.removeItem('editorAccessToken');
                    localStorage.removeItem('editorRefreshToken');
                    localStorage.removeItem('email');
                    localStorage.removeItem('name');
                    localStorage.removeItem('editorRole');
                    navigate('/main');
                } else {
                    console.error('Failed to logout');
                }
            } catch (error) {
                console.error('Logout error:', error);
            }
        }
    };

    const fetchEditorProjects = async (token) => {
        try {
            const response = await fetchWithAuth("http://localhost:8080/project/client_projects", {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error('Failed to fetch editor projects');
            }

            const data = await response.json();

            console.log(data);
            setProjects(data);
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    };


    const fetchEditorSharedEditProjects = async (token) => {
        try {
            const response = await fetchWithAuth("http://localhost:8080/project/shared_projects_edit", {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error('Failed to fetch editor shared projects');
            }

            const data = await response.json();
            setSharedEditProjects(data);
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    };

    const fetchEditorSharedViewProjects = async (token) => {
        try {
            const response = await fetchWithAuth("http://localhost:8080/project/shared_projects_view", {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error('Failed to fetch editor shared projects');
            }

            const data = await response.json();
            setSharedViewProjects(data);
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    };

    const createProject = async () => {
        const token = localStorage.getItem('editorAccessToken');
        const projectName = prompt("Enter the name of the project: ");
        if (!projectName) {
            alert("Enter a valid project name");
            return;
        }

        try {
            const response = await fetchWithAuth("http://localhost:8080/project/create", {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify({ projectName }),
            });

            if (!response.ok) {
                throw new Error('Failed to create the project ' + projectName);
            }

            const data = await response.json();
            console.log(data);
            setProjects((prevProjects) => [...prevProjects, data]);
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    };

    const deleteProject = async () => {
        if (!currentSelected) {
            alert("Failed to Delete.\n\nNo project selected!");
            return;
        }

        const userConfirmed = window.confirm("Are you sure you want to delete the project " + currentSelected.name + "?");
        if (!userConfirmed) {
            return;
        }

        const projectId = currentSelected.id;
        const ownerId = currentSelected.client.id;

        const token = localStorage.getItem('editorAccessToken');

        try {
            const response = await fetchWithAuth(`http://localhost:8080/project/delete/${projectId}/${ownerId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            console.log(response);
            setProjects((prevProjects) => prevProjects.filter((project) => project.id !== projectId));
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    }

    const handleProjectClick = (project, event) => {
        const name = event.target.name;
        if (name == 'own') {
            localStorage.setItem('own', true);
        }

        if (name == 'view') {
            localStorage.removeItem('edit');
        } else {
            localStorage.setItem('edit', true);
        }
        const projectId = project.id;
        const ownerId = project.client.id;
        localStorage.setItem('project', projectId + "." + ownerId);
        navigate('/editor');
    };

    const submitChangeVisibility = async () => {
        if (change != "change") {
            alert("So you don't want to change it, ok!")
            setIsModalVisibile(false);
            setChange('');
            return;
        }

        const projectId = currentSelected.id;
        const token = localStorage.getItem('editorAccessToken');

        try {
            if (visibleProject == "private") {
                await fetchWithAuth(`http://localhost:8080/editor/share-project-public/${projectId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    },
                });
                alert("Visibility changed to public");
            } else {
                await fetchWithAuth(`http://localhost:8080/editor/remove-project-public/${projectId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    },
                });
                alert("Visibility changed to private");
            }
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }

        closeModal();
    }

    const changeVisibility = async () => {
        if (!currentSelected) {
            alert("Failed to Delete.\n\nNo project selected!");
            return;
        }
        fetchProjectVisibility();
        setIsModalVisibile(true);
    }

    const fetchProjectVisibility = async () => {
        const projectId = currentSelected.id;
        const token = localStorage.getItem('editorAccessToken');

        try {
            const response = await fetchWithAuth(`http://localhost:8080/editor/check-project-public/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            const data = await response.json();

            if (data) {
                setVisibleProject("public");
            } else {
                setVisibleProject("private");
            }
        } catch (error) {
            console.error("Error fetching project visibility:", error);
        }
    }

    const handleSelect = (project) => {
        setVisibleProject("private");
        setCurrentSelected(project);
    }


    const closeModal = () => {
        setIsModalVisibile(false);
        setChange('');
    };

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <TwinCodeBrand fallbackLabel="editor" />
                <div className="auth-buttons">
                    <button onClick={createProject} className="auth-button">New Project</button>
                    <button onClick={deleteProject} className="auth-button">Delete Project</button>
                    <button onClick={changeVisibility} className="auth-button">Change visibility</button>
                    <button onClick={handleLogout} className="auth-button">Log out</button>
                    <button onClick={() => navigate('/main')} className="auth-button">Home</button>
                </div>
            </header>

            <div>
                <h1 className="welcome-text" style={{ fontFamily: "Times New Roman", color: '#353d4c', marginLeft: '40%' }}>
                    Editor Projects
                </h1>
                <hr />
                <h2 style={{ marginLeft: '12px' }}>My Projects</h2>
                <div className="project_show_editor" style={{ marginLeft: '10px', width: '97%' }}>
                    {projects.length === 0 ? (
                        <p>Create a new project!</p>
                    ) : (
                        <div style={{ display: 'flex', flexWrap: 'wrap' }}> {/* Use flexbox to display buttons horizontally */}
                            {projects.map((project, index) => (
                                <button
                                    key={index} // Add a key prop for better performance
                                    className="projectButton"
                                    value={project}
                                    name='own'
                                    onDoubleClick={(e) => handleProjectClick(project, e)}
                                    onClick={() => handleSelect(project)}
                                    style={{ marginRight: '10px', marginBottom: '10px' }} // Add some margin for spacing
                                >
                                    {project.name}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
                <br />
                {isModalVisible && (
                    <div className="modal-overlay">
                        <div className="modal">
                            <h2>Change visibility</h2>
                            <p>The current visibility of the project <i>"{currentSelected.name}"</i> is:&nbsp;
                                <i><b>
                                    {visibleProject == "private" ? (
                                        <span style={{ color: "red" }}>{visibleProject}</span>
                                    ) : (
                                        <span style={{ color: "green" }}>{visibleProject}</span>
                                    )
                                    }
                                </b></i>
                            </p>
                            <p>If you want to change it, type the word '<i>change</i>' in the textbox and click <b>Change</b></p>
                            <br />
                            <input
                                type="text"
                                className="modal_input"
                                value={change}
                                onChange={(e) => setChange(e.target.value)}
                                placeholder="change"
                            />
                            <div className="modal-buttons">
                                <button className="submit-button" onClick={submitChangeVisibility}>Change</button>
                                <button className="submit-button" onClick={closeModal}>Close</button>
                            </div>
                        </div>
                    </div>
                )}

                <h2 style={{ marginLeft: '12px' }}>Shared (to edit)</h2>
                <div className="project_show_editor" style={{ marginLeft: '10px', width: '97%' }}>
                    {sharedEditProjects.length === 0 ? (
                        <p>No shared projects found.</p>
                    ) : (
                        <div style={{ display: 'flex', flexWrap: 'wrap' }}> {/* Use flexbox to display buttons horizontally */}
                            {sharedEditProjects.map((project, index) => (
                                <button
                                    key={index} // Add a key prop for better performance
                                    className="projectButton"
                                    value={project}
                                    name='edit'
                                    onDoubleClick={(e) => handleProjectClick(project, e)}
                                    onClick={() => handleSelect(project)}
                                    style={{ marginRight: '10px', marginBottom: '10px' }} // Add some margin for spacing
                                >
                                    {project.name}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
                <br />

                <h2 style={{ marginLeft: '12px' }}>Shared (to view)</h2>
                <div className="project_show_editor" style={{ marginLeft: '10px', width: '97%' }}>
                    {sharedViewProjects.length === 0 ? (
                        <p> No shared projects found.</p>
                    ) : (
                        <div style={{ display: 'flex', flexWrap: 'wrap' }}> {/* Use flexbox to display buttons horizontally */}
                            {sharedViewProjects.map((project, index) => (
                                <button
                                    key={index} // Add a key prop for better performance
                                    className="projectButton"
                                    value={project}
                                    name='view'
                                    onDoubleClick={(e) => handleProjectClick(project, e)}
                                    onClick={() => handleSelect(project)}
                                    style={{ marginRight: '10px', marginBottom: '10px' }} // Add some margin for spacing
                                >
                                    {project.name}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default EditorProject;

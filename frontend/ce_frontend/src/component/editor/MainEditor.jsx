import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import TwinCodeBrand from '../common/TwinCodeBrand';

import CppIcon from './images/cpp.png';
import JavaIcon from './images/java.png';
import PythonIcon from './images/pyth.png';

import addFolderIcon from './images/new-folder.png';  
import addFileIcon from './images/new-file.png'; 
import deleteIcon from './images/delete.png'; 

import './EditorStyle.css';
import './init'

import SockJS from "sockjs-client";
import { Stomp } from "@stomp/stompjs";

const EditorMain = () => {
    const navigate = useNavigate();
    const [code, setCode] = useState();
    const [output, setOutput] = useState('');
    const [language, setLanguage] = useState('cpp');
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [files, setFiles] = useState([]);
    const [currentSelected, setCurrentSelected] = useState(null);
    const editorRef = useRef(null);
    const [stompClient, setStompClient] = useState(null);
    const [commandOutput, setCommandOutput] = useState('');

    const [commandLineVisible, setCommandLineVisible] = useState(false);
    const [command, setCommand] = useState("");
    const [isVcsProject, setIsVcsProject] = useState(false);

    const [isModalVisible, setModalVisible] = useState(false)
    const [email, setEmail] = useState('');
    const [mode, setMode] = useState('view');
    const [canEdit, setCanEdit] = useState(false);

    const [isCommentButtonVisible, setCommentButtonVisible] = useState(false);
    const [selectedLines, setSelectedLines] = useState({ startLine: 0, endLine: 0 });

    const [showComments, setShowComments] = useState(false);
    const [comments, setComments] = useState([]);

    const [isModalCommentVisible, setModalCommentVisible] = useState(false);
    const [comment, setComment] = useState('');
    const [lines, setLines] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('editorAccessToken');
        
        if (!token) {
            navigate("/editor/login");
        } else {
            if (!localStorage.getItem('project')) {
                navigate('/editor/projects')
                return;
            }
            if (localStorage.getItem('edit')) {
                setCanEdit(true);
                handleIsVCSProject();
            }
            fetchEditorDirectory(token);
        }
    }, [navigate]);

    const webSocketConnection = (snippetId) => {
        const socket = new SockJS("http://localhost:8080/registerWS");
        const client = Stomp.over(socket);

        client.connect(
            {},
            (frame) => {
                console.log("Connected to WebSocket:", frame);

                client.subscribe(`/topic/snippet/${snippetId}`, async (resBody) => {
                    const message = JSON.parse(resBody.body);
                    if (message.token != localStorage.getItem('editorAccessToken')) {
                        setCode(message.change);
                        console.log("Code changed~!")
                    }
                });

                setStompClient(client);
            },
            (error) => {
                console.error("STOMP error:", error);
            }
        );

        return () => {
            if (stompClient !== null) {
                stompClient.disconnect();
            }
        };
    };

    const sendChange = (change, token, snippetId) => {
        const headers = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        };

        if (stompClient !== null) {
            stompClient.send("/app/editor/change", headers, JSON.stringify({
                snippetId: snippetId,
                change: change,
                token: token
            }));
        }
    };

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

    const fetchEditorDirectory = async (token) => {
        const [projectId, ownerId] = localStorage.getItem('project').split('.'); // TODO: extract function
        if (!ownerId || !projectId) {
            console.error("ownerId or projectId is not defined");
            navigate("/editor/projects");
            return;
        }

        try {
            const response = await fetchWithAuth(`http://localhost:8080/editor/directory/${ownerId}/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error('Failed to fetch editor directory');
            }

            const data = await response.json();

            const transformedFiles = transformToFrontendForm(data.tree);
            setFiles(transformedFiles);
        } catch (error) {
            console.error("Error fetching editor directory:", error);
        }
    };

    const transformToFrontendForm = (tree) => {
        const transformNode = (id, tree) => {
            const fileNode = tree[id];
            if (!fileNode) return null;

            const { children, name, parentId } = fileNode;

            return {
                id: id,
                name: name || '',
                isFolder: true,
                parent: parentId !== null ? parentId : null,
                children: children.map(file => {
                    if (file.isFolder) {
                        return transformNode(file.id, tree);
                    } else {
                        return {
                            id: file.id,
                            name: file.name,
                            isFolder: false,
                            parent: id
                        };
                    }
                })
            };
        };

        return [transformNode(0, tree)];
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

    const handleProjectSharing = async (emailToShareWith, confirmEdit) => {
        const token = localStorage.getItem('editorAccessToken');
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        if (confirmEdit == 'edit') {
            try {
                const response = await fetchWithAuth(`http://localhost:8080/editor/share_project_edit/${emailToShareWith}/${ownerId}/${projectId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    }
                });
                if (response.ok) {
                    alert("Project: " + projectId + " with owner: " + ownerId + " have been shared to the client: " + emailToShareWith + " successfully!");
                } else if (response.status == 403) {
                    console.error("You aren't allowed to share this project!");
                } else {
                    console.error("Not Found");
                }
            } catch (error) {
                console.error("Error while sharing the project:", error);
            }
        } else {
            try {
                const response = await fetchWithAuth(`http://localhost:8080/editor/share_project_view/${emailToShareWith}/${ownerId}/${projectId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    }
                });
                if (response.ok) {
                    alert("Project: " + projectId + " with owner: " + ownerId + " have been shared to the client: " + emailToShareWith + " successfully!");
                } else if (response.status == 403) {
                    console.error("You aren't allowed to share this project!");
                } else {
                    console.error("Not Found");
                }
            } catch (error) {
                console.error("Error while sharing the project:", error);
            }
        }
    }

    const handleEditorChange = (async (value) => {
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        const token = localStorage.getItem('editorAccessToken');

        sendChange(value, token, currentSelected.id);
        setCode(value); 
        try {
            await fetchWithAuth(`http://localhost:8080/snippet/update/${currentSelected.id}/${currentSelected.name}/${ownerId}/${projectId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: value
            });
            console.log(`Snippet ${currentSelected.id}_${currentSelected.name} updated`);
        } catch (error) {
            console.error('Error updating snippet:', error);
        }
    });

    const handleCodeExecute = () => {
        const token = localStorage.getItem('editorAccessToken');
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        const reqBody = {
            code: code,
            language: language
        };
        fetchWithAuth("http://localhost:8080/snippet/execute", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`,
            },
            body: JSON.stringify(reqBody),
        })
            .then(response => response.text())
            .then(data => {
                console.log('Response from server: ', data);
                setOutput(data);
            })
            .catch(error => {
                console.log("Error while sending Code: ", error);
                setOutput("Error while executing code.");
            });
    };

    const toggleSidebar = () => {
        setSidebarOpen(!sidebarOpen);
    };

    const handleFolderCreation = async (folderName, parentFolder) => {
        const token = localStorage.getItem('editorAccessToken');
        console.log("----------->" + folderName);
        console.log("----------->" + parentFolder.id);
        const [projectId, ownerId] = localStorage.getItem('project').split('.');

        const reqBody = {
            name: folderName,
            parentId: parentFolder.id
        };

        try {
            const response = await fetchWithAuth(`http://localhost:8080/folder/create/${ownerId}/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify(reqBody)
            });
            const folderId = await response.text();
            console.log("Folder with name: " + folderName + " and id: " + folderId + " created successfully!");
            return folderId;
        } catch (error) {
            console.error("Error while sending Code: ", error);
            return null;
        }
    };

    const handleFileCreation = async (snippetName, parent) => {
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        const token = localStorage.getItem('editorAccessToken');
        const reqBody = {
            name: snippetName,
            parentId: parent.id
        }
        try {
            const response = await fetchWithAuth(`http://localhost:8080/snippet/create/${ownerId}/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
                body: JSON.stringify(reqBody)
            });
            const snippet = await response.json();
            console.log(snippet);
            console.log("Snippet with name: " + snippetName + " and id: " + snippet.id + " created successfully!");
            return snippet;
        } catch (error) {
            console.log("Error while sending Code: ", error);
            return null;
        }
    }

    const addFile = async () => {
        const newFileName = prompt("Enter the name of the new file:");
        const pattern = /^[a-zA-Z][a-zA-Z0-9 ]*[a-zA-Z0-9]\.(cpp|java|py)$/;
        if (!pattern.test(newFileName)) {
            console.log("Error creating file " + newFileName);
            alert("Invalid file name or extension\n\n- Enter a file name: \n\t With no special characters\n\t Begins with alphabet\n\t No spaces before the extension\n\t ends with one of the extensions: .cpp .java .py");
            return;
        }

        if (currentSelected && currentSelected.isFolder) {
            const parentFolder = currentSelected;

            if (isDuplicateFileName(newFileName, parentFolder, false)) {
                alert(`A file named "${newFileName}" already exists in this folder\nPick a different name.`);
                return;
            }

            const snippet = await handleFileCreation(newFileName, parentFolder);
            setFiles((prevFiles) => {
                const updatedFiles = addFileToFolder(prevFiles, snippet.id, newFileName, currentSelected);
                console.log(updatedFiles);
                return updatedFiles;
            });
        }
    };

    const findFile = (files, fileName) => {
        for (let file of files) {
            if (file.name === fileName) {
                return file;
            }
            if (file.isFolder && file.children) {
                const foundFile = findFile(file.children, fileName);
                if (foundFile) {
                    return foundFile;
                }
            }
        }
        return null;
    };

    const isDuplicateFileName = (fileName, parentFolder, isFolder) => {
        if (parentFolder.children) {
            return parentFolder.children.some(file => file.name === fileName && file.isFolder == isFolder);
        }
        return false;
    };

    const getAllParents = (files, targetFile, parents = []) => {
        for (const file of files) {
            if (file.isFolder) {
                if (file.children) {
                    if (file.children.some(child => child.name === targetFile.name)) {
                        parents.push(file);
                        return getAllParents(files, file, parents);
                    } else {
                        const result = getAllParents(file.children, targetFile, parents);
                        if (result) {
                            if (!parents.includes(file)) {
                                parents.push(file);
                            }
                            return parents;
                        }
                    }
                }
            }
        }
        return parents.length ? parents : null;
    };

    const addFolder = async () => {
        const newFolderName = prompt("Enter the name of the new folder:");
        if (newFolderName) {
            const parentFolder = currentSelected && currentSelected.isFolder ? currentSelected : files[0];
            if (isDuplicateFileName(newFolderName, parentFolder, true)) {
                alert(`A folder named "${newFolderName}" already exists in this folder\nPick a different name.`);
                return;
            }
            var folderId = await handleFolderCreation(newFolderName, parentFolder)
            setFiles((prevFiles) => {
                return addFolderToFolder(prevFiles, newFolderName, folderId, parentFolder);
            });
        }
    };

    const addFileToFolder = (files, snippetId, fileName, parentFolder) => {
        return files.map((file) => {
            if (file === parentFolder) {
                return {
                    ...file,
                    children: [...(file.children || []), { id: snippetId, parent: parentFolder.id, name: fileName, isFolder: false }], // update parentId to parent?
                };
            } else if (file.isFolder && file.children) {
                return {
                    ...file,
                    children: addFileToFolder(file.children, snippetId, fileName, parentFolder),
                };
            }
            return file;
        });
    };

    const addFolderToFolder = (files, folderName, folderId, parentFolder) => {
        return files.map((file) => {
            if (file === parentFolder) {
                return {
                    ...file,
                    children: [...(file.children || []), { id: folderId, name: folderName, isFolder: true, parent: parentFolder.id }], //updated: added parent
                };
            } else if (file.isFolder && file.children) {
                return {
                    ...file,
                    children: addFolderToFolder(file.children, folderName, folderId, parentFolder),
                };
            }
            return file;
        });
    };

    const fetchSnippetContent = async (item) => {
        const token = localStorage.getItem('editorAccessToken');
        const name = encodeURIComponent(item.name);
        const id = encodeURIComponent(item.id);
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        try {
            const response = await fetchWithAuth(`http://localhost:8080/snippet/content/${id}/${name}/${ownerId}/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                },
            });
            const content = await response.text();
            console.log("Snippet with name: " + item.name + " and id: " + item.id + " loaded successfully!");
            return content;
        } catch (error) {
            console.error("Error while sending Code: ", error);
            return null;
        }
    }

    const handleItemClick = async (item) => {
        setCurrentSelected(item);
        if (!item.isFolder) {
            webSocketConnection(item.id);
            const content = await fetchSnippetContent(item);
            const fileExtension = item.name.split('.').pop();
            switch (fileExtension) {
                case 'cpp':
                    setLanguage('cpp');
                    break;
                case 'java':
                    setLanguage('java');
                    break;
                case 'py':
                    setLanguage('python');
                    break;
                default:
                    setLanguage('plaintext');
            }
            setCode(content);
        }
    };

    const deleteItem = (item) => {
        const [projectId, ownerId] = localStorage.getItem('project').split('.');
        const confirmDelete = window.confirm(`Are you sure you want to delete "${item.name}"?`);
        if (confirmDelete) {
            const token = localStorage.getItem('editorAccessToken');
            console.log(item);
            const requestBody = {
                parentId: item.parent,
                name: item.name,
                id: item.id
            };

            if (item.isFolder) {
                fetchWithAuth(`http://localhost:8080/folder/delete/${ownerId}/${projectId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(requestBody)
                })
                    .then((response) => {
                        if (!response.ok) {
                            throw new Error('Failed to delete folder');
                        }
                        alert(item.name + " deleted successfully!");
                    })
                    .then(() => {
                        setFiles((prevFiles) => deleteFromFolder(prevFiles, item));
                        setCurrentSelected(null);
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                    });
            } else {
                fetchWithAuth(`http://localhost:8080/snippet/delete/${ownerId}/${projectId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(requestBody)
                })
                    .then((response) => {
                        if (!response.ok) {
                            throw new Error('Failed to delete folder');
                        }
                        alert(item.name + " deleted successfully!");
                    })
                    .then(() => {
                        setFiles((prevFiles) => deleteFromFolder(prevFiles, item));
                        setCurrentSelected(null);
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                    });
            }
        }
    };

    const deleteFromFolder = (files, itemToDelete) => {
        return files.reduce((acc, file) => {
            if (file === itemToDelete) {
                return acc;
            }

            if (file.isFolder && file.children) {
                return [
                    ...acc,
                    {
                        ...file,
                        children: deleteFromFolder(file.children, itemToDelete),
                    }
                ];
            }

            return [...acc, file];
        }, []);
    };

    const [openFolders, setOpenFolders] = useState({});

    const handleFolderClick = (file) => {
        handleItemClick(file); 
        setOpenFolders((prevState) => ({
            ...prevState,
            [file.name]: !prevState[file.name], 
        }));
    };

    const getFileIcon = (fileName) => {
        if (fileName.endsWith('.cpp')) return CppIcon;
        if (fileName.endsWith('.java')) return JavaIcon;
        if (fileName.endsWith('.py')) return PythonIcon;
        return null; // No icon for other file types
    };


    const renderFiles = (filesToRender) => {
        return filesToRender.map((file, index) => {
            if (file.isFolder && !file.name) {
                return (
                    <div key={index} style={{ paddingLeft: '20px' }}>
                        {renderFiles(file.children)}
                    </div>
                );
            }

            const isSelected = file === currentSelected;

            return (
                <div key={index}>
                    <span
                        className="file-item"
                        onClick={() => file.isFolder ? handleFolderClick(file) : handleItemClick(file)}
                        style={{
                            fontWeight: file.isFolder ? 'bold' : 'normal',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            backgroundColor: isSelected ? '#d0eaff' : 'transparent', // Set background color if selected
                            padding: '4px', // Optional: Padding for better look when selected
                            borderRadius: '4px', // Optional: Rounded corners for the selected highlight
                        }}
                    >
                        {file.isFolder && (
                            <span style={{ marginRight: '5px' }}>
                                {openFolders[file.name] ? '▼' : '▶'}
                            </span>
                        )}
                        {/* Display icon if file has a recognized type */}
                        {!file.isFolder && getFileIcon(file.name) && (
                            <img
                                src={getFileIcon(file.name)}
                                alt={`${file.name} icon`}
                                style={{ width: '16px', height: '16px', marginRight: '5px', opacity: 0.8 }}
                            />
                        )}
                        {file.name}
                    </span>
                    {file.isFolder && file.children && openFolders[file.name] && (
                        <div style={{ paddingLeft: '20px' }}>
                            {renderFiles(file.children, openFolders, handleFolderClick, currentSelected)}
                        </div>
                    )}
                </div>
            );
        });
    };

    const handleFileManagerClick = (e) => {
        if (e.target.classList.contains('fileManager')) {
            setCurrentSelected(null);
        }
    };

    const handleProjectsClick = () => {
        localStorage.removeItem('project');
        localStorage.removeItem('vcs');
        localStorage.removeItem('edit');
        localStorage.removeItem('own');
        navigate('/editor/projects');
    }

    const handleEditorMount = (editor) => {
        editorRef.current = editor;

        editor.onDidChangeCursorSelection(() => {
            const selection = editor.getSelection();

            const startLine = selection.startLineNumber;
            const endLine = selection.endLineNumber;

            if (startLine !== endLine || selection.startColumn !== selection.endColumn) {
                setSelectedLines({ startLine, endLine });
                setCommentButtonVisible(true);
            } else {
                setCommentButtonVisible(false);
            }
        });
    }

    const handleCommentClick = () => {
        const { startLine, endLine } = selectedLines;
        let currentLines = startLine;
        if (startLine != endLine) {
            currentLines = startLine + " " + endLine;
        }
        setLines(currentLines);
        setCommentButtonVisible(false);
        setModalCommentVisible(true);
    };

    const handleCommentSubmit = async () => {
        const { startLine, endLine } = selectedLines;
        const body = {
            comment: comment,
            start: startLine,
            end: endLine
        }
        const token = localStorage.getItem('editorAccessToken');
        const projectId = localStorage.getItem('project').split('.')[0];
        const response = await fetchWithAuth(`http://localhost:8080/snippet/comment/${projectId}/${currentSelected.id}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(body)
        });
        if (response.ok) {
            alert("Commented on snippet: " + currentSelected.name);
        } else {
            console.error("Error while commenting");
        }
        closeCommentModal();
    }

    const handleAllCommentsClick = async () => {
        setShowComments(!showComments);
        const token = localStorage.getItem('editorAccessToken');
        const projectId = localStorage.getItem('project').split('.')[0];
        if (!currentSelected || currentSelected.isFolder) {
            alert("Choose some file to show the comments");
            setShowComments(false);
            return;
        }
        const response = await fetchWithAuth(`http://localhost:8080/snippet/get-comments/${projectId}/${currentSelected.id}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
        });
        const data = await response.json();
        setComments(data);
    }

    const closeCommentModal = () => {
        setModalCommentVisible(false);
        setComment('');
    };

    //Command-line
    const handleCommandInput = (event) => {
        const text = event.target.textContent;
        const words = text.split(" ");
        if (words.length > 0) {
            const firstWord = words[0];
            event.target.innerHTML = `<span style="color: yellow">${firstWord}</span> ${words.slice(1).join(" ")}`;
        }

        setCommand(event.target.value);
    };

    const handleCommandSubmit = (e) => {
        if (e.key === 'Enter') {
            handleCommandExecution(command);
            setCommand('');
        }
    };

    const handleIsVCSProject = async () => {
        const token = localStorage.getItem('editorAccessToken');
        const projectId = localStorage.getItem('project').split('.')[0];
        const response = await fetchWithAuth(`http://localhost:8080/vcs/check-vcs/${projectId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
        })
        const data = await response.json();
        setIsVcsProject(data);
    }

    const handleCommandExecution = async (cmd) => {
        console.log("Executing command:", cmd);
        const cmdd = String(cmd);
        if (cmdd.trim() == "clear") {
            setCommandOutput("");
            return;
        }
        const token = localStorage.getItem('editorAccessToken');
        const projectId = localStorage.getItem('project').split('.')[0];
        if (!cmdd.trim().startsWith("vcs ")) {
            setCommandOutput("Start the command with 'vcs'");
            return;
        }

        if (cmdd.trim() == "vcs init") {                                             //init
            await fetchWithAuth(`http://localhost:8080/vcs/init/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
            })
                .then((response) => {
                    if (!response.ok) {
                        throw new Error('Failed to intitialize .vcs folder');
                    }
                    setCommandOutput(".vcs directory initialized successfully");
                })
                .catch((error) => {
                    console.error('Error:', error);
                });
            setIsVcsProject(true);
        } else if (cmdd.trim() == "vcs delete") {                                    //delete
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const areYouSure = window.confirm(`Are you sure you want to delete the .vcs folder?`);
            if (!areYouSure) {
                return;
            }
            await fetchWithAuth(`http://localhost:8080/vcs/delete/${projectId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
            })
                .then((response) => {
                    if (!response.ok) {
                        throw new Error('Failed to delete .vcs folder');
                    }
                    setCommandOutput(" .vcs folder delete successfully!");
                })
                .catch((error) => {
                    console.error('Error:', error);
                });
            localStorage.removeItem("vcs");
        } else if (cmdd.trim() == "vcs status") {                                    //status
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const response = await fetchWithAuth(`http://localhost:8080/vcs/status/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
            })
            if (!response.ok) {
                throw new Error('Failed to delete .vcs folder');
            }
            const data = await response.json();
            if (data.untracked.length == 0 && data.tracked.length == 0) {
                setCommandOutput("No changes, working on clean tree");
                return;
            }
            let result = "";
            if (data.untracked.length != 0) {
                result = "Untracked changes: \n";
                data.untracked.map(untrachedFile => {
                    result += "    -" + untrachedFile + "\n";
                })
            }
            if (data.tracked.length != 0) {
                if (result.length > 0) {
                    result += '\n';
                }
                result += "Tracked changes: \n";
                data.tracked.map(trachedFile => {
                    result += "    -" + trachedFile + "\n";
                })
            }

            setCommandOutput(result);
        } else if (cmdd.trim().startsWith("vcs add ")) {                             //add
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const files = cmdd.trim().replace("vcs add ", "").split(" ");
            const response = await fetchWithAuth(`http://localhost:8080/vcs/add/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ files })
            });
            const data = await response.json();
            let output = "Traking changes:\n"
            data.map((fileName) => {
                output += "\t- " + fileName + "\n"
            });
            setCommandOutput(output);
        } else if (cmdd.trim().startsWith("vcs commit -m ")) {                             //commit
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const message = cmdd.trim().replace("vcs commit -m ", "");
            const response = await fetchWithAuth(`http://localhost:8080/vcs/commit/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(message)
            });
            const data = await response.json();
            let output = "Commiting changes:\n"
            data.map((fileName) => {
                output += "\t- " + fileName + "\n"
            });
            setCommandOutput(output);
        } else if (cmdd.trim() == "vcs log") {                             //log
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const response = await fetchWithAuth(`http://localhost:8080/vcs/log/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
            });
            if (!response.ok) {
                throw new Error('Failed to load the log');
            }
            const log = await response.text();
            setCommandOutput(log);
            console.log("Log loaded successfully")
        } else if (cmdd.trim().startsWith("vcs revert ")) {                             //revert
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const commitId = cmdd.replace("vcs revert ", "");
            await fetchWithAuth(`http://localhost:8080/vcs/revert/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(commitId)
            })
                .then((response) => {
                    if (!response.ok) {
                        throw new Error('Failed to revert to the commit with Id ' + commitId);
                    }
                    setCommandOutput("Reverted to commit with id " + commitId + " successfully");
                    fetchEditorDirectory(token);
                    if (currentSelected && !currentSelected.isFolder) {
                        handleItemClick(currentSelected);
                    }
                })
                .catch((error) => {
                    console.error('Error:', error);
                });
            console.log(files);
        } else if (cmdd.trim() == "vcs branch") {                             //all branches 
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const response = await fetchWithAuth(`http://localhost:8080/vcs/branches/${projectId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
            });

            const data = await response.json();
            let branches = "";
            data.map((branchName) => {
                branches += branchName + "\n";
            })
            setCommandOutput(branches);
        } else if (cmdd.trim().startsWith("vcs branch -d")) {                             //delete branch
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            if (!cmdd.trim().startsWith("vcs branch -d ")) {
                setCommandOutput("To delete a branch, type 'vcs branch -d <branch-name>'");
                return;
            }
            const rcmd = cmdd.replace("vcs branch -d ", "");
            if (rcmd.trim().includes(" ")) {
                setCommandOutput(rcmd);
                setCommandOutput("The branch name shouldn't contain any spaces!")
                return;
            }
            const branchName = rcmd.trim();
            await fetchWithAuth(`http://localhost:8080/vcs/delete-branch/${projectId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(branchName)
            });
            setCommandOutput("Deleted branch " + branchName);
        } else if (cmdd.trim().startsWith("vcs branch ")) {                             //create branch
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const rcmd = cmdd.replace("vcs branch ", "");
            if (rcmd.trim().includes(" ")) {
                setCommandOutput("The branch name shouldn't contain any spaces!")
                return;
            }
            const branchName = rcmd.trim();
            await fetchWithAuth(`http://localhost:8080/vcs/create-branch/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(branchName)
            });
            setCommandOutput("Created a new branch " + branchName);
        } else if (cmdd.trim().startsWith("vcs fork ")) {                             //fork
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const rcmd = cmdd.replace("vcs fork ", "");
            if (!rcmd.trim().includes(" ") || rcmd.trim().split(" ").length != 2) {
                setCommandOutput("To fork a repository, type 'vcs fork <owner-email> <project-name>'");
                return;
            }
            const [ownerEmail, projectName] = rcmd.trim().split(" ");
            const body = {
                owner: ownerEmail,
                project: projectName
            }
            await fetchWithAuth(`http://localhost:8080/vcs/fork`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(body)
            });
            setCommandOutput("Successfully forked the repositor with name: " + projectName + " and owner email " + ownerEmail);
        } else if (cmdd.trim().startsWith("vcs checkout -b")) {                             //checkout create
            if (!cmdd.trim().startsWith("vcs checkout -b ")) {
                setCommandOutput("To create and switch to another new branch, type 'vcs checkout -b <branch-name>");
                return;
            }
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const rcmd = cmdd.replace("vcs checkout -b ", "");
            if (rcmd.trim().includes(" ")) {
                setCommandOutput("Branch name shouldn't contain any spaces!")
                return;
            }
            const branchName = rcmd.trim();
            await fetchWithAuth(`http://localhost:8080/vcs/checkout-create/${projectId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(branchName)
            });
            setCommandOutput("Created the branch: " + branchName + " and switched to it");
        } else if (cmdd.trim().startsWith("vcs checkout ")) {                             //checkout
            if (!isVcsProject) {
                setCommandOutput("Not a vcs project");
                return;
            }
            const rcmd = cmdd.replace("vcs checkout ", "");
            if (rcmd.trim().includes(" ")) {
                setCommandOutput("Branch name shouldn't contain any spaces!")
                return;
            }
            const branchName = rcmd.trim();
            await fetchWithAuth(`http://localhost:8080/vcs/checkout/${projectId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(branchName)
            });
            fetchEditorDirectory(token);
            if (currentSelected && !currentSelected.isFolder) {
                handleItemClick(currentSelected);
            }
            setCommandOutput("Switched to branch: " + branchName);
        } else if (cmdd.trim() == "vcs help") {
            setCommandOutput("Available commands are:\n\t- init\n\t- delete\n\t- status\n\t- add <files>\n\t- commit -m <message>\n\t- log\n\t- revert <commitId>\n\t- branch\n\t- branch <branch-name>\n\t- branch -m <branch-name>\n\t- checkout <branch-name>\n\t- checkout -b <branch-name>\n\t- fork <owner-email> <project-name>");
        }
        else {
            setCommandOutput("Not a vcs command, type 'vcs help' to get the list of the available commands");
        }

        if (cmdd.trim() == "hello") {
            return "Hey, you!";
        }
    };

    const handleForkProject = async () => {
        const confirm = window.confirm("Are you sure you want ot fork this project?")
        if (!confirm) {
            return;
        }
        const projectId = localStorage.getItem('project').split('.')[0];
        const token = localStorage.getItem('editorAccessToken');
        await fetchWithAuth(`http://localhost:8080/vcs/fork/${projectId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error('Failed to fork the project with id ' + projectId);
                }
                setCommandOutput("Forked the project successfully");
            })
            .catch((error) => {
                console.error('Error:', error);
            });
    }

    const openModal = () => {
        setModalVisible(true);
    };

    const closeModal = () => {
        setModalVisible(false);
        setMode('view');
        setEmail('');
    };

    const handleSubmit = () => {
        handleProjectSharing(email, mode);
        closeModal();
    };

    return (
        <div className="welcome-page">
            <header className="welcome-header">
                <TwinCodeBrand fallbackLabel="editor" />
                <div className="auth-buttons">
                    {localStorage.getItem('own') && (
                        <div>
                            <button onClick={openModal} className="auth-button">Share Project</button>
                        </div>
                    )
                    }
                    <button onClick={handleProjectsClick} className="auth-button">Projects</button>
                    {!localStorage.getItem('edit') && (
                        <button onClick={handleForkProject} className="auth-button">Fork</button>
                    )}
                    {canEdit && (
                        <button onClick={() => setCommandLineVisible(!commandLineVisible)} className="auth-button">VCS</button>
                    )
                    }

                    <button onClick={handleLogout} className="auth-button">Log out</button>
                    <button className="auth-button" onClick={() => navigate('/')}>Home</button>
                </div>
            </header>
            <br />
            <div className="container">
                <div className="header">
                    <button onClick={toggleSidebar} className="buttonEditor">File</button>
                    {canEdit && (
                        <div>
                            <button onClick={handleCodeExecute} className="buttonEditor runButtonEditor">Run</button>
                        </div>
                    )
                    }
                    {currentSelected && !currentSelected.isFolder && (
                        <button onClick={handleAllCommentsClick} className="buttonEditor">All comments</button>
                    )}

                    {isModalVisible && (
                        <div className="modal-overlay">
                            <div className="modal">
                                <h2>Project sharing</h2>
                                <br />
                                <input
                                    type="email"
                                    className="modal_input"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="Email to share with"
                                />
                                <select
                                    className="dropdown"
                                    value={mode}
                                    onChange={(e) => setMode(e.target.value)}
                                >
                                    <option value="edit">Edit</option>
                                    <option value="view">View</option>
                                </select>
                                <div className="modal-buttons">
                                    <button className="submit-button" onClick={handleSubmit}>Share</button>
                                    <button className="submit-button" onClick={closeModal}>Close</button>
                                </div>
                            </div>
                        </div>
                    )}



                    {isCommentButtonVisible && (
                        <button
                            onClick={handleCommentClick}
                            className="buttonEditor"
                            style={{
                                margin: "-2px", 
                                background: "#4a3e3e",
                                color: "white",
                            }}
                        >
                            Comment
                        </button>
                    )}

                    {isModalCommentVisible && (
                        <div className="modal-overlay">
                            <div className="modal">
                                <h2>Snippet comment</h2>
                                <p>write comment for lines: {lines}</p>
                                <br />
                                <input
                                    type="text"
                                    className="modal_input"
                                    value={comment}
                                    onChange={(e) => setComment(e.target.value)}
                                    placeholder="Write some comment"
                                />
                                <div className="modal-buttons">
                                    <button className="submit-button" onClick={handleCommentSubmit}>Comment</button>
                                    <button className="submit-button" onClick={closeCommentModal}>Close</button>
                                </div>
                            </div>
                        </div>
                    )}

                </div>

                <div className="editorContainer">
                    {sidebarOpen && (
                        <div className="fileManager" style={{ width: '300px', borderRight: '1px solid #ccc', paddingRight: '10px' }} onClick={handleFileManagerClick}>
                            <h3>File Manager</h3>
                            {canEdit && (
                                <div>
                                    <button onClick={addFolder} className="icon-button" style={{ marginRight: '5px' }}>
                                        <img
                                            src={addFolderIcon}
                                            alt="Add Folder"
                                            style={{ width: '25px', height: '25px', marginRight: '10px' }}
                                        />
                                    </button>

                                    <button
                                        onClick={addFile}
                                        disabled={!currentSelected || !currentSelected.isFolder}
                                        className="icon-button"
                                        style={{ marginRight: '5px' }}
                                    >
                                        <img
                                            src={addFileIcon}
                                            alt="Add File"
                                            style={{ width: '25px', height: '25px', marginRight: '5px' }}
                                        />
                                    </button>

                                    <button
                                        onClick={() => deleteItem(currentSelected)}
                                        disabled={!currentSelected}
                                        className="icon-button"
                                        style={{ marginRight: '10px' }}
                                    >
                                        <img
                                            src={deleteIcon}
                                            alt="Delete"
                                            style={{ width: '24px', height: '24px', marginRight: '5px' }}
                                        />
                                    </button>
                                </div>
                            )}
                            <hr />
                            <div>
                                {renderFiles(files)}
                            </div>
                        </div>
                    )}

                    <div className="editor">
                        {currentSelected && !currentSelected.isFolder ? (
                            <Editor
                                height='85vh'
                                width='90%'
                                value={code}
                                onChange={handleEditorChange}
                                language={language}
                                onMount={handleEditorMount}
                                options={{ readOnly: !canEdit }}

                            />
                        ) : (
                            <div style={{ padding: '20px', textAlign: 'center', fontStyle: 'italic' }}>
                                Choose a file to edit
                            </div>
                        )}
                    </div>

                    <div className="output">
                        <h3>Output:</h3>
                        <textarea
                            className="textarea"
                            value={output}
                            readOnly
                        />
                    </div>
                </div>

                {showComments && (
                    <div className="comments_section" style={{ marginLeft: '10px', width: '97%' }}>
                        <h3 className="comments-heading">Comments:</h3>
                        <div className="comments-buttons">
                            {comments && comments.length > 0 ? (
                                comments.map((comment, index) => (
                                    <button key={index} className="comment-button">
                                        <pre>
                                            <span className="label">Comment :</span> {comment.content} <br />
                                            <span className="label">Editor  :</span> {comment.editorEmail} <br />
                                            <span className="label">Date    :</span> {comment.date} <br />
                                            <span className="label">Lines   :</span> {comment.start} {comment.start !== comment.end && (comment.end)}
                                        </pre>
                                    </button>
                                ))
                            ) : (
                                <p>No comments yet</p>
                            )}
                        </div>
                    </div>
                )}

                {commandLineVisible && (
                    <div className="command-line" style={{ marginTop: '30px' }}>
                        <h3>VCS Command-Line:</h3>
                        <input
                            type="text"
                            value={command}
                            onChange={handleCommandInput}
                            onKeyDown={handleCommandSubmit}
                            placeholder="Type a command and press Enter"
                            className="command-input"
                            style={{ width: '98%', height: "35px", padding: '10px', fontFamily: 'monospace' }}
                        />
                        <textarea
                            value={commandOutput}
                            readOnly
                            className="command-output"
                            placeholder='Output will be shown here'
                        />
                        <br /><br /><br />
                    </div>
                )}

            </div>
        </div>



    );
};

export default EditorMain;

import React from 'react';
import { Navigate } from 'react-router-dom';

const ProtectAdmin = ({ component: Component }) => {
    const token = localStorage.getItem('editorAccessToken');

    if (!token) {
        return <Navigate to="/editor/login" replace />;
    }

    return <Component />;
};

export default ProtectAdmin;

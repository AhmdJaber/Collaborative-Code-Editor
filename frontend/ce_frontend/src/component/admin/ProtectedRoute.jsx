import React from 'react';
import { Navigate } from 'react-router-dom';

const ProtectAdmin = ({ component: Component }) => {
    const token = localStorage.getItem('adminAccessToken');

    if (!token) {
        return <Navigate to="/admin/login" />;
    }

    return <Component />;
};

export default ProtectAdmin;

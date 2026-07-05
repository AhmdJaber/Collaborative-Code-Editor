import React from 'react';
import { Navigate } from 'react-router-dom';

function AdminLogin() {
    return <Navigate to="/editor/login" replace />;
}

export default AdminLogin;

import React from 'react';
import { Navigate } from 'react-router-dom';

function AdminRegister() {
    return <Navigate to="/editor/register" replace />;
}

export default AdminRegister;

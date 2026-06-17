import './App.css'
import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import LoginPage from './component/login/Login'
import RegisterPage from './component/register/Register'

import AdminRegister from './component/admin/RegisterAdmin'
import EditorRegister from './component/editor/RegisterEditor'

import EditorMain from './component/editor/MainEditor';
import EditorLogin from './component/editor/LoginEditor';
import ProtectEditor from './component/editor/ProtectedRoute';
import EditorProjects from './component/editor/EditorProjects'

import AdminMain from './component/admin/MainAdmin';
import AdminLogin from './component/admin/LoginAdmin';
import ProtectAdmin from './component/admin/ProtectedRoute';

import RepoMain from './component/repos/MainRepos';

import WelcomePage from './component/welcome/WelcomePage';


if (typeof window !== 'undefined') {
  window.global = window;
}


function App() {
  return (
    <>
      <BrowserRouter>
        <Routes>

          <Route path='/register' element={<RegisterPage />}></Route>
          <Route path='/login' element={<LoginPage />}></Route>

          <Route path='/admin/register' element={<AdminRegister />}></Route>
          <Route path='/editor/register' element={<EditorRegister />}></Route>
          
          <Route path="/editor/login" element={<EditorLogin />} />
          <Route path="/editor/projects" element={<EditorProjects />} />
          <Route
              path="/editor"
              element={<ProtectEditor component={EditorMain} />}
          />

          <Route path="/admin/login" element={<AdminLogin />} />
          <Route
              path="/admin"
              element={<ProtectAdmin component={AdminMain} />}
          />

          <Route
              path="/repos"
              element={<RepoMain/>}
          />

          <Route path='/main' element={<WelcomePage />}></Route>
          <Route path='/' element={<WelcomePage />}></Route>

        </Routes>
      </BrowserRouter>
    </>
  )
}

export default App

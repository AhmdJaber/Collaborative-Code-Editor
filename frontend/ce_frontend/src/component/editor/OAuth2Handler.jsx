import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function OAuth2RedirectHandler() {
    const navigate = useNavigate();

    useEffect(() => {
        var accessToken = localStorage.getItem('editorAccessToken');
        var refreshToken = localStorage.getItem('editorRefreshToken');
        if (!accessToken && !refreshToken){
            const params = new URLSearchParams(window.location.search);
            console.log([...params.entries()]);
            accessToken = params.get('access_token');
            refreshToken = params.get('refresh_token');
        }

        if (accessToken && refreshToken) {
            localStorage.setItem('editorAccessToken', accessToken);
            localStorage.setItem('editorRefreshToken', refreshToken);
            navigate('/editor');
        } else {
            console.error('OAuth2 login failed');
            navigate('/editor/login');   
        }
    }, [navigate]);

    return null;   
}
export default OAuth2RedirectHandler;



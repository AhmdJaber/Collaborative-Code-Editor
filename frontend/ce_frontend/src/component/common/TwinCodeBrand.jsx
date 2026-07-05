import { useEffect, useLayoutEffect, useState } from 'react';

const getStoredRole = () => {
    if (typeof window === 'undefined') {
        return null;
    }

    return localStorage.getItem('editorRole')?.toLowerCase() || null;
};

const TwinCodeBrand = ({ label, fallbackLabel = 'editor' }) => {
    const [resolvedLabel, setResolvedLabel] = useState(
        () => label?.toLowerCase() || getStoredRole() || fallbackLabel
    );

    useLayoutEffect(() => {
        const cachedRole = getStoredRole();
        const nextLabel = label?.toLowerCase() || cachedRole || fallbackLabel;
        setResolvedLabel(nextLabel);

        if (label || cachedRole) {
            return;
        }

        const token = localStorage.getItem('editorAccessToken');
        if (!token) {
            return;
        }

        const loadRole = async () => {
            try {
                const response = await fetch('http://localhost:8080/auth/me', {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    return;
                }

                const currentUser = await response.json();
                const currentRole = (currentUser.role || fallbackLabel).toLowerCase();
                localStorage.setItem('editorRole', currentRole);
                setResolvedLabel(currentRole);
            } catch (error) {
                setResolvedLabel(nextLabel);
            }
        };

        loadRole();
    }, [label, fallbackLabel]);

    return (
        <div className="welcome-brand">
            <div className="app-name">TwinCode</div>
            <span className={`brand-eyebrow brand-eyebrow--${resolvedLabel || fallbackLabel}`}>
                {resolvedLabel || fallbackLabel}
            </span>
        </div>
    );
};

export default TwinCodeBrand;

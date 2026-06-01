import { createContext, useContext, useMemo, useState } from "react";

const AlertContext = createContext(null);

export function AlertProvider({ children }) {
    const [alert, setAlert] = useState(null);

    function showAlert(config) {
        setAlert(config);

        const duration = config?.duration ?? 5000;
        if (duration > 0) {
            window.clearTimeout(showAlert._timeoutId);
            showAlert._timeoutId = window.setTimeout(() => {
                setAlert(null);
            }, duration);
        }
    }

    function clearAlert() {
        window.clearTimeout(showAlert._timeoutId);
        setAlert(null);
    }

    const value = useMemo(
        () => ({
            alert,
            showAlert,
            clearAlert,
        }),
        [alert]
    );

    return <AlertContext.Provider value={value}>{children}</AlertContext.Provider>;
}

export function useAlert() {
    const context = useContext(AlertContext);

    if (!context) {
        throw new Error("useAlert must be used within an AlertProvider");
    }

    return context;
}
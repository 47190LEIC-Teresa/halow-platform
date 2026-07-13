import { useCallback, useEffect, useMemo, useState } from "react";

export function useDashboardPage(authFetch, token) {
    const [user, setUser] = useState(null);
    const [simulations, setSimulations] = useState([]);

    const loadUser = useCallback(async () => {
        try {
            const res = await authFetch("/api/user", token);
            if (!res.ok) throw new Error("Failed to load user");
            const data = await res.json();
            setUser(data);
        } catch {
            setUser(null);
        }
    }, [authFetch, token]);

    const loadSimulations = useCallback(async () => {
        try {
            const res = await authFetch("/api/simulations", token);

            if (!res.ok) throw new Error("Failed to load simulations");

            const data = await res.json();
            setSimulations(data);
        } catch {
            setSimulations([]);
        }
    }, [authFetch, token]);

    useEffect(() => {
        loadUser();
        loadSimulations();
    }, [loadUser, loadSimulations]);

    const hasActiveSimulations = useMemo(
        () =>
            simulations.some(
                (s) => s.status === "RUNNING" || s.status === "CREATED"
            ),
        [simulations]
    );

    useEffect(() => {
        if (!hasActiveSimulations) return;

        const intervalId = setInterval(() => {
            void loadSimulations();
        }, 3000);

        return () => {
            clearInterval(intervalId);
        };
    }, [hasActiveSimulations, loadSimulations]);

    const stats = useMemo(
        () => ({
            total: simulations.length,
            running: simulations.filter((s) => s.status === "RUNNING").length,
            completed: simulations.filter((s) => s.status === "COMPLETED").length,
        }),
        [simulations]
    );

    const recentSims = useMemo(
        () => simulations.slice(-5).reverse(),
        [simulations]
    );

    return {
        user,
        stats,
        recentSims,
    };
}
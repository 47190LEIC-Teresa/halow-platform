export async function parseApiError(res, fallbackMessage) {
    try {
        const data = await res.json();

        console.log ("Parsed API error:", data);

        return {
            status: data?.status ?? res.status,
            error: data?.error ?? "UNKNOWN_ERROR",
            code: data?.code ?? null,
            message: data?.message ?? fallbackMessage,
            source: data?.source ?? null,
            path: data?.path ?? null,
            timestamp: data?.timestamp ?? null,
        };
    } catch {
        return {
            status: res.status,
            error: "UNKNOWN_ERROR",
            code: null,
            message: fallbackMessage,
            source: null,
            path: null,
            timestamp: null,
        };
    }
}

export function toClientError(err, fallbackMessage) {
    return {
        status: null,
        error: "CLIENT_ERROR",
        code: null,
        message: err?.message || fallbackMessage,
        source: "CLIENT",
        path: null,
        timestamp: null,
    };
}

export function isApiError(err) {
    return !!(err?.status || err?.error || err?.code);
}

export function normalizeError(err, fallbackMessage) {
    if (isApiError(err)) {
        return err;
    }

    if (typeof err === "string") {
        try {
            const parsed = JSON.parse(err);

            if (isApiError(parsed)) {
                return parsed;
            }
        } catch {
            // ignore parse failure
        }

        return toClientError({ message: err }, fallbackMessage);
    }

    return toClientError(err, fallbackMessage);
}

export function showApiAlert(showAlert, apiError, fallbackTitle = "Something went wrong") {
    if (!apiError) return;

    if (apiError.status >= 500) {
        showAlert({
            type: "error",
            title: fallbackTitle,
            message: `Please contact the developers and provide this code: ${apiError.code || apiError.error || "UNKNOWN_ERROR"}`,
            code: apiError.code || apiError.error || "UNKNOWN_ERROR",
            duration: 15000,
        });
        return;
    }

    showAlert({
        type: "error",
        title: fallbackTitle,
        message: apiError.message || "An unexpected error occurred.",
        code: apiError.code || null,
        duration: 15000,
    });
}

export function showServerAlert(showAlert) {
    showAlert({
        type: "server",
        title: "Server unreachable",
        message: "The server could not be reached. Please try again later.",
        duration: 15000,
    });
}
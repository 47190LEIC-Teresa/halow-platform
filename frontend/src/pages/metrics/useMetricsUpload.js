import { useState } from "react";
import { useAlert } from "../../context/AlertContext";
import {
    parseApiError,
    normalizeError,
    showApiAlert,
    showServerAlert,
} from "../../utils/apiAlerts";

export function useMetricsUpload(authFetch, token) {
    const { showAlert, clearAlert } = useAlert();

    const [selectedFile, setSelectedFile] = useState(null);
    const [metrics, setMetrics] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [isMetricsOpen, setIsMetricsOpen] = useState(false);
    const [message, setMessage] = useState("");

    function handleFileChange(e) {
        const file = e.target.files?.[0] ?? null;
        setSelectedFile(file);
        setMetrics(null);
        clearAlert();
    }

    async function handleUpload() {
        if (!selectedFile) {
            setMessage("Please select a .zip log file first.");
            return;
        }

        clearAlert();
        setIsUploading(true);

        try {
            const formData = new FormData();
            formData.append("file", selectedFile);

            const res = await authFetch("/api/metrics", token, {
                method: "POST",
                body: formData,
            });

            if (!res.ok) {
                throw await parseApiError(res, "Failed to compute metrics");
            }

            const data = await res.json();
            setMetrics(data);
            setIsMetricsOpen(false);
        } catch (err) {
            setMetrics(null);

            const apiError = normalizeError(err.message, "Failed to compute metrics");

            if (apiError.source === "CLIENT" && apiError.message === "Failed to fetch") {
                showServerAlert(showAlert);
                return;
            }

            showApiAlert(showAlert, apiError, "Metrics failed");
        } finally {
            setIsUploading(false);
        }
    }

    function resetMetrics() {
        setSelectedFile(null);
        setMetrics(null);
        setIsMetricsOpen(false);
        clearAlert();
    }

    return {
        selectedFile,
        metrics,
        message,
        isUploading,
        isMetricsOpen,
        setIsMetricsOpen,
        handleFileChange,
        handleUpload,
        resetMetrics,
    };
}
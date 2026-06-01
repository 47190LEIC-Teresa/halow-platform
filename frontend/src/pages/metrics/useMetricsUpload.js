
import { useState } from "react";

export function useMetricsUpload(authFetch, token) {
    const [selectedFile, setSelectedFile] = useState(null);
    const [metrics, setMetrics] = useState(null);
    const [message, setMessage] = useState("");
    const [isUploading, setIsUploading] = useState(false);
    const [isMetricsOpen, setIsMetricsOpen] = useState(false);

    function handleFileChange(e) {
        const file = e.target.files?.[0] ?? null;
        setSelectedFile(file);
        setMetrics(null);
        setMessage("");
    }

    async function handleUpload() {
        if (!selectedFile) {
            setMessage("Please select a .zip log file first.");
            return;
        }

        setIsUploading(true);
        setMessage("");

        try {
            const formData = new FormData();
            formData.append("file", selectedFile);

            const res = await authFetch("/api/metrics", token, {
                method: "POST",
                body: formData,
            });

            if (!res.ok) throw new Error("Failed to compute metrics");

            const data = await res.json();
            setMetrics(data);
            setIsMetricsOpen(false);
        } catch (err) {
            setMetrics(null);
            setMessage(err.message || "Failed to compute metrics");
        } finally {
            setIsUploading(false);
        }
    }

    function resetMetrics() {
        setSelectedFile(null);
        setMetrics(null);
        setMessage("");
        setIsMetricsOpen(false);
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
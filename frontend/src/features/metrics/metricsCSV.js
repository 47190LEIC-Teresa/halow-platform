import {
    metricGroups,
    formatMetricName,
    formatMetricValue,
} from "./metricsFormatters";
import {
    simulationGroups,
    formatSimulationName,
    formatSimulationValue,
} from "../../pages/simulationDetails/simulationFormatters";

function escapeCsv(value) {
    const stringValue = value == null ? "" : String(value);
    return `"${stringValue.replace(/"/g, '""')}"`;
}

function rowsToCsv(rows) {
    return rows.map((row) => row.map(escapeCsv).join(",")).join("\n");
}

function pushSection(rows, title, header, entries) {
    if (rows.length > 0) {
        rows.push([]);
    }

    rows.push([title, ""]);
    rows.push(header);
    rows.push(...entries);
}

export function buildMetricsCsv(metrics) {
    const rows = [];

    const entries = metricGroups.map((key) => [
            formatMetricName(key),
            formatMetricValue(key, metrics?.[key]),
        ]);

    pushSection(rows,"Metrics", ["Metric", "Value"], entries);

    return rowsToCsv(rows);
}

export function buildSimulationCsv(simulation, config, metrics = null) {
    const rows = [];

    const mainEntries = simulationGroups.Main.map((key) => [
        formatSimulationName(key),
        formatSimulationValue(key, simulation?.[key]),
    ]);

    const configEntries = simulationGroups.Config.map((key) => [
        formatSimulationName(key),
        formatSimulationValue(key, config?.[key]),
    ]);


    pushSection(rows, "Simulation", ["Field", "Value"], mainEntries);
    pushSection(rows, "Configuration", ["Parameter", "Value"], configEntries);


    if (metrics) {
        const metricEntries = metricGroups.map((key) => [
            formatMetricName(key),
            formatMetricValue(key, metrics?.[key]),
        ]);

        pushSection(rows, "Metrics", ["Metric", "Value"], metricEntries);
    }

    return rowsToCsv(rows);
}

export function downloadMetricsCsv(metrics, filename = "metrics.csv") {
    const csv = buildMetricsCsv(metrics);
    downloadCsv(csv, filename);
}

export function downloadSimulationCsv(
    simulation,
    config,
    metrics = null,
    filename = "simulation.csv"
) {
    const csv = buildSimulationCsv(simulation, config, metrics);
    downloadCsv(csv, filename);
}

function downloadCsv(csv, filename) {
    const blob = new Blob(["\uFEFF", csv], {
        type: "text/csv;charset=utf-8;",
    });

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
}
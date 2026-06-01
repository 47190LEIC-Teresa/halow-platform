
export const simulationGroups = {
    Main: [
        "owner",
        "createdAt",
        "startedAt",
        "finishedAt",
        "label"
    ],

    Config: [
        "seed",
        "stations",
        "groups",
        "height",
        "width",
        "verbosity",
        "simLength",
        "packetRate",
        "slotLength",
    ]
};

const customLabels = {
    createdAt: "Time at creation",
    startedAt: "Time at start",
    finishedAt: "Time at finish",
    seed: "Seed",
    stations: "Number of station",
    groups: "Number of groups",
    height: "Height (m)",
    width: "Width (m)",
    verbosity: "Verbosity",
    simLength: "Simulation Length (us)",
    packetRate: "Packet Rate (packet/us)",
    slotLength: "Slot Length (us)",
    label: "Label"
};

export function formatSimulationName(key) {
    return (
        customLabels[key] ||
        key.replace(/([A-Z])/g, " $1").replace(/^./, (str) => str.toUpperCase())
    );
}

export function formatSimulationValue(key, value) {
    if (typeof value !== "number") return value;
    if (!Number.isInteger(value)) return value.toFixed(3);
    return value;
}
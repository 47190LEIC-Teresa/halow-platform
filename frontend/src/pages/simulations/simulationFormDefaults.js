export const simulationFormDefaults = {
    n: 1,
    g: 1,
    h: 1000,
    w: 1000,
    seed: null,
    verbosity: 0,
    simLength: 20000000,
    packetRate: 10000,
    slotLength: 50000,
    zippedOutput: true,
    runSimParser: true,
    pEEnabled: false,
    pE: "link-rssi.txt",
    pPEnabled: false,
    pP: "station-coordinates.txt",
    mpEnabled: false,
    mp: "path-loss.txt",
    fileGroups: "",
    label: "",

    // For the Run mode
    runMode: "single",           // "single" | "batch"
    simulationCount: 2,
    randomSeed: true,
    seedMin: null,
    seedMax: null
};
function buildBaseSimulationPayload(form) {
    return {
        n: Number(form.n),
        g: Number(form.g),
        h: Number(form.h),
        w: Number(form.w),
        verbosity: Number(form.verbosity),
        simLength: Number(form.simLength),
        packetRate: Number(form.packetRate),
        slotLength: Number(form.slotLength),
        zippedOutput: form.zippedOutput,
        pE: (form.pEEnabled && form.pE) || null,
        pP: (form.pPEnabled && form.pP) || null,
        mp: (form.mpEnabled && form.mp) || null,
        fileGroups: form.fileGroups || null,
        runSimParser: form.runSimParser,
        label: form.label
    };
}

export function buildSimulationPayload(form) {
    return {
        ...buildBaseSimulationPayload(form),
        seed:
            form.seed === "" || form.seed == null
                ? Math.floor(Math.random() * 100000000)
                : form.seed
    };
}

export function buildSimulationBatchPayload(form) {
    return {
        ...buildBaseSimulationPayload(form),
        batchSize: Number(form.simulationCount),
        seedMin: Number(form.seedMin),
        seedMax: Number(form.seedMax),
        randomSeed: form.randomSeed
    };
}
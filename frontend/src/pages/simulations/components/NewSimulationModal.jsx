import React from "react";
import { FaTrashAlt, FaUpload, FaPlay } from "react-icons/fa";

function GroupFileInput({ form, updateField }) {
    const inputRef = React.useRef(null);
    const file = form.fileGroups;

    const openPicker = () => {
        inputRef.current?.click();
    };

    const removeFile = () => {
        updateField("fileGroups", null);
        updateField("n", 1);

        if (inputRef.current) {
            inputRef.current.value = "";
        }
    };

    const handleChange = async (e) => {
        const selected = e.target.files?.[0] ?? null;
        updateField("fileGroups", selected);

        if (!selected) {
            updateField("n", 1);
            return;
        }

        const text = await selected.text();
        const trimmed = text.trimEnd();
        const lineCount =
            trimmed === "" ? 0 : trimmed.split(/\r?\n/).length;

        updateField("n", lineCount);
    };

    return (
        <div className="optional-input-row" style={{ fontSize: "0.88rem" }}>
            <label>Groups File</label>

            {!file ? (
                <button
                    type="button"
                    className="btn-secondary"
                    onClick={openPicker}
                    title="Upload"
                    style={{
                        background: "transparent",
                        border: "none",
                        color: "darkgray",
                    }}
                >
                    <FaUpload />
                </button>
            ) : (
                <div className="file-chip">
                    <span className="file-name">{file.name}</span>
                    <button
                        type="button"
                        className="link-button danger"
                        onClick={removeFile}
                        title="Remove"
                        style={{
                            background: "transparent",
                            border: "none",
                            color: "darkgray",
                        }}
                    >
                        <FaTrashAlt />
                    </button>
                </div>
            )}

            <input
                ref={inputRef}
                type="file"
                accept=".txt"
                style={{ display: "none" }}
                onChange={handleChange}
            />
        </div>
    );
}

export default function NewSimulationModal({
                                               form,
                                               showOptional,
                                               setShowOptional,
                                               updateField,
                                               onClose,
                                               onSubmit,
                                           }) {
    const isGroupMode = form.runMode === "batch";

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div
                className="modal-content modal-content--compact"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-header">
                    <h3>{isGroupMode ? "New Simulation Group" : "New Simulation"}</h3>
                    <button
                        type="button"
                        className="collapse-icon"
                        onClick={onClose}
                    >
                        ✕
                    </button>
                </div>

                <form
                    onSubmit={onSubmit}
                    className="simulation-form simulation-form--compact"
                >
                    {/* Run mode */}
                    <div className="section-block">
                        <p className="eyebrow">Run mode</p>

                        <div className="form-row">

                            <div className="mode-switch" role="batch" aria-label="Simulation mode">
                                <button
                                    type="button"
                                    className={form.runMode === "single" ? "mode-switch-btn active" : "mode-switch-btn"}
                                    onClick={() => updateField("runMode", "single")}
                                >
                                    Single
                                </button>

                                <button
                                    type="button"
                                    className={form.runMode === "batch" ? "mode-switch-btn active" : "mode-switch-btn"}
                                    onClick={() => updateField("runMode", "batch")}
                                >
                                    Batch
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Batch settings */}
                    {isGroupMode && (
                        <div className="section-block">
                            <p className="eyebrow">Batch settings</p>

                            <div className="batch-list">

                                <div className="form-batch-row">
                                    <label>Number of simulations</label>
                                    <input
                                        type="number"
                                        min={2}
                                        value={form.simulationCount}
                                        onChange={(e) =>
                                            updateField("simulationCount", e.target.value)
                                        }
                                    />
                                </div>

                                <div className="form-batch-row">
                                    <label>
                                        Label
                                    </label>
                                    <input
                                        value={form.label}
                                        required={true}
                                        onChange={(e) =>
                                            updateField("label", e.target.value)
                                        }
                                    />
                                </div>

                                <div className="form-batch-row">
                                    <label>Random seed</label>

                                    <label className="switch">
                                    <input
                                        type="checkbox"
                                        checked={form.randomSeed}
                                        onChange={(e) => updateField("randomSeed", e.target.checked)}
                                    />
                                    <span className="switch-slider" />
                                </label>
                                </div>


                            </div>
                        </div>
                    )}

                    {/* Core parameters */}
                    <div className="section-block">
                        <p className="eyebrow">Core parameters</p>

                        <div className="form-grid compact-grid compact-grid--compact">
                            <div className="form-row">
                                <label>Stations</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.n}
                                    disabled={!!form.fileGroups}
                                    onChange={(e) => updateField("n", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>Groups</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.g}
                                    onChange={(e) => updateField("g", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>{form.runMode === "batch" ? "Seed range" : "Seed"}</label>

                                {form.runMode === "batch" ? (
                                    <div
                                        className="seed-range-inputs"
                                        style={{
                                            display: "flex",
                                            flexDirection: "row",
                                            gap: "0.5rem",
                                            width: "100%",
                                        }}
                                    >
                                        <input
                                            type="number"
                                            min={1}
                                            max={form.seedMax || 99999999}
                                            required={true}
                                            value={form.seedMin}
                                            placeholder="Min"
                                            onChange={(e) => updateField("seedMin", e.target.value)}
                                            style={{
                                                flex: 1,
                                                minWidth: 0,
                                            }}
                                        />
                                        <input
                                            type="number"
                                            min={form.seedMin || 1}
                                            max={99999999}
                                            required={true}
                                            value={form.seedMax}
                                            placeholder="Max"
                                            onChange={(e) => updateField("seedMax", e.target.value)}
                                            style={{
                                                flex: 1,
                                                minWidth: 0,
                                            }}
                                        />
                                    </div>
                                ) : (
                                    <input
                                        type="number"
                                        min={1}
                                        value={form.seed || ""}
                                        placeholder="Leave empty for random"
                                        onChange={(e) => updateField("seed", e.target.value)}
                                    />
                                )}
                            </div>


                            <div className="form-row">
                                <label>Height (m)</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.h}
                                    onChange={(e) => updateField("h", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>Width (m)</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.w}
                                    onChange={(e) => updateField("w", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>Verbosity</label>
                                <input
                                    type="number"
                                    min={0}
                                    value={form.verbosity}
                                    onChange={(e) =>
                                        updateField("verbosity", e.target.value)
                                    }
                                />
                            </div>

                            <div className="form-row">
                                <label>Simulation Length (us)</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.simLength}
                                    onChange={(e) =>
                                        updateField("simLength", e.target.value)
                                    }
                                />
                            </div>

                            <div className="form-row">
                                <label>Packet Rate (packet/us)</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.packetRate}
                                    onChange={(e) =>
                                        updateField("packetRate", e.target.value)
                                    }
                                />
                            </div>

                            <div className="form-row">
                                <label>Slot Length (us)</label>
                                <input
                                    type="number"
                                    min={1}
                                    value={form.slotLength}
                                    onChange={(e) =>
                                        updateField("slotLength", e.target.value)
                                    }
                                />
                            </div>
                        </div>
                    </div>

                    {/* Optional outputs */}
                    <div className="section-block collapsible-section">
                        <div className="section-header">
                            <p className="eyebrow">Optional outputs</p>
                            <button
                                type="button"
                                className="collapse-icon"
                                onClick={() => setShowOptional((prev) => !prev)}
                            >
                                {showOptional ? "−" : "+"}
                            </button>
                        </div>

                        {showOptional && (
                            <div className="optional-output-list">
                                {/* Single mode label only */}
                                {!isGroupMode && (
                                    <div className="optional-output-row">
                                        <label style={{ fontSize: "0.88rem" }}>
                                            Label
                                        </label>
                                        <input
                                            value={form.label}
                                            onChange={(e) =>
                                                updateField("label", e.target.value)
                                            }
                                        />
                                    </div>
                                )}

                                <div className="optional-output-row">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            checked={form.pEEnabled}
                                            onChange={(e) =>
                                                updateField("pEEnabled", e.target.checked)
                                            }
                                        />
                                        Generate RSSI file
                                    </label>

                                    {form.pEEnabled && (
                                        <input
                                            value={form.pE}
                                            onChange={(e) =>
                                                updateField("pE", e.target.value)
                                            }
                                            placeholder="link-rssi.txt"
                                        />
                                    )}
                                </div>

                                <div className="optional-output-row">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            checked={form.pPEnabled}
                                            onChange={(e) =>
                                                updateField("pPEnabled", e.target.checked)
                                            }
                                        />
                                        Generate station coordinates file
                                    </label>

                                    {form.pPEnabled && (
                                        <input
                                            value={form.pP}
                                            onChange={(e) =>
                                                updateField("pP", e.target.value)
                                            }
                                            placeholder="station-coordinates.txt"
                                        />
                                    )}
                                </div>

                                <div className="optional-output-row">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            checked={form.mpEnabled}
                                            onChange={(e) =>
                                                updateField("mpEnabled", e.target.checked)
                                            }
                                        />
                                        Generate path loss file
                                    </label>

                                    {form.mpEnabled && (
                                        <input
                                            value={form.mp}
                                            onChange={(e) =>
                                                updateField("mp", e.target.value)
                                            }
                                            placeholder="path-loss.txt"
                                        />
                                    )}
                                </div>

                                <div className="optional-output-row">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            checked={form.runSimParser}
                                            onChange={(e) =>
                                                updateField("runSimParser", e.target.checked)
                                            }
                                        />
                                        Run SimParser after simulation
                                    </label>
                                </div>

                                <GroupFileInput
                                    form={form}
                                    updateField={updateField}
                                />
                            </div>
                        )}
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="primary-btn">
                            <FaPlay />
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
// src/pages/metrics/components/MetricBar.jsx
export default function MetricBar({
                                      label,
                                      value,
                                      max,
                                      suffix = "",
                                      decimals = 0,
                                  }) {
    const safeValue = typeof value === "number" ? value : 0;
    const safeMax = typeof max === "number" && max > 0 ? max : 0;
    const percent = safeMax > 0 ? (safeValue / safeMax) * 100 : 0;

    return (
        <div className="metric-bar-block">
            <div className="metric-bar-header">
                <span className="config-label">{label}</span>
                <span className="config-value">
          {typeof safeValue === "number" && !Number.isInteger(safeValue)
              ? safeValue.toFixed(decimals)
              : safeValue}
                    {suffix}
        </span>
            </div>

            <div className="metric-bar-track">
                <div
                    className="metric-bar-fill"
                    style={{ width: `${Math.max(0, Math.min(percent, 100))}%` }}
                />
            </div>
        </div>
    );
}
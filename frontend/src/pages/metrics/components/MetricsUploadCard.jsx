import { FiUpload, FiFileText } from "react-icons/fi";

export default function MetricsUploadCard({
  selectedFile,
  onFileChange,
}){
    return (
        <>
            <p className="helper-text">
                Upload a <strong>log.zip</strong> file to compute metrics. This page does
                not save the file or stores the results in the database.
            </p>

            <div className="metrics-upload-box">
                <label htmlFor="metrics-file-input" className="file-upload-trigger">
                    <FiUpload/>
                    <span>{selectedFile ? "Change file" : "Choose log file"}</span>
                </label>

                <input
                    id="metrics-file-input"
                    className="file-input-hidden"
                    type="file"
                    accept=".txt,text/plain"
                    onChange={onFileChange}
                />

                <div className="selected-file-row">
                    {selectedFile ? (
                        <>
                            <FiFileText/>
                            <span>{selectedFile.name}</span>
                        </>
                    ) : (
                        <span className="helper-text">No file selected</span>
                    )}
                </div>
            </div>
        </>
    );
}
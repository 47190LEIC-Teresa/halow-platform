package backend.model.dto

data class ZipDownloadData(
    val fileName: String,
    val entries: List<ZipEntryData>
)
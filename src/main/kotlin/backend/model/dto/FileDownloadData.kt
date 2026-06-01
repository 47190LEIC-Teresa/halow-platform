package backend.model.dto

data class FileDownloadData(
    val fileName: String,
    val contentType: String,
    val data: ByteArray
)
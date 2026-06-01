package backend.model.dto

import backend.model.entity.SimulationFile
import backend.model.enums.FileType

data class SimulationFileResponse(
    val id: Long,
    val fileType: FileType,
    val fileName: String,
    val fileSize: Long,
    val downloadUrl: String,
    val downloaded: Boolean
)

fun toFileResponse(file: SimulationFile): SimulationFileResponse = SimulationFileResponse(
    id = file.id!!,
    fileType = file.fileType,
    fileName = file.fileName,
    fileSize = file.fileSize,
    downloadUrl = "/api/files/${file.id}/download",
    downloaded = file.downloaded
)
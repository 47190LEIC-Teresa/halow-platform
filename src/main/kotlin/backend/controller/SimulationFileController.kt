package backend.controller

import backend.model.dto.BulkSimulationDownloadRequest
import backend.model.dto.SimulationFileResponse
import backend.service.SimulationFileService
import backend.service.SimulationService
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestController
@RequestMapping("/api/files")
class SimulationFileController(
    private val simulationFileService: SimulationFileService,
    private val simulationService: SimulationService
) {

    @PreAuthorize("@simulationSecurity.isOwner(#simulationID, authentication.name)")
    @GetMapping("/{simulationID}")
    fun getSimulationFiles(
        @PathVariable simulationID: Long
    ): List<SimulationFileResponse> {
        return simulationFileService.getVisibleFilesForSimulation(simulationID)
    }

    @PreAuthorize("@fileSecurity.canDownload(#fileId, authentication.name)")
    @GetMapping("/{fileId}/download")
    fun downloadFile(
        @PathVariable fileId: Long
    ): ResponseEntity<ByteArray> {
        val download = simulationFileService.prepareFileDownload(fileId)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(download.contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"${download.fileName}\""
            )
            .body(download.data)
    }

    @PreAuthorize("@simulationSecurity.isOwner(#simulationID, authentication.name)")
    @GetMapping("/{simulationID}/download-all")
    fun downloadAllFiles(
        @PathVariable simulationID: Long
    ): ResponseEntity<StreamingResponseBody> {
        val download = simulationFileService.prepareZipDownload(simulationID)

        val contentDisposition = ContentDisposition.attachment()
            .filename(download.fileName, StandardCharsets.UTF_8)
            .build()

        val body = StreamingResponseBody { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                val usedNames = mutableSetOf<String>()

                download.entries.forEach { file ->
                    val entryName = uniqueFileName(file.fileName, usedNames)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    zipOut.write(file.data)
                    zipOut.closeEntry()
                }

                zipOut.finish()
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(body)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/download-selected")
    fun downloadSelectedFiles(
        @RequestBody request: BulkSimulationDownloadRequest,
        authentication: Authentication
    ): ResponseEntity<StreamingResponseBody> {
        val username = authentication.name
        val download = simulationFileService.prepareSelectedZipDownload(
            simulationIds = request.simulationIds,
            username = username
        )

        val contentDisposition = ContentDisposition.attachment()
            .filename(download.fileName, StandardCharsets.UTF_8)
            .build()

        val body = StreamingResponseBody { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                val usedNames = mutableSetOf<String>()

                download.entries.forEach { file ->
                    val entryName = uniqueZipPath(file.fileName, usedNames)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    zipOut.write(file.data)
                    zipOut.closeEntry()
                }

                zipOut.finish()
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(body)
    }

    private fun uniqueFileName(original: String, usedNames: MutableSet<String>): String {
        if (usedNames.add(original)) return original

        val dotIndex = original.lastIndexOf('.')
        val base = if (dotIndex >= 0) original.substring(0, dotIndex) else original
        val ext = if (dotIndex >= 0) original.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidate = "$base($counter)$ext"
            if (usedNames.add(candidate)) return candidate
            counter++
        }
    }

    private fun uniqueZipPath(original: String, usedNames: MutableSet<String>): String {
        if (usedNames.add(original)) return original

        val slashIndex = original.lastIndexOf('/')
        val folder = if (slashIndex >= 0) original.substring(0, slashIndex + 1) else ""
        val fileName = if (slashIndex >= 0) original.substring(slashIndex + 1) else original

        val dotIndex = fileName.lastIndexOf('.')
        val base = if (dotIndex >= 0) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex >= 0) fileName.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidate = "$folder$base($counter)$ext"
            if (usedNames.add(candidate)) return candidate
            counter++
        }
    }
}
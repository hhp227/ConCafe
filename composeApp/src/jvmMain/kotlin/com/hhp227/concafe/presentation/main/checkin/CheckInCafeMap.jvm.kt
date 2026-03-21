package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.image.BufferedImage
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

@Composable
actual fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    modifier: Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            CheckInDesktopMapPanel(
                onCafeClick = onCafeClick
            )
        },
        update = { panel ->
            panel.bind(
                cafes = cafes,
                onCafeClick = onCafeClick
            )
        }
    )
}

private class CheckInDesktopMapPanel(
    private var onCafeClick: (String) -> Unit
) : JPanel(BorderLayout()) {
    private val mapLabel = JLabel("지도를 불러오는 중...", SwingConstants.CENTER)

    private val chipPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 8))

    private var currentMapUrl: String? = null

    fun bind(cafes: List<CheckInCafeSummary>, onCafeClick: (String) -> Unit) {
        this.onCafeClick = onCafeClick
        bindCafeChips(cafes)
        val mapUrl = buildMapImageUrl(cafes)

        if (currentMapUrl != mapUrl) {
            currentMapUrl = mapUrl
            loadMapImageAsync(
                primaryUrl = mapUrl,
                fallbackUrl = buildFallbackTileUrl(cafes)
            )
        }
    }

    private fun bindCafeChips(cafes: List<CheckInCafeSummary>) {
        chipPanel.removeAll()
        if (cafes.isEmpty()) {
            chipPanel.add(JLabel("지도에 표시할 카페가 아직 없습니다."))
        } else {
            cafes.take(12).forEach { cafe ->
                val chip = JButton(cafe.name).apply {
                    isFocusPainted = false
                    background = Color.WHITE
                    foreground = Color(0x5A, 0x4C, 0x56)
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color(0xF2, 0xD4, 0xE1)),
                        BorderFactory.createEmptyBorder(4, 10, 4, 10)
                    )
                    addActionListener { onCafeClick(cafe.id) }
                }
                chipPanel.add(chip)
            }
        }
        chipPanel.revalidate()
        chipPanel.repaint()
    }

    private fun loadMapImageAsync(primaryUrl: String, fallbackUrl: String) {
        mapLabel.text = "지도를 불러오는 중..."
        mapLabel.icon = null

        Thread {
            val primary = loadImage(primaryUrl)
            val resolved = primary ?: loadImage(fallbackUrl)

            SwingUtilities.invokeLater {
                if (resolved != null) {
                    mapLabel.icon = ImageIcon(resolved)
                    mapLabel.text = null
                } else {
                    mapLabel.icon = null
                    mapLabel.text = "지도 이미지를 불러오지 못했습니다."
                }
            }
        }.start()
    }

    private fun loadImage(url: String): BufferedImage? {
        return runCatching {
            val connection = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 ConCafeDesktop/1.0")
                connectTimeout = 5000
                readTimeout = 5000
            }
            connection.getInputStream().use { stream ->
                ImageIO.read(stream)
            }
        }.getOrNull()
    }

    init {
        mapLabel.preferredSize = Dimension(900, 420)
        mapLabel.minimumSize = Dimension(640, 300)
        mapLabel.border = BorderFactory.createLineBorder(Color(0xF2, 0xD4, 0xE1), 1)
        mapLabel.font = Font("Dialog", Font.PLAIN, 13)
        chipPanel.background = Color(0xFF, 0xF5, 0xF9)
        val content = JPanel(BorderLayout()).apply {
            background = Color(0xFF, 0xF5, 0xF9)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(mapLabel, BorderLayout.CENTER)
            add(chipPanel, BorderLayout.SOUTH)
        }
        add(JScrollPane(content), BorderLayout.CENTER)
    }
}

private fun buildMapImageUrl(cafes: List<CheckInCafeSummary>): String {
    val centerLatitude = cafes.map { it.geoPoint.latitude }.averageOrDefault(37.5665)
    val centerLongitude = cafes.map { it.geoPoint.longitude }.averageOrDefault(126.9780)
    val markerQuery = cafes.take(12).joinToString("|") { cafe ->
        "${cafe.geoPoint.latitude},${cafe.geoPoint.longitude},lightblue1"
    }
    return buildString {
        append("https://staticmap.openstreetmap.de/staticmap.php")
        append("?center=")
        append(centerLatitude)
        append(",")
        append(centerLongitude)
        append("&zoom=13&size=900x420")
        if (markerQuery.isNotBlank()) {
            append("&markers=")
            append(encode(markerQuery))
        }
    }
}

private fun buildFallbackTileUrl(cafes: List<CheckInCafeSummary>): String {
    val centerLatitude = cafes.map { it.geoPoint.latitude }.averageOrDefault(37.5665)
    val centerLongitude = cafes.map { it.geoPoint.longitude }.averageOrDefault(126.9780)
    val zoom = 13
    val latRad = Math.toRadians(centerLatitude)
    val scale = 1 shl zoom
    val tileX = ((centerLongitude + 180.0) / 360.0 * scale).toInt()
    val tileY = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * scale).toInt()
    return "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png"
}

private fun encode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

private fun List<Double>.averageOrDefault(default: Double): Double {
    return if (isEmpty()) default else average()
}

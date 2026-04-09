package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.RoundRectangle2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh

@Composable
actual fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    onCafeCheckIn: (String) -> Unit,
    cameraTarget: CheckInMapCameraTarget?,
    modifier: Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            JvmCheckInTileMapPanel()
        },
        update = { panel ->
            panel.bind(
                cafes = cafes,
                cameraTarget = cameraTarget,
                onCafeClick = onCafeClick,
                onCafeCheckIn = onCafeCheckIn
            )
        }
    )
}

private class JvmCheckInTileMapPanel : JPanel(BorderLayout()) {
    private var cafes: List<CheckInCafeSummary> = emptyList()

    private var normalizedCafes: List<NormalizedCafeMapItem> = emptyList()

    private var onCafeClick: (String) -> Unit = {}

    private var onCafeCheckIn: (String) -> Unit = {}

    private var centerLatitude: Double = DEFAULT_LATITUDE

    private var centerLongitude: Double = DEFAULT_LONGITUDE

    private var zoom: Int = DEFAULT_ZOOM

    private var selectedCafeId: String? = null

    private var currentCameraTarget: CheckInMapCameraTarget? = null

    private var lastDragPoint: Point? = null

    private val markerHitAreas = mutableListOf<MarkerHitArea>()

    private var popupNameHitArea: PopupHitArea? = null

    private var popupCheckInHitArea: PopupHitArea? = null

    private val tileCache = ConcurrentHashMap<TileKey, BufferedImage>()

    private val loadingTiles = ConcurrentHashMap.newKeySet<TileKey>()

    private val tileExecutor = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable, "concafe-map-tile-loader").apply {
            isDaemon = true
        }
    }

    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                lastDragPoint = event.point
            }

            override fun mouseReleased(event: MouseEvent) {
                lastDragPoint = null
            }

            override fun mouseClicked(event: MouseEvent) {
                handleClick(event.point)
            }
        })
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                val previous = lastDragPoint ?: return
                val dx = event.x - previous.x
                val dy = event.y - previous.y
                val centerWorld = latLngToWorldPixel(centerLatitude, centerLongitude, zoom)
                val nextCenter = worldPixelToLatLng(
                    worldX = centerWorld.x - dx,
                    worldY = centerWorld.y - dy,
                    zoom = zoom
                )

                centerLatitude = nextCenter.latitude
                centerLongitude = nextCenter.longitude
                lastDragPoint = event.point
                repaint()
            }
        })
        addMouseWheelListener { event ->
            handleWheel(event)
        }
    }

    fun bind(
        cafes: List<CheckInCafeSummary>,
        cameraTarget: CheckInMapCameraTarget?,
        onCafeClick: (String) -> Unit,
        onCafeCheckIn: (String) -> Unit
    ) {
        val nextNormalizedCafes = cafes.mapNotNull { cafe ->
            val normalizedLatitude = normalizeLatitude(cafe.geoPoint.latitude) ?: return@mapNotNull null
            val normalizedLongitude = normalizeLongitude(cafe.geoPoint.longitude) ?: return@mapNotNull null

            NormalizedCafeMapItem(
                id = cafe.id,
                name = cafe.name,
                latitude = normalizedLatitude,
                longitude = normalizedLongitude
            )
        }
        val shouldResetCamera = this.cafes != cafes
            || this.currentCameraTarget != cameraTarget

        this.cafes = cafes
        this.normalizedCafes = nextNormalizedCafes
        this.onCafeClick = onCafeClick
        this.onCafeCheckIn = onCafeCheckIn
        this.currentCameraTarget = cameraTarget

        if (shouldResetCamera) {
            val camera = resolveCamera(nextNormalizedCafes, cameraTarget)

            centerLatitude = camera.latitude
            centerLongitude = camera.longitude
            zoom = camera.zoom
            selectedCafeId = selectedCafeId?.takeIf { cafeId ->
                nextNormalizedCafes.any { cafe -> cafe.id == cafeId }
            }
        }
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.clip = RoundRectangle2D.Double(
                0.0,
                0.0,
                width.toDouble(),
                height.toDouble(),
                MAP_CORNER_RADIUS.toDouble(),
                MAP_CORNER_RADIUS.toDouble()
            )
            markerHitAreas.clear()
            popupNameHitArea = null
            popupCheckInHitArea = null
            paintTiles(g)
            paintMarkers(g)
            paintSelectedPopup(g)
        } finally {
            g.dispose()
        }
    }

    private fun paintTiles(g: Graphics2D) {
        val centerWorld = latLngToWorldPixel(centerLatitude, centerLongitude, zoom)
        val topLeftX = centerWorld.x - width / 2.0
        val topLeftY = centerWorld.y - height / 2.0
        val minTileX = floor(topLeftX / TILE_SIZE).toInt()
        val maxTileX = floor((topLeftX + width) / TILE_SIZE).toInt()
        val minTileY = floor(topLeftY / TILE_SIZE).toInt()
        val maxTileY = floor((topLeftY + height) / TILE_SIZE).toInt()
        val tileCount = 2.0.pow(zoom).toInt()
        g.color = Color(0xFFFFF5F9.toInt())
        g.fillRect(0, 0, width, height)

        for (tileY in minTileY..maxTileY) {
            if (tileY < 0 || tileY >= tileCount) continue
            for (tileX in minTileX..maxTileX) {
                val wrappedTileX = wrapTileX(tileX, tileCount)
                val key = TileKey(zoom, wrappedTileX, tileY)
                val drawX = (tileX * TILE_SIZE - topLeftX).roundToInt()
                val drawY = (tileY * TILE_SIZE - topLeftY).roundToInt()
                val tile = tileCache[key]

                if (tile != null) {
                    g.drawImage(tile, drawX, drawY, TILE_SIZE, TILE_SIZE, null)
                } else {
                    paintTilePlaceholder(g, drawX, drawY)
                    requestTile(key)
                }
            }
        }
    }

    private fun paintTilePlaceholder(g: Graphics2D, x: Int, y: Int) {
        g.color = Color(0xFFFFF5F9.toInt())
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE)
        g.color = Color(0xFFF5DDE7.toInt())
        g.drawRect(x, y, TILE_SIZE, TILE_SIZE)
    }

    private fun paintMarkers(g: Graphics2D) {
        for (cafe in normalizedCafes) {
            val point = latLngToScreenPoint(cafe.latitude, cafe.longitude)
            val radius = if (cafe.id == selectedCafeId) 11 else 9

            g.color = Color(0x44000000, true)
            g.fillOval(point.x - radius + 1, point.y - radius + 2, radius * 2, radius * 2)
            g.color = Color.WHITE
            g.fillOval(point.x - radius, point.y - radius, radius * 2, radius * 2)
            g.color = Color(0xFFEF6797.toInt())
            g.fillOval(point.x - radius + 3, point.y - radius + 3, (radius - 3) * 2, (radius - 3) * 2)
            markerHitAreas += MarkerHitArea(cafe.id, point.x, point.y, radius + 8)
        }
    }

    private fun paintSelectedPopup(g: Graphics2D) {
        val selectedCafe = normalizedCafes.firstOrNull { cafe -> cafe.id == selectedCafeId } ?: return
        val point = latLngToScreenPoint(selectedCafe.latitude, selectedCafe.longitude)
        val nameFont = Font(Font.SANS_SERIF, Font.BOLD, 13)
        val checkFont = Font(Font.SANS_SERIF, Font.BOLD, 16)
        val metrics = g.getFontMetrics(nameFont)
        val nameWidth = metrics.stringWidth(selectedCafe.name)
        val popupWidth = max(96, nameWidth + 54)
        val popupHeight = 38
        val popupX = (point.x - popupWidth / 2).coerceIn(8, max(8, width - popupWidth - 8))
        val popupY = (point.y - popupHeight - 20).coerceIn(8, max(8, height - popupHeight - 8))
        val anchorX = point.x.coerceIn(popupX + 18, popupX + popupWidth - 18)
        val bubbleBottom = popupY + popupHeight
        val popupShape = RoundRectangle2D.Double(
            popupX.toDouble(),
            popupY.toDouble(),
            popupWidth.toDouble(),
            popupHeight.toDouble(),
            16.0,
            16.0
        )
        val pointerShape = Path2D.Double().apply {
            moveTo(anchorX - 8.0, bubbleBottom - 1.0)
            lineTo(anchorX + 8.0, bubbleBottom - 1.0)
            lineTo(point.x.toDouble(), (point.y - 10).toDouble())
            closePath()
        }

        g.color = Color(0x33000000, true)
        g.fill(
            RoundRectangle2D.Double(
                popupX + 1.0,
                popupY + 2.0,
                popupWidth.toDouble(),
                popupHeight.toDouble(),
                16.0,
                16.0
            )
        )
        g.color = Color.WHITE
        g.fill(popupShape)
        g.fill(pointerShape)
        g.color = Color(0xFFE9D5DE.toInt())
        g.stroke = BasicStroke(1f)
        g.draw(popupShape)

        val textX = popupX + 12
        val textY = popupY + 24
        val checkX = popupX + popupWidth - 34
        val checkY = popupY + 7

        g.font = nameFont
        g.color = Color(0xFF2B2330.toInt())
        g.drawString(selectedCafe.name, textX, textY)
        g.font = checkFont
        g.color = Color(0xFFEF6797.toInt())
        g.drawString("✓", checkX + 8, checkY + 19)

        popupNameHitArea = PopupHitArea(selectedCafe.id, textX, popupY, nameWidth, popupHeight)
        popupCheckInHitArea = PopupHitArea(selectedCafe.id, checkX, checkY, 28, 28)
    }

    private fun handleClick(point: Point) {
        val checkHit = popupCheckInHitArea
        if (checkHit != null && checkHit.contains(point)) {
            onCafeCheckIn(checkHit.cafeId)
            return
        }
        val nameHit = popupNameHitArea
        if (nameHit != null && nameHit.contains(point)) {
            onCafeClick(nameHit.cafeId)
            return
        }
        val markerHit = markerHitAreas.lastOrNull { hitArea -> hitArea.contains(point) }
        selectedCafeId = markerHit?.cafeId
        repaint()
    }

    private fun handleWheel(event: MouseWheelEvent) {
        val nextZoom = (zoom - event.wheelRotation).coerceIn(MIN_ZOOM, MAX_ZOOM)

        if (nextZoom == zoom) return
        zoom = nextZoom
        repaint()
    }

    private fun latLngToScreenPoint(latitude: Double, longitude: Double): Point {
        val centerWorld = latLngToWorldPixel(centerLatitude, centerLongitude, zoom)
        val itemWorld = latLngToWorldPixel(latitude, longitude, zoom)
        return Point(
            (width / 2.0 + itemWorld.x - centerWorld.x).roundToInt(),
            (height / 2.0 + itemWorld.y - centerWorld.y).roundToInt()
        )
    }

    private fun requestTile(key: TileKey) {
        if (!loadingTiles.add(key)) return
        tileExecutor.execute {
            try {
                val tile = loadTile(key)

                if (tile != null) {
                    tileCache[key] = tile
                }
            } finally {
                loadingTiles.remove(key)
                SwingUtilities.invokeLater {
                    repaint()
                }
            }
        }
    }

    private fun loadTile(key: TileKey): BufferedImage? {
        val url = URL("https://tile.openstreetmap.org/${key.zoom}/${key.x}/${key.y}.png")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TILE_CONNECT_TIMEOUT_MS
            readTimeout = TILE_READ_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("User-Agent", TILE_USER_AGENT)
        }
        return connection.inputStream.use { input ->
            ImageIO.read(input)
        }
    }
}

private fun resolveCamera(
    cafes: List<NormalizedCafeMapItem>,
    cameraTarget: CheckInMapCameraTarget?
): MapCamera {
    val latitude = when {
        cafes.isNotEmpty() -> cafes.map { cafe -> cafe.latitude }.averageOrDefault(DEFAULT_LATITUDE)
        cameraTarget != null -> cameraTarget.latitude
        else -> DEFAULT_LATITUDE
    }
    val longitude = when {
        cafes.isNotEmpty() -> cafes.map { cafe -> cafe.longitude }.averageOrDefault(DEFAULT_LONGITUDE)
        cameraTarget != null -> cameraTarget.longitude
        else -> DEFAULT_LONGITUDE
    }
    val zoom = when {
        cafes.size == 1 -> 15
        cafes.size > 1 -> 13
        cameraTarget != null -> cameraTarget.zoom.roundToInt()
        else -> DEFAULT_ZOOM
    }.coerceIn(MIN_ZOOM, MAX_ZOOM)
    return MapCamera(
        latitude = latitude,
        longitude = longitude,
        zoom = zoom
    )
}

private fun latLngToWorldPixel(latitude: Double, longitude: Double, zoom: Int): WorldPixel {
    val sinLatitude = kotlin.math.sin(latitude.coerceIn(MIN_LATITUDE, MAX_LATITUDE) * PI / 180.0)
    val scale = TILE_SIZE * 2.0.pow(zoom)
    val x = (longitude + 180.0) / 360.0 * scale
    val y = (0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * PI)) * scale
    return WorldPixel(x, y)
}

private fun worldPixelToLatLng(worldX: Double, worldY: Double, zoom: Int): MapCamera {
    val scale = TILE_SIZE * 2.0.pow(zoom)
    val longitude = worldX / scale * 360.0 - 180.0
    val latitude = atan(sinh(PI * (1.0 - 2.0 * worldY / scale))) * 180.0 / PI
    return MapCamera(
        latitude = latitude.coerceIn(MIN_LATITUDE, MAX_LATITUDE),
        longitude = normalizeLongitudeValue(longitude),
        zoom = zoom
    )
}

private fun normalizeLongitudeValue(longitude: Double): Double {
    var result = longitude

    while (result < -180.0) result += 360.0
    while (result > 180.0) result -= 360.0
    return result
}

private fun wrapTileX(tileX: Int, tileCount: Int): Int {
    return ((tileX % tileCount) + tileCount) % tileCount
}

private fun List<Double>.averageOrDefault(default: Double): Double {
    return if (isEmpty()) default else average()
}

private fun normalizeLatitude(value: Double): Double? {
    if (!value.isFinite()) {
        return null
    }
    if (value < -90.0 || value > 90.0) {
        return null
    }
    return value
}

private fun normalizeLongitude(value: Double): Double? {
    if (!value.isFinite()) {
        return null
    }
    if (value < -180.0 || value > 180.0) {
        return null
    }
    return value
}

private data class TileKey(
    val zoom: Int,
    val x: Int,
    val y: Int
)

private data class WorldPixel(
    val x: Double,
    val y: Double
)

private data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Int
)

private data class NormalizedCafeMapItem(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private data class MarkerHitArea(
    val cafeId: String,
    val x: Int,
    val y: Int,
    val radius: Int
) {
    fun contains(point: Point): Boolean {
        val dx = point.x - x
        val dy = point.y - y
        return dx * dx + dy * dy <= radius * radius
    }
}

private data class PopupHitArea(
    val cafeId: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun contains(point: Point): Boolean {
        return point.x in x..(x + width) && point.y in y..(y + height)
    }
}

private const val TILE_SIZE = 256

private const val MAP_CORNER_RADIUS = 48

private const val MIN_ZOOM = 3

private const val MAX_ZOOM = 18

private const val DEFAULT_ZOOM = 13

private const val MIN_LATITUDE = -85.05112878

private const val MAX_LATITUDE = 85.05112878

private const val DEFAULT_LATITUDE = 37.5665

private const val DEFAULT_LONGITUDE = 126.9780

private const val TILE_CONNECT_TIMEOUT_MS = 5000

private const val TILE_READ_TIMEOUT_MS = 5000

private const val TILE_USER_AGENT = "ConCafeDesktop/1.0 (https://concafe.app)"

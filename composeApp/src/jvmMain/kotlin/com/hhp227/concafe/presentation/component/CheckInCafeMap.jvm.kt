package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject

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
            JvmCheckInGoogleMapPanel()
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

private class JvmCheckInGoogleMapPanel : JPanel(BorderLayout()) {
    private var jfxPanel: JFXPanel? = null

    private var webEngine: WebEngine? = null

    private var cafes: List<CheckInCafeSummary> = emptyList()

    private var cameraTarget: CheckInMapCameraTarget? = null

    private var onCafeClick: (String) -> Unit = {}

    private var onCafeCheckIn: (String) -> Unit = {}

    private var isBridgeListenerAttached: Boolean = false

    fun bind(
        cafes: List<CheckInCafeSummary>,
        cameraTarget: CheckInMapCameraTarget?,
        onCafeClick: (String) -> Unit,
        onCafeCheckIn: (String) -> Unit
    ) {
        this.cafes = cafes
        this.cameraTarget = cameraTarget
        this.onCafeClick = onCafeClick
        this.onCafeCheckIn = onCafeCheckIn

        if (webEngine != null) {
            Platform.runLater {
                renderMapHtml()
            }
        }
    }

    private fun renderMapHtml() {
        val engine = webEngine ?: return
        val apiKey = resolveGoogleMapsApiKey()
        val html = buildCheckInMapHtml(
            apiKey = apiKey,
            cafes = cafes,
            cameraTarget = cameraTarget
        )

        if (isBridgeListenerAttached == false) {
            engine.loadWorker.stateProperty().addListener { _, _, newState ->
                if (newState == Worker.State.SUCCEEDED) {
                    val window = engine.executeScript("window") as? JSObject ?: return@addListener

                    window.setMember("ConCafeBridge", CafeClickBridge(
                        onCafeClick = { cafeId -> onCafeClick(cafeId) },
                        onCafeCheckIn = { cafeId -> onCafeCheckIn(cafeId) }
                    ))
                }
            }
            isBridgeListenerAttached = true
        }
        engine.loadContent(html)
    }

    init {
        SwingUtilities.invokeLater {
            val panel = JFXPanel()
            jfxPanel = panel
            add(panel, BorderLayout.CENTER)
            revalidate()
            repaint()
            Platform.setImplicitExit(false)
            Platform.runLater {
                val webView = WebView()
                webEngine = webView.engine
                panel.scene = Scene(webView)
                renderMapHtml()
            }
        }
    }
}

private class CafeClickBridge(
    private val onCafeClick: (String) -> Unit,
    private val onCafeCheckIn: (String) -> Unit
) {
    fun onCafeClicked(cafeId: String) {
        SwingUtilities.invokeLater {
            onCafeClick(cafeId)
        }
    }

    fun onCafeCheckInClicked(cafeId: String) {
        SwingUtilities.invokeLater {
            onCafeCheckIn(cafeId)
        }
    }
}

private fun buildCheckInMapHtml(
    apiKey: String,
    cafes: List<CheckInCafeSummary>,
    cameraTarget: CheckInMapCameraTarget?
): String {
    if (apiKey.isBlank()) {
        return """
            <html><body style="font-family:sans-serif;padding:16px;">
            Google Maps API Key가 설정되지 않았습니다.
            </body></html>
        """.trimIndent()
    }

    val normalizedCafes = cafes.mapNotNull { cafe ->
        val normalizedLatitude = normalizeLatitude(cafe.geoPoint.latitude) ?: return@mapNotNull null
        val normalizedLongitude = normalizeLongitude(cafe.geoPoint.longitude) ?: return@mapNotNull null
        NormalizedCafeMapItem(
            id = cafe.id,
            name = cafe.name,
            latitude = normalizedLatitude,
            longitude = normalizedLongitude
        )
    }
    val centerLatitude = when {
        normalizedCafes.isNotEmpty() -> normalizedCafes.map { it.latitude }.averageOrDefault(DEFAULT_LATITUDE)
        cameraTarget != null -> cameraTarget.latitude
        else -> DEFAULT_LATITUDE
    }
    val centerLongitude = when {
        normalizedCafes.isNotEmpty() -> normalizedCafes.map { it.longitude }.averageOrDefault(DEFAULT_LONGITUDE)
        cameraTarget != null -> cameraTarget.longitude
        else -> DEFAULT_LONGITUDE
    }
    val zoom = when {
        normalizedCafes.size == 1 -> 14.5f
        normalizedCafes.size > 1 -> 12.5f
        cameraTarget != null -> cameraTarget.zoom
        else -> 13f
    }
    val cafesJson = normalizedCafes.joinToString(prefix = "[", postfix = "]") { cafe ->
        """
        {
          id: "${escapeJs(cafe.id)}",
          name: "${escapeJs(cafe.name)}",
          latitude: ${cafe.latitude},
          longitude: ${cafe.longitude}
        }
        """.trimIndent()
    }

    return """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8" />
            <style>
              html, body, #map { margin:0; padding:0; width:100%; height:100%; background:#fff5f9; }
              .callout {
                display:flex; align-items:center; gap:6px;
                background:#fff; border-radius:12px;
                padding:6px 8px 6px 12px;
                box-shadow:0 2px 8px rgba(0,0,0,0.15);
                white-space:nowrap;
              }
              .callout-name {
                font-family:sans-serif; font-size:13px; font-weight:600;
                color:#2B2330; cursor:pointer; text-decoration:none;
              }
              .callout-name:hover { text-decoration:underline; }
              .callout-checkin {
                background:none; border:none; cursor:pointer; padding:2px;
                font-size:16px; color:#EF6797; line-height:1;
              }
              .callout-checkin:hover { color:#c94c7e; }
            </style>
          </head>
          <body>
            <div id="map"></div>
            <script>
              let map;
              let currentInfoWindow = null;
              function initMap() {
                const center = { lat: $centerLatitude, lng: $centerLongitude };
                map = new google.maps.Map(document.getElementById("map"), {
                  center: center,
                  zoom: $zoom,
                  mapTypeId: "roadmap",
                  mapTypeControl: false,
                  streetViewControl: false
                });
                map.addListener("click", function() {
                  if (currentInfoWindow) { currentInfoWindow.close(); currentInfoWindow = null; }
                });
                const cafes = $cafesJson;
                cafes.forEach(function(cafe) {
                  const marker = new google.maps.Marker({
                    position: { lat: cafe.latitude, lng: cafe.longitude },
                    map: map,
                    title: cafe.name
                  });
                  const infoWindow = new google.maps.InfoWindow({
                    content: '<div class="callout">' +
                      '<span class="callout-name" onclick="onCafeNameClick(\'' + cafe.id + '\')">' + cafe.name + '</span>' +
                      '<button class="callout-checkin" onclick="onCheckInClick(\'' + cafe.id + '\')" title="체크인">&#10003;</button>' +
                      '</div>',
                    disableAutoPan: false
                  });
                  marker.addListener("click", function() {
                    if (currentInfoWindow) { currentInfoWindow.close(); }
                    infoWindow.open(map, marker);
                    currentInfoWindow = infoWindow;
                  });
                });
              }
              function onCafeNameClick(cafeId) {
                if (currentInfoWindow) { currentInfoWindow.close(); currentInfoWindow = null; }
                if (window.ConCafeBridge && window.ConCafeBridge.onCafeClicked) {
                  window.ConCafeBridge.onCafeClicked(cafeId);
                }
              }
              function onCheckInClick(cafeId) {
                if (currentInfoWindow) { currentInfoWindow.close(); currentInfoWindow = null; }
                if (window.ConCafeBridge && window.ConCafeBridge.onCafeCheckInClicked) {
                  window.ConCafeBridge.onCafeCheckInClicked(cafeId);
                }
              }
            </script>
            <script async defer src="https://maps.googleapis.com/maps/api/js?key=$apiKey&callback=initMap"></script>
          </body>
        </html>
    """.trimIndent()
}

private fun resolveGoogleMapsApiKey(): String {
    val fromEnv = System.getenv("GOOGLE_MAPS_API_KEY")?.trim().orEmpty()
    if (fromEnv.isNotBlank()) {
        return fromEnv
    }

    val fromProperty = System.getProperty("google.maps.api.key")?.trim().orEmpty()
    if (fromProperty.isNotBlank()) {
        return fromProperty
    }

    val fromAndroidXml = resolveGoogleMapsApiKeyFromAndroidXml()
    if (fromAndroidXml.isNotBlank()) {
        return fromAndroidXml
    }

    return ""
}

private fun resolveGoogleMapsApiKeyFromAndroidXml(): String {
    val candidatePaths = listOf(
        "composeApp/src/androidMain/res/values/google_maps.xml",
        "src/androidMain/res/values/google_maps.xml"
    )
    val keyPattern = Regex("""<string\s+name=["']google_maps_api_key["'][^>]*>([^<]+)</string>""")

    for (path in candidatePaths) {
        val file = File(path)

        if (file.exists()) {
            val xml = file.readText()
            val key = keyPattern.find(xml)?.groupValues?.get(1)?.trim().orEmpty()

            if (key.isNotBlank()) {
                return key
            }
        }
    }
    return ""
}

private fun escapeJs(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
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

private data class NormalizedCafeMapItem(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private const val DEFAULT_LATITUDE = 37.5665

private const val DEFAULT_LONGITUDE = 126.9780

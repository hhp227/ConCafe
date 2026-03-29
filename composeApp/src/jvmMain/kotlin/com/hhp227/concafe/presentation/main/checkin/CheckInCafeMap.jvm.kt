package com.hhp227.concafe.presentation.main.checkin

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
                onCafeClick = onCafeClick
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

    private var isBridgeListenerAttached: Boolean = false

    fun bind(
        cafes: List<CheckInCafeSummary>,
        cameraTarget: CheckInMapCameraTarget?,
        onCafeClick: (String) -> Unit
    ) {
        this.cafes = cafes
        this.cameraTarget = cameraTarget
        this.onCafeClick = onCafeClick

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
                    window.setMember("ConCafeBridge", CafeClickBridge { cafeId ->
                        onCafeClick(cafeId)
                    })
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
    private val onCafeClick: (String) -> Unit
) {
    fun onCafeClicked(cafeId: String) {
        SwingUtilities.invokeLater {
            onCafeClick(cafeId)
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
    val centerLatitude = cameraTarget?.latitude ?: normalizedCafes.map { it.latitude }.averageOrDefault(DEFAULT_LATITUDE)
    val centerLongitude = cameraTarget?.longitude ?: normalizedCafes.map { it.longitude }.averageOrDefault(DEFAULT_LONGITUDE)
    val zoom = cameraTarget?.zoom ?: 13f
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
            </style>
          </head>
          <body>
            <div id="map"></div>
            <script>
              let map;
              function initMap() {
                const center = { lat: $centerLatitude, lng: $centerLongitude };
                map = new google.maps.Map(document.getElementById("map"), {
                  center: center,
                  zoom: $zoom,
                  mapTypeId: "roadmap",
                  mapTypeControl: false,
                  streetViewControl: false
                });
                const cafes = $cafesJson;
                cafes.forEach(function(cafe) {
                  const marker = new google.maps.Marker({
                    position: { lat: cafe.latitude, lng: cafe.longitude },
                    map: map,
                    title: cafe.name
                  });
                  marker.addListener("click", function() {
                    if (window.ConCafeBridge && window.ConCafeBridge.onCafeClicked) {
                      window.ConCafeBridge.onCafeClicked(cafe.id);
                    }
                  });
                });
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

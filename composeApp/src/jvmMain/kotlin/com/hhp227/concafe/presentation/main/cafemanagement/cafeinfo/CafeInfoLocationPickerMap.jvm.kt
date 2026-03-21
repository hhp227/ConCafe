package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
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
actual fun CafeInfoLocationPickerMap(
    latitude: Double,
    longitude: Double,
    onLocationSelected: (latitude: Double, longitude: Double, address: String?) -> Unit,
    modifier: Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            JvmCafeInfoLocationPickerPanel()
        },
        update = { panel ->
            panel.bind(
                latitude = latitude,
                longitude = longitude,
                onLocationSelected = onLocationSelected
            )
        }
    )
}

private class JvmCafeInfoLocationPickerPanel : JPanel(BorderLayout()) {
    private var jfxPanel: JFXPanel? = null

    private var webEngine: WebEngine? = null

    private var latitude: Double = 37.5665

    private var longitude: Double = 126.9780

    private var onLocationSelected: (Double, Double, String?) -> Unit = { _, _, _ -> }

    private var isBridgeListenerAttached: Boolean = false

    private fun renderMapHtml() {
        val engine = webEngine ?: return
        val apiKey = resolveGoogleMapsApiKey()
        val html = buildLocationPickerMapHtml(
            apiKey = apiKey,
            latitude = latitude,
            longitude = longitude
        )

        if (!isBridgeListenerAttached) {
            engine.loadWorker.stateProperty().addListener { _, _, newState ->
                if (newState == Worker.State.SUCCEEDED) {
                    val window = engine.executeScript("window") as? JSObject ?: return@addListener
                    window.setMember("ConCafeBridge", LocationBridge { pickedLatitude, pickedLongitude, address ->
                        onLocationSelected(pickedLatitude, pickedLongitude, address)
                    })
                }
            }
            isBridgeListenerAttached = true
        }
        engine.loadContent(html)
    }

    fun bind(
        latitude: Double,
        longitude: Double,
        onLocationSelected: (Double, Double, String?) -> Unit
    ) {
        this.latitude = latitude
        this.longitude = longitude
        this.onLocationSelected = onLocationSelected

        if (webEngine != null) {
            Platform.runLater {
                renderMapHtml()
            }
        }
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

private class LocationBridge(
    private val onLocationSelected: (Double, Double, String?) -> Unit
) {
    fun onLocationSelected(latitude: Double, longitude: Double, address: String?) {
        SwingUtilities.invokeLater {
            onLocationSelected(latitude, longitude, address)
        }
    }
}

private fun buildLocationPickerMapHtml(
    apiKey: String,
    latitude: Double,
    longitude: Double
): String {
    if (apiKey.isBlank()) {
        return """
            <html><body style="font-family:sans-serif;padding:16px;">
            Google Maps API Key가 설정되지 않았습니다.
            </body></html>
        """.trimIndent()
    }

    val normalizedLatitude = normalizeLatitude(latitude) ?: DEFAULT_LATITUDE
    val normalizedLongitude = normalizeLongitude(longitude) ?: DEFAULT_LONGITUDE

    return """
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8" />
            <style>
              html, body, #map { margin:0; padding:0; width:100%; height:100%; background:#f4eff2; }
            </style>
          </head>
          <body>
            <div id="map"></div>
            <script>
              let map;
              let marker;
              let geocoder;
              function initMap() {
                const selected = { lat: $normalizedLatitude, lng: $normalizedLongitude };
                geocoder = new google.maps.Geocoder();
                map = new google.maps.Map(document.getElementById("map"), {
                  center: selected,
                  zoom: 15,
                  mapTypeId: "roadmap",
                  mapTypeControl: false,
                  streetViewControl: false
                });
                marker = new google.maps.Marker({
                  position: selected,
                  map: map,
                  title: "선택한 위치"
                });
                map.addListener("click", function(event) {
                  const picked = event.latLng;
                  marker.setPosition(picked);
                  geocoder.geocode({ location: picked }, function(results, status) {
                    let address = "";
                    if (status === "OK" && results && results.length > 0) {
                      address = results[0].formatted_address;
                    }
                    if (window.ConCafeBridge && window.ConCafeBridge.onLocationSelected) {
                      window.ConCafeBridge.onLocationSelected(
                        picked.lat(),
                        picked.lng(),
                        address
                      );
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

private const val DEFAULT_LATITUDE = 37.5665

private const val DEFAULT_LONGITUDE = 126.9780

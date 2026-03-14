package com.hhp227.concafe.presentation.external

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.BorderLayout
import java.net.URL
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane

@Composable
actual fun ExternalLinkWebView(
    url: String,
    modifier: Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val editorPane = JEditorPane().apply {
                isEditable = false
                contentType = "text/html"
                text = "<html><body style='font-family:sans-serif;padding:16px;'>로딩 중...</body></html>"
            }

            JPanel(BorderLayout()).apply {
                add(JScrollPane(editorPane), BorderLayout.CENTER)

                runCatching {
                    editorPane.page = URL(url)
                }.onFailure {
                    editorPane.text =
                        "<html><body style='font-family:sans-serif;padding:16px;'>페이지를 불러올 수 없습니다.<br/><br/>$url</body></html>"
                }
            }
        }
    )
}

package com.music.bitchord.desktop

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.net.URI

object DesktopExternalLinks {
    /** Puts [text] on the system clipboard. */
    fun copy(text: String): Result<Unit> = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(java.awt.datatransfer.StringSelection(text), null)
    }

    /** Reads plain text from the system clipboard, or returns null when unavailable. */
    fun readClipboardText(): String? = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val contents = clipboard.getContents(null) ?: return null
        if (!contents.isDataFlavorSupported(DataFlavor.stringFlavor)) return null
        (contents.getTransferData(DataFlavor.stringFlavor) as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun open(uri: String): Result<Unit> = runCatching {
        check(Desktop.isDesktopSupported()) { "This desktop environment cannot open links" }
        Desktop.getDesktop().browse(URI(uri))
    }
}

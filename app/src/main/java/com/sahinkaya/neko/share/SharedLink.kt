package com.sahinkaya.neko.share

import android.content.Intent

/** Pulls the link out of text shared from another app ("Check this out https://..." → the URL). */
object SharedLink {
    private val urlPattern = Regex("""https?://\S+""")

    fun from(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return urlPattern.find(text)?.value?.trimEnd('.', ',', ')', '"', '\'')
    }
}

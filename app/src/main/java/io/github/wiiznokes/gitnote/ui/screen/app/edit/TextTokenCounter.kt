package io.github.wiiznokes.gitnote.ui.screen.app.edit

import com.google.genai.LocalTokenizer
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.EncodingType
import io.github.wiiznokes.gitnote.data.TokenCounterMode

internal object TextTokenCounter {
    private val genericEncoding by lazy {
        Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.O200K_BASE)
    }

    private val geminiTokenizer by lazy {
        LocalTokenizer("gemini-2.5-flash")
    }

    fun count(text: String, mode: TokenCounterMode): Int {
        if (text.isEmpty()) return 0

        return when (mode) {
            TokenCounterMode.Generic -> genericEncoding.encode(text).size()
            TokenCounterMode.Gemini ->
                geminiTokenizer.countTokens(text).totalTokens().orElse(0)
        }
    }
}

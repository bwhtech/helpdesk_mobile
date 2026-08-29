package com.example.helpdeskanalytics.ui.components

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.MaterialTheme
import androidx.core.text.HtmlCompat

private const val URL_TAG = "url"

/**
 * Renders an HTML fragment (Frappe rich-text fields are HTML) as formatted text:
 * paragraphs, line breaks, bold/italic, strike, underline, list bullets and
 * tappable links. Tags the platform can't style are dropped to plain text.
 */
@Composable
fun HtmlText(
    html: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(html, linkColor) { htmlToAnnotatedString(html, linkColor) }
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            annotated.getStringAnnotations(URL_TAG, offset, offset).firstOrNull()?.let {
                uriHandler.openWebLink(it.item)
            }
        }
    )
}

/** True when [html] has visible text once tags are stripped. */
fun htmlHasContent(html: String): Boolean =
    HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().isNotBlank()

private fun htmlToAnnotatedString(html: String, linkColor: Color): AnnotatedString {
    // HtmlCompat drops the BulletSpan glyph in plain-text conversion; inline a bullet.
    val prepared = html.replace("<li>", "<li>•  ", ignoreCase = true)
    val spanned: Spanned = HtmlCompat.fromHtml(prepared, HtmlCompat.FROM_HTML_MODE_COMPACT)
    // trimEnd only; trimming the start would shift every span offset out of alignment.
    val text = spanned.toString().trimEnd()

    return buildAnnotatedString {
        append(text)
        if (text.isEmpty()) return@buildAnnotatedString
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span).coerceIn(0, text.length)
            val end = spanned.getSpanEnd(span).coerceIn(0, text.length)
            if (start >= end) return@forEach
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD ->
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC ->
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC ->
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                            start, end
                        )
                }
                is UnderlineSpan ->
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                is StrikethroughSpan ->
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                is URLSpan -> {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start, end
                    )
                    addStringAnnotation(URL_TAG, span.url ?: "", start, end)
                }
            }
        }
    }
}

package com.privatemessenger.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.privatemessenger.app.i18n.AppLanguage
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.i18n.stringsFor

// ---------------------------------------------------------------------------
// Tema do app. Duas coisas configuráveis pela pessoa (tela Preferences):
//   1) MODO: escuro (o do seu mockup) ou claro (azul/branco).
//   2) TAMANHO DA LETRA: pequena / normal / grande.
//
// O tamanho da letra é aplicado de um jeito esperto: em vez de mudar o tamanho
// de CADA texto na mão, a gente "engana" o Compose sobre o fontScale do
// aparelho (via LocalDensity). Aí TODO texto em `sp` cresce/encolhe junto,
// numa tacada só.
// ---------------------------------------------------------------------------

// O modo escolhido. ESCURO é o padrão (combina com o mockup).
enum class ThemeMode { ESCURO, CLARO }

// Cada opção carrega o "fator" de escala aplicado nas letras.
enum class FontSize(val scale: Float) { PEQUENA(0.85f), NORMAL(1.0f), GRANDE(1.25f) }

// Papel de parede do chat (fundo atrás das bolhas de mensagem). PADRAO = usa a
// cor de fundo do tema; as outras são cores próprias. `color = null` quer dizer
// "usa o fundo do tema".
enum class ChatWallpaper(val label: String, val color: Color?) {
    PADRAO("Padrão", null),
    NOITE("Azul-noite", Color(0xFF0E1A2B)),
    OCEANO("Oceano", Color(0xFF0B2530)),
    ROXO("Roxo", Color(0xFF1E1533)),
    VERDE("Verde", Color(0xFF10231A)),
    GRAFITE("Grafite", Color(0xFF17181C)),
    BEGE("Bege", Color(0xFFEDE6D6)),
}

// --- Paleta ESCURA (o visual do mockup: azul-marinho quase preto) ---
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3E5BF0),          // azul dos botões
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6E86FF),
    background = Color(0xFF0A0F1E),        // fundo da tela
    onBackground = Color(0xFFECEFF8),      // texto claro
    surface = Color(0xFF161C30),           // cards / caixas
    onSurface = Color(0xFFECEFF8),
    surfaceVariant = Color(0xFF232B45),    // campos de texto
    onSurfaceVariant = Color(0xFFAEB6D0),  // texto secundário / labels
    outline = Color(0xFF3A4568),
    error = Color(0xFFC15B5B),             // vermelho suave (botão "Log out")
    onError = Color(0xFFFFFFFF),
)

// --- Paleta CLARA (azul e branco, como você pediu) ---
private val LightColors = lightColorScheme(
    primary = Color(0xFF3E5BF0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3E5BF0),
    background = Color(0xFFEEF3FF),        // branco levemente azulado
    onBackground = Color(0xFF0E1430),      // texto azul-marinho escuro
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1430),
    surfaceVariant = Color(0xFFE1E8FB),    // campos de texto
    onSurfaceVariant = Color(0xFF5A6488),  // texto secundário
    outline = Color(0xFFB8C2E0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    fontSize: FontSize,
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val colors = if (themeMode == ThemeMode.ESCURO) DarkColors else LightColors

    // Reaproveita a densidade atual do aparelho, só trocando o fontScale.
    val base = LocalDensity.current
    val scaledDensity = Density(density = base.density, fontScale = base.fontScale * fontSize.scale)

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalStrings provides stringsFor(language), // textos no idioma escolhido
    ) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

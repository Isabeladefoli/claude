package com.privatemessenger.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// "Kit" de peças visuais reaproveitadas nas telas, pra tudo ficar com a mesma
// cara do mockup (cantos arredondados, setinha de voltar, avatar redondo, botão
// azul). Assim não repetimos o mesmo código de layout em cada tela.
// ---------------------------------------------------------------------------

// Cabeçalho: uma setinha redonda de "voltar" (se onBack != null) + o título.
@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// Base do servidor (ex: http://192.168.x.x:8080), pra montar a URL completa das
// imagens. É preenchida lá no topo do app (MainActivity) e lida aqui pelo Avatar.
val LocalBaseUrl = staticCompositionLocalOf { "" }

// Avatar redondo. Se houver foto (avatarPath, ex: "/api/media/xxx"), mostra a
// foto de verdade; senão, cai na inicial do nome (ou "?" se não houver nome).
@Composable
fun Avatar(seed: String?, size: Dp, avatarPath: String? = null) {
    val base = LocalBaseUrl.current
    val fullUrl = avatarPath?.takeIf { it.isNotBlank() }?.let { base.trimEnd('/') + it }
    val photo = rememberNetworkImage(fullUrl)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Sem foto (ou ainda carregando/falhou): mostra a inicial ou "?".
            val initial = seed?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
            Text(
                text = initial ?: "?",
                fontSize = (size.value / 2.4f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// Botão azul "cheio", igual aos do mockup (Login/Signup/Send).
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text)
    }
}

// Uma linha do menu de configurações: rótulo à esquerda, "›" à direita, clicável.
@Composable
fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Texto centralizado auxiliar (usado em telas simples).
@Composable
fun CenteredHint(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

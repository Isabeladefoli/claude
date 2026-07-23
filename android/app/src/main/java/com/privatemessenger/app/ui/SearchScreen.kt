package com.privatemessenger.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.privatemessenger.app.i18n.LocalStrings
import com.privatemessenger.app.ui.components.TopBar

// Busca de usuário por nome. Ao pesquisar, vai pra página de perfil da pessoa.
// (A validação de "existe ou não" acontece lá no perfil, que mostra o resultado.)
@Composable
fun SearchScreen(onBack: () -> Unit, onSearch: (username: String) -> Unit) {
    val s = LocalStrings.current
    var query by remember { mutableStateOf("") }

    fun go() {
        val q = query.trim()
        if (q.isNotEmpty()) onSearch(q)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar(onBack = onBack, title = s.searchHint)
        Spacer(Modifier.height(40.dp))

        Text(
            s.searchExplain,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(s.searchHint) },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { go() }) { Text("Buscar") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { go() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

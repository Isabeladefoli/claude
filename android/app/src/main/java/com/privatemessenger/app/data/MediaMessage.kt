package com.privatemessenger.app.data

// ---------------------------------------------------------------------------
// Como ainda NAO temos E2E, uma mensagem de midia (foto/audio) e guardada como
// se fosse texto, no mesmo campo, com um "prefixo secreto" que texto normal
// nunca teria: o caractere de controle STX (codigo 2).
//
// Formato:   <SEP><tipo><SEP><caminho>
//   ex:      image  /api/media/abc123
//            audio  /api/media/def456
//
// Assim nao precisamos mudar a tabela de mensagens agora. Quando o E2E entrar,
// isso vira parte do conteudo criptografado, sem mudar o resto.
// ---------------------------------------------------------------------------
object MediaMessage {
    // Separador: caractere de controle STX (codigo 2). Montado pelo codigo do
    // caractere pra nao precisar digitar nada invisivel no arquivo-fonte.
    private val SEP: String = 2.toChar().toString()

    const val IMAGE = "image"
    const val AUDIO = "audio"

    fun encode(kind: String, path: String): String = SEP + kind + SEP + path

    // Devolve (tipo, caminho) se for midia; null se for texto normal.
    fun parse(text: String): Pair<String, String>? {
        if (!text.startsWith(SEP)) return null
        val parts = text.split(SEP)
        if (parts.size < 3) return null
        return parts[1] to parts[2]
    }
}

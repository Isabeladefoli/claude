package com.privatemessenger.app.i18n

import androidx.compose.runtime.staticCompositionLocalOf

// ---------------------------------------------------------------------------
// Internacionalização (i18n) simples e "à mão".
//
// Ideia: cada idioma é um objeto AppStrings com TODOS os textos das telas. A
// pessoa escolhe o idioma na tela Languages; o app guarda a escolha e as telas
// leem os textos de LocalStrings.current. Trocar de idioma é instantâneo.
//
// Por enquanto cobrimos as telas novas (configurações, conta, suporte, perfil).
// As telas antigas vão sendo traduzidas aos poucos — a "máquina" já está pronta,
// é só ir preenchendo mais campos aqui.
// ---------------------------------------------------------------------------

// Os quatro idiomas, cada um com a bandeira (emoji) e o nome no próprio idioma.
enum class AppLanguage(val flag: String, val label: String) {
    PORTUGUES("🇵🇹", "Português"),
    INGLES("🇬🇧", "English"),
    ESPANHOL("🇪🇸", "Español"),
    NORUEGUES("🇳🇴", "Norsk"),
}

// Todos os textos usados nas telas novas. Um campo por frase.
data class AppStrings(
    // gerais
    val back: String,
    val save: String,
    val cancel: String,
    val yes: String,
    val no: String,
    val delete: String,
    // menu de configurações (engrenagem)
    val settings: String,
    val accountDetails: String,
    val preferences: String,
    val languages: String,
    val support: String,
    val version: String,
    val logout: String,
    val logoutConfirmTitle: String,
    val logoutConfirmMsg: String,
    // preferences
    val appearance: String,
    val darkMode: String,
    val lightMode: String,
    val textSize: String,
    val small: String,
    val normal: String,
    val large: String,
    // languages
    val chooseLanguage: String,
    // support
    val supportEmailLabel: String,
    val supportSubjectLabel: String,
    val supportBodyLabel: String,
    val supportNotice: String,
    val send: String,
    val supportSent: String,
    // account details
    val unlockMsg: String,
    val enterPassword: String,
    val unlock: String,
    val wrongPassword: String,
    val fieldUsername: String,
    val fieldName: String,
    val fieldBirthday: String,
    val fieldPassword: String,
    val changePassword: String,
    val currentPassword: String,
    val newPassword: String,
    val passwordChanged: String,
    val deleteAccount: String,
    val deleteAccountConfirm: String,
    val accountDeleted: String,
    val saved: String,
    // busca / perfil
    val searchHint: String,
    val searchExplain: String,
    val userNotFound: String,
    val profileMessage: String,
    val profileAddFriend: String,
)

private val PT = AppStrings(
    back = "Voltar", save = "Salvar", cancel = "Cancelar", yes = "Sim", no = "Não", delete = "Apagar",
    settings = "Configurações", accountDetails = "Detalhes da conta", preferences = "Preferências",
    languages = "Idiomas", support = "Suporte", version = "Versão 1.0", logout = "Sair",
    logoutConfirmTitle = "Sair da conta", logoutConfirmMsg = "Tem certeza mesmo que quer sair?",
    appearance = "Aparência", darkMode = "Modo escuro", lightMode = "Modo claro",
    textSize = "Tamanho da letra", small = "Pequena", normal = "Normal", large = "Grande",
    chooseLanguage = "Escolha o idioma",
    supportEmailLabel = "Seu e-mail (pra resposta)", supportSubjectLabel = "Título",
    supportBodyLabel = "Escreva seu report", supportNotice = "Esse report vai para o e-mail de suporte do app: isabeladefoli@gmail.com",
    send = "Enviar", supportSent = "Report enviado! Obrigado :)",
    unlockMsg = "Digite sua senha para ver os detalhes da conta.", enterPassword = "Senha", unlock = "Entrar",
    wrongPassword = "Senha incorreta",
    fieldUsername = "Nome de usuário", fieldName = "Nome", fieldBirthday = "Aniversário", fieldPassword = "Senha",
    changePassword = "Trocar senha", currentPassword = "Senha atual", newPassword = "Nova senha",
    passwordChanged = "Senha trocada!", deleteAccount = "Apagar conta",
    deleteAccountConfirm = "Isso apaga sua conta e tudo para sempre. Digite sua senha para confirmar.",
    accountDeleted = "Conta apagada.", saved = "Salvo!",
    searchHint = "Buscar", searchExplain = "Essa busca encontra pessoas pelo nome de usuário.",
    userNotFound = "Usuário não encontrado", profileMessage = "Mensagem", profileAddFriend = "Adicionar",
)

private val EN = AppStrings(
    back = "Back", save = "Save", cancel = "Cancel", yes = "Yes", no = "No", delete = "Delete",
    settings = "Settings", accountDetails = "Account details", preferences = "Preferences",
    languages = "Languages", support = "Support", version = "Version 1.0", logout = "Log out",
    logoutConfirmTitle = "Log out", logoutConfirmMsg = "Are you sure you want to log out?",
    appearance = "Appearance", darkMode = "Dark mode", lightMode = "Light mode",
    textSize = "Text size", small = "Small", normal = "Normal", large = "Large",
    chooseLanguage = "Choose language",
    supportEmailLabel = "Your email (for the reply)", supportSubjectLabel = "Title",
    supportBodyLabel = "Write your report", supportNotice = "This report goes to the app's support email: isabeladefoli@gmail.com",
    send = "Send", supportSent = "Report sent! Thank you :)",
    unlockMsg = "Enter your password to see account details.", enterPassword = "Password", unlock = "Enter",
    wrongPassword = "Wrong password",
    fieldUsername = "Username", fieldName = "Name", fieldBirthday = "Birthday", fieldPassword = "Password",
    changePassword = "Change password", currentPassword = "Current password", newPassword = "New password",
    passwordChanged = "Password changed!", deleteAccount = "Delete account",
    deleteAccountConfirm = "This deletes your account and everything forever. Enter your password to confirm.",
    accountDeleted = "Account deleted.", saved = "Saved!",
    searchHint = "Search", searchExplain = "This search finds people by their username.",
    userNotFound = "User not found", profileMessage = "Message", profileAddFriend = "Add",
)

private val ES = AppStrings(
    back = "Volver", save = "Guardar", cancel = "Cancelar", yes = "Sí", no = "No", delete = "Eliminar",
    settings = "Ajustes", accountDetails = "Detalles de la cuenta", preferences = "Preferencias",
    languages = "Idiomas", support = "Soporte", version = "Versión 1.0", logout = "Salir",
    logoutConfirmTitle = "Cerrar sesión", logoutConfirmMsg = "¿Seguro que quieres salir?",
    appearance = "Apariencia", darkMode = "Modo oscuro", lightMode = "Modo claro",
    textSize = "Tamaño del texto", small = "Pequeña", normal = "Normal", large = "Grande",
    chooseLanguage = "Elige el idioma",
    supportEmailLabel = "Tu correo (para la respuesta)", supportSubjectLabel = "Título",
    supportBodyLabel = "Escribe tu reporte", supportNotice = "Este reporte va al correo de soporte de la app: isabeladefoli@gmail.com",
    send = "Enviar", supportSent = "¡Reporte enviado! Gracias :)",
    unlockMsg = "Escribe tu contraseña para ver los detalles.", enterPassword = "Contraseña", unlock = "Entrar",
    wrongPassword = "Contraseña incorrecta",
    fieldUsername = "Usuario", fieldName = "Nombre", fieldBirthday = "Cumpleaños", fieldPassword = "Contraseña",
    changePassword = "Cambiar contraseña", currentPassword = "Contraseña actual", newPassword = "Nueva contraseña",
    passwordChanged = "¡Contraseña cambiada!", deleteAccount = "Eliminar cuenta",
    deleteAccountConfirm = "Esto elimina tu cuenta y todo para siempre. Escribe tu contraseña para confirmar.",
    accountDeleted = "Cuenta eliminada.", saved = "¡Guardado!",
    searchHint = "Buscar", searchExplain = "Esta búsqueda encuentra personas por su usuario.",
    userNotFound = "Usuario no encontrado", profileMessage = "Mensaje", profileAddFriend = "Añadir",
)

private val NO = AppStrings(
    back = "Tilbake", save = "Lagre", cancel = "Avbryt", yes = "Ja", no = "Nei", delete = "Slett",
    settings = "Innstillinger", accountDetails = "Kontodetaljer", preferences = "Innstillinger",
    languages = "Språk", support = "Støtte", version = "Versjon 1.0", logout = "Logg ut",
    logoutConfirmTitle = "Logg ut", logoutConfirmMsg = "Er du sikker på at du vil logge ut?",
    appearance = "Utseende", darkMode = "Mørk modus", lightMode = "Lys modus",
    textSize = "Tekststørrelse", small = "Liten", normal = "Normal", large = "Stor",
    chooseLanguage = "Velg språk",
    supportEmailLabel = "E-posten din (for svar)", supportSubjectLabel = "Tittel",
    supportBodyLabel = "Skriv rapporten din", supportNotice = "Denne rapporten går til appens støtte-e-post: isabeladefoli@gmail.com",
    send = "Send", supportSent = "Rapport sendt! Takk :)",
    unlockMsg = "Skriv inn passordet ditt for å se kontodetaljer.", enterPassword = "Passord", unlock = "Åpne",
    wrongPassword = "Feil passord",
    fieldUsername = "Brukernavn", fieldName = "Navn", fieldBirthday = "Bursdag", fieldPassword = "Passord",
    changePassword = "Bytt passord", currentPassword = "Nåværende passord", newPassword = "Nytt passord",
    passwordChanged = "Passord byttet!", deleteAccount = "Slett konto",
    deleteAccountConfirm = "Dette sletter kontoen din og alt for alltid. Skriv inn passordet for å bekrefte.",
    accountDeleted = "Konto slettet.", saved = "Lagret!",
    searchHint = "Søk", searchExplain = "Dette søket finner folk med brukernavnet deres.",
    userNotFound = "Fant ikke brukeren", profileMessage = "Melding", profileAddFriend = "Legg til",
)

fun stringsFor(lang: AppLanguage): AppStrings = when (lang) {
    AppLanguage.PORTUGUES -> PT
    AppLanguage.INGLES -> EN
    AppLanguage.ESPANHOL -> ES
    AppLanguage.NORUEGUES -> NO
}

// LocalStrings deixa qualquer tela ler os textos do idioma atual sem precisar
// receber tudo por parâmetro: basta `LocalStrings.current`.
val LocalStrings = staticCompositionLocalOf { PT }

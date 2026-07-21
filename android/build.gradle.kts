// Build raiz. Declara os plugins que os módulos podem usar, mas sem aplicá-los
// aqui (apply false). Cada versão fica centralizada neste ponto.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

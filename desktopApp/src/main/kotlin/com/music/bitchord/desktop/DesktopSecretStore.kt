package com.music.bitchord.desktop

import com.music.bitchord.desktop.secrets.SecretServiceApi
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.types.Variant

/** The platform's own password store, for the credentials this app holds. */
internal object DesktopSecretStore {

    /** Plain-text transport. */
    private const val PLAIN = "plain"

    private const val DEFAULT_COLLECTION = "/org/freedesktop/secrets/aliases/default"

    /** Runs [block] with an open Secret Service session, or returns null. */
    private fun <T> withService(
        block: (DBusConnection, SecretServiceApi.Service, DBusPath) -> T?,
    ): T? = runCatching {
        DBusConnectionBuilder.forSessionBus().withShared(false).build().use { connection ->
            val service = connection.getRemoteObject(
                SecretServiceApi.BUS_NAME,
                SecretServiceApi.SERVICE_PATH,
                SecretServiceApi.Service::class.java,
            )
            val session = service.OpenSession(PLAIN, Variant("")).session
            block(connection, service, session)
        }
    }.getOrElse {
        DesktopTrackLog.log("secret store unavailable: ${it.message}")
        null
    }

    /** The first secret stored under [attributes], as bytes. */
    fun lookup(attributes: Map<String, String>): ByteArray? = withService { _, service, session ->
        val found = service.SearchItems(attributes)
        val items = buildList {
            addAll(found.unlocked)
            if (found.locked.isNotEmpty()) {
                runCatching { service.Unlock(found.locked) }.getOrNull()?.let { addAll(it.unlocked) }
            }
        }
        if (items.isEmpty()) return@withService null
        service.GetSecrets(items, session).values.firstOrNull()?.value
    }

    /** Stores [secret] under [attributes], replacing whatever was there. */
    fun store(label: String, attributes: Map<String, String>, secret: ByteArray): Boolean =
        withService { connection, service, session ->
            val collection = runCatching { service.ReadAlias("default")?.path }
                .getOrNull()
                ?.takeIf { it.isNotBlank() && it != "/" }
                ?: DEFAULT_COLLECTION
            val properties = mapOf<String, Variant<*>>(
                "org.freedesktop.Secret.Item.Label" to Variant(label),
                "org.freedesktop.Secret.Item.Attributes" to Variant(attributes, "a{ss}"),
            )
            val item = connection.getRemoteObject(
                SecretServiceApi.BUS_NAME,
                collection,
                SecretServiceApi.Collection::class.java,
            )
            item.CreateItem(
                properties,
                SecretServiceApi.Secret(session, ByteArray(0), secret, "text/plain; charset=utf8"),
                true,
            )
            true
        } ?: false

    /** Forgets everything stored under [attributes]. */
    fun remove(attributes: Map<String, String>): Boolean = withService { connection, service, _ ->
        val found = service.SearchItems(attributes)
        (found.unlocked + found.locked).forEach { path ->
            runCatching {
                connection.getRemoteObject(
                    SecretServiceApi.BUS_NAME,
                    path.path,
                    SecretServiceApi.Item::class.java,
                ).Delete()
            }
        }
        true
    } ?: false

    /** Whether a store is actually reachable, for the settings screen to say so. */
    fun isAvailable(): Boolean = withService { _, _, _ -> true } == true
}

/**
 * Cross-platform encrypted/keyring credential storage used across desktop secrets
 * (Google session cookies, Discord tokens, Last.fm/ListenBrainz keys, Spotify sp_dc).
 */
internal object DesktopSecretVault {
    private const val DPAPI_PREFIX = "dpapi:v1:"

    private fun attrs(secretId: String) = mapOf("application" to "bitchord", "account" to secretId)

    fun write(
        persistence: DesktopPersistence,
        prefKey: String,
        secretId: String,
        label: String,
        secret: String,
    ) {
        if (secret.isBlank()) {
            remove(persistence, prefKey, secretId)
            return
        }
        if (DesktopPlatform.isWindows) {
            val sealed = DesktopWindowsCrypto.protect(secret.toByteArray(Charsets.UTF_8))
                ?: error("Windows DPAPI could not seal credential '$secretId'")
            DesktopPreferenceChunks.write(
                persistence.preferences,
                prefKey,
                DPAPI_PREFIX + java.util.Base64.getEncoder().encodeToString(sealed),
            )
            return
        }
        if (DesktopSecretStore.store(label, attrs(secretId), secret.toByteArray(Charsets.UTF_8))) {
            DesktopPreferenceChunks.remove(persistence.preferences, prefKey)
            deleteLocalSecretFile(secretId)
        } else {
            writeLocalSecretFile(secretId, secret)
            DesktopPreferenceChunks.remove(persistence.preferences, prefKey)
        }
    }

    fun read(
        persistence: DesktopPersistence,
        prefKey: String,
        secretId: String,
    ): String? {
        if (DesktopPlatform.isWindows) {
            val stored = DesktopPreferenceChunks.read(persistence.preferences, prefKey)
                ?.takeIf { it.isNotBlank() } ?: return null
            if (stored.startsWith(DPAPI_PREFIX)) {
                val decoded = runCatching {
                    java.util.Base64.getDecoder().decode(stored.removePrefix(DPAPI_PREFIX))
                }.getOrNull() ?: return null
                return DesktopWindowsCrypto.unprotect(decoded)
                    ?.toString(Charsets.UTF_8)
                    ?.takeIf { it.isNotBlank() }
            }
            // Migrate legacy plaintext registry entries into DPAPI automatically.
            DesktopWindowsCrypto.protect(stored.toByteArray(Charsets.UTF_8))?.let { sealed ->
                DesktopPreferenceChunks.write(
                    persistence.preferences,
                    prefKey,
                    DPAPI_PREFIX + java.util.Base64.getEncoder().encodeToString(sealed),
                )
            }
            return stored
        }
        DesktopSecretStore.lookup(attrs(secretId))
            ?.toString(Charsets.UTF_8)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        readLocalSecretFile(secretId)?.takeIf { it.isNotBlank() }?.let { return it }
        // Migrate legacy preference value out of java.util.prefs.
        val legacy = DesktopPreferenceChunks.read(persistence.preferences, prefKey)?.takeIf { it.isNotBlank() }
        if (legacy != null) {
            write(persistence, prefKey, secretId, secretId, legacy)
            return legacy
        }
        return null
    }

    fun remove(
        persistence: DesktopPersistence,
        prefKey: String,
        secretId: String,
    ) {
        DesktopSecretStore.remove(attrs(secretId))
        deleteLocalSecretFile(secretId)
        DesktopPreferenceChunks.remove(persistence.preferences, prefKey)
    }

    private fun secretFileFor(secretId: String): java.io.File {
        val safeName = secretId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dir = java.io.File(System.getProperty("user.home"), ".local/share/AuraMusic/.secrets")
        return java.io.File(dir, "$safeName.sec")
    }

    private fun writeLocalSecretFile(secretId: String, secret: String) {
        val file = secretFileFor(secretId)
        val dir = file.parentFile
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
            runCatching {
                java.nio.file.Files.setPosixFilePermissions(
                    dir.toPath(),
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),
                )
            }
        }
        file.writeText(secret, Charsets.UTF_8)
        runCatching {
            java.nio.file.Files.setPosixFilePermissions(
                file.toPath(),
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),
            )
        }
    }

    private fun readLocalSecretFile(secretId: String): String? = runCatching {
        val file = secretFileFor(secretId)
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    }.getOrNull()

    private fun deleteLocalSecretFile(secretId: String) {
        runCatching { secretFileFor(secretId).delete() }
    }
}

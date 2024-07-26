package codes.laivy.auth.impl;

import codes.laivy.auth.LaivyAuth;
import codes.laivy.auth.api.LaivyAuthApi;
import codes.laivy.auth.config.Configuration;
import codes.laivy.auth.core.Account;
import codes.laivy.auth.exception.AccountExistsException;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;

final class LaivyAuthApiImpl implements LaivyAuthApi {

    // Static initializers

    private static final @NotNull Logger log = LoggerFactory.getLogger(LaivyAuthApiImpl.class);

    // Object

    private final @NotNull ReentrantLock lock = new ReentrantLock();
    private volatile boolean flushed = false;

    private final @NotNull Map<UUID, Account> accounts = new HashMap<>();

    private final boolean successful;

    private final @NotNull Configuration configuration;
    private final @NotNull Set<Mapping> mappings = new HashSet<>();
    private @UnknownNullability Mapping mapping;

    private final @NotNull LaivyAuth plugin;

    private LaivyAuthApiImpl(@NotNull LaivyAuth plugin) {
        this.plugin = plugin;
        this.configuration = Configuration.read(plugin.getConfig());

        // Load all mappings
        boolean successful = false;
        @NotNull File file = new File(plugin.getDataFolder(), "/mappings/");

        @NotNull File @Nullable [] mappingFiles = file.listFiles();
        if (mappingFiles != null) for (@NotNull File mappingFile : mappingFiles) try {
            if (!mappingFile.isFile() || !mappingFile.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }

            try (@NotNull JarFile jar = new JarFile(mappingFile)) {
                @NotNull URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{ mappingFile.toURI().toURL() }, LaivyAuth.class.getClassLoader());
                @NotNull Class<?> main = classLoader.loadClass(jar.getManifest().getMainAttributes().getValue("Main-Class"));

                if (Mapping.class.isAssignableFrom(main)) {
                    //noinspection unchecked
                    @NotNull Constructor<Mapping> constructor = ((Class<Mapping>) main).getDeclaredConstructor(ClassLoader.class, LaivyAuthApi.class, Configuration.class);
                    constructor.setAccessible(true);

                    @NotNull Mapping mapping = constructor.newInstance(classLoader, this, getConfiguration());
                    mappings.add(mapping);
                } else {
                    log.error("The main class of mapping '{}' isn't an instance of '{}'.", mappingFile.getName(), Mapping.class);
                }
            } catch (@NotNull InvocationTargetException | @NotNull InstantiationException | @NotNull IllegalAccessException e) {
                log.error("Cannot instantiate main class of mapping '{}': {}", mappingFile.getName(), e.getMessage());
                log.atDebug().setCause(e).log();
            }
        } catch (@NotNull NoSuchMethodException e) {
            log.error("Cannot find a valid constructor of mapping '{}'. It should have a constructor with '{}', '{}' and '{}' parameters.", mappingFile.getName(), ClassLoader.class, LaivyAuthApi.class, Configuration.class);
            log.atDebug().setCause(e).log();
        } catch (@NotNull ClassNotFoundException e) {
            log.error("Cannot find main class of mapping '{}'. It should have the 'Main-Class' attribute at jar meta file.", mappingFile.getName());
            log.atDebug().setCause(e).log();
        } catch (@NotNull IOException e) {
            log.error("Cannot load mapping file '{}': {}", mappingFile.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        } catch (@NotNull Throwable e) {
            log.error("An unknown exception occurred trying to load mapping '{}': {}", mappingFile.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        }

        // Get compatible module and load it
        for (@NotNull Mapping mapping : mappings) if (mapping.isCompatible()) try {
            this.mapping = mapping;
            mapping.start();
            successful = true;

            log.info("Successfully loaded mapping {}", mapping.getName());
            break;
        } catch (@NotNull Throwable e) {
            this.mapping = null; // Remove mapping reference, it's not compatible.

            log.error("Cannot load mapping '{}': {}", mapping.getName(), e.getMessage());
            log.atDebug().setCause(e).log();
        }

        // Check if there's a loaded module
        if (mapping == null) {
            log.error("There's no mapping available.");
        }

        this.successful = successful;
    }

    // Getters

    @Override
    public @NotNull Optional<Account> getAccount(@NotNull String nickname) {
        lock.lock();

        try {
            if (getConfiguration().isCaseSensitiveNicknames()) {
                return accounts.values().stream().filter(account -> account.getName().equals(nickname)).findFirst();
            } else {
                return accounts.values().stream().filter(account -> account.getName().equalsIgnoreCase(nickname)).findFirst();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Optional<Account> getAccount(@NotNull UUID uuid) {
        lock.lock();

        try {
            return Optional.ofNullable(accounts.getOrDefault(uuid, null));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @NotNull Account create(@NotNull UUID uuid, @NotNull String nickname) throws AccountExistsException {
        lock.lock();

        try {
            // Checks
            if (getAccount(uuid).isPresent()) {
                throw new AccountExistsException("an account with the uuid '" + uuid + "' already exists.");
            } else if (getAccount(nickname).isPresent()) {
                throw new AccountExistsException("an account with the nickname '" + nickname + "' already exists.");
            } else {
                @Nullable Instant last = Bukkit.getPlayer(uuid) != null ? Instant.now() : null;
                @NotNull Account account = new AccountImpl(this, nickname, uuid, null, null, false, null, last, Duration.ZERO);

                accounts.put(uuid, account);
                return account;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public synchronized @NotNull Account getOrCreate(@NotNull UUID uuid, @NotNull String nickname) {
        lock.lock();

        try {
            @Nullable Account byUuid = getAccount(uuid).orElse(null);
            @Nullable Account byNickname = getAccount(nickname).orElse(null);

            if (byUuid != byNickname) {
                throw new IllegalStateException("there's multiples accounts with this uuid and nickname");
            } else if (byUuid != null) {
                return byUuid;
            } else try {
                return create(uuid, nickname);
            } catch (@NotNull AccountExistsException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

    public @NotNull LaivyAuth getPlugin() {
        return plugin;
    }

    public @NotNull Mapping getMapping() {
        if (flushed) {
            throw new IllegalStateException("the implementation api is closed");
        } else if (mapping == null) {
            throw new NullPointerException("there's no compatible LaivyAuth module available");
        }

        return mapping;
    }

    // Loaders

    @Override
    public void flush() throws IOException {
        if (flushed) {
            throw new IOException("the implementation api already is flushed");
        }

        flushed = true;

        try {
            if (mapping != null) mapping.close();
        } finally {
            mappings.clear();
            mapping = null;
        }
    }

}

package dev.xyat.kineticcore.feature.datapack;

import dev.xyat.kineticcore.feature.datapack.util.ColorText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class RepositorySource extends FolderRepositorySource {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Path folderPath;
    private final PackType packType;
    private final boolean hiddenDataPack;

    public static final PackSource KT_SOURCE = PackSource.create(
            comp -> ColorText.translatable(
                    "datapack.kineticcore.name_and_source",
                    comp,
                    ColorText.translatable("datapack.kineticcore.source_name")
            ),
            true
    );

    public RepositorySource(Path folderPath, PackType packType) {
        super(folderPath, packType, KT_SOURCE);
        this.folderPath = folderPath;
        this.packType = packType;
        this.hiddenDataPack = packType == PackType.SERVER_DATA;
    }

    @Override
    public void loadPacks(@NotNull Consumer<Pack> packAdder) {
        if (this.packType == PackType.SERVER_DATA) {
            PackModule.refreshDataPacksOnly();
        }

        List<String> loadOrder = this.packType == PackType.SERVER_DATA
                ? PackModule.datapackOrderSnapshot()
                : PackModule.resourcePackOrderSnapshot();
        for (String packName : loadOrder) {
            Path packPath = folderPath.resolve(packName);
            File file = packPath.toFile();
            if (!file.exists()) continue;

            try {
                if (file.isFile() && file.getName().endsWith(".zip")) {
                    try (ZipFile zip = new ZipFile(file)) {
                        zip.size();
                    } catch (Exception e) {
                        PackModule.addFailedPack(packName, ColorText.translatable("datapack.kineticcore.error.zip"));
                        continue;
                    }
                }

                Pack.ResourcesSupplier supplier;

                if (file.isDirectory()) {
                    if (new File(file, "pack.mcmeta").exists()) {
                        supplier = FolderRepositorySource.detectPackResources(packPath, false);
                        supplier = wrapHiddenDataSupplier(supplier);
                    } else if (new File(file, "data").exists() || new File(file, "assets").exists()) {
                        supplier = name -> new LooseFolderPackResources(
                                name,
                                packPath,
                                this.packType,
                                false,
                                this.hiddenDataPack
                        );
                    } else {
                        supplier = name -> new LooseFolderPackResources(
                                name,
                                packPath,
                                this.packType,
                                true,
                                this.hiddenDataPack
                        );
                    }
                } else {
                    supplier = FolderRepositorySource.detectPackResources(packPath, false);
                    supplier = wrapHiddenDataSupplier(supplier);
                }

                if (supplier == null) {
                    PackModule.addFailedPack(packName, ColorText.translatable("datapack.kineticcore.error.format"));
                    continue;
                }

                Pack pack = Pack.readMetaAndCreate(
                        packName,
                        Component.literal(packName),
                        true,
                        supplier,
                        this.packType,
                        Pack.Position.TOP,
                        KT_SOURCE
                );

                if (pack != null) {
                    packAdder.accept(pack);
                } else {
                    PackModule.addFailedPack(packName, ColorText.translatable("datapack.kineticcore.error.format"));
                }
            } catch (Exception e) {
                PackModule.addFailedPack(
                        packName,
                        ColorText.translatable("datapack.kineticcore.error.crash", e.getMessage())
                );
            }
        }
    }

    @Nullable
    private Pack.ResourcesSupplier wrapHiddenDataSupplier(@Nullable Pack.ResourcesSupplier supplier) {
        if (supplier == null) return null;
        if (!this.hiddenDataPack) return supplier;
        return name -> new HiddenMetadataPackResources(name, supplier.open(name));
    }

    private static IoSupplier<InputStream> createMetadataSupplier(
            @Nullable IoSupplier<InputStream> original,
            boolean hidden
    ) {
        return () -> {
            JsonObject root;

            if (original == null) {
                root = createDefaultMetadata();
            } else {
                try (InputStream inputStream = original.get()) {
                    String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    root = JsonParser.parseString(text).getAsJsonObject();
                } catch (Exception ignored) {
                    root = createDefaultMetadata();
                }
            }

            if (hidden && root.has("pack") && root.get("pack").isJsonObject()) {
                JsonObject pack = root.getAsJsonObject("pack");
                pack.addProperty("hidden", true);
                root.addProperty("hidden", true);
            }

            return new ByteArrayInputStream(GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        };
    }

    private static JsonObject createDefaultMetadata() {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        JsonObject description = new JsonObject();

        description.addProperty("translate", "datapack.kineticcore.virtual_desc");
        pack.add("description", description);
        pack.addProperty("pack_format", 15);
        root.add("pack", pack);

        return root;
    }

    private static class HiddenMetadataPackResources extends AbstractPackResources {
        private final PackResources delegate;

        private HiddenMetadataPackResources(String packId, PackResources delegate) {
            super(packId, false);
            this.delegate = delegate;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String @NotNull ... paths) {
            IoSupplier<InputStream> original = delegate.getRootResource(paths);

            if (paths.length == 1 && paths[0].equals("pack.mcmeta")) {
                return createMetadataSupplier(original, true);
            }

            return original;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(
                @NotNull PackType type,
                @NotNull ResourceLocation location
        ) {
            return delegate.getResource(type, location);
        }

        @Override
        public void listResources(
                @NotNull PackType type,
                @NotNull String namespace,
                @NotNull String path,
                @NotNull PackResources.ResourceOutput output
        ) {
            delegate.listResources(type, namespace, path, output);
        }

        @Override
        public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
            return delegate.getNamespaces(type);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    private static class LooseFolderPackResources extends AbstractPackResources {
        private final Path folderPath;
        private final PackType packType;
        private final boolean namespaceRoot;
        private final boolean hidden;

        private LooseFolderPackResources(
                String packId,
                Path folderPath,
                PackType packType,
                boolean namespaceRoot,
                boolean hidden
        ) {
            super(packId, false);
            this.folderPath = folderPath;
            this.packType = packType;
            this.namespaceRoot = namespaceRoot;
            this.hidden = hidden;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String @NotNull ... paths) {
            if (paths.length == 1 && paths[0].equals("pack.mcmeta")) {
                return createMetadataSupplier(null, hidden);
            }

            return null;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(
                @NotNull PackType type,
                @NotNull ResourceLocation location
        ) {
            if (type != this.packType) return null;

            Path resourcePath;

            if (namespaceRoot) {
                if (!location.getNamespace().equals(packId())) return null;
                resourcePath = folderPath.resolve(location.getPath());
            } else {
                resourcePath = folderPath
                        .resolve(type.getDirectory())
                        .resolve(location.getNamespace())
                        .resolve(location.getPath());
            }

            if (Files.exists(resourcePath) && Files.isRegularFile(resourcePath)) {
                return IoSupplier.create(resourcePath);
            }

            return null;
        }

        @Override
        public void listResources(
                @NotNull PackType type,
                @NotNull String namespace,
                @NotNull String path,
                @NotNull PackResources.ResourceOutput output
        ) {
            if (type != this.packType) return;

            Path namespaceDirectory;

            if (namespaceRoot) {
                if (!namespace.equals(packId())) return;
                namespaceDirectory = folderPath;
            } else {
                namespaceDirectory = folderPath.resolve(type.getDirectory()).resolve(namespace);
            }

            Path directory = namespaceDirectory.resolve(path);
            if (!Files.exists(directory) || !Files.isDirectory(directory)) return;

            try (Stream<Path> stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile).forEach(file -> {
                    String relative = namespaceDirectory.relativize(file).toString().replace('\\', '/');
                    output.accept(new ResourceLocation(namespace, relative), IoSupplier.create(file));
                });
            } catch (Exception ignored) {
            }
        }

        @Override
        public @NotNull Set<String> getNamespaces(@NotNull PackType type) {
            if (type != this.packType) return Set.of();

            if (namespaceRoot) {
                return Set.of(packId());
            }

            Path typeDirectory = folderPath.resolve(type.getDirectory());
            if (!Files.exists(typeDirectory) || !Files.isDirectory(typeDirectory)) {
                return Set.of();
            }

            try (Stream<Path> stream = Files.list(typeDirectory)) {
                return stream
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                return Set.of();
            }
        }

        @Override
        public void close() {
        }
    }
}

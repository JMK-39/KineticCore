package dev.xyat.kineticcore.feature.datapack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DatapackDiagnosticsRegression {
    public static void main(String[] args) throws Exception {
        Class<?> diagnostics = Class.forName("dev.xyat.kineticcore.feature.datapack.recovery.DatapackDiagnostics");
        var parse = diagnostics.getMethod("parse", Throwable.class, String.class, String.class);
        Object old = parse.invoke(null, new IllegalStateException("Failed to parse test:worldgen/biome/broken.json from pack 实际覆盖包", new IllegalArgumentException("Missing structure other:absent")), "worldgen/biome", "test:broken");
        require(get(old, "packId").equals("实际覆盖包"), "use the actual pack, not the referenced namespace");
        require(get(old, "resourceId").equals("test:worldgen/biome/broken.json"), "keep the exact old-version resource path");
        require(get(old, "reason").contains("Missing structure other:absent"), "keep the underlying error");
        Object newer = parse.invoke(null, new IllegalStateException("Failed to parse test:broken from pack modern"), "worldgen/biome", "test:broken");
        require(get(newer, "resourceId").equals("test:worldgen/biome/broken.json"), "26 element ID must become its registry resource path");
        Object missing = parse.invoke(null, new IllegalStateException("Unbound values in registry minecraft:worldgen/structure: [other:missing]"), "worldgen/structure", "minecraft:worldgen/structure");
        require(get(missing, "packId").isEmpty(), "unbound references do not prove which pack is faulty");
        Class<?> files = Class.forName("dev.xyat.kineticcore.feature.datapack.recovery.DatapackFiles");
        var locate = files.getMethod("locate", String.class, String.class, Path.class, Path.class);
        Path temp = Files.createTempDirectory("datapack-files-");
        Path core = temp.resolve("core"), world = temp.resolve("世界");
        Path low = core.resolve("low/data/test/worldgen/biome/broken.json");
        Path high = core.resolve("high/data/test/worldgen/biome/broken.json");
        Files.createDirectories(low.getParent()); Files.createDirectories(high.getParent());
        Files.writeString(low, "{}"); Files.writeString(high, "broken");
        Object source = locate.invoke(null, "high", "test:worldgen/biome/broken.json", core, world);
        require(get(source, "openPath").equals(high.toAbsolutePath().normalize().toString()), "overrides must resolve to the reported pack");
        Path loose = core.resolve("test/worldgen/biome/broken.json");
        Files.createDirectories(loose.getParent()); Files.writeString(loose, "broken");
        require(get(locate.invoke(null, "test", "test:worldgen/biome/broken.json", core, world), "openPath").equals(loose.toAbsolutePath().normalize().toString()), "loose namespaces are supported");
        Path zip = world.resolve("datapacks/含空格的 包.zip"); Files.createDirectories(zip.getParent());
        try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("data/test/worldgen/biome/broken.json")); output.write("broken".getBytes(java.nio.charset.StandardCharsets.UTF_8)); output.closeEntry();
        }
        Object archive = locate.invoke(null, "file/含空格的 包.zip", "test:worldgen/biome/broken.json", core, world);
        require(get(archive, "openPath").equals(zip.toAbsolutePath().normalize().toString()), "ZIP opens the real archive");
        require(get(archive, "displayPath").endsWith("!/data/test/worldgen/biome/broken.json"), "ZIP internal path must be visible");
        require(locate.invoke(null, "../escape", "test:worldgen/biome/broken.json", core, world) == null, "pack paths cannot escape their root");
        require(locate.invoke(null, "mod:example", "test:worldgen/biome/broken.json", core, world) == null, "non-filesystem pack IDs must not crash file resolution");
        require(locate.invoke(null, "high", "test:../../outside.json", core, world) == null, "resource paths cannot escape their pack");
        references();
        System.out.println("Datapack diagnosis and file provenance regression passed.");
    }
    private static void references() throws Exception {
        var registryId = net.minecraft.resources.ResourceLocation.tryParse("minecraft:worldgen/structure");
        var keyConstructor = net.minecraft.resources.ResourceKey.class.getDeclaredConstructor(net.minecraft.resources.ResourceLocation.class, net.minecraft.resources.ResourceLocation.class);
        keyConstructor.setAccessible(true);
        var registry = (net.minecraft.resources.ResourceKey<?>) keyConstructor.newInstance(net.minecraft.resources.ResourceLocation.tryParse("minecraft:root"), registryId);
        var reference = (net.minecraft.resources.ResourceKey<?>) keyConstructor.newInstance(registryId, net.minecraft.resources.ResourceLocation.tryParse("missing:absent"));
        var pack = (net.minecraft.server.packs.PackResources) java.lang.reflect.Proxy.newProxyInstance(DatapackDiagnosticsRegression.class.getClassLoader(),
                new Class[]{net.minecraft.server.packs.PackResources.class}, (proxy, method, args) -> method.getName().equals("packId") ? "actual_override" : method.getReturnType() == boolean.class ? false : null);
        var resource = new net.minecraft.server.packs.resources.Resource(pack, () -> new java.io.ByteArrayInputStream(new byte[0]));
        var resourceId = net.minecraft.resources.ResourceLocation.tryParse("test:worldgen/structure_set/references.json");
        var references = dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences.class;
        var remember = references.getMethod("remember", java.util.Map.class);
        var wrap = references.getMethod("reader", net.minecraft.server.packs.resources.Resource.class, java.io.BufferedReader.class);
        var value = references.getMethod("value", net.minecraft.resources.ResourceKey.class);
        var sources = references.getMethod("sources", net.minecraft.resources.ResourceKey.class, String.class);
        String error = "Unbound values in registry " + registry + ": [missing:absent]";
        references.getMethod("clear").invoke(null);
        remember.invoke(null, java.util.Map.of(resourceId, resource));
        try (var reader = (java.io.BufferedReader) wrap.invoke(null, resource, new java.io.BufferedReader(new java.io.StringReader("{}")))) {
            value.invoke(null, reference);
        }
        var origins = (java.util.List<?>) sources.invoke(null, registry, error);
        require(origins.size() == 1 && get(origins.get(0), "packId").equals("actual_override"), "forward reference must retain the actual originating pack");
        require(get(origins.get(0), "resourceId").equals(resourceId.toString()), "forward reference must retain its exact source file");
        references.getMethod("clear").invoke(null);
        value.invoke(null, reference);
        require(((java.util.List<?>) sources.invoke(null, registry, error)).isEmpty(), "closed or stale reader context must not claim unrelated references");
        String localError = "Unbound values in registry " + registry + ": [minecraft:missing_structure]";
        for (String spelling : java.util.List.of("minecraft:missing_structure", "missing_structure", ":missing_structure")) {
            references.getMethod("clear").invoke(null);
            remember.invoke(null, java.util.Map.of(resourceId, resource));
            try (var reader = (java.io.BufferedReader) wrap.invoke(null, resource, new java.io.BufferedReader(new java.io.StringReader("{}")))) {
                references.getMethod("value", net.minecraft.resources.ResourceKey.class, String.class).invoke(null, registry, spelling);
            }
            require(((java.util.List<?>) sources.invoke(null, registry, localError)).size() == 1, "valid reference spelling lost its origin: " + spelling);
        }
        long generation = (long) references.getMethod("generation").invoke(null);
        references.getMethod("finish", long.class).invoke(null, generation);
        remember.invoke(null, java.util.Map.of(resourceId, resource));
        var field = references.getDeclaredField("SOURCES"); field.setAccessible(true);
        require(((java.util.Map<?, ?>) field.get(null)).isEmpty(), "finished attempts must not retain normal reload resources");
    }
    private static String get(Object object, String field) throws Exception {
        return String.valueOf(object.getClass().getMethod(field).invoke(object));
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}

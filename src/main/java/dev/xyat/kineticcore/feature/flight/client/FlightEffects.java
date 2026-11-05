package dev.xyat.kineticcore.feature.flight.client;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.render.KineticWorldRender;
import dev.xyat.kineticcore.api.client.sound.KineticClientSounds;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.flight.config.SuperFlightClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Speed effects of super flight, all worked out on this client from how far each nearby player moved since the last
 * tick (so other players' booms and vapour cones need no network traffic): a wind loop that rises with speed, air
 * streaks, first-person speed lines, a vapour cone near and past the sound barrier, a sonic boom with a flash and an
 * expanding shock ring when the barrier is crossed, a smaller blast on a fast take-off (with a dust ring near the
 * ground), and plasma at twice the barrier speed. Speeds are in metres (blocks) per second.
 */
public final class FlightEffects {
    private static final double TICKS_PER_SECOND = 20.0D;
    private static final double RANGE = 160.0D;
    private static final int MAX_SHOCKS = 24;
    private static final String WIND = "kineticcore:flight.wind";
    private static final Map<UUID, Track> TRACKS = new HashMap<>();
    private static final List<Shock> SHOCKS = new ArrayList<>();
    private static KineticClientSounds.Loop wind;
    private static int flashTicks;
    private static long ticks;

    private FlightEffects() {
    }

    public static void register() {
        KineticClientEvents.onTick(KineticClientEvents.TickPhase.END, FlightEffects::tick);
        // After clouds and weather, which would otherwise be drawn over the effects.
        KineticClientEvents.onLevelRender(KineticClientEvents.LevelRenderStage.AFTER_WEATHER, FlightEffects::render);
        KineticClientEvents.onHudRender(KineticClientEvents.HudStage.HOTBAR, FlightEffects::hud);
        KineticClientEvents.onLogout(FlightEffects::clear);
    }

    /** One player's movement this tick. */
    private static final class Track {
        Vec3 last;
        Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
        double speed;
        boolean flying;
        boolean pastBarrier;
        int sinceSlow = 1000;
        int cooldown;

        Track(Vec3 position) {
            last = position;
        }
    }

    /** An expanding ring of light in the plane across {@code normal}, with a flash of {@code flash} blocks as it starts. */
    private static final class Shock {
        final Vec3 center;
        final Vec3 normal;
        final double radius;
        final int maxAge;
        final int rgb;
        final double flash;
        int age;

        Shock(Vec3 center, Vec3 normal, double radius, int maxAge, int rgb, double flash) {
            this.center = center;
            this.normal = normal;
            this.radius = radius;
            this.maxAge = maxAge;
            this.rgb = rgb;
            this.flash = flash;
        }
    }

    private static void clear() {
        TRACKS.clear();
        SHOCKS.clear();
        flashTicks = 0;
        if (wind != null) {
            wind.end();
            wind = null;
        }
    }

    // ---------------------------------------------------------------- tick

    private static void tick() {
        ClientLevel level = KineticClientRuntime.currentLevel();
        Player local = KineticClientRuntime.localPlayer();
        if (level == null || local == null || !SuperFlightClientConfig.EFFECTS.get()) {
            clear();
            return;
        }
        if (KineticClientRuntime.paused()) {
            return;
        }
        ticks++;
        Set<UUID> seen = new HashSet<>();
        for (Player player : level.players()) {
            if (player.distanceToSqr(local) > RANGE * RANGE) {
                continue;
            }
            seen.add(player.getUUID());
            Track track = TRACKS.computeIfAbsent(player.getUUID(), id -> new Track(player.position()));
            update(level, local, player, track);
        }
        TRACKS.keySet().retainAll(seen);
        windSound(local);
        SHOCKS.removeIf(shock -> ++shock.age >= shock.maxAge);
        if (flashTicks > 0) {
            flashTicks--;
        }
    }

    private static void update(ClientLevel level, Player local, Player player, Track track) {
        Vec3 position = player.position();
        Vec3 delta = position.subtract(track.last);
        track.last = position;
        double measured = delta.length() * TICKS_PER_SECOND;
        // Teleports are not flight.
        if (measured > 4000.0D) {
            measured = 0.0D;
        }
        track.speed = track.speed * 0.6D + measured * 0.4D;
        if (delta.lengthSqr() > 1.0E-6D) {
            track.direction = delta.normalize();
        }
        if (track.cooldown > 0) {
            track.cooldown--;
        }
        // Other players are judged by speed alone: nothing else in vanilla moves a player this fast in the air.
        track.flying = player == local ? KineticFlightClient.superFlightActive() : !player.onGround() && track.speed > 40.0D;
        if (!track.flying) {
            track.pastBarrier = false;
            track.sinceSlow = 0;
            return;
        }
        if (track.speed < 8.0D) {
            track.sinceSlow = 0;
        } else if (track.sinceSlow < 1000) {
            track.sinceSlow++;
        }
        double barrier = SuperFlightClientConfig.BARRIER_SPEED.get();
        if (SuperFlightClientConfig.TAKEOFF_BOOM.get() && track.speed >= SuperFlightClientConfig.TAKEOFF_SPEED.get()
                && track.sinceSlow <= 10 && track.cooldown == 0) {
            takeoff(level, player, track);
            track.cooldown = 30;
            track.sinceSlow = 1000;
        }
        if (!track.pastBarrier && track.speed >= barrier) {
            track.pastBarrier = true;
            if (SuperFlightClientConfig.SONIC_BOOM.get() && track.cooldown == 0) {
                boom(level, local, player, track);
                track.cooldown = 40;
            }
        } else if (track.pastBarrier && track.speed < barrier * 0.9D) {
            track.pastBarrier = false;
        }
        streaks(level, local, player, track);
    }

    private static void boom(ClientLevel level, Player local, Player player, Track track) {
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        float volume = SuperFlightClientConfig.BOOM_VOLUME.get().floatValue();
        KineticClientSounds.play(center, "kineticcore:flight.sonic_boom", 2.2F * volume, 1.0F);
        addShock(new Shock(center, track.direction, 7.0D, 14, SuperFlightClientConfig.VAPOR_COLOR.get(), 2.4D));
        level.addParticle(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 0.0D, 0.0D, 0.0D);
        Vec3[] basis = basis(track.direction);
        for (int index = 0; index < 18; index++) {
            double angle = index * Math.PI * 2.0D / 18.0D;
            Vec3 out = basis[0].scale(Math.cos(angle)).add(basis[1].scale(Math.sin(angle)));
            level.addParticle(ParticleTypes.CLOUD, center.x, center.y, center.z, out.x * 0.6D, out.y * 0.6D, out.z * 0.6D);
        }
        if (player == local) {
            flashTicks = 8;
        }
    }

    /** A ring across the flight and a puff behind the flyer; taking off near the ground also raises a dust ring. */
    private static void takeoff(ClientLevel level, Player player, Track track) {
        Vec3 dir = track.direction;
        Vec3 behind = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D).subtract(dir.scale(0.6D));
        float volume = SuperFlightClientConfig.BOOM_VOLUME.get().floatValue();
        KineticClientSounds.play(behind, "kineticcore:flight.takeoff", 1.2F * volume, 1.0F);
        addShock(new Shock(behind, dir, 3.5D, 10, 0xFFFFFF, 1.3D));
        Vec3[] basis = basis(dir);
        for (int index = 0; index < 16; index++) {
            Vec3 out = around(basis, index * Math.PI * 2.0D / 16.0D, 1.0D);
            level.addParticle(ParticleTypes.POOF, behind.x, behind.y, behind.z,
                    out.x * 0.3D - dir.x * 0.15D, out.y * 0.3D - dir.y * 0.15D, out.z * 0.3D - dir.z * 0.15D);
        }
        Vec3 feet = player.position();
        if (!level.getBlockState(BlockPos.containing(feet.x, feet.y - 1.5D, feet.z)).isAir()) {
            addShock(new Shock(feet.add(0.0D, 0.1D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D), 4.0D, 12, 0xFFFFFF, 0.0D));
            for (int index = 0; index < 20; index++) {
                double angle = index * Math.PI * 2.0D / 20.0D;
                level.addParticle(ParticleTypes.POOF, feet.x, feet.y + 0.1D, feet.z,
                        Math.cos(angle) * 0.35D, 0.02D, Math.sin(angle) * 0.35D);
            }
        }
    }

    private static void addShock(Shock shock) {
        if (SHOCKS.size() >= MAX_SHOCKS) {
            SHOCKS.remove(0);
        }
        SHOCKS.add(shock);
    }

    /** White air streaks: rushing past the camera for the local player, trailing behind everyone else. */
    private static void streaks(ClientLevel level, Player local, Player player, Track track) {
        double density = SuperFlightClientConfig.PARTICLES.get();
        if (density <= 0.0D || track.speed < 40.0D) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double wanted = density * Math.min(3.0D, track.speed / 150.0D);
        int count = (int) wanted + (random.nextDouble() < wanted - (int) wanted ? 1 : 0);
        boolean own = player == local && KineticClientRuntime.cameraType().isFirstPerson();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
        Vec3 dir = track.direction;
        double drift = Math.min(3.0D, track.speed / 120.0D);
        for (int index = 0; index < count; index++) {
            Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D, random.nextDouble() - 0.5D).scale(own ? 6.0D : 1.6D);
            Vec3 at = own ? center.add(dir.scale(4.0D + random.nextDouble() * 4.0D)).add(offset) : center.subtract(dir.scale(1.2D)).add(offset);
            level.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, -dir.x * drift, -dir.y * drift, -dir.z * drift);
        }
    }

    private static void windSound(Player local) {
        Track track = TRACKS.get(local.getUUID());
        // The vanilla elytra rush is silenced during super flight (see FlightClientMixins), so this is the only wind.
        boolean wanted = SuperFlightClientConfig.WIND_SOUND.get() && track != null && track.flying && track.speed > 15.0D;
        if (!wanted) {
            if (wind != null) {
                wind.end();
                wind = null;
            }
            return;
        }
        if (wind == null || !wind.playing()) {
            wind = KineticClientSounds.loop(WIND, FlightEffects::windState);
        }
    }

    private static KineticClientSounds.LoopState windState() {
        Player local = KineticClientRuntime.localPlayer();
        Track track = local == null ? null : TRACKS.get(local.getUUID());
        if (track == null || !track.flying) {
            return null;
        }
        double loud = Math.max(0.05D, Math.min(1.0D, (track.speed - 10.0D) / 250.0D));
        float volume = (float) (SuperFlightClientConfig.WIND_VOLUME.get() * loud);
        float pitch = (float) (0.8D + Math.min(1.0D, track.speed / 800.0D) * 0.6D);
        return new KineticClientSounds.LoopState(local.getEyePosition(), volume, pitch);
    }

    // ---------------------------------------------------------------- world

    private static void render(KineticClientEvents.LevelRenderContext context) {
        ClientLevel level = KineticClientRuntime.currentLevel();
        Player local = KineticClientRuntime.localPlayer();
        if (level == null || local == null || (TRACKS.isEmpty() && SHOCKS.isEmpty())) {
            return;
        }
        float partialTick = context.partialTick();
        boolean firstPerson = KineticClientRuntime.cameraType().isFirstPerson();
        double barrier = SuperFlightClientConfig.BARRIER_SPEED.get();
        List<Player> flyers = new ArrayList<>();
        boolean anyPlasma = false;
        for (Player player : level.players()) {
            Track track = TRACKS.get(player.getUUID());
            if (track == null || !track.flying || (player == local && firstPerson)) {
                continue;
            }
            flyers.add(player);
            anyPlasma |= SuperFlightClientConfig.PLASMA.get() && track.speed >= barrier * 2.0D;
        }
        if (flyers.isEmpty() && SHOCKS.isEmpty()) {
            return;
        }
        // The orange body of the plasma first, so it stays orange against a bright sky; all the light goes over it.
        if (anyPlasma) {
            try (KineticWorldRender.GlowBatch tint = KineticWorldRender.beginTintBatch(context)) {
                for (Player player : flyers) {
                    plasma(tint, player, TRACKS.get(player.getUUID()), partialTick, barrier, false);
                }
            }
        }
        try (KineticWorldRender.GlowBatch glow = KineticWorldRender.beginGlowBatch(context)) {
            for (Player player : flyers) {
                Track track = TRACKS.get(player.getUUID());
                Vec3 center = player.getPosition(partialTick).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
                if (SuperFlightClientConfig.VAPOR_CONE.get() && track.speed >= barrier * 0.85D) {
                    double strength = Math.min(1.0D, (track.speed - barrier * 0.85D) / (barrier * 0.15D));
                    vaporCone(glow, center, track.direction, SuperFlightClientConfig.VAPOR_COLOR.get(), strength);
                }
                plasma(glow, player, track, partialTick, barrier, true);
            }
            for (Shock shock : SHOCKS) {
                ring(glow, shock, partialTick);
            }
        }
    }

    /**
     * The vapour cone: a bell of white haze around the flyer, clear at its front lip, densest a little behind the
     * chest and fading out behind, with a light haze inside so it reads as a cloud from behind as well as from the side.
     */
    private static void vaporCone(KineticWorldRender.GlowBatch glow, Vec3 center, Vec3 dir, int rgb, double strength) {
        Vec3[] basis = basis(dir);
        double[] along = {0.55D, -0.2D, -1.6D};
        double[] radius = {0.7D, 1.15D, 1.75D};
        double[] alpha = {0.0D, 0.4D, 0.0D};
        double pulse = 0.85D + 0.15D * Math.sin(ticks * 0.9D);
        int segments = 32;
        for (int ring = 0; ring + 1 < along.length; ring++) {
            Vec3 near = center.add(dir.scale(along[ring]));
            Vec3 far = center.add(dir.scale(along[ring + 1]));
            int nearColor = argb(rgb, alpha[ring] * strength * pulse);
            int farColor = argb(rgb, alpha[ring + 1] * strength * pulse);
            for (int index = 0; index < segments; index++) {
                double a0 = index * Math.PI * 2.0D / segments;
                double a1 = (index + 1) * Math.PI * 2.0D / segments;
                glow.quad(near.add(around(basis, a0, radius[ring])), near.add(around(basis, a1, radius[ring])),
                        far.add(around(basis, a1, radius[ring + 1])), far.add(around(basis, a0, radius[ring + 1])),
                        nearColor, nearColor, farColor, farColor);
            }
        }
        glow.glow(center.subtract(dir.scale(0.3D)), 1.4D, argb(rgb, 0.13D * strength * pulse));
    }

    /**
     * Plasma at twice the barrier speed: a white-hot shield just ahead of the flyer and tongues of flame peeling off its
     * rim and streaming back. The tint pass draws the orange body of each part, the glow pass its white-hot core.
     */
    private static void plasma(KineticWorldRender.GlowBatch batch, Player player, Track track, float partialTick, double barrier,
                               boolean glow) {
        if (!SuperFlightClientConfig.PLASMA.get() || track.speed < barrier * 2.0D) {
            return;
        }
        double strength = Math.min(1.0D, (track.speed - barrier * 2.0D) / (barrier * 2.0D));
        int rgb = SuperFlightClientConfig.PLASMA_COLOR.get();
        Vec3 dir = track.direction;
        Vec3 center = player.getPosition(partialTick).add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 shield = center.add(dir.scale(0.85D));
        if (glow) {
            batch.glow(shield, 1.6D, argb(mix(rgb, 0xFFFFFF, 0.3D), 0.32D));
            batch.glow(shield, 0.6D, argb(0xFFFFFF, 0.85D));
        } else {
            batch.glow(shield, 1.2D + 0.3D * strength, argb(rgb, 0.6D));
        }
        Vec3[] basis = basis(dir);
        int tongues = 10;
        int points = 5;
        for (int tongue = 0; tongue < tongues; tongue++) {
            double base = tongue * Math.PI * 2.0D / tongues + 0.15D * Math.sin(ticks * 0.4D + tongue);
            double length = (2.6D + 1.6D * strength) * (0.75D + 0.25D * Math.sin(ticks * 0.9D + tongue * 1.7D + player.getId()));
            double curl = 0.35D * Math.sin(ticks * 0.5D + tongue * 2.3D);
            Vec3[] path = new Vec3[points];
            double[] half = new double[points];
            int[] colors = new int[points];
            for (int index = 0; index < points; index++) {
                double t = index / (points - 1.0D);
                path[index] = center.add(dir.scale(0.6D - length * t)).add(around(basis, base + curl * t, 0.55D + 0.75D * t));
                half[index] = index == points - 1 ? 0.0D : (0.24D * (1.0D - t) + 0.06D) * (glow ? 0.4D : 1.0D);
                int hue = t < 0.5D ? mix(0xFFE08A, rgb, t * 2.0D) : mix(rgb, 0xB01800, (t - 0.5D) * 2.0D);
                colors[index] = glow ? argb(mix(hue, 0xFFFFFF, 0.55D), 0.55D * (1.0D - t))
                        : argb(hue, (0.35D + 0.3D * strength) * (1.0D - t * 0.8D));
            }
            ribbon(batch, path, half, colors);
        }
    }

    /** A camera-facing ribbon through {@code points}: its colour along the middle, clear at both edges. */
    private static void ribbon(KineticWorldRender.GlowBatch batch, Vec3[] points, double[] half, int[] colors) {
        Vec3 camera = batch.cameraPosition();
        Vec3[] side = new Vec3[points.length];
        Vec3 last = null;
        for (int index = 0; index < points.length; index++) {
            Vec3 tangent = points[Math.min(index + 1, points.length - 1)].subtract(points[Math.max(index - 1, 0)]);
            Vec3 across = tangent.cross(camera.subtract(points[index]));
            across = across.lengthSqr() < 1.0E-10D ? (last != null ? last : batch.cameraUp()) : across.normalize();
            if (last != null && across.dot(last) < 0.0D) {
                across = across.scale(-1.0D);
            }
            side[index] = last = across;
        }
        for (int index = 0; index + 1 < points.length; index++) {
            Vec3 a = points[index];
            Vec3 b = points[index + 1];
            Vec3 edgeA = side[index].scale(half[index]);
            Vec3 edgeB = side[index + 1].scale(half[index + 1]);
            int colorA = colors[index];
            int colorB = colors[index + 1];
            batch.quad(a.subtract(edgeA), a, b, b.subtract(edgeB), colorA & 0xFFFFFF, colorA, colorB, colorB & 0xFFFFFF);
            batch.quad(a, a.add(edgeA), b.add(edgeB), b, colorA, colorA & 0xFFFFFF, colorB & 0xFFFFFF, colorB);
        }
    }

    private static void ring(KineticWorldRender.GlowBatch glow, Shock shock, float partialTick) {
        double t = Math.min(1.0D, (shock.age + partialTick) / shock.maxAge);
        double eased = 1.0D - Math.pow(1.0D - t, 3.0D);
        double outer = Math.max(0.05D, shock.radius * eased);
        double inner = Math.max(0.0D, outer - (0.5D + 0.8D * (1.0D - t)));
        double alpha = 0.8D * (1.0D - t);
        double middle = (inner + outer) * 0.5D;
        Vec3[] basis = basis(shock.normal);
        int segments = 32;
        // Clear at both edges and brightest along the middle of the band, so the ring has no hard rim.
        int bright = argb(shock.rgb, alpha);
        int clear = shock.rgb & 0xFFFFFF;
        for (int index = 0; index < segments; index++) {
            double a0 = index * Math.PI * 2.0D / segments;
            double a1 = (index + 1) * Math.PI * 2.0D / segments;
            Vec3 in0 = shock.center.add(around(basis, a0, inner));
            Vec3 in1 = shock.center.add(around(basis, a1, inner));
            Vec3 mid0 = shock.center.add(around(basis, a0, middle));
            Vec3 mid1 = shock.center.add(around(basis, a1, middle));
            Vec3 out0 = shock.center.add(around(basis, a0, outer));
            Vec3 out1 = shock.center.add(around(basis, a1, outer));
            glow.quad(in0, mid0, mid1, in1, clear, bright, bright, clear);
            glow.quad(mid0, out0, out1, mid1, bright, clear, clear, bright);
        }
        // A quick white flash where the ring starts.
        if (shock.flash > 0.0D && t < 0.4D) {
            double flash = t / 0.4D;
            glow.glow(shock.center, shock.flash * (0.5D + flash), argb(0xFFFFFF, 0.6D * (1.0D - flash)));
        }
    }

    /** Two unit vectors across {@code axis}. */
    private static Vec3[] basis(Vec3 axis) {
        Vec3 helper = Math.abs(axis.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 u = axis.cross(helper).normalize();
        Vec3 v = axis.cross(u).normalize();
        return new Vec3[]{u, v};
    }

    private static Vec3 around(Vec3[] basis, double angle, double radius) {
        return basis[0].scale(Math.cos(angle) * radius).add(basis[1].scale(Math.sin(angle) * radius));
    }

    private static int argb(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0.0D, Math.min(1.0D, alpha)) * 255.0D);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int rgb, int other, double amount) {
        int r = (int) (((rgb >> 16) & 0xFF) * (1.0D - amount) + ((other >> 16) & 0xFF) * amount);
        int g = (int) (((rgb >> 8) & 0xFF) * (1.0D - amount) + ((other >> 8) & 0xFF) * amount);
        int b = (int) ((rgb & 0xFF) * (1.0D - amount) + (other & 0xFF) * amount);
        return (r << 16) | (g << 8) | b;
    }

    // ---------------------------------------------------------------- screen

    private static void hud(KineticGraphics graphics, float partialTick) {
        Player local = KineticClientRuntime.localPlayer();
        Track track = local == null ? null : TRACKS.get(local.getUUID());
        if (track == null || !track.flying || KineticClientRuntime.guiHidden() || KineticClientRuntime.currentScreen() != null
                || !KineticClientRuntime.cameraType().isFirstPerson()) {
            if (flashTicks > 0 && local != null) {
                flash(graphics);
            }
            return;
        }
        int width = KineticClientRuntime.guiScaledWidth();
        int height = KineticClientRuntime.guiScaledHeight();
        if (SuperFlightClientConfig.SPEED_LINES.get() && track.speed > 120.0D) {
            speedLines(graphics, width, height, Math.min(1.0D, (track.speed - 120.0D) / 600.0D));
        }
        if (track.pastBarrier && SuperFlightClientConfig.VAPOR_CONE.get()) {
            int band = Math.max(8, height / 8);
            int rgb = SuperFlightClientConfig.VAPOR_COLOR.get();
            graphics.fillGradient(0, 0, width, band, argb(rgb, 0.16D), rgb & 0xFFFFFF);
            graphics.fillGradient(0, height - band, width, height, rgb & 0xFFFFFF, argb(rgb, 0.16D));
        }
        flash(graphics);
    }

    /** Thin streaks from the screen edge toward the centre, redrawn at new angles every tick. */
    private static void speedLines(KineticGraphics graphics, int width, int height, double strength) {
        java.util.Random random = new java.util.Random(ticks * 31L);
        float reach = (float) Math.hypot(width, height) * 0.5F;
        int lines = 24 + (int) (24 * strength);
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;
        for (int index = 0; index < lines; index++) {
            float angle = random.nextFloat() * 360.0F;
            int inner = (int) (reach * (0.58F - 0.18F * (float) strength) + random.nextFloat() * reach * 0.12F);
            int alpha = (int) (255 * (0.12D + 0.33D * strength * (0.5D + random.nextDouble() * 0.5D)));
            graphics.push();
            graphics.translate(centerX, centerY);
            graphics.rotate(angle);
            graphics.fill(inner, 0, (int) (reach * 1.1F), 1, (alpha << 24) | 0xFFFFFF);
            graphics.pop();
        }
    }

    private static void flash(KineticGraphics graphics) {
        if (flashTicks <= 0) {
            return;
        }
        int alpha = (int) (255 * 0.35D * flashTicks / 8.0D);
        graphics.fill(0, 0, KineticClientRuntime.guiScaledWidth(), KineticClientRuntime.guiScaledHeight(), (alpha << 24) | 0xFFFFFF);
    }
}

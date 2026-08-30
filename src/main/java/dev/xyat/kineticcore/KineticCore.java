package dev.xyat.kineticcore;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import dev.xyat.kineticcore.bootstrap.client.KineticCoreClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(KineticCore.MODID)
public class KineticCore {
    public static final String MODID = "kineticcore";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String CORE_PACKAGE_PREFIX = "dev.xyat.kineticcore.";

    public KineticCore(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModContainer modContainer = context.getContainer();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> KineticCoreClientBootstrap.beforeModuleScan(context, modEventBus));

        scanAndLoadCoreModules(modEventBus, modContainer);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> KineticCoreClientBootstrap.afterModuleScan(modContainer));
    }

    private void scanAndLoadCoreModules(IEventBus modEventBus, ModContainer modContainer) {
        ModFileScanData scanData = ModList.get().getModFileById(MODID).getFile().getScanResult();
        boolean isClient = FMLEnvironment.dist == Dist.CLIENT;

        String ktModule = KTModule.class.getName();
        String ktClient = KTClientModule.class.getName();
        String ktNetwork = KTNetwork.class.getName();

        List<Class<?>> modules = new ArrayList<>();
        Set<Class<?>> loadedClasses = new HashSet<>();

        ClassLoader classLoader = KineticCore.class.getClassLoader();
        scanData.getAnnotations().stream()
                .filter(data -> {
                    String name = data.annotationType().getClassName();
                    return name.equals(ktModule) || name.equals(ktNetwork) || (isClient && name.equals(ktClient));
                })
                .forEach(data -> {
                    String className = data.clazz().getClassName();
                    if (!className.startsWith(CORE_PACKAGE_PREFIX)) return;
                    try {
                        Class<?> clazz = Class.forName(className, false, classLoader);
                        if (loadedClasses.add(clazz)) {
                            modules.add(clazz);
                        }
                    } catch (Throwable throwable) {
                        if (isClientClassError(throwable)) {
                            LOGGER.debug("跳过扫描到的客户端模块(非客户端环境): {}", className);
                        } else {
                            LOGGER.error("无法在扫描阶段加载类 -> {}", className, throwable);
                        }
                    }
                });

        for (Class<?> clazz : modules) {
            handleCoreModuleLifecycle(clazz, modContainer, modEventBus);
        }
    }

    private void handleCoreModuleLifecycle(Class<?> clazz, ModContainer modContainer, IEventBus modEventBus) {
        tryInvoke(clazz, "register", ModContainer.class, modContainer);
        tryInvoke(clazz, "register", IEventBus.class, modEventBus);
        tryInvoke(clazz, "register", null, null);

        tryInvoke(clazz, "load", ModContainer.class, modContainer);
        tryInvoke(clazz, "load", IEventBus.class, modEventBus);
        tryInvoke(clazz, "load", null, null);
    }

    private void tryInvoke(Class<?> clazz, String methodName, Class<?> parameterType, Object parameterValue) {
        try {
            Method method;
            if (parameterType != null) {
                method = clazz.getDeclaredMethod(methodName, parameterType);
                method.invoke(null, parameterValue);
            } else {
                method = clazz.getDeclaredMethod(methodName);
                method.invoke(null);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable throwable) {
            String signature = methodName + (parameterType != null ? "(" + parameterType.getSimpleName() + ")" : "()");
            logModuleError(clazz.getName(), throwable, signature);
        }
    }

    private void logModuleError(String className, Throwable throwable, String signature) {
        if (isClientClassError(throwable)) {
            LOGGER.debug("跳过服务端不支持的客户端模块 (在{}中失败): {}", signature, className);
        } else {
            LOGGER.error("模块执行 {} 失败: {}", signature, className, throwable);
        }
    }

    private boolean isClientClassError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("Attempted to load class net/minecraft/client/Minecraft for invalid dist")) {
                return true;
            }
            if (current instanceof ClassNotFoundException || current instanceof NoClassDefFoundError) {
                String name = current.getMessage();
                if (name != null && name.contains("net/minecraft/client")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}

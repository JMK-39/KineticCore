package dev.xyat.kineticcore.feature.datapack.mixin;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import dev.xyat.kineticcore.feature.datapack.PackModule;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(PackRepository.class)
public abstract class DatapackRepositoryMixin {

    @Inject(at = @At("RETURN"), method = "rebuildSelected", cancellable = true)
    private void kineticcore$datapack$reorderSelectedDataPacks(CallbackInfoReturnable<List<Pack>> cir) {
        PackModule.refreshDataPacksOnly();
        List<String> configuredOrder = PackModule.datapackOrderSnapshot();
        if (configuredOrder.isEmpty()) return;

        List<Pack> selectedPacks = new ArrayList<>(cir.getReturnValue());
        Map<String, Pack> selectedById = new LinkedHashMap<>();

        for (Pack pack : selectedPacks) {
            selectedById.put(pack.getId(), pack);
        }

        List<Pack> customPacks = new ArrayList<>();
        for (String packId : configuredOrder) {
            Pack pack = selectedById.get(packId);
            if (pack != null) {
                customPacks.add(pack);
            }
        }

        if (customPacks.isEmpty()) return;

        selectedPacks.removeAll(customPacks);
        Collections.reverse(customPacks);

        for (Pack pack : customPacks) {
            pack.getDefaultPosition().insert(selectedPacks, pack, Functions.identity(), false);
        }

        cir.setReturnValue(ImmutableList.copyOf(selectedPacks));
    }
}

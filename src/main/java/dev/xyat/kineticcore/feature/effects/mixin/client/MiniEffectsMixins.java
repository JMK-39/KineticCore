package dev.xyat.kineticcore.feature.effects.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xyat.kineticcore.feature.effects.client.IAreasGetter;
import dev.xyat.kineticcore.feature.effects.client.MiniEffectsFeature;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

public class MiniEffectsMixins {

    @Mixin(EffectRenderingInventoryScreen.class)
    public static abstract class DisplayEffectsScreenMixin<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements IAreasGetter {
        public DisplayEffectsScreenMixin(T abstractContainerMenu, Inventory inventory, Component component) {
            super(abstractContainerMenu, inventory, component);
        }

        @Unique private boolean kineticrefined$expand;
        @Unique private int kineticrefined$effects;
        @Unique private Rect2i kineticrefined$area;
        @Unique private final ItemStack kineticrefined$iconItem = new ItemStack(Items.POTION);

        @Shadow public abstract boolean canSeeEffects();

        @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
        private void minieffects$renderEffects(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
            minieffects$updateArea();
            if (kineticrefined$area == null) {
                ci.cancel();
                return;
            }

            if (this.minecraft == null || this.minecraft.player == null) {
                return;
            }

            int effects = 0, bad = 0;
            LocalPlayer player = this.minecraft.player;

            for (MobEffectInstance effectInstance : player.getActiveEffects()) {
                ++effects;
                if (!effectInstance.getEffect().isBeneficial()) ++bad;
            }

            this.kineticrefined$effects = effects;
            int x = (int) (this.minecraft.mouseHandler.xpos() * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth());
            int y = (int) (this.minecraft.mouseHandler.ypos() * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight());

            boolean expand = MiniEffectsFeature.CLIENT.requiresHoldingTab.get() || kineticrefined$area.contains(x, y);
            if (expand != this.kineticrefined$expand) {
                this.kineticrefined$expand = expand;
                minieffects$updateArea();
            }

            if (effects > 0) {
                if (!expand) {
                    x = kineticrefined$area.getX();
                    y = kineticrefined$area.getY();
                    guiGraphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x, y, 0, 141, 166, 24, 24, 256, 256);
                    var poseStack = guiGraphics.pose();

                    if (!MiniEffectsFeature.CLIENT.potionItemIcon.get()) {
                        var effectsToShow = player.getActiveEffects().stream().skip(Math.max(0, effects - 4)).toList();
                        var mobEffectTextures = this.minecraft.getMobEffectTextures();
                        if (effectsToShow.size() == 1) {
                            guiGraphics.blit(x + 4, y + 4, 0, 16, 16, mobEffectTextures.get(effectsToShow.get(0).getEffect()));
                        } else if (effectsToShow.size() == 2) {
                            guiGraphics.blit(x + 3, y + 4, 0, 10, 10, mobEffectTextures.get(effectsToShow.get(0).getEffect()));
                            guiGraphics.blit(x + 3 + 8, y + 4 + 8, 0, 10, 10, mobEffectTextures.get(effectsToShow.get(1).getEffect()));
                        } else if (effectsToShow.size() > 2) {
                            var effectsPerLine = Mth.ceil(effectsToShow.size() / 2f);
                            var effectWidth = 16 / effectsPerLine;
                            for (var i1 = 0; i1 < effectsPerLine; i1++) {
                                var effectInstance = effectsToShow.get(i1);
                                guiGraphics.blit(x + 3 + effectWidth * i1, y + 3, 0, 8, 8, mobEffectTextures.get(effectInstance.getEffect()));
                            }
                            for (var i1 = 0; i1 < effectsToShow.size() - effectsPerLine; i1++) {
                                var effectInstance = effectsToShow.get(i1 + effectsPerLine);
                                guiGraphics.blit(x + 3 + effectWidth * i1, y + 3 + 9, 0, 8, 8, mobEffectTextures.get(effectInstance.getEffect()));
                            }
                        }
                    } else {
                        // 【修改点】调用刚才拆分出去的新独立接口
                        int color = player.getEntityData().get(LivingEntityAccessor.getParameter());
                        kineticrefined$iconItem.getOrCreateTag().putInt("CustomPotionColor", color);
                        guiGraphics.renderFakeItem(kineticrefined$iconItem, x + 3, y + 4);
                    }

                    poseStack.pushPose();
                    poseStack.translate(0, 0, 200);
                    var yOffset = 0;
                    if (effects - bad > 0) {
                        yOffset = -10;
                        String s = Integer.toString(effects - bad);
                        guiGraphics.drawString(this.minecraft.font, s, x + 22 - this.minecraft.font.width(s), y + 14, 16777215);
                    }
                    if (bad > 0) {
                        String s = Integer.toString(bad);
                        guiGraphics.drawString(this.minecraft.font, s, x + 22 - this.minecraft.font.width(s), y + 14 + yOffset, 16733525);
                    }
                    poseStack.popPose();
                    ci.cancel();
                } else {
                    int left = kineticrefined$area.getX();
                    int startY = kineticrefined$area.getY();
                    boolean fullWidth = kineticrefined$area.getWidth() > 32;

                    int step = 33;
                    if (effects > 1) {
                        step = (kineticrefined$area.getHeight() - 32) / (effects - 1);
                    }

                    var list = player.getActiveEffects().stream().sorted().toList();
                    int hoveredIndex = -1;
                    MobEffectInstance hoveredEffect = null;

                    for (int i = list.size() - 1; i >= 0; --i) {
                        int cardY = startY + i * step;
                        if (mouseX >= left && mouseX <= left + kineticrefined$area.getWidth() && mouseY >= cardY && mouseY <= cardY + 32) {
                            hoveredIndex = i;
                            hoveredEffect = list.get(i);
                            break;
                        }
                    }

                    for (int i = 0; i < list.size(); ++i) {
                        if (i == hoveredIndex) continue;
                        minieffects$renderEffectCard(guiGraphics, list.get(i), left, startY + i * step, fullWidth);
                    }

                    if (hoveredEffect != null) {
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(0, 0, 400);
                        minieffects$renderEffectCard(guiGraphics, hoveredEffect, left, startY + hoveredIndex * step, fullWidth);
                        guiGraphics.pose().popPose();
                    }
                    ci.cancel();
                }
            }
        }

        @Unique
        private void minieffects$renderEffectCard(GuiGraphics guiGraphics, MobEffectInstance effect, int x, int y, boolean fullWidth) {
            guiGraphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x, y, fullWidth ? 0 : 141, 166, fullWidth ? 120 : 32, 32);
            MobEffectTextureManager textureManager = null;
            if (this.minecraft != null) {
                textureManager = this.minecraft.getMobEffectTextures();
            }
            TextureAtlasSprite sprite = null;
            if (textureManager != null) {
                sprite = textureManager.get(effect.getEffect());
            }
            if (sprite != null) {
                guiGraphics.blit(x + (fullWidth ? 6 : 7), y + 7, 0, 18, 18, sprite);
            }

            if (fullWidth) {
                Component name = minieffects$getEffectName(effect);
                guiGraphics.drawString(this.minecraft.font, name, x + 28, y + 6, 16777215);
                String duration = minieffects$getDurationText(effect);
                guiGraphics.drawString(this.minecraft.font, duration, x + 28, y + 6 + 10, 8355711);
            }
        }

        @Unique
        private Component minieffects$getEffectName(MobEffectInstance effect) {
            net.minecraft.network.chat.MutableComponent component = effect.getEffect().getDisplayName().copy();
            if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
                component.append(" ").append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
            }
            return component;
        }

        @Unique
        private String minieffects$getDurationText(MobEffectInstance effect) {
            int ticks = effect.getDuration();
            if (ticks > 32104) return "**:**";
            int seconds = ticks / 20;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }

        @Unique
        private void minieffects$updateArea() {
            if (!canSeeEffects()) {
                kineticrefined$area = null;
                return;
            }
            int left;
            boolean fullWidth;
            if (MiniEffectsFeature.isLeftSide()) {
                fullWidth = leftPos > 120;
                left = kineticrefined$expand ? (fullWidth ? leftPos - 120 - 4 : leftPos - 32 - 4) : leftPos - 20 - 8;
            } else {
                left = leftPos + imageWidth + 2;
                fullWidth = (width - left) >= 120;
            }

            if (kineticrefined$expand) {
                int totalAvailableHeight = (int)(this.height * 0.90);
                int step = 33;
                if (kineticrefined$effects > 1) {
                    step = (totalAvailableHeight - 32) / (kineticrefined$effects - 1);
                    step = Math.min(step, 33);
                }
                int totalHeight = (kineticrefined$effects - 1) * step + 32;

                int startY = topPos;
                if (startY + totalHeight > this.height - 10) {
                    startY = this.height - totalHeight - 10;
                }
                startY = Math.max(10, startY);

                kineticrefined$area = new Rect2i(left, startY, fullWidth ? 120 : 32, totalHeight);
            } else {
                kineticrefined$area = new Rect2i(left, topPos, 20, 20);
            }
        }

        @Override
        public List<Rect2i> kineticRefined$getAreas() {
            if (kineticrefined$area == null || kineticrefined$effects == 0) return List.of();
            return List.of(kineticrefined$area);
        }

        @Override
        public boolean kineticRefined$isExpanded() {
            return kineticrefined$expand;
        }

        @Inject(at = @At("HEAD"), method = "canSeeEffects", cancellable = true)
        private void minieffects$canSeeEffects(CallbackInfoReturnable<Boolean> ci) {
            if (this.minecraft == null) return;
            if (MiniEffectsFeature.CLIENT.requiresHoldingTab.get()) {
                long window = this.minecraft.getWindow().getWindow();
                int tabKey = InputConstants.KEY_TAB;
                if (this.minecraft.options.keyInventory.getKey().getValue() == tabKey || !InputConstants.isKeyDown(window, tabKey)) {
                    ci.setReturnValue(false);
                    return;
                }
            }
            if (this instanceof RecipeUpdateListener listener && listener.getRecipeBookComponent().isVisible()) {
                ci.setReturnValue(false);
            }
        }
    }

    @Mixin(value = EffectRenderingInventoryScreen.class, priority = 1100)
    public static abstract class DisplayEffectsScreenMixinFailsafe<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
        public DisplayEffectsScreenMixinFailsafe(T abstractContainerMenu, Inventory inventory, Component component) {
            super(abstractContainerMenu, inventory, component);
        }

        @Inject(at = @At("HEAD"), method = "canSeeEffects", cancellable = true)
        private void minieffects$canSeeEffects(CallbackInfoReturnable<Boolean> ci) {
            if (MiniEffectsFeature.isLeftSide()) ci.setReturnValue(leftPos >= 36);
        }

        @ModifyVariable(method = "renderEffects", at = @At(value = "STORE", ordinal = 0), index = 4)
        private int minieffects$renderEffectsK(int k) {
            return MiniEffectsFeature.isLeftSide() ? (leftPos > 120 ? leftPos - 120 : leftPos - 32) : k;
        }

        @ModifyVariable(method = "renderEffects", at = @At(value = "STORE", ordinal = 0), index = 5)
        private int minieffects$renderEffectsL(int l) {
            return MiniEffectsFeature.isLeftSide() ? leftPos - 2 : l;
        }

        @ModifyVariable(method = "renderEffects", at = @At(value = "STORE", ordinal = 0), index = 7)
        private boolean minieffects$renderEffectsBl(boolean bl) {
            int l = MiniEffectsFeature.isLeftSide() ? leftPos - 2 : width - (leftPos + imageWidth + 2);
            return l >= 120;
        }
    }

    @Mixin(GameRenderer.class)
    public static class GameRendererMixin {
        @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
        private static void minieffects$getNightVisionScale(LivingEntity livingEntity, float partialTick, CallbackInfoReturnable<Float> cir) {
            MobEffectInstance instance = livingEntity.getEffect(MobEffects.NIGHT_VISION);
            if (instance == null) {
                return;
            }
            if (instance.isInfiniteDuration()) {
                cir.setReturnValue(1.0F);
                return;
            }
            int duration = instance.getDuration();
            cir.setReturnValue(duration > 20 ? 1.0F : duration * 0.05F);
        }
    }

    @Mixin(targets = "mezz.jei.library.plugins.vanilla.gui.InventoryEffectRendererGuiHandler", remap = false)
    public static class InventoryEffectRendererGuiHandlerMixin {
        @Inject(method = "getGuiExtraAreas(Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;)Ljava/util/List;", at = @At("HEAD"), cancellable = true, require = 0)
        private void getGuiExtraAreas(EffectRenderingInventoryScreen<?> containerScreen, CallbackInfoReturnable<List<Rect2i>> ci) {
            if (containerScreen instanceof IAreasGetter getter) {
                ci.setReturnValue(getter.kineticRefined$getAreas());
            }
        }
    }
}
package net.bustin.vending_companions.screen;

import net.bustin.vending_companions.VendingCompanions;
import net.minecraft.resources.ResourceLocation;

public final class CompanionLockerTextures {

    private CompanionLockerTextures() {
    }

    public static final ResourceLocation BACKGROUND = modTexture("textures/gui/companion_vending_machine_gui.png");
    public static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");

    public static final ResourceLocation SCROLLBAR = modTexture("textures/gui/scrollbar.png");
    public static final ResourceLocation SCROLLBAR_HOVER = modTexture("textures/gui/scrollbar_highlighted.png");
    public static final ResourceLocation SCROLLBAR_DISABLED = modTexture("textures/gui/scrollbar_disabled.png");

    public static final ResourceLocation RELIC_SLOT_UNLOCKED = modTexture("textures/gui/relic_slot_unlocked.png");
    public static final ResourceLocation RELIC_SLOT_LOCKED = modTexture("textures/gui/relic_slot_locked.png");
    public static final ResourceLocation RELIC_SLOT_FILLED = modTexture("textures/gui/relic_slot_filled.png");

    public static final ResourceLocation TRAIL_SLOT_UNLOCKED = modTexture("textures/gui/trail_slot_unlocked.png");
    public static final ResourceLocation TRAIL_SLOT_LOCKED = modTexture("textures/gui/trail_slot_locked.png");

    public static final ResourceLocation XP_BAR = modTexture("textures/gui/companion_xp_bar.png");
    public static final ResourceLocation XP_BAR_FILL = modTexture("textures/gui/companion_xp_bar_progress.png");

    public static final ResourceLocation CYCLE = modTexture("textures/gui/cycle.png");
    public static final ResourceLocation CYCLE_HOVER = modTexture("textures/gui/cycle_highlight.png");

    public static final ResourceLocation HEAL_BUTTON = modTexture("textures/gui/heal_button.png");
    public static final ResourceLocation HEAL_BUTTON_HOVER = modTexture("textures/gui/heal_button_highlighted.png");
    public static final ResourceLocation HEAL_BUTTON_DISABLED = modTexture("textures/gui/heal_button_disabled.png");

    public static final ResourceLocation DISPLAY_BUTTON = modTexture("textures/gui/companion_display_button.png");
    public static final ResourceLocation DISPLAY_BUTTON_HOVER = modTexture("textures/gui/companion_display_button_highlighted.png");
    public static final ResourceLocation DISPLAY_BUTTON_SELECTED = modTexture("textures/gui/companion_display_button_selected.png");

    public static final ResourceLocation QUICK_EQUIP_BUTTON = modTexture("textures/gui/quick_equip_button.png");
    public static final ResourceLocation QUICK_EQUIP_BUTTON_HOVER = modTexture("textures/gui/quick_equip_button_highlighted.png");
    public static final ResourceLocation SWAP_BUTTON = modTexture("textures/gui/swap_button.png");
    public static final ResourceLocation SWAP_BUTTON_HOVER = modTexture("textures/gui/swap_button_highlighted.png");

    public static final ResourceLocation FAVOURITE_BUTTON = modTexture("textures/gui/favourite_button.png");
    public static final ResourceLocation FAVOURITE_BUTTON_HOVER = modTexture("textures/gui/favourite_button_highlighted.png");
    public static final ResourceLocation FAVOURITE_ON_BUTTON = modTexture("textures/gui/favourite_on_button.png");
    public static final ResourceLocation FAVOURITE_ON_BUTTON_HOVER = modTexture("textures/gui/favourite_on_button_highlighted.png");

    public static final ResourceLocation RELEASE_BUTTON = modTexture("textures/gui/release_button.png");
    public static final ResourceLocation RELEASE_BUTTON_HOVER = modTexture("textures/gui/release_button_hover.png");

    public static ResourceLocation temporalModifier(String id) {
        return modTexture("textures/gui/temporal_modifiers/" + id + ".png");
    }

    public static ResourceLocation modifier(String id) {
        return modTexture("textures/gui/modifiers/" + id + ".png");
    }

    private static ResourceLocation modTexture(String path) {
        return new ResourceLocation(VendingCompanions.MOD_ID, path);
    }
}

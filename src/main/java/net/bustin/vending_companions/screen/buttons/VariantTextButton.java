package net.bustin.vending_companions.screen.buttons;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class VariantTextButton extends Button {
    private final Component tooltip;

    public VariantTextButton(int x, int y, int w, int h,
                             Component label,
                             Component tooltip,
                             OnPress onPress) {
        super(x, y, w, h, label, onPress);
        this.tooltip = tooltip;
    }

    public Component getTooltip() {
        return tooltip;
    }
}

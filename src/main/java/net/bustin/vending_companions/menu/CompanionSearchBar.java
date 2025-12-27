package net.bustin.vending_companions.menu;

import com.mojang.datafixers.util.Pair;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.core.vault.modifier.spi.VaultModifier;
import iskallia.vault.item.CompanionItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class CompanionSearchBar {
    private final EditBox box;
    private Runnable onChange = () -> {};

    public List<Component> getTooltipLines() {
        if (Screen.hasShiftDown()) {
            return List.of(
                    new TextComponent("'#' to search by Modifier"),
                    new TextComponent("'>' to search Greater Than"),
                    new TextComponent("'<' to search Less Than"),
                    new TextComponent("'=' to search Exact Match")
            );
        }
        return List.of(new TextComponent("Hold Shift for Filters"));
    }

    public CompanionSearchBar(Font font, int x, int y, int w, int h) {
        this.box = new EditBox(font, x, y, w, h, TextComponent.EMPTY);
        this.box.setMaxLength(50);

        // remove vanilla background/border
        this.box.setBordered(false);

        // optional styling
        this.box.setTextColor(0xFFFFFF);
        this.box.setTextColorUneditable(0xAAAAAA);
        // this.box.setHint(new TextComponent("Search")); // only if available

        this.box.setResponder(s -> onChange.run());
    }

    public EditBox widget() {
        return box;
    }

    public void setOnChange(Runnable r) {
        this.onChange = (r == null) ? () -> {} : r;
    }

    public void tick() {
        box.tick();
    }

    public String query() {
        return box.getValue();
    }

    public void clear() {
        this.box.setValue("");
    }

    /** Returns REAL indices into menu.getCompanions() that match the current query */
    public List<Integer> filter(List<ItemStack> companions) {
        String q = query().trim().toLowerCase(Locale.ROOT);

        List<Integer> out = new ArrayList<>();
        if (companions == null) return out;

        for (int i = 0; i < companions.size(); i++) {
            ItemStack stack = companions.get(i);
            if (stack == null || stack.isEmpty()) continue;

            if (matches(stack, q)) {
                out.add(i);
            }
        }
        return out;
    }

    private boolean matches(ItemStack stack, String q) {
        if (q.isEmpty()) return true;

        if (q.startsWith("#")) {
            return matchesModifierQuery(stack, q);
        }

        String name = CompanionItem.getPetName(stack);
        if (name == null || name.isEmpty()) name = stack.getHoverName().getString();

        return name.toLowerCase(Locale.ROOT).contains(q);
    }

    private static class TagQuery {
        final String tag;     // "ornate"
        final char op;        // 0 (none), '>', '<', '='
        final int n;          // threshold

        TagQuery(String tag, char op, int n) {
            this.tag = tag;
            this.op = op;
            this.n = n;
        }
    }

    private boolean matchesModifierQuery(ItemStack stack, String raw) {
        TagQuery tq = parseTagQuery(raw);
        if (tq == null || tq.tag.isEmpty()) return true;

        int count = countMatchingModifiers(stack, tq.tag);

        // no comparator => "has at least 1"
        if (tq.op == 0) return count > 0;

        return switch (tq.op) {
            case '>' -> count > tq.n;
            case '<' -> count < tq.n;
            case '=' -> count == tq.n;
            default  -> count > 0;
        };
    }

    private TagQuery parseTagQuery(String raw) {
        // raw like "#ornate>2" or "#ornate = 2"
        String s = raw.substring(1).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return new TagQuery("", (char)0, 0);

        // find first comparator char
        int idx = -1;
        char op = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '>' || c == '<' || c == '=') {
                idx = i;
                op = c;
                break;
            }
        }

        if (idx == -1) {
            // just "#tag"
            return new TagQuery(s.trim(), (char)0, 0);
        }

        String tag = s.substring(0, idx).trim();
        String numStr = s.substring(idx + 1).trim();

        int n = 0;
        try {
            n = Integer.parseInt(numStr);
        } catch (Exception ignored) {
            // bad number -> treat as "#tag"
            op = 0;
            n = 0;
        }

        return new TagQuery(tag, op, n);
    }

    private int countMatchingModifiers(ItemStack stack, String tag) {
        int count = 0;

        // --- temporal modifier (0 or 1) ---
        Optional<ResourceLocation> temporalOpt = CompanionItem.getTemporalModifier(stack);
        if (temporalOpt.isPresent() && modifierMatchesTag(temporalOpt.get(), tag)) {
            count++;
        }

        // --- relic modifiers (can be many, can duplicate) ---
        // CompanionItem.getAllRelics(stack) returns Map<Integer, Pair<Integer, List<ResourceLocation>>>
        try {
            Map<Integer, com.mojang.datafixers.util.Pair<Integer, List<ResourceLocation>>> relicMap =
                    CompanionItem.getAllRelics(stack);

            if (relicMap != null) {
                for (var entry : relicMap.values()) {
                    if (entry == null) continue;
                    List<ResourceLocation> ids = entry.getSecond();
                    if (ids == null) continue;

                    for (ResourceLocation id : ids) {
                        if (id != null && modifierMatchesTag(id, tag)) {
                            count++;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return count;
    }

    private boolean modifierMatchesTag(ResourceLocation id, String tag) {
        // match against id path
        if (id.getPath().toLowerCase(Locale.ROOT).contains(tag)) return true;

        // also match against VH display name if available
        Optional<VaultModifier<?>> vmOpt = VaultModifierRegistry.getOpt(id);
        if (vmOpt.isPresent()) {
            String dn = vmOpt.get().getDisplayName();
            if (dn != null && dn.toLowerCase(Locale.ROOT).contains(tag)) return true;
        }

        return false;
    }

    private boolean hasAnyModifierLike(ItemStack stack, String tag) {
        tag = tag.toLowerCase(Locale.ROOT);

        // 1) temporal modifier
        if (hasModifierIdLike(CompanionItem.getTemporalModifier(stack), tag)) return true;

        // 2) relic modifiers (the ones you render in the row button)
        Map<Integer, Pair<Integer, List<ResourceLocation>>> relicMap = CompanionItem.getAllRelics(stack);
        if (relicMap != null && !relicMap.isEmpty()) {
            for (Pair<Integer, List<ResourceLocation>> entry : relicMap.values()) {
                if (entry == null) continue;
                List<ResourceLocation> list = entry.getSecond();
                if (list == null || list.isEmpty()) continue;

                for (ResourceLocation id : list) {
                    if (id == null) continue;
                    if (id.getPath().toLowerCase(Locale.ROOT).contains(tag)) return true;

                    // also match display name if it exists in VH registry
                    Optional<VaultModifier<?>> vmOpt = VaultModifierRegistry.getOpt(id);
                    if (vmOpt.isPresent()) {
                        String dn = vmOpt.get().getDisplayName();
                        if (dn != null && dn.toLowerCase(Locale.ROOT).contains(tag)) return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean hasModifierIdLike(Optional<ResourceLocation> opt, String tag) {
        if (opt == null || opt.isEmpty()) return false;

        ResourceLocation id = opt.get();
        if (id.getPath().toLowerCase(Locale.ROOT).contains(tag)) return true;

        Optional<VaultModifier<?>> vmOpt = VaultModifierRegistry.getOpt(id);
        if (vmOpt.isPresent()) {
            String dn = vmOpt.get().getDisplayName();
            return dn != null && dn.toLowerCase(Locale.ROOT).contains(tag);
        }

        return false;
    }

    private boolean hasTemporalModifierLike(ItemStack stack, String tag) {
        Optional<ResourceLocation> temporalOpt = CompanionItem.getTemporalModifier(stack);
        if (temporalOpt.isEmpty()) return false;

        ResourceLocation id = temporalOpt.get();

        // match #ornate against id path (fast)
        if (id.getPath().toLowerCase(Locale.ROOT).contains(tag)) return true;

        // also match against VH display name if available
        Optional<VaultModifier<?>> vmOpt = VaultModifierRegistry.getOpt(id);
        if (vmOpt.isPresent()) {
            VaultModifier<?> vm = vmOpt.get();
            String dn = vm.getDisplayName();
            if (dn != null && dn.toLowerCase(Locale.ROOT).contains(tag)) return true;
        }

        return false;
    }
}

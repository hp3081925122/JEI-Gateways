package com.hp.jei_gateways.jei;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class JeiTextUtil {
    private JeiTextUtil() {
    }

    static Component blackName(ItemStack stack) {
        Component name = Component.literal(stack.getHoverName().getString()).withStyle(ChatFormatting.BLACK);
        return Component.translatable("jei.jei_gateways.name", name).withStyle(ChatFormatting.BLACK);
    }

    static int drawWrapped(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int maxWidth, int lineHeight, int color, boolean dropShadow, int maxLines) {
        if (maxLines <= 0) {
            return 0;
        }
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int linesToDraw = Math.min(lines.size(), maxLines);
        for (int index = 0; index < linesToDraw; index++) {
            guiGraphics.text(font, lines.get(index), x, y + index * lineHeight, color, dropShadow);
        }
        return linesToDraw;
    }

    static int lineCount(Font font, Component text, int maxWidth) {
        return Math.max(1, font.split(text, maxWidth).size());
    }
}

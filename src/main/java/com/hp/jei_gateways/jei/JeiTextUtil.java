package com.hp.jei_gateways.jei;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

final class JeiTextUtil {
    private JeiTextUtil() {
    }

    static int drawWrapped(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int maxWidth, int lineHeight, int color, int maxLines) {
        if (maxLines <= 0) {
            return 0;
        }
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        int linesToDraw = Math.min(lines.size(), maxLines);
        for (int index = 0; index < linesToDraw; index++) {
            guiGraphics.drawString(font, lines.get(index), x, y + index * lineHeight, color, false);
        }
        return linesToDraw;
    }

    static int lineCount(Font font, Component text, int maxWidth) {
        return Math.max(1, font.split(text, maxWidth).size());
    }
}

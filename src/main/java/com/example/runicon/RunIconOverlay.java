package com.example.runicon;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class RunIconOverlay {

    private static final ResourceLocation RUN_TEXTURE =
            new ResourceLocation(RunIconMod.MODID, "textures/gui/run_icon.png");
    private static final ResourceLocation SWIM_TEXTURE =
            new ResourceLocation(RunIconMod.MODID, "textures/gui/swim_icon.png");

    // Обе текстуры: 4 кадра по 16x16, вертикальная лента 16x64
    private static final int FRAME_SIZE = 16;
    private static final int FRAME_COUNT = 4;
    private static final int TEX_W = 16;
    private static final int TEX_H = FRAME_SIZE * FRAME_COUNT;

    // Размер на экране (в 2 раза больше, пиксели остаются чёткими)
    private static final int DRAW_SIZE = 32;
    private static final int MARGIN = 6;

    // Скорость смены кадров, мс на кадр
    private static final long RUN_FRAME_MS = 90L;
    private static final long SWIM_FRAME_MS = 150L;

    // Плавное появление/исчезновение
    private static float alpha = 0.0F;
    private static long lastTime = 0L;
    // Какую иконку рисовать (запоминаем, чтобы она корректно гасла)
    private static boolean lastWasSwim = false;

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            alpha = 0.0F;
            return;
        }

        long now = Util.getMillis();
        float dt = lastTime == 0L ? 0.0F : (now - lastTime) / 1000.0F;
        lastTime = now;

        boolean swimming = false;
        boolean running = false;
        if (!player.isSpectator()) {
            // Плавание: игрок в режиме плавания, либо под водой и движется
            Vec3 motion = player.getDeltaMovement();
            boolean moving = motion.x * motion.x + motion.z * motion.z + motion.y * motion.y > 0.0004D;
            swimming = player.isSwimming() || (player.isUnderWater() && moving);
            running = !swimming && player.isSprinting() && !player.isInWater();
        }

        if (swimming) {
            lastWasSwim = true;
        } else if (running) {
            lastWasSwim = false;
        }

        float target = (swimming || running) ? 1.0F : 0.0F;
        float step = dt * 6.0F; // ~0.17 с на полное появление
        if (alpha < target) {
            alpha = Math.min(target, alpha + step);
        } else if (alpha > target) {
            alpha = Math.max(target, alpha - step);
        }

        if (alpha <= 0.01F) {
            return;
        }

        long frameMs = lastWasSwim ? SWIM_FRAME_MS : RUN_FRAME_MS;
        int frame = (int) ((now / frameMs) % FRAME_COUNT);
        int x = width - DRAW_SIZE - MARGIN;
        int y = height - DRAW_SIZE - MARGIN;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);

        graphics.blit(lastWasSwim ? SWIM_TEXTURE : RUN_TEXTURE,
                x, y, DRAW_SIZE, DRAW_SIZE,
                0.0F, (float) (frame * FRAME_SIZE),
                FRAME_SIZE, FRAME_SIZE,
                TEX_W, TEX_H);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}

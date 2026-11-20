package com.watermelon0117.aicraft.common;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.Optional;

/**
 * Turns the image model's output into the image returned to the mod.
 *
 * <p>The server pixelates the model image itself ({@link SpritePixelizer}) and returns the
 * sprite enlarged so the released mods reconstruct it exactly ({@link LegacyModEncoder}).
 * When either step fails, the model image is flattened onto a plain background and returned
 * as before, so the mod pixelates it the old way.
 *
 * <p>Copied from the texture server's pixelizer, so that an own OpenAI key gets the same pictures: change both.
 */
public final class SpritePipeline {
    private static final System.Logger LOGGER = System.getLogger(SpritePipeline.class.getName());

    public enum Route { SPRITE, FALLBACK_NO_SPRITE, FALLBACK_NOT_REPRODUCIBLE, FALLBACK_UNREADABLE }

    /**
     * @param modImageB64   base64 PNG to send to the mod
     * @param playerTexture PNG the mod will produce from it, or null if unknown
     */
    public record Output(String modImageB64, byte[] playerTexture, Route route) {
    }

    private SpritePipeline() {
    }

    public static Output process(String modelImageB64) {
        byte[] bytes = Base64.getDecoder().decode(modelImageB64);
        BufferedImage model = LegacyModEncoder.readLikeMod(bytes);
        if (model == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Model image is unreadable; returning it unchanged");
            return new Output(modelImageB64, null, Route.FALLBACK_UNREADABLE);
        }
        Optional<SpritePixelizer.Result> sprite = SpritePixelizer.pixelize(model);
        if (sprite.isEmpty()) {
            LOGGER.log(System.Logger.Level.WARNING, "No pixel grid found in the model image; falling back to mod-side pixelation");
            return fallback(model, Route.FALLBACK_NO_SPRITE);
        }
        Optional<LegacyModEncoder.Encoded> encoded = LegacyModEncoder.encode(sprite.get().sprite());
        if (encoded.isEmpty()) {
            LOGGER.log(System.Logger.Level.WARNING, "Sprite {0}x{1} cannot be reproduced by the mod; falling back to mod-side pixelation",
                    sprite.get().contentWidth(), sprite.get().contentHeight());
            return fallback(model, Route.FALLBACK_NOT_REPRODUCIBLE);
        }
        return new Output(Base64.getEncoder().encodeToString(encoded.get().png()),
                LegacyModEncoder.toPng(encoded.get().playerTexture()), Route.SPRITE);
    }

    /** The model image on a flat background the mod can remove, plus what the mod makes of it. */
    private static Output fallback(BufferedImage model, Route route) {
        BufferedImage flat = flatten(model);
        byte[] png = LegacyModEncoder.toPng(flat);
        BufferedImage texture = ImageGridProcessor.tryProcess(LegacyModEncoder.readLikeMod(png));
        return new Output(Base64.getEncoder().encodeToString(png),
                texture == null ? null : LegacyModEncoder.toPng(texture), route);
    }

    static BufferedImage flatten(BufferedImage model) {
        int w = model.getWidth(), h = model.getHeight();
        int background = LegacyModEncoder.rankBackgrounds(model, new int[]{0, 0, w, h}).stream()
                .findFirst().orElse(0xFF00FF);
        BufferedImage flat = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = model.getRGB(x, y);
                flat.setRGB(x, y, (argb >>> 24) >= 128 ? argb & 0xFFFFFF : background);
            }
        }
        return flat;
    }
}

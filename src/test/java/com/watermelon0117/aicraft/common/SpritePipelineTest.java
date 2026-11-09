package com.watermelon0117.aicraft.common;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpritePipelineTest {

    @Test
    void returnsAnImageTheModTurnsIntoTheServerSprite() {
        BufferedImage sprite = SyntheticSprites.sprite(SyntheticSprites.diamond(14), 21);
        String model = b64(SyntheticSprites.modelImage(sprite, 34, 280, 300, true));

        SpritePipeline.Output out = SpritePipeline.process(model);

        assertEquals(SpritePipeline.Route.SPRITE, out.route());
        BufferedImage produced = ImageGridProcessor.tryProcess(decode(out.modImageB64()));
        assertTrue(LegacyModEncoder.sameTexture(produced, decode(out.playerTexture())));
        assertTrue(SyntheticSprites.sameContent(sprite, produced));
    }

    @Test
    void fallsBackToTheFlattenedModelImageWhenNoGridIsFound() {
        String empty = b64(new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB));

        SpritePipeline.Output out = SpritePipeline.process(empty);

        assertEquals(SpritePipeline.Route.FALLBACK_NO_SPRITE, out.route());
        BufferedImage returned = decode(out.modImageB64());
        assertNotNull(returned);
        assertEquals(1024, returned.getWidth());
        assertEquals(0xFF00FF, returned.getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void returnsUnreadableImagesUnchanged() {
        String garbage = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});

        SpritePipeline.Output out = SpritePipeline.process(garbage);

        assertEquals(SpritePipeline.Route.FALLBACK_UNREADABLE, out.route());
        assertEquals(garbage, out.modImageB64());
    }

    private static String b64(BufferedImage image) {
        return Base64.getEncoder().encodeToString(LegacyModEncoder.toPng(image));
    }

    private static BufferedImage decode(String b64) {
        return decode(Base64.getDecoder().decode(b64));
    }

    private static BufferedImage decode(byte[] bytes) {
        return LegacyModEncoder.readLikeMod(bytes);
    }
}

package com.watermelon0117.aicraft.gpt.openai;

import com.mojang.logging.LogUtils;
import com.watermelon0117.aicraft.common.BlockSheet;
import com.watermelon0117.aicraft.common.CrossTexture;
import com.watermelon0117.aicraft.gpt.GeneratedBlock;
import com.watermelon0117.aicraft.gpt.GeneratedResult;
import com.watermelon0117.aicraft.gpt.GenerationException;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** What the texture server's {@code /textures/auto} does before it makes an item, for an own OpenAI key. */
public class GPTBlockGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final OpenAIChatClient judge = new OpenAIChatClient(BlockJudge.MODEL, 0.0, BlockJudge.MAX_COMPLETION_TOKENS, "", "json_schema");
    private final GPTImageGenerator images = new GPTImageGenerator();

    /**
     * A judge that fails is an error, as an item made instead would stay as what that recipe makes.
     *
     * @return the block, or empty when this should be an item
     */
    public CompletableFuture<Optional<GeneratedBlock>> generate(String id, String[] grid) {
        // A recipe shaped like a stick tool or armor needs no judge, and a block there would cost the player the function.
        if (RecipeShapeMatcher.match(grid) != null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return judge.chatStructured(BlockJudge.SCHEMA_NAME, BlockJudge.schema(), BlockJudge.buildPrompt(id, grid), BlockJudge.TIMEOUT)
                .thenApply(rawJson -> {
                    LOGGER.info("Block judge: {} is {}", id, rawJson);
                    return BlockJudge.parse(rawJson, id, grid);
                })
                .exceptionally(error -> {
                    throw new GenerationException(GenerationException.describe(error, Component.translatable("error.aicraft.bad_answer")), error);
                })
                .thenCompose(plan -> plan.isEmpty()
                        ? CompletableFuture.completedFuture(Optional.empty())
                        : draw(id, plan.get()).thenApply(Optional::of));
    }

    private CompletableFuture<GeneratedBlock> draw(String id, BlockJudge.Plan plan) {
        if (plan.isCross()) {
            return images.generateDrawing(BlockImagePrompts.cross(id, plan.description(), plan.attach()))
                    .thenApply(modelB64 -> cross(plan, CrossTexture.process(decode(modelB64), plan.attach().equals("above"))));
        }
        return images.generateSheet(BlockImagePrompts.sheet(id, plan), plan.seeThrough())
                .thenApply(modelB64 -> sheet(plan, BlockSheet.process(decode(modelB64), plan.seeThrough())));
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static GeneratedBlock cross(BlockJudge.Plan plan, byte[] texture) {
        return GeneratedBlock.fromResponse(response(plan, encode(texture), null, null, null));
    }

    /** The squares of a material are variations of one face, and the first was the most reliable. */
    private static GeneratedBlock sheet(BlockJudge.Plan plan, List<byte[]> squares) {
        boolean material = "material".equals(plan.kind());
        return GeneratedBlock.fromResponse(response(plan, null, encode(squares.get(0)), encode(squares.get(material ? 0 : 1)), encode(squares.get(material ? 0 : 2))));
    }

    /** The texture server's answer, so that a block made here passes the same checks. */
    private static GeneratedResult.Response response(BlockJudge.Plan plan, String texture, String top, String front, String side) {
        return new GeneratedResult.Response("block", null, null, plan.model(), plan.kind(), plan.tool(), plan.hardness(), plan.light(), plan.sound(),
                plan.flammable(), plan.count(), plan.shapeless(), texture, top, front, side, plan.attach());
    }
}

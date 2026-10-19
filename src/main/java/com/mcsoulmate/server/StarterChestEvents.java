package com.mcsoulmate.server;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.mcsoulmate.McSoulmate;
import com.mcsoulmate.entity.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = McSoulmate.MOD_ID)
public final class StarterChestEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STARTER_CHEST_GRANTED_TAG = "mcsoulmate_starter_chest_granted";
    private static final int SPAWN_EGG_SLOT = 0;
    private static final int DIAMOND_SLOT = 1;
    private static final int NAME_TAG_SLOT = 2;
    private static final List<BlockPos> ADJACENT_OFFSETS = List.of(
            new BlockPos(0, 0, -1),
            new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, -1),
            new BlockPos(-1, 0, 1),
            new BlockPos(1, 0, 1)
    );

    private StarterChestEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.entityTags().contains(STARTER_CHEST_GRANTED_TAG)) {
            return;
        }

        createStarterChest(serverPlayer);
    }

    private static void createStarterChest(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel level = server.overworld();
        BlockPos spawnPosition = level.getRespawnData().pos();
        BlockPos chestPosition = findChestPosition(level, spawnPosition);
        if (chestPosition == null) {
            LOGGER.warn("Could not place MC Soulmate starter chest beside world spawn at {} for player {}", spawnPosition, player.getScoreboardName());
            return;
        }
        if (!player.addTag(STARTER_CHEST_GRANTED_TAG)) {
            LOGGER.error("Could not mark MC Soulmate starter chest as granted for player {}", player.getScoreboardName());
            return;
        }
        if (!level.setBlock(chestPosition, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL)) {
            player.removeTag(STARTER_CHEST_GRANTED_TAG);
            LOGGER.error("Could not place MC Soulmate starter chest at {} for player {}", chestPosition, player.getScoreboardName());
            return;
        }
        if (!(level.getBlockEntity(chestPosition) instanceof ChestBlockEntity chest)) {
            level.removeBlock(chestPosition, false);
            player.removeTag(STARTER_CHEST_GRANTED_TAG);
            LOGGER.error("Chest block entity was not created at {} for player {}", chestPosition, player.getScoreboardName());
            return;
        }

        fillStarterChest(chest);
        LOGGER.info("Placed MC Soulmate starter chest at {} for player {}", chestPosition, player.getScoreboardName());
    }

    private static BlockPos findChestPosition(ServerLevel level, BlockPos spawnPosition) {
        for (BlockPos offset : ADJACENT_OFFSETS) {
            BlockPos position = spawnPosition.offset(offset);
            BlockPos floorPosition = position.below();
            if (!level.isEmptyBlock(position)) {
                continue;
            }
            if (!level.getBlockState(floorPosition).isFaceSturdy(level, floorPosition, Direction.UP)) {
                continue;
            }
            return position;
        }
        return null;
    }

    private static void fillStarterChest(ChestBlockEntity chest) {
        chest.setItem(SPAWN_EGG_SLOT, new ItemStack(ModItems.SOULMATE_SPAWN_EGG.get()));
        chest.setItem(DIAMOND_SLOT, Items.DIAMOND.getDefaultInstance());
        chest.setItem(NAME_TAG_SLOT, Items.NAME_TAG.getDefaultInstance());
        chest.setChanged();
    }
}

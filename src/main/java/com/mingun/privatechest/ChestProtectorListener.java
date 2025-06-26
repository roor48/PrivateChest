package com.mingun.privatechest;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.sign.Side;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChestProtectorListener implements Listener {

    private final JavaPlugin plugin;

    private static final List<Material> SIGN_MATERIALS = Arrays.asList(
            Material.OAK_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.BIRCH_WALL_SIGN,
            Material.JUNGLE_WALL_SIGN, Material.ACACIA_WALL_SIGN, Material.DARK_OAK_WALL_SIGN,
            Material.MANGROVE_WALL_SIGN, Material.CHERRY_WALL_SIGN, Material.PALE_OAK_WALL_SIGN,
            Material.BAMBOO_WALL_SIGN, Material.CRIMSON_WALL_SIGN, Material.WARPED_WALL_SIGN
    );

    public ChestProtectorListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || !(clickedBlock.getState() instanceof Container))
            return;


        List<Sign> foundSigns = FindAttachedSign(clickedBlock, false);
        if (!foundSigns.isEmpty()) {
            // UUID가 유효하고 현재 플레이어의 UUID와 다르면 접근을 막습니다.
            if (!IsOwner(player, foundSigns)) {
                player.sendMessage("§c[PrivateChest] 이 상자는 상호작용 할 수 없습니다!");
                event.setCancelled(true);
            }
        }
    }

    private final BlockFace[] horizontalFaces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
    private List<Sign> FindAttachedSign(Block containerBlock, boolean isSecond) {
        List<Sign> signs = new ArrayList<>();

        BlockData blockData = containerBlock.getBlockData();
        plugin.getLogger().info(blockData.toString());

        if (!isSecond && blockData instanceof Chest chestData) {

            BlockFace facing = chestData.getFacing(); // 상자가 바라보는 방향
            BlockFace connectedFace = null;
            // 상자의 타입과 바라보는 방향을 기반으로 연결된 다른 절반의 방향을 결정합니다.
            if (chestData.getType() == Chest.Type.LEFT) {
                // LEFT 타입 상자의 다른 절반은 바라보는 방향의 '시계 방향'에 있습니다.
                if (facing == BlockFace.NORTH) connectedFace = BlockFace.EAST;
                else if (facing == BlockFace.EAST) connectedFace = BlockFace.SOUTH;
                else if (facing == BlockFace.SOUTH) connectedFace = BlockFace.WEST;
                else if (facing == BlockFace.WEST) connectedFace = BlockFace.NORTH;
            } else if (chestData.getType() == Chest.Type.RIGHT) { // Type.RIGHT
                // RIGHT 타입 상자의 다른 절반은 바라보는 방향의 '반시계 방향'에 있습니다.
                if (facing == BlockFace.NORTH) connectedFace = BlockFace.WEST;
                else if (facing == BlockFace.EAST) connectedFace = BlockFace.NORTH;
                else if (facing == BlockFace.SOUTH) connectedFace = BlockFace.EAST;
                else if (facing == BlockFace.WEST) connectedFace = BlockFace.SOUTH;
            }

            if (connectedFace != null) {
                Block otherHalf = containerBlock.getRelative(connectedFace);
                if (otherHalf.getType().equals(containerBlock.getType()) && otherHalf.getState() instanceof Container) {
                    // 옆 블럭 기준으로 한번 더 탐색
                    signs = FindAttachedSign(otherHalf, true);
                }
            }
        }

        for (BlockFace face : horizontalFaces) {
            Block relativeBlock = containerBlock.getRelative(face);
            BlockState state = relativeBlock.getState();

            if (SIGN_MATERIALS.contains(relativeBlock.getType())) {
                if (state instanceof Sign sign) {
                    BlockData signData = relativeBlock.getBlockData();

                    if (signData instanceof Directional directional) {
                        if (directional.getFacing().equals(face)) {
                            signs.add(sign);
                        }
                    }
                }
            }
        }
        return signs;
    }

    private boolean IsOwner(Player player, List<Sign> foundSigns) {
        if (player.isOp())
            return true;

        boolean hasOwner = false;
        UUID playerUUID = player.getUniqueId();
        for (Sign foundSign : foundSigns) {
            // 표지판 앞면의 모든 줄을 Component 리스트로 불러옴
            List<Component> signLines = foundSign.getSide(Side.FRONT).lines();

            String ownerUUIDString = "";
            if (signLines.size() > 3) {
                Component uuidComponent = signLines.get(3);
                if (uuidComponent != null) {
                    ownerUUIDString = PlainTextComponentSerializer.plainText().serialize(uuidComponent);
                }
            }

            // 탐지 중에 오류 uuid가 있고, 유효한 uuid가 있으며 그게 나의 uuid가 아니면 false
            int tmp = IsOwner(playerUUID, ownerUUIDString);
            if (tmp == 1)
                return true;

            if (tmp == 0)
                hasOwner = true;

        }

        return !hasOwner;
    }

    /**
     * 표지판이 현재 플레이어 것인지 확인
     * -1 : 유효하지 않은 uuid
     *  0 : 주인이 아님
     *  1 : 주인임
     */
    private int IsOwner(UUID playerUUID, String ownerUUIDString) {
        if (ownerUUIDString.isEmpty())
            return -1;

        // UUID와 현재 플레이어의 UUID 비교
        try {
            UUID ownerUUID = UUID.fromString(ownerUUIDString);
            if (playerUUID.equals(ownerUUID)) {
                return 1;
            }
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 UUID가 발견된 경우
            plugin.getLogger().warning("Invalid UUID found on a sign: " + ownerUUIDString);
            return -1;
        }

        return 0;
    }
}

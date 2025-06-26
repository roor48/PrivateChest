package com.mingun.privatechest;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import org.bukkit.plugin.java.JavaPlugin;


public class SignEngraverListener implements Listener {

    private final JavaPlugin plugin;

    private static final List<Material> SIGN_MATERIALS = Arrays.asList(
            Material.OAK_WALL_SIGN, Material.SPRUCE_WALL_SIGN, Material.BIRCH_WALL_SIGN,
            Material.JUNGLE_WALL_SIGN, Material.ACACIA_WALL_SIGN, Material.DARK_OAK_WALL_SIGN,
            Material.MANGROVE_WALL_SIGN, Material.CHERRY_WALL_SIGN, Material.PALE_OAK_WALL_SIGN,
            Material.BAMBOO_WALL_SIGN, Material.CRIMSON_WALL_SIGN, Material.WARPED_WALL_SIGN
    );

    public SignEngraverListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        Action action = event.getAction();

        if (    clickedBlock == null || // 블럭이 없거나
                !SIGN_MATERIALS.contains(clickedBlock.getType()) || // 지정된 표지판이 아니거나
                !isSignAttachedToProtectableContainer(clickedBlock)) // Container에 부착되지 않았으면
            return;

        BlockState state = clickedBlock.getState();
        Sign sign = (Sign)state;

        List<Component> signLines = sign.getSide(Side.FRONT).lines();

        // 주인 이름 가져오기
        String ownerName = "알 수 없는 플레이어";
        if (!signLines.isEmpty()) {
            Component nameComponent = signLines.getFirst();
            if (nameComponent != null) {
                // Component를 String으로 변환
                ownerName = PlainTextComponentSerializer.plainText().serialize(nameComponent);
            }
        }

        if (!IsOwner(player, signLines))
        {
            event.setCancelled(true);
            player.sendMessage("§c[PrivateChest] 이 표지판은 §e" + ownerName + "§c님만 상호작용 할 수 있습니다!");
            return;
        }

        if (player.isSneaking()) // 웅크리면
        {
            SignSide frontSide = sign.getSide(Side.FRONT);
            SignSide backSide = sign.getSide(Side.BACK);

            for(int i = 0; i < 4; i++) {
                // 표지판 내용 지우기
                frontSide.line(i, Component.empty());
                backSide.line(i, Component.empty());
            }

            if (action.isRightClick()) {

                frontSide.line(0, Component.text(player.getName())); // line 0: 플레이어 이름
                frontSide.line(3, Component.text(player.getUniqueId().toString())); // line 3: 플레이어 UUID

                player.sendMessage("§a[PrivateChest] 표지판에 당신의 이름과 보호 정보가 새겨졌습니다!");
            }
            else {
                // 좌클을 하면 표지판 내용만 제거됨
                player.sendMessage("§a[PrivateChest] 표지판 내용이 제거되었습니다!");
            }

            sign.update(); // 변경사항 적용

            event.setCancelled(true);
        }
        else if (action.isRightClick()) {
            // 표지판 내용 변경 방지
            player.sendMessage("§a[PrivateChest] 상자에 부착된 표지판은 수정할 수 없습니다.");
            event.setCancelled(true);
        }
    }

    /**
     * 표지판이 Container에 부착되었는지 확인
     */
    private boolean isSignAttachedToProtectableContainer(Block signBlock) {
        // 표지판의 BlockData를 가져와서 방향 정보를 확인합니다.

        Directional directional = (Directional)signBlock.getBlockData();
        BlockFace attachedFace = directional.getFacing().getOppositeFace(); // 표지판이 붙어있는 면 (바라보는 방향의 반대)

        BlockState attachedBlockState = signBlock.getRelative(attachedFace).getState(); // 표지판이 붙어있는 블록

        // 부착된 블록이 컨테이너면
        return (attachedBlockState instanceof Container);
    }


    /**
     * 표지판이 현재 플레이어 것인지 확인
     */
    private boolean IsOwner(Player player, List<Component> signLines) {
        if (player.isOp())
            return true;

        // UUID 가져오기
        UUID playerUUID = player.getUniqueId();
        String ownerUUIDString = "";
        if (signLines.size() > 3) { // 4번째 줄 (인덱스 3)이 존재하는지 확인
            Component uuidComponent = signLines.get(3);
            if (uuidComponent != null) {
                // Component를 순수 텍스트로 변환
                ownerUUIDString = PlainTextComponentSerializer.plainText().serialize(uuidComponent);
//                plugin.getLogger().info(ownerUUIDString);
            }
        }


        // UUID와 현재 플레이어의 UUID 비교
        if (!ownerUUIDString.isEmpty()) {
            try {
                UUID ownerUUID = UUID.fromString(ownerUUIDString);
                if (!playerUUID.equals(ownerUUID)) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID found on a sign: " + ownerUUIDString);
                // 유효하지 않은 UUID가 발견된 경우, 경고 로그를 남기고 상자 접근을 허용
            }
        }

        return true;
    }
}
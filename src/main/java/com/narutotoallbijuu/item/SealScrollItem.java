package com.narutotoallbijuu.item;

import com.narutotoallbijuu.data.JinchuurikiData;
import com.narutotoallbijuu.entity.BijuuEntity;
import com.narutotoallbijuu.logic.Msg;
import com.narutotoallbijuu.logic.Rituals;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Pergaminho de selamento: sela um Bijuu enfraquecido ou extrai o Bijuu de um hospedeiro. */
public class SealScrollItem extends Item {
    public SealScrollItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (target instanceof BijuuEntity be && be.isWeak()) {
            Rituals.start(sp, be, Rituals.Type.SEAL_ENTITY, Rituals.sealTicks());
        } else if (target instanceof ServerPlayer host && host != sp && JinchuurikiData.of(host).bijuu >= 0) {
            Rituals.start(sp, host, Rituals.Type.EXTRACT, Rituals.extractTicks());
        } else {
            Msg.bar(sp, "scroll_invalid");
        }
        return InteractionResult.CONSUME;
    }
}

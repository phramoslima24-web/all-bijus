package com.narutotoallbijuu.item;

import com.narutotoallbijuu.bijuu.Bijuu;
import com.narutotoallbijuu.bijuu.BijuuState;
import com.narutotoallbijuu.data.BijuuWorldData;
import com.narutotoallbijuu.logic.Rituals;
import com.narutotoallbijuu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Receptaculo com um Bijuu extraido. So vale enquanto o token bater com o registro mundial. */
public class VesselItem extends Item {
    public VesselItem(Properties props) {
        super(props);
    }

    public static ItemStack make(Bijuu b, UUID token) {
        ItemStack s = new ItemStack(ModItems.VESSEL.get());
        CompoundTag t = s.getOrCreateTag();
        t.putInt("bijuu", b.ordinal());
        t.putUUID("token", token);
        return s;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        Bijuu b = t == null ? null : Bijuu.byIndex(t.getInt("bijuu"));
        if (b != null) {
            tip.add(b.displayName());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            Rituals.start(sp, sp, Rituals.Type.SEAL_VESSEL, Rituals.sealTicks());
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 40 != 0 || level.getServer() == null) {
            return;
        }
        CompoundTag t = stack.getTag();
        boolean valid = false;
        if (t != null && t.hasUUID("token")) {
            Bijuu b = Bijuu.byIndex(t.getInt("bijuu"));
            if (b != null) {
                BijuuWorldData.Entry e = BijuuWorldData.get(level.getServer()).get(b);
                valid = e.state == BijuuState.VESSEL && t.getUUID("token").equals(e.token);
            }
        }
        if (!valid) {
            stack.shrink(stack.getCount());
        }
    }
}

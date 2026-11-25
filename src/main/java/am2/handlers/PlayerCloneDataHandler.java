package am2.handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Copies AM2 extended data across the player death/respawn clone. */
public class PlayerCloneDataHandler {

    @SubscribeEvent
    public void onclone(PlayerEvent.Clone event) {
        if (event.original == null || event.entityPlayer == null || event.entityPlayer instanceof FakePlayer) {
            return;
        }
        EntityPlayer original = event.original;
        EntityPlayer newplayer = event.entityPlayer;

        NBTTagCompound oldNbt = new NBTTagCompound();
        ExtendedProperties.For(original)
            .saveNBTData(oldNbt);
        ExtendedProperties.For(newplayer)
            .loadNBTData(oldNbt);

        NBTTagCompound oldSkill = new NBTTagCompound();
        SkillData.For(original)
            .saveNBTData(oldSkill);
        SkillData.For(newplayer)
            .loadNBTData(oldSkill);

        NBTTagCompound oldRift = new NBTTagCompound();
        RiftStorage.For(original)
            .saveNBTData(oldRift);
        RiftStorage.For(newplayer)
            .loadNBTData(oldRift);

        NBTTagCompound oldAffinity = new NBTTagCompound();
        AffinityData.For(original)
            .saveNBTData(oldAffinity);
        AffinityData.For(newplayer)
            .loadNBTData(oldAffinity);
    }
}

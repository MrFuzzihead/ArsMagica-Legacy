package am2.handlers;

import am2.api.events.ManaCostEvent;
import am2.items.ItemsCommonProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Arcane spellbook discount applied after spell cost calculation. */
public class ManaCostHandler {

    @SubscribeEvent
    public void onManaCost(ManaCostEvent event) {
        if (event.caster.getHeldItem() != null && event.caster.getHeldItem()
            .getItem() == ItemsCommonProxy.arcaneSpellbook) {
            event.manaCost *= 0.75f;
            event.burnout *= 0.4f;
        }
    }
}

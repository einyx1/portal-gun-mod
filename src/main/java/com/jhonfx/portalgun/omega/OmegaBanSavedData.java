package com.jhonfx.portalgun.omega;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class OmegaBanSavedData extends SavedData {
    private static final String FILE_ID = "portalgun_omega_erased_entities";
    private final Set<ResourceLocation> erased = new LinkedHashSet<>();

    public static OmegaBanSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                OmegaBanSavedData::load, OmegaBanSavedData::new, FILE_ID);
    }

    public static OmegaBanSavedData load(CompoundTag tag) {
        OmegaBanSavedData data = new OmegaBanSavedData();
        ListTag list = tag.getList("Erased", Tag.TAG_STRING);
        for (Tag entry : list) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) data.erased.add(id);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        erased.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        tag.put("Erased", list);
        return tag;
    }

    public boolean contains(ResourceLocation id) { return erased.contains(id); }
    public Set<ResourceLocation> entries() { return Collections.unmodifiableSet(erased); }

    public boolean erase(ResourceLocation id) {
        boolean changed = erased.add(id);
        if (changed) setDirty();
        return changed;
    }

    public boolean restore(ResourceLocation id) {
        boolean changed = erased.remove(id);
        if (changed) setDirty();
        return changed;
    }
}

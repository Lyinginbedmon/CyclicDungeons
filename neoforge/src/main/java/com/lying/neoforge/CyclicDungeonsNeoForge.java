package com.lying.neoforge;

import net.neoforged.fml.common.Mod;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;

@Mod(Reference.ModInfo.MOD_ID)
public final class CyclicDungeonsNeoForge
{
    public CyclicDungeonsNeoForge()
    {
        // Run our common setup.
        CyclicDungeons.init();
    }
}

package com.watermelon0117.aicraft.init;

import com.watermelon0117.aicraft.AICraftingTable;
import com.watermelon0117.aicraft.commands.MyItemArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ArgumentInit {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENTS =
            DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, AICraftingTable.MODID);
    private static final RegistryObject<SingletonArgumentInfo<MyItemArgument>> MY_COMMAND_ARGUMENT_TYPE =
            ARGUMENTS.register("my_item", () -> ArgumentTypeInfos.registerByClass(MyItemArgument.class,
                    SingletonArgumentInfo.contextFree(MyItemArgument::item)));
}

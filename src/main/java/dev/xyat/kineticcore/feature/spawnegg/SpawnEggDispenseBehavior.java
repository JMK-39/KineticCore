package dev.xyat.kineticcore.feature.spawnegg;

//? if >=1.21 {
/*import net.minecraft.core.dispenser.BlockSource;
*///?} else {
import net.minecraft.core.BlockSource;
//?}
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public final class SpawnEggDispenseBehavior extends OptionalDispenseItemBehavior {
    public static final SpawnEggDispenseBehavior INSTANCE = new SpawnEggDispenseBehavior();
    private SpawnEggDispenseBehavior() { }
    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        //? if >=1.21 {
        /*var level = source.level();
        var direction = source.state().getValue(DispenserBlock.FACING);
        *///?} else {
        var level = source.getLevel();
        var direction = source.getBlockState().getValue(DispenserBlock.FACING);
        //?}
        var position = DispenserBlock.getDispensePosition(source);
        setSuccess(SpawnEggLauncher.dispense(level, new Vec3(position.x(), position.y(), position.z()),
                new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()), stack));
        if (isSuccess()) stack.shrink(1);
        return stack;
    }
    @Override protected void playAnimation(BlockSource source, net.minecraft.core.Direction direction) {
        if (isSuccess()) super.playAnimation(source, direction);
    }
}

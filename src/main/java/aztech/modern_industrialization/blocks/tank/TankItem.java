/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.blocks.tank;

import aztech.modern_industrialization.util.FluidHelper;
import aztech.modern_industrialization.util.NbtHelper;
import dev.technici4n.fasttransferlib.experimental.api.context.ContainerItemContext;
import dev.technici4n.fasttransferlib.experimental.api.fluid.ItemFluidStorage;
import dev.technici4n.fasttransferlib.experimental.api.item.ItemKey;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidKey;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleViewIterator;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.World;

public class TankItem extends BlockItem {
    public final long capacity;

    public TankItem(Block block, Settings settings, long capacity) {
        super(block, settings);
        this.capacity = capacity;
    }

    public void registerItemApi() {
        ItemFluidStorage.ITEM.registerForItems(TankItemStorage::new, this);
    }

    public boolean isEmpty(ItemStack stack) {
        return stack.getSubTag("BlockEntityTag") == null;
    }

    public FluidKey getFluid(ItemStack stack) {
        return NbtHelper.getFluidCompatible(stack.getSubTag("BlockEntityTag"), "fluid");
    }

    private void setFluid(ItemStack stack, FluidKey fluid) {
        NbtHelper.putFluid(stack.getOrCreateSubTag("BlockEntityTag"), "fluid", fluid);
    }

    public long getAmount(ItemStack stack) {
        if (getFluid(stack).isEmpty()) {
            return 0;
        }
        NbtCompound tag = stack.getSubTag("BlockEntityTag");
        if (tag == null)
            return 0;
        else if (tag.contains("amount"))
            return tag.getInt("amount") * 81;
        else
            return tag.getLong("amt");
    }

    private void setAmount(ItemStack stack, long amount) {
        stack.getOrCreateSubTag("BlockEntityTag").putLong("amt", amount);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(0xa9a9a9)).withItalic(true);
        if (!isEmpty(stack)) {
            tooltip.add(FluidHelper.getFluidName(getFluid(stack), true));
            tooltip.add(FluidHelper.getFluidAmount(getAmount(stack), capacity));
        } else {
            tooltip.add(new TranslatableText("text.modern_industrialization.empty").setStyle(style));
        }
    }

    class TankItemStorage implements Storage<FluidKey>, StorageView<FluidKey> {
        private final FluidKey fluid;
        private final long amount;
        private final ContainerItemContext ctx;

        TankItemStorage(ItemStack stack, ContainerItemContext ctx) {
            this.fluid = TankItem.this.getFluid(stack);
            this.amount = getAmount(stack);
            this.ctx = ctx;
        }

        private boolean updateTank(FluidKey fluid, long amount, Transaction tx) {
            ItemStack result = new ItemStack(TankItem.this);
            if (amount > 0) {
                setFluid(result, fluid);
                setAmount(result, amount);
            }
            ItemKey into = ItemKey.of(result);

            return ctx.transform(1, into, tx);
        }

        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public long insert(FluidKey fluid, long maxAmount, Transaction transaction) {
            StoragePreconditions.notEmptyNotNegative(fluid, maxAmount);
            if (ctx.getCount(transaction) == 0)
                return 0;

            long inserted = 0;
            if (TankItemStorage.this.fluid.isEmpty()) {
                inserted = Math.min(capacity, maxAmount);
            } else if (TankItemStorage.this.fluid.equals(fluid)) {
                inserted = Math.min(capacity - amount, maxAmount);
            }
            if (inserted > 0) {
                if (updateTank(fluid, amount + inserted, transaction)) {
                    return inserted;
                }
            }
            return 0;
        }

        @Override
        public boolean supportsExtraction() {
            return true;
        }

        @Override
        public long extract(FluidKey fluid, long maxAmount, Transaction transaction) {
            StoragePreconditions.notEmptyNotNegative(fluid, maxAmount);
            if (ctx.getCount(transaction) == 0)
                return 0;

            long extracted = 0;
            if (TankItemStorage.this.fluid.equals(fluid)) {
                extracted = Math.min(maxAmount, amount);
            }
            if (extracted > 0) {
                if (updateTank(fluid, amount - extracted, transaction)) {
                    return extracted;
                }
            }
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return resource().isEmpty();
        }

        @Override
        public FluidKey resource() {
            return fluid;
        }

        @Override
        public long amount() {
            return amount;
        }

        @Override
        public long capacity() {
            return capacity;
        }

        @Override
        public Iterator<StorageView<FluidKey>> iterator(Transaction transaction) {
            return SingleViewIterator.create(this, transaction);
        }
    }
}

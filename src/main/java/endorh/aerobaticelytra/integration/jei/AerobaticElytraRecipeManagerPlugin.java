package endorh.aerobaticelytra.integration.jei;

import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.util.recipe.RecipeManagerHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AerobaticElytraRecipeManagerPlugin implements IRecipeManagerPlugin {
   protected final List<ContextualRecipeCategory<?, ?>> contextualCategories;
   protected final IFocusFactory focusFactory;

   public AerobaticElytraRecipeManagerPlugin(
      List<ContextualRecipeCategory<?, ?>> contextualCategories,
      IFocusFactory focusFactory
   ) {
      this.contextualCategories = contextualCategories;
      this.focusFactory = focusFactory;
   }

   @Override public <V> @NotNull List<RecipeType<?>> getRecipeTypes(@NotNull IFocus<V> focus) {
      Optional<ItemStack> stack = focus.getTypedValue().getItemStack();
      if (stack.isPresent() && (
         stack.get().getItem() == AerobaticElytraItems.AEROBATIC_ELYTRA
         || stack.get().getItem() == AerobaticElytraItems.AEROBATIC_ELYTRA_WING))
         // noinspection unchecked
         return (List<RecipeType<?>>) (List<?>) contextualCategories.stream()
            .map(ContextualRecipeCategory::getContextualRecipeType)
            .toList();
      return Collections.emptyList();
   }

   @Override public <T, V> @NotNull List<T> getRecipes(@NotNull IRecipeCategory<T> cat, @NotNull IFocus<V> focus) {
      IFocusGroup fg = focusFactory.createFocusGroup(List.of(focus));
      if (cat instanceof ContextualRecipeCategory<?, ?> c) {
         //noinspection unchecked
         return (List<T>) c.getContextualRecipes(
            RecipeManagerHelper.getRecipeManager(), fg);
      }
      return Collections.emptyList();
   }

   @Override public <T> @NotNull List<T> getRecipes(@NotNull IRecipeCategory<T> cat) {
      if (cat instanceof ContextualRecipeCategory<?, ?> c) {
         RecipeManager manager = RecipeManagerHelper.getRecipeManager();
         // noinspection unchecked
         return (List<T>) c.getContextualRecipes(manager);
      }
      return Collections.emptyList();
   }
}

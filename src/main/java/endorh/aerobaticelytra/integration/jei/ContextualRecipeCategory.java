package endorh.aerobaticelytra.integration.jei;

import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collections;
import java.util.List;

public interface ContextualRecipeCategory<
   W extends ContextualRecipeWrapper, R extends IRecipeCategory<W>
> {
   RecipeType<?> getContextualRecipeType();
   default List<W> getContextualRecipes(RecipeManager manager) {
      return Collections.emptyList();
   }

   default List<W> getContextualRecipes(RecipeManager manager, IFocusGroup focuses) {
      return getContextualRecipes(manager).stream()
         .filter(r -> r.matchesFocus(focuses))
         .toList();
   }
}

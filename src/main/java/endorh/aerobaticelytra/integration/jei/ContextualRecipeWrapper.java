package endorh.aerobaticelytra.integration.jei;

import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

/**
 * A recipe wrapper for complex recipes where not all output items are always reachable.<br>
 *
 * Only supports {@link ItemStack} ingredients.
 */
public interface ContextualRecipeWrapper {


   /**
    * Get the set of input ingredients for this recipe.
    */
   @NotNull Collection<Ingredient> getInputIngredients();

   /**
    * Get the set of output items for this recipe.
    */
   @NotNull Collection<Ingredient> getOutputIngredients();


   boolean isOutputReachable(ItemStack output);

   default boolean isInputUsable(ItemStack input) {
      return true;
   }

   default boolean matchesFocus(@NotNull IFocusGroup focus) {
      return matchesOutput(focus) || matchesInput(focus);
   }

   default boolean matchesOutput(@NotNull IFocusGroup focus) {
      Collection<Ingredient> outputs = getOutputIngredients();
      return outputs.stream().anyMatch(out ->
         focus.getItemStackFocuses(OUTPUT).anyMatch(f -> {
            ItemStack output = f.getTypedValue().getIngredient();
            return out.test(output) && isOutputReachable(output);
         }));
   }

   default boolean matchesInput(@NotNull IFocusGroup focus) {
      Collection<Ingredient> inputs = getInputIngredients();
      return inputs.stream().anyMatch(in ->
         focus.getItemStackFocuses(INPUT).anyMatch(f -> {
            ItemStack input = f.getTypedValue().getIngredient();
            return in.test(input) && isInputUsable(input);
         }));
   }
}

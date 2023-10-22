package endorh.aerobaticelytra.integration.jei;

import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public abstract class AbstractContextualRecipeWrapper implements ContextualRecipeWrapper {
   protected final List<Ingredient> inputs;
   protected final List<Ingredient> outputs;


   protected AbstractContextualRecipeWrapper(List<Ingredient> inputs, List<Ingredient> outputs) {
      this.inputs = inputs;
      this.outputs = outputs;
   }

   @Override public @NotNull Collection<Ingredient> getInputIngredients() {
      return inputs;
   }

   @Override public @NotNull Collection<Ingredient> getOutputIngredients() {
      return outputs;
   }
}

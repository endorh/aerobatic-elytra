package endorh.aerobaticelytra.integration.jei;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.*;
import static net.minecraft.util.Mth.sqrt;
import static net.minecraft.util.Mth.*;

/**
 * Performs a lazy search for a <i>hopefully</i> close dye mix resulting in a given color.<br>
 * May also generate a remainder color to be used in a recipe as the color of the dyed item.<br>
 * <br>
 * See {@link #generateMix} for details.
 */
public class DyeMixGenerator {
   private final RandomSource RANDOM;

   public DyeMixGenerator(RandomSource randomSource) {
      RANDOM = randomSource;
   }

   public DyeMixGenerator() {
      this(RandomSource.create());
   }

   public record DyeMix(@Nullable Integer remainder, List<DyeColor> dyes, float error) {
      public static DyeMix of(DyeColor... colors) {
         return new DyeMix(null, Arrays.asList(colors), 0F);
      }
      public static DyeMix of(int rem, DyeColor... colors) {
         return new DyeMix(rem, Arrays.asList(colors), 0F);
      }

      public List<DyeItem> getDyeItems() {
         return dyes.stream().map(DyeItem::byColor).toList();
      }
      public List<ItemStack> getDyeStacks() {
         return dyes.stream().map(DyeItem::byColor).map(ItemStack::new).toList();
      }
   }

   public List<DyeColor> separateRemainderIntoDyes(int rem, int n) {
      return generateMix(rem, n).dyes();
   }

   /**
    * Splits a remainder color into two colors that would add up to it.<br>
    * Useful to generate independent colors for each elytra wing so they add up to a given color.
    */
   public int[] splitRemainder(int rem) {
      float rf = RANDOM.nextFloat();
      float gf = RANDOM.nextFloat();
      float bf = RANDOM.nextFloat();
      int r = (int) ((float) (rem >> 16 & 0xFF) * rf) & 0xFF;
      int g = (int) ((float) (rem >> 8 & 0xFF) * gf) & 0xFF;
      int b = (int) ((float) (rem & 0xFF) * bf) & 0xFF;
      return new int[] {r << 16 | g << 8 | b, 0xFF - r << 16 | 0xFF - g << 8 | 0xFF - b};
   }

   private final List<DyeColor> dyes = new ArrayList<>(9);
   private int targetAmount;
   private int target;
   private float error;
   private @Nullable Integer remainder;
   private int remainingAttempts;

   private float targetValue;
   private final float[] targetHueCoords = new float[3];
   private float currentValue;
   private final float[] currentHueCoords = new float[3];
   private final int[] currentSum = new int[3];


   /**
    * Performs a lazy search for a <i>hopefully</i> close dye mix resulting in a given color.<br>
    * May also generate a remainder color to be used in a recipe as the color of the dyed item.<br>
    * <br>
    * The strategy used is greedy search with a Gaussian temperature for choosing poorer choices
    * and random backtracking for a finite number of attempts.
    * The temperature lowers as fewer attempts remain, but the backtracking amount doesn't really
    * depend on any form of error criterion.<br>
    * <br>
    * It should work fine for simple mixes, and give approximate guesses for most, but it's only
    * meant to give reasonable examples in little time.<br>
    * <br>
    * If the search does not succeed, resorts to a greedy search to ensure it returns a mix of the
    * requested size ({@code dyeNum}).
    * If the requested size ({@code dyeNum}) is {@code -1}, an arbitrary length is returned.
    *
    * @param targetColor Target color to reach, in (A)RGB format (alpha channel is ignored).
    * @param dyeNum Requested number of dye items to find, or -1 (any negative value) to find
    *               an arbitrary amount.
    * @return A {@link DyeMix} containing the result list of dye items as well as a remainder color,
    *         which is impossible to guarantee won't be necessary.
    *         The lazy approach used can't either guarantee that the actual dyes found will exactly
    *         add up with the remainder to the requested targetColor.
    */
   public DyeMix generateMix(int targetColor, int dyeNum) {
      dyes.clear();
      targetAmount = dyeNum;
      target = targetColor;
      remainder = targetColor;
      remainingAttempts = 16;
      initSearch();
      if (searchMix()) return result();
      while (remainingAttempts > 0) {
         backtrackSome();
         if (searchMix()) return result();
      }
      return result();
   }

   private DyeMix result() {
      if (targetAmount > 0) {
         if (dyes.size() > targetAmount) {
            dyes.subList(targetAmount + 1, dyes.size()).clear();
         } else if (!dyes.isEmpty()) {
            int s = dyes.size();
            for (int i = 0; i < s && dyes.size() < targetAmount; i = (i + 1) % s)
               dyes.add(dyes.get(i));
         } else while (dyes.size() < targetAmount) dyes.add(nextRandomDyeByHueValueDistance());
      }
      updateState();
      calculateRemainder();
      return new DyeMix(remainder, new ArrayList<>(dyes), error);
   }

   private void initSearch() {
      int r = target >> 16 & 0xFF;
      int g = target >> 8 & 0xFF;
      int b = target & 0xFF;
      targetValue = max3(r, g, b) / 255F;
      targetHueCoords[0] = r / 255F / targetValue;
      targetHueCoords[1] = g / 255F / targetValue;
      targetHueCoords[2] = b / 255F / targetValue;
      Arrays.fill(currentSum, 0);
      error = 0F;
   }

   private boolean searchMix() {
      if (remainingAttempts <= 0) return false;
      if (dyes.size() < targetAmount) {
         DyeColor dye = nextRandomDyeByHueValueDistance();
         addDye(dye);
         updateState();
         calculateRemainder();
      }
      if (targetAmount > 0 && dyes.size() != targetAmount)
         return searchMix();
      float chance = getSatisfactionChance();
      return remainder == null && chance > 0.05F
         || error == 0 && chance > 0.4F
         || error < 0.4F && chance > 0.7F;
   }

   private void addDye(DyeColor color) {
      dyes.add(color);
      float[] c = color.getTextureDiffuseColors();
      int r = (int) (c[0] * 255F);
      int g = (int) (c[1] * 255F);
      int b = (int) (c[2] * 255F);
      currentSum[0] += r;
      currentSum[1] += g;
      currentSum[2] += b;
   }

   private void updateState() {
      currentHueCoords[0] = currentSum[0] / 255F / dyes.size();
      currentHueCoords[1] = currentSum[1] / 255F / dyes.size();
      currentHueCoords[2] = currentSum[2] / 255F / dyes.size();
      currentValue = max3(currentHueCoords[0], currentHueCoords[1], currentHueCoords[2]);
      currentHueCoords[0] /= currentValue;
      currentHueCoords[1] /= currentValue;
      currentHueCoords[2] /= currentValue;
   }

   private void calculateRemainder() {
      error = 0F;
      if (abs(currentValue - targetValue) < EPSILON
         && abs(currentHueCoords[0] - targetHueCoords[0]) + abs(currentHueCoords[1] - targetHueCoords[1])
            + abs(currentHueCoords[2] - targetHueCoords[2]) < 3 * EPSILON
      ) {
         remainder = null;
         return;
      }
      int s = dyes.size();
      int S = s + 1;
      float v = correctFloat(targetValue * S - currentValue * s);
      float rF = correctFloat(targetHueCoords[0] * S - currentHueCoords[0] * s);
      float gF = correctFloat(targetHueCoords[1] * S - currentHueCoords[1] * s);
      float bF = correctFloat(targetHueCoords[2] * S - currentHueCoords[2] * s);
      int c = (int) (rF * 255F * v) & 0xFF;
      c = c << 8 | (int) (gF * 255F * v) & 0xFF;
      c = c << 8 | (int) (bF * 255F * v) & 0xFF;
      remainder = c;
   }

   private float correctFloat(float f) {
      if (f < 0F) {
         error += -f;
         return 0F;
      } else if (f > 1F) {
         error += f - 1F;
         return 1F;
      } else return f;
   }

   private static int max3(int r, int g, int b) {
      return max(r, max(g, b));
   }

   private static float max3(float r, float g, float b) {
      return max(r, max(g, b));
   }

   private static float value(DyeColor color) {
      float[] t = color.getTextureDiffuseColors();
      return max3(t[0], t[1], t[2]);
   }

   private static float[] hueCoords(DyeColor color) {
      float[] c = color.getTextureDiffuseColors();
      float v = value(color);
      return new float[]{c[0] / v, c[1] / v, c[2] / v};
   }

   private static float hueDistance(DyeColor color, float[] tc) {
      float[] cc = hueCoords(color);
      return sqrt(square(cc[0] - tc[0]) + square(cc[1] - tc[1]) + square(cc[2] - tc[2]));
   }

   private static float lightDistance(DyeColor color, float value) {
      return Mth.abs(value(color) - value);
   }

   private List<DyeColor> dyesByHueValueDistance() {
      return Arrays.stream(DyeColor.values()).sorted(Comparator
         .<DyeColor, Float>comparing(c -> hueDistance(c, targetHueCoords))
         .thenComparing(c -> lightDistance(c, targetValue))).toList();
   }

   private int nextSortIndex() {
      return (int) (abs(RANDOM.nextGaussian()) * 1.2F * (4F / min(4, remainingAttempts)));
   }

   private DyeColor nextRandomDyeByHueValueDistance() {
      List<DyeColor> sorted = dyesByHueValueDistance();
      return sorted.get(nextSortIndex() % sorted.size());
   }

   private void backtrackSome() {
      float min = min(RANDOM.nextFloat(), RANDOM.nextFloat());
      int i = (int) (min * dyes.size()) % dyes.size();
      dyes.subList(i, dyes.size()).clear();
      remainingAttempts--;
      Arrays.fill(currentSum, 0);
      for (int j = 0; j < i; j++) {
         addDye(dyes.get(j));
         dyes.remove(dyes.size() - 1);
      }
      updateState();
   }

   private float getSatisfactionChance() {
      return RANDOM.nextFloat() / (1F + error * 0.8F);
   }
}

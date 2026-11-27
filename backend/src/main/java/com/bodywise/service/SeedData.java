package com.bodywise.service;

import com.bodywise.domain.FlashCard;
import com.bodywise.domain.Food;
import com.bodywise.domain.MealType;
import com.bodywise.repo.FlashCardRepo;
import com.bodywise.repo.FoodRepo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.bodywise.domain.MealType.*;

/**
 * Built-in Indian food catalog: approximate values for ONE unit, with alternate names used when
 * parsing typed meals. Built-in rows are refreshed from this list on every start.
 * Anything missing can be added as "my food" from the app.
 */
@Component
public class SeedData implements ApplicationRunner {

    private final FoodRepo foods;
    private final FlashCardRepo cards;
    private final CardService cardService;
    private final com.bodywise.repo.UserRepo users;
    private final com.bodywise.repo.DailyLogRepo logs;
    private final java.time.Clock clock;

    public SeedData(FoodRepo foods, FlashCardRepo cards, CardService cardService,
                    com.bodywise.repo.UserRepo users, com.bodywise.repo.DailyLogRepo logs, java.time.Clock clock) {
        this.foods = foods;
        this.cards = cards;
        this.cardService = cardService;
        this.users = users;
        this.logs = logs;
        this.clock = clock;
    }

    private record F(String name, String unit, double kcal, double protein, double carbs, double fat, double fiber, String aliases) {}

    private static F f(String name, String unit, double kcal, double p, double c, double fat, double fi, String aliases) {
        return new F(name, unit, kcal, p, c, fat, fi, aliases);
    }

    private static final List<F> CATALOG = List.of(
        //  name                          unit                     kcal   prot  carb   fat  fiber  aliases
        // --- morning drinks, seeds & nuts
        f("Coriander seed water",       "glass",                   1.4,  0.1,  0.2,  0.0,  0.1, "dhaniya water, dhania water, coriander water, dhaniya seed water"),
        f("Jeera water",                "glass",                   5,    0.1,  1.0,  0.1,  0.1, "cumin water, jeera pani"),
        f("Methi water",                "glass",                   4,    0.3,  0.6,  0.1,  0.3, "fenugreek water, methi seed water"),
        f("Ajwain water",               "glass",                   3,    0.1,  0.5,  0.1,  0.2, "carom water"),
        f("Saunf water",                "glass",                   4,    0.1,  0.8,  0.1,  0.4, "fennel water, fennel tea, saunf tea"),
        f("Warm lemon water",           "glass",                  10,    0.1,  3.0,  0.0,  0.1, "lemon water, nimbu pani, nimbu water"),
        f("Warm water",                 "glass",                   0,    0,    0,    0,    0,   "water, hot water"),
        f("Chia seed water",            "glass (1 tbsp seeds)",   58,    2.0,  5.0,  3.7,  4.1, "chia water, chia seeds water"),
        f("Sabja water",                "glass (1 tsp seeds)",    20,    0.8,  1.5,  1.2,  1.5, "basil seed water, sabja seeds water, tukmaria water"),
        f("Soaked almonds",             "piece",                   8,    0.3,  0.3,  0.6,  0.15,"almond, almonds, badam, soaked badam"),
        f("Soaked walnuts",             "piece",                  28,    0.6,  0.6,  2.6,  0.3, "walnut, walnuts, akhrot, soaked walnut"),
        f("Soaked raisins",             "10 pieces",              30,    0.3,  8.0,  0.0,  0.4, "raisins, kishmish, munakka"),
        f("Cashews",                    "piece",                   9,    0.3,  0.5,  0.7,  0.05,"cashew, kaju"),
        f("Pistachios",                 "10 pieces",              40,    1.5,  2.0,  3.2,  0.8, "pista"),
        f("Pumpkin seeds",              "tbsp (9 g)",             50,    2.7,  1.0,  4.3,  0.5, "pumpkin seed"),
        f("Sunflower seeds",            "tbsp (9 g)",             52,    1.9,  1.8,  4.6,  0.8, "sunflower seed"),
        f("Chia seeds",                 "tbsp (12 g)",            58,    2.0,  5.0,  3.7,  4.1, "chia"),
        f("Flaxseeds",                  "tbsp (10 g)",            53,    1.8,  2.9,  4.2,  2.7, "alsi, flax seeds, flaxseed"),
        // --- tea, coffee, milk
        f("Green tea",                  "cup",                     2,    0,    0,    0,    0,   ""),
        f("Spearmint tea",              "cup",                     2,    0,    0.4,  0,    0,   "mint tea, pudina tea, spearmint"),
        f("Chamomile tea",              "cup",                     2,    0,    0.4,  0,    0,   "chamomile"),
        f("Herbal tea",                 "cup",                     2,    0,    0.4,  0,    0,   "lemongrass tea, ginger tea, kadha"),
        f("Black tea",                  "cup",                     2,    0,    0.5,  0,    0,   "black chai"),
        f("Black coffee",               "cup",                     5,    0.3,  0,    0,    0,   "americano"),
        f("Tea with milk",              "cup",                    70,    2.0,  9.0,  2.5,  0,   "tea, milk tea, masala tea, adrak tea"),
        f("Coffee with milk",           "cup",                    80,    3.0,  9.0,  3.0,  0,   "coffee, filter coffee"),
        f("Toned milk",                 "glass (200 ml)",        120,    6.4,  9.4,  6.0,  0,   "milk"),
        f("Skimmed milk",               "glass (200 ml)",         70,    7.0, 10.0,  0.2,  0,   "skim milk, double toned milk"),
        f("Full cream milk",            "glass (200 ml)",        150,    6.4,  9.6,  9.6,  0,   "whole milk"),
        f("Haldi milk",                 "cup (200 ml)",          125,    6.4, 10.0,  6.0,  0.3, "turmeric milk, haldi doodh, golden milk"),
        f("Buttermilk",                 "glass",                  40,    2.5,  3.5,  1.5,  0,   "chaas, chhach, masala chaas"),
        f("Sweet lassi",                "glass",                 160,    5.0, 25.0,  4.5,  0,   "lassi"),
        f("Coconut water",              "glass",                  45,    1.7,  9.0,  0.5,  2.6, "nariyal pani, tender coconut"),
        f("Whey protein",               "scoop (30 g)",          120,   24.0,  3.0,  1.5,  0,   "whey, protein shake, protein powder"),
        // --- breakfast
        f("Paneer masala oats",         "bowl (200 g)",          280,   15.0, 30.0, 10.0,  4.5, "paneer oats, masala oats with paneer"),
        f("Masala oats",                "bowl (200 g)",          200,    6.0, 32.0,  5.0,  4.5, "vegetable oats, savoury oats"),
        f("Oats porridge",              "bowl (250 g)",          250,   11.0, 38.0,  6.0,  4.0, "oats with milk, oatmeal, porridge"),
        f("Oats (dry)",                 "40 g",                  150,    5.3, 27.0,  2.6,  4.0, "rolled oats, oats"),
        f("Overnight oats",             "jar (250 g)",           290,   12.0, 42.0,  8.0,  6.0, ""),
        f("Muesli",                     "40 g",                  150,    4.0, 27.0,  3.0,  4.0, "granola"),
        f("Daliya",                     "bowl (200 g)",          180,    6.0, 33.0,  2.5,  5.0, "dalia, broken wheat, lapsi"),
        f("Mixed sprouts",              "bowl (100 g)",          110,    8.0, 18.0,  1.0,  5.0, "sprouts, mix sprouts, sprout salad, sprouts chaat, mixed sprout"),
        f("Moong sprouts",              "100 g",                  30,    3.0,  6.0,  0.2,  1.8, "green moong sprouts"),
        f("Moong dal chilla",           "piece",                 120,    7.0, 15.0,  3.5,  3.0, "moong chilla, moong cheela, chilla"),
        f("Besan chilla",               "piece",                 130,    6.0, 15.0,  5.0,  3.0, "besan cheela, gram flour chilla"),
        f("Oats chilla",                "piece",                 110,    5.0, 14.0,  4.0,  2.5, "oats cheela"),
        f("Poha",                       "bowl (150 g)",          250,    5.0, 45.0,  6.0,  2.0, "kanda poha, aval"),
        f("Upma",                       "bowl (150 g)",          220,    5.0, 32.0,  8.0,  3.0, "rava upma, suji upma, vegetable upma"),
        f("Idli",                       "piece",                  58,    2.0, 12.0,  0.2,  0.6, "idly"),
        f("Plain dosa",                 "piece",                 133,    3.0, 22.0,  3.7,  1.0, "dosa, sada dosa"),
        f("Masala dosa",                "piece",                 300,    6.0, 40.0, 12.0,  3.0, ""),
        f("Ragi dosa",                  "piece",                 120,    3.0, 22.0,  2.5,  3.0, "nachni dosa"),
        f("Uttapam",                    "piece",                 180,    5.0, 30.0,  4.0,  2.5, "uthappam"),
        f("Sambar",                     "bowl (150 g)",          130,    6.0, 18.0,  4.0,  5.0, "sambhar"),
        f("Coconut chutney",            "tbsp (15 g)",            35,    0.4,  1.5,  3.0,  0.8, "nariyal chutney"),
        f("Dhokla",                     "piece (30 g)",           50,    2.0,  8.0,  1.2,  0.6, "khaman, khaman dhokla"),
        f("Methi thepla",               "piece",                 120,    3.0, 16.0,  5.0,  2.0, "thepla"),
        f("Aloo paratha",               "piece",                 250,    5.0, 35.0, 10.0,  3.5, "potato paratha"),
        f("Paneer paratha",             "piece",                 280,   11.0, 32.0, 12.0,  3.5, ""),
        f("Plain paratha",              "piece",                 180,    4.0, 25.0,  7.0,  3.0, "paratha, lachha paratha"),
        f("Veg sandwich",               "piece",                 180,    7.0, 28.0,  4.0,  4.0, "vegetable sandwich, sandwich"),
        f("Paneer sandwich",            "piece",                 260,   13.0, 28.0, 10.0,  4.0, ""),
        f("Multigrain bread",           "slice",                  70,    3.5, 12.0,  1.0,  2.0, "bread, brown bread, whole wheat bread"),
        f("Peanut butter",              "tbsp (16 g)",            95,    4.0,  3.0,  8.0,  1.0, ""),
        f("Boiled egg",                 "piece",                  78,    6.3,  0.6,  5.3,  0,   "egg, eggs, anda"),
        f("Egg white",                  "piece",                  17,    3.6,  0.2,  0.1,  0,   "egg whites"),
        f("Omelette",                   "2 eggs",                190,   13.0,  1.5, 15.0,  0.3, "omelet, masala omelette"),
        f("Egg bhurji",                 "2 eggs",                210,   13.0,  4.0, 16.0,  0.8, "anda bhurji, scrambled eggs"),
        // --- curd & dairy
        f("Low fat curd",               "100 g",                  60,    3.5,  4.7,  3.0,  0,   "curd, low fat dahi, yogurt, plain curd"),
        f("Greek yogurt",               "100 g",                  60,   10.0,  3.6,  0.4,  0,   "hung curd"),
        f("Raita",                      "bowl (100 g)",           70,    3.5,  6.0,  3.5,  0.6, "cucumber raita, boondi raita, lauki raita"),
        f("Paneer",                     "100 g",                 265,   18.0,  1.2, 20.8,  0,   "cottage cheese"),
        f("Low fat paneer",             "100 g",                 160,   20.0,  3.0,  8.0,  0,   "low fat cottage cheese"),
        f("Tofu",                       "100 g",                  76,    8.0,  1.9,  4.8,  0.3, "soya paneer"),
        // --- fruit
        f("Seasonal fruit",             "serving (150 g)",        75,    1.0, 18.0,  0.3,  3.0, "any seasonal fruit, fruit, fruits, fruit bowl, mixed fruit"),
        f("Apple",                      "medium",                 95,    0.5, 25.0,  0.3,  4.4, "seb"),
        f("Banana",                     "medium",                105,    1.3, 27.0,  0.4,  3.1, "kela"),
        f("Orange",                     "medium",                 62,    1.2, 15.4,  0.2,  3.1, "santra, kinnow"),
        f("Guava",                      "medium",                 37,    1.4,  8.0,  0.5,  3.0, "amrood"),
        f("Papaya",                     "cup (150 g)",            65,    0.7, 16.0,  0.4,  2.5, "papita"),
        f("Pomegranate",                "cup (90 g)",             75,    1.5, 17.0,  1.0,  3.6, "anar"),
        f("Watermelon",                 "cup (150 g)",            45,    0.9, 11.0,  0.2,  0.6, "tarbooz"),
        f("Muskmelon",                  "cup (150 g)",            55,    1.3, 13.0,  0.3,  1.4, "kharbooja, cantaloupe"),
        f("Pear",                       "medium",                100,    0.6, 27.0,  0.2,  5.5, "nashpati"),
        f("Mango",                      "cup (150 g)",           100,    1.4, 25.0,  0.6,  2.6, "aam"),
        f("Grapes",                     "cup (150 g)",           104,    1.0, 27.0,  0.2,  1.4, "angoor"),
        f("Kiwi",                       "piece",                  42,    0.8, 10.0,  0.4,  2.1, ""),
        // --- rotis & rice
        f("Whole wheat roti",           "piece",                 100,    3.0, 18.0,  2.0,  2.5, "roti, chapati, phulka, wheat roti, atta roti"),
        f("Jowar roti",                 "piece",                 110,    3.5, 22.0,  1.0,  3.0, "jowar bhakri, jowar rotla, sorghum roti"),
        f("Bajra roti",                 "piece",                 120,    3.5, 22.0,  2.0,  3.0, "bajra bhakri, bajra rotla, millet roti"),
        f("Ragi roti",                  "piece",                 100,    2.5, 20.0,  1.0,  3.0, "nachni roti, ragi bhakri"),
        f("Multigrain roti",            "piece",                 105,    3.5, 19.0,  2.0,  3.5, "multigrain chapati"),
        f("Missi roti",                 "piece",                 130,    5.0, 20.0,  3.5,  3.5, "besan roti"),
        f("White rice",                 "cup (150 g)",           195,    4.0, 42.0,  0.4,  0.6, "rice, steamed rice, chawal"),
        f("Brown rice",                 "cup (150 g)",           165,    3.8, 34.0,  1.3,  2.7, ""),
        f("Jeera rice",                 "cup (150 g)",           220,    4.0, 40.0,  5.0,  1.0, ""),
        f("Veg pulao",                  "cup (150 g)",           230,    5.0, 38.0,  6.0,  2.5, "pulao, vegetable pulao"),
        f("Curd rice",                  "cup (150 g)",           220,    6.0, 32.0,  7.0,  1.0, "dahi chawal"),
        f("Veg biryani",                "cup (150 g)",           290,    6.0, 42.0, 10.0,  3.0, "biryani"),
        f("Khichdi",                    "bowl (200 g)",          240,    9.0, 40.0,  5.0,  4.0, "moong dal khichdi"),
        f("Quinoa",                     "cup (150 g)",           180,    6.6, 32.0,  2.9,  4.2, "cooked quinoa"),
        // --- dals & curries
        f("Dal",                        "bowl (150 g)",          150,    9.0, 22.0,  3.0,  4.0, "dal tadka, yellow dal, toor dal, arhar dal, masoor dal, moong dal"),
        f("Dal makhani",                "bowl (150 g)",          280,   10.0, 25.0, 15.0,  6.0, ""),
        f("Rajma",                      "bowl (150 g)",          200,   12.0, 30.0,  4.0,  9.0, "rajma curry, kidney beans"),
        f("Chole",                      "bowl (150 g)",          220,   11.0, 30.0,  6.0,  9.0, "chana masala, chickpea curry, chhole"),
        f("Kadhi",                      "bowl (150 g)",          150,    6.0, 12.0,  8.0,  1.0, "kadhi pakora"),
        f("Rasam",                      "bowl (150 g)",           60,    2.0, 10.0,  1.5,  1.5, ""),
        f("Soya chunk curry",           "bowl (150 g)",          180,   16.0, 12.0,  7.0,  5.0, "soya curry, soyabean curry, nutrela curry"),
        f("Soya chunks (dry)",          "30 g",                  104,   15.6,  9.9,  0.2,  3.9, "soya chunks, nutrela"),
        // --- sabzi
        f("Lauki sabzi",                "bowl (150 g)",           90,    2.0,  9.0,  5.0,  3.0, "lauki, bottle gourd sabzi, ghiya sabzi, doodhi sabzi"),
        f("Tori sabzi",                 "bowl (150 g)",           85,    2.0,  8.0,  5.0,  3.0, "turai sabzi, ridge gourd sabzi"),
        f("Bhindi sabzi",               "bowl (150 g)",          130,    3.0, 12.0,  8.0,  4.5, "bhindi, okra, bhindi masala"),
        f("Cabbage sabzi",              "bowl (150 g)",           90,    2.0, 10.0,  5.0,  3.0, "patta gobi sabzi"),
        f("Aloo gobi",                  "bowl (150 g)",          150,    3.5, 16.0,  8.0,  4.0, "gobi aloo"),
        f("Gobi sabzi",                 "bowl (150 g)",          110,    3.0, 10.0,  6.5,  3.5, "cauliflower sabzi"),
        f("Baingan bharta",             "bowl (150 g)",          120,    3.0, 12.0,  7.0,  5.0, "baingan, brinjal sabzi"),
        f("Palak sabzi",                "bowl (150 g)",           90,    4.0,  7.0,  5.0,  3.5, "palak, spinach sabzi"),
        f("Mixed veg sabzi",            "bowl (150 g)",          120,    3.0, 12.0,  7.0,  4.0, "mix veg, mixed vegetables, sabzi"),
        f("Palak paneer",               "bowl (150 g)",          250,   12.0,  8.0, 19.0,  3.0, ""),
        f("Matar paneer",               "bowl (150 g)",          260,   12.0, 15.0, 17.0,  4.0, "mutter paneer"),
        f("Paneer bhurji",              "100 g",                 220,   15.0,  6.0, 15.0,  1.0, ""),
        f("Paneer tikka",               "100 g",                 230,   18.0,  6.0, 15.0,  1.0, ""),
        f("Low fat paneer wrap",        "piece",                 300,   20.0, 32.0,  9.0,  5.0, "paneer wrap, paneer roll, paneer kathi roll"),
        f("Veg wrap",                   "piece",                 250,    7.0, 38.0,  7.0,  5.0, "wrap, veg roll"),
        // --- salads & sides
        f("Salad",                      "bowl (150 g)",           35,    1.5,  7.0,  0.3,  2.5, "green salad, kachumber, kachumber salad, veg salad"),
        f("Cucumber",                   "bowl (150 g)",           25,    1.0,  5.0,  0.2,  1.0, "kheera, cucumber salad"),
        f("Dhaniya chutney",            "tbsp (15 g)",             8,    0.4,  1.0,  0.3,  0.6, "coriander chutney, green chutney, hari chutney, pudina chutney, mint chutney"),
        f("Roasted papad",              "piece",                  35,    2.5,  6.0,  0.2,  1.4, "papad"),
        // --- non-veg
        f("Chicken breast",             "100 g",                 165,   31.0,  0,    3.6,  0,   "grilled chicken, boiled chicken, chicken"),
        f("Chicken curry",              "bowl (150 g)",          250,   25.0,  6.0, 14.0,  1.0, ""),
        f("Fish curry",                 "bowl (150 g)",          220,   22.0,  6.0, 12.0,  1.0, ""),
        f("Grilled fish",               "100 g",                 140,   24.0,  0,    4.5,  0,   "fish, fish tikka"),
        f("Egg curry",                  "2 eggs",                260,   14.0,  8.0, 19.0,  1.5, "anda curry"),
        // --- snacks
        f("Roasted chana",              "30 g",                  110,    6.5, 17.0,  1.8,  5.0, "bhuna chana, roasted gram, chana"),
        f("Roasted makhana",            "25 g",                   90,    2.4, 19.0,  0.3,  3.6, "makhana, fox nuts, phool makhana"),
        f("Puffed rice",                "20 g",                   80,    1.4, 18.0,  0.2,  0.4, "murmura, kurmura, bhel"),
        f("Roasted peanuts",            "30 g",                  170,    7.5,  5.0, 14.0,  2.5, "peanuts, moongphali"),
        f("Khakhra",                    "piece",                  60,    1.8,  9.0,  2.0,  1.5, ""),
        f("Boiled corn",                "cup",                   130,    4.5, 27.0,  1.8,  3.5, "corn, sweet corn, bhutta"),
        f("Digestive biscuit",          "piece",                  70,    1.0, 10.0,  3.0,  0.6, "biscuit, marie biscuit"),
        f("Dark chocolate",             "10 g",                   55,    0.8,  4.6,  4.0,  1.0, "chocolate"),
        // --- eating out
        f("Samosa",                     "piece",                 260,    4.0, 30.0, 14.0,  2.5, ""),
        f("Pakora",                     "100 g",                 300,    7.0, 25.0, 19.0,  3.0, "pakoda, bhajiya, bhaji"),
        f("Pav bhaji",                  "plate",                 400,   10.0, 55.0, 16.0,  6.0, ""),
        f("Chole bhature",              "plate",                 550,   14.0, 65.0, 26.0,  8.0, ""),
        f("Pizza slice",                "slice",                 285,   12.0, 36.0, 10.0,  2.5, "pizza"),
        f("Burger",                     "piece",                 350,   12.0, 40.0, 15.0,  3.0, "veg burger"),
        f("Gulab jamun",                "piece",                 150,    2.0, 22.0,  6.0,  0.3, ""),
        // --- fats
        f("Ghee",                       "tsp (5 g)",              45,    0,    0,    5.0,  0,   "desi ghee"),
        f("Oil",                        "tsp (5 ml)",             40,    0,    0,    4.5,  0,   "olive oil, cooking oil, mustard oil"),
        f("Butter",                     "tsp (5 g)",              36,    0,    0,    4.1,  0,   "makhan"),

        // --- vegetables, soups & salads
        f("Roasted vegetables",         "bowl (150 g)",           95,    3.0, 13.0,  4.0,  4.5, "roast vegetables, oven roasted vegetables, roasted veggies"),
        f("Steamed vegetables",         "bowl (150 g)",           55,    3.0, 10.0,  0.4,  4.5, "steam vegetables, steamed veggies"),
        f("Boiled vegetables",          "bowl (150 g)",           55,    3.0, 10.0,  0.4,  4.5, "boiled veggies"),
        f("Grilled vegetables",         "bowl (150 g)",           90,    3.0, 12.0,  4.0,  4.5, "grilled veggies, bbq vegetables"),
        f("Stir fried vegetables",      "bowl (150 g)",          110,    3.5, 12.0,  6.0,  4.0, "stir fry vegetables, stir fry, sauteed vegetables"),
        f("Steamed broccoli",           "cup (90 g)",             31,    2.5,  6.0,  0.3,  2.4, "broccoli"),
        f("Sauteed mushrooms",          "bowl (100 g)",           70,    3.5,  4.0,  4.5,  1.5, "mushroom, mushrooms, mushroom sabzi"),
        f("Sauteed spinach",            "bowl (100 g)",           60,    3.0,  4.0,  3.5,  2.5, "spinach, palak stir fry"),
        f("Carrot sticks",              "cup (100 g)",            41,    0.9, 10.0,  0.2,  2.8, "carrot, carrots, gajar"),
        f("Beetroot salad",             "bowl (100 g)",           50,    1.6, 10.0,  0.3,  2.8, "beetroot, beet, chukandar"),
        f("Corn salad",                 "bowl (150 g)",          150,    5.0, 28.0,  3.0,  4.0, "sweet corn salad, corn chaat"),
        f("Chickpea salad",             "bowl (150 g)",          200,   10.0, 28.0,  5.0,  8.0, "chana salad, kabuli chana salad"),
        f("Paneer salad",               "bowl (150 g)",          220,   15.0,  8.0, 14.0,  2.5, "paneer tikka salad"),
        f("Fruit chaat",                "bowl (150 g)",           90,    1.2, 22.0,  0.4,  3.2, "fruit salad"),
        f("Vegetable soup",             "bowl (250 ml)",          80,    3.0, 12.0,  2.0,  3.0, "veg soup, mixed vegetable soup, clear soup"),
        f("Tomato soup",                "bowl (250 ml)",          90,    2.0, 15.0,  2.5,  2.0, "tamatar soup"),
        f("Sweet corn soup",            "bowl (250 ml)",         130,    3.5, 22.0,  3.0,  2.0, "corn soup"),
        f("Moong dal soup",             "bowl (250 ml)",         110,    7.0, 16.0,  1.5,  3.0, "dal soup, lentil soup"),
        f("Chicken soup",               "bowl (250 ml)",         110,   12.0,  6.0,  4.0,  1.0, "chicken clear soup"),
        f("Hummus",                     "tbsp (15 g)",            25,    1.2,  2.1,  1.4,  0.9, ""),
        // --- more sabzi & curries
        f("Aloo sabzi",                 "bowl (150 g)",          180,    3.0, 25.0,  8.0,  3.0, "aloo, potato sabzi, aloo bhaji"),
        f("Jeera aloo",                 "bowl (150 g)",          190,    3.0, 26.0,  8.5,  3.0, ""),
        f("Aloo matar",                 "bowl (150 g)",          170,    5.0, 22.0,  7.0,  5.0, "aloo mutter, matar aloo"),
        f("Beans sabzi",                "bowl (150 g)",           95,    2.5, 10.0,  5.0,  4.0, "french beans sabzi, beans poriyal"),
        f("Karela sabzi",               "bowl (150 g)",          110,    2.5, 10.0,  7.0,  4.0, "bitter gourd sabzi, karela"),
        f("Tinda sabzi",                "bowl (150 g)",           85,    1.8,  8.0,  5.0,  2.5, "tinda"),
        f("Arbi sabzi",                 "bowl (150 g)",          180,    2.5, 26.0,  7.0,  5.0, "arbi, colocasia"),
        f("Capsicum sabzi",             "bowl (150 g)",          100,    2.0, 10.0,  6.0,  3.0, "shimla mirch sabzi, capsicum"),
        f("Methi sabzi",                "bowl (150 g)",           95,    4.0,  8.0,  5.5,  4.5, "methi, fenugreek sabzi"),
        f("Mushroom matar",             "bowl (150 g)",          150,    7.0, 14.0,  7.5,  5.0, ""),
        f("Kadai paneer",               "bowl (150 g)",          290,   14.0, 10.0, 22.0,  3.0, "kadhai paneer"),
        f("Paneer butter masala",       "bowl (150 g)",          350,   13.0, 12.0, 28.0,  2.0, "butter paneer, shahi paneer, paneer makhani"),
        f("Malai kofta",                "bowl (150 g)",          380,    9.0, 22.0, 28.0,  3.0, "kofta"),
        f("Dum aloo",                   "bowl (150 g)",          220,    3.5, 24.0, 12.0,  3.0, ""),
        f("Chana dal",                  "bowl (150 g)",          180,   10.0, 26.0,  4.0,  6.0, "chana daal, bengal gram dal"),
        f("Moong dal",                  "bowl (150 g)",          140,    9.0, 20.0,  2.5,  4.0, "yellow moong dal, moong daal"),
        f("Masoor dal",                 "bowl (150 g)",          150,   10.0, 21.0,  3.0,  5.0, "red lentil dal"),
        f("Urad dal",                   "bowl (150 g)",          190,   11.0, 24.0,  6.0,  5.0, "kali dal, maa ki dal"),
        f("Avial",                      "bowl (150 g)",          160,    3.0, 12.0, 11.0,  4.0, "aviyal"),
        f("Thoran",                     "bowl (100 g)",          110,    2.5,  8.0,  7.5,  3.5, "poriyal, cabbage thoran"),
        f("Kootu",                      "bowl (150 g)",          150,    6.0, 18.0,  6.0,  5.0, ""),
        f("Sprouts curry",              "bowl (150 g)",          160,   10.0, 20.0,  4.5,  6.0, "usal, misal (no farsan)"),
        // --- more breakfast & south indian
        f("Pongal",                     "bowl (150 g)",          250,    7.0, 35.0,  9.0,  2.5, "ven pongal, khara pongal"),
        f("Appam",                      "piece",                 120,    2.0, 22.0,  2.5,  1.0, ""),
        f("Medu vada",                  "piece",                 135,    4.5, 14.0,  7.0,  2.0, "vada, urad vada"),
        f("Pesarattu",                  "piece",                 140,    7.0, 20.0,  3.5,  3.5, "moong dosa, green gram dosa"),
        f("Rava idli",                  "piece",                  80,    2.5, 12.0,  2.5,  0.8, "suji idli"),
        f("Sabudana khichdi",           "bowl (150 g)",          300,    3.5, 50.0, 10.0,  1.5, "sago khichdi"),
        f("Sabudana vada",              "piece",                 130,    1.5, 18.0,  6.0,  0.8, ""),
        f("Thalipeeth",                 "piece",                 170,    5.0, 24.0,  6.0,  4.0, ""),
        f("Gobi paratha",               "piece",                 230,    5.0, 32.0,  9.0,  4.0, "cauliflower paratha"),
        f("Methi paratha",              "piece",                 190,    5.0, 26.0,  7.5,  3.5, ""),
        f("Kuttu roti",                 "piece",                 110,    3.5, 20.0,  2.0,  3.0, "kuttu ki roti, buckwheat roti, kuttu chapati"),
        f("Kuttu puri",                 "piece",                 120,    2.5, 14.0,  6.0,  2.0, "kuttu ki puri, buckwheat puri"),
        f("Singhara roti",              "piece",                 110,    2.0, 21.0,  2.0,  2.5, "singhare ki roti, water chestnut roti"),
        f("Puri",                       "piece",                 100,    2.0, 12.0,  5.0,  1.0, "poori"),
        f("Avocado toast",              "slice",                 190,    5.0, 18.0, 11.0,  6.0, ""),
        f("Egg sandwich",               "piece",                 280,   15.0, 26.0, 12.0,  3.5, ""),
        f("Peanut butter toast",        "slice",                 165,    7.5, 15.0,  9.0,  3.0, "pb toast"),
        // --- breads & rice
        f("Naan",                       "piece",                 260,    8.0, 45.0,  5.0,  2.0, "plain naan"),
        f("Butter naan",                "piece",                 320,    8.0, 45.0, 11.0,  2.0, ""),
        f("Tandoori roti",              "piece",                 120,    4.0, 24.0,  1.0,  3.0, "tandoor roti"),
        f("Rumali roti",                "piece",                 120,    3.5, 22.0,  2.0,  1.0, ""),
        f("Kulcha",                     "piece",                 250,    7.0, 40.0,  7.0,  2.0, "amritsari kulcha"),
        f("Pav",                        "piece",                 120,    4.0, 22.0,  2.0,  1.0, "bun, ladi pav"),
        f("Lemon rice",                 "cup (150 g)",           230,    4.0, 40.0,  6.0,  1.5, "chitranna"),
        f("Fried rice",                 "cup (150 g)",           240,    5.0, 38.0,  7.5,  1.5, "veg fried rice"),
        f("Sambar rice",                "cup (200 g)",           260,    7.0, 45.0,  5.0,  4.0, "sambar sadam, bisi bele bath"),
        // --- non-veg
        f("Butter chicken",             "bowl (150 g)",          330,   24.0,  8.0, 22.0,  1.0, "murgh makhani"),
        f("Tandoori chicken",           "piece (leg)",           260,   28.0,  4.0, 14.0,  0.5, ""),
        f("Chicken tikka",              "100 g",                 180,   26.0,  4.0,  7.0,  0.5, ""),
        f("Chicken biryani",            "cup (200 g)",           360,   18.0, 45.0, 12.0,  2.0, ""),
        f("Mutton curry",               "bowl (150 g)",          320,   24.0,  6.0, 22.0,  1.0, "goat curry, rogan josh"),
        f("Keema",                      "bowl (150 g)",          300,   22.0,  8.0, 20.0,  2.0, "keema matar, mince"),
        f("Prawn curry",                "bowl (150 g)",          200,   20.0,  6.0, 10.0,  1.0, "shrimp curry, jhinga"),
        f("Fish fry",                   "piece (100 g)",         220,   20.0,  6.0, 13.0,  0.5, "fried fish"),
        // --- snacks & street food
        f("Plain popcorn",              "cup (8 g)",              31,    1.0,  6.2,  0.4,  1.2, "popcorn"),
        f("Trail mix",                  "30 g",                  150,    4.0, 13.0, 10.0,  2.0, "mixed nuts, dry fruit mix"),
        f("Protein bar",                "bar (60 g)",            220,   20.0, 22.0,  7.0,  5.0, ""),
        f("Granola bar",                "bar (35 g)",            140,    3.0, 22.0,  5.0,  2.0, ""),
        f("Rusk",                       "piece",                  40,    1.0,  7.0,  1.0,  0.3, "toast rusk"),
        f("Namkeen",                    "30 g",                  160,    4.0, 14.0, 10.0,  2.0, "bhujia, mixture, aloo bhujia"),
        f("Banana chips",               "30 g",                  155,    0.7, 17.0, 10.0,  2.0, ""),
        f("Kachori",                    "piece",                 280,    5.0, 30.0, 16.0,  2.5, ""),
        f("Vada pav",                   "piece",                 300,    6.0, 40.0, 13.0,  3.0, ""),
        f("Pani puri",                  "6 pieces",              180,    3.5, 28.0,  6.0,  3.0, "golgappa, puchka"),
        f("Bhel puri",                  "plate (150 g)",         250,    6.0, 40.0,  8.0,  4.0, "bhelpuri"),
        f("Sev puri",                   "plate (6 pcs)",         300,    5.0, 35.0, 16.0,  3.0, ""),
        f("Misal pav",                  "plate",                 480,   16.0, 60.0, 19.0, 10.0, ""),
        f("Khandvi",                    "piece (25 g)",           40,    1.8,  4.0,  1.8,  0.5, ""),
        f("Momos (veg, steamed)",       "piece",                  40,    1.0,  6.0,  1.2,  0.5, "momos, momo, dumplings"),
        f("Maggi noodles",              "pack (70 g)",           310,    7.0, 44.0, 12.0,  2.0, "maggi, instant noodles"),
        f("French fries",               "medium (115 g)",        365,    4.0, 48.0, 17.0,  4.0, "fries"),
        // --- sweets
        f("Kheer",                      "bowl (150 g)",          230,    6.0, 34.0,  8.0,  0.3, "rice kheer, payasam"),
        f("Suji halwa",                 "bowl (100 g)",          320,    4.0, 45.0, 14.0,  1.0, "sheera, halwa"),
        f("Gajar halwa",                "bowl (100 g)",          280,    5.0, 36.0, 13.0,  2.5, "carrot halwa"),
        f("Rasgulla",                   "piece",                 125,    3.0, 26.0,  1.0,  0,   "rasagola"),
        f("Besan ladoo",                "piece",                 180,    3.5, 20.0, 10.0,  1.0, "ladoo, laddu"),
        f("Jalebi",                     "piece (30 g)",          150,    0.8, 25.0,  5.5,  0.2, ""),
        f("Kaju barfi",                 "piece",                  70,    1.5,  8.0,  3.5,  0.2, "barfi, kaju katli"),
        f("Shrikhand",                  "bowl (100 g)",          250,    6.0, 38.0,  8.0,  0,   ""),
        f("Ice cream",                  "scoop (65 g)",          140,    2.5, 16.0,  7.0,  0.5, "icecream"),
        f("Honey",                      "tsp (7 g)",              21,    0,    5.8,  0,    0,   "shahad"),
        f("Jaggery",                    "piece (10 g)",           38,    0,    9.8,  0,    0,   "gud, gur"),
        f("Sugar",                      "tsp (4 g)",              16,    0,    4.0,  0,    0,   "cheeni"),
        // --- drinks
        f("Banana smoothie",            "glass (300 ml)",        240,    9.0, 40.0,  5.0,  3.0, "banana shake, smoothie"),
        f("Cold coffee",                "glass (300 ml)",        190,    7.0, 26.0,  6.5,  0,   ""),
        f("Shikanji",                   "glass",                  60,    0,   15.0,  0,    0.2, "lemonade, nimbu shikanji"),
        f("Orange juice",               "glass (250 ml)",        110,    1.7, 26.0,  0.5,  0.5, "juice"),
        f("ABC juice",                  "glass (250 ml)",        120,    2.0, 27.0,  0.5,  2.0, "apple beetroot carrot juice"),
        f("Sugarcane juice",            "glass (250 ml)",        180,    0,   45.0,  0,    0,   "ganne ka ras"),
        f("Jaljeera",                   "glass",                  25,    0.2,  6.0,  0,    0.3, ""),
        f("Aam panna",                  "glass",                  90,    0.3, 22.0,  0.2,  0.5, ""),
        f("Sattu drink",                "glass (2 tbsp sattu)",  120,    7.0, 18.0,  2.0,  4.0, "sattu"),
        f("Soy milk",                   "glass (200 ml)",         90,    6.5,  8.0,  3.5,  1.0, ""),
        f("Almond milk",                "glass (200 ml)",         35,    1.0,  1.5,  2.5,  0.5, ""),
        f("Cola",                       "can (330 ml)",          140,    0,   35.0,  0,    0,   "coke, pepsi, soda, soft drink"),
        // --- fruit & dry fruit
        f("Pineapple",                  "cup (150 g)",            75,    0.8, 20.0,  0.2,  2.1, "ananas"),
        f("Strawberries",               "cup (150 g)",            48,    1.0, 11.5,  0.5,  3.0, "strawberry"),
        f("Chikoo",                     "medium",                 95,    0.5, 23.0,  1.2,  6.0, "sapota, chiku"),
        f("Custard apple",              "medium",                120,    2.5, 30.0,  0.4,  5.5, "sitaphal, sharifa"),
        f("Jamun",                      "cup (100 g)",            60,    0.7, 14.0,  0.2,  0.6, ""),
        f("Lychee",                     "10 pieces",              66,    0.8, 16.5,  0.4,  1.3, "litchi"),
        f("Plum",                       "medium",                 30,    0.5,  7.5,  0.2,  0.9, "aloo bukhara"),
        f("Dates",                      "piece",                  23,    0.2,  6.0,  0,    0.6, "khajur"),
        f("Dried figs",                 "piece",                  21,    0.3,  5.0,  0.1,  0.8, "anjeer, figs"),
        f("Dried apricots",             "piece",                  17,    0.2,  4.0,  0,    0.5, "khubani, apricot"),
        // --- protein extras
        f("Boiled chickpeas",           "bowl (100 g)",          165,    9.0, 27.0,  2.6,  7.6, "kabuli chana, chickpeas, chole boiled"),
        f("Boiled rajma",               "bowl (100 g)",          127,    8.7, 23.0,  0.5,  6.4, "kidney beans boiled"),
        f("Cheese slice",               "slice (20 g)",           60,    3.5,  1.0,  4.6,  0,   "cheese"),
        f("Mozzarella",                 "30 g",                   85,    6.3,  0.7,  6.3,  0,   ""),
        // --- condiments
        f("Pickle",                     "tsp (10 g)",             25,    0.1,  0.5,  2.5,  0.2, "achaar, achar"),
        f("Tomato ketchup",             "tbsp (15 g)",            17,    0.2,  4.0,  0,    0.1, "ketchup, sauce"),
        f("Mayonnaise",                 "tbsp (15 g)",            95,    0.1,  0.1, 10.0,  0,   "mayo"),
        f("Peanut chutney",             "tbsp (15 g)",            60,    2.5,  2.0,  5.0,  0.8, "groundnut chutney, shengdana chutney"),
        f("Imli chutney",               "tbsp (15 g)",            40,    0.1, 10.0,  0,    0.3, "tamarind chutney, saunth")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Drop built-in foods that are no longer in the catalog (diary entries keep their own copy of the numbers).
        java.util.Set<String> names = new java.util.HashSet<>();
        for (F x : CATALOG) names.add(x.name().toLowerCase());
        foods.findAll().stream()
                .filter(f -> f.userId == null && !names.contains(f.name.toLowerCase()))
                .forEach(foods::delete);
        for (F x : CATALOG) {
            Food f = foods.findFirstByUserIdIsNullAndNameIgnoreCase(x.name()).orElseGet(Food::new);
            f.name = x.name();
            f.unit = x.unit();
            f.kcal = x.kcal();
            f.protein = x.protein();
            f.carbs = x.carbs();
            f.fat = x.fat();
            f.fiber = x.fiber();
            f.aliases = x.aliases();
            f.ingredients = Ingredients.forFood(x.name());
            foods.save(f);
        }

        // Re-read every card's text so improvements to the food list and matching apply to old cards too.
        for (FlashCard c : cards.findAll()) {
            cardService.reparse(c);
            cards.save(c);
        }

        // One-time upgrade for accounts made before whole-day cards existed (createdOn was added with them):
        // set their start date and give them the sample day card if they have none.
        for (com.bodywise.domain.User u : users.findAll()) {
            if (u.createdOn != null) continue;
            u.createdOn = logs.findFirstByUserIdOrderByDateAsc(u.id).map(l -> l.date)
                    .orElse(java.time.LocalDate.now(clock));
            users.save(u);
            if (cards.findByUserIdOrderByTitleAsc(u.id).isEmpty()) seedCardsFor(u.id);
        }
    }

    /** A sample whole-day card for new accounts, so the daily plan works right away. */
    @Transactional
    public void seedCardsFor(Long userId) {
        Map<MealType, String> meals = new EnumMap<>(MealType.class);
        meals.put(EARLY_MORNING, "1 glass coriander seed water, 5 soaked almonds, 1 soaked walnut");
        meals.put(BREAKFAST, "1 bowl paneer masala oats, 1 bowl mixed sprouts");
        meals.put(MID_MORNING, "1 cup spearmint tea, 1 seasonal fruit");
        meals.put(LUNCH, "1 bowl lauki sabzi, 2 jowar roti, 100 g low fat curd, 1 bowl salad");
        meals.put(TEA_TIME, "30 g roasted chana, 1 glass chia seed water");
        meals.put(DINNER, "1 low fat paneer wrap, 1 tbsp dhaniya chutney");
        meals.put(POST_DINNER, "1 cup chamomile tea");
        FlashCard c = new FlashCard();
        c.userId = userId;
        c.title = "Sample day (edit me)";
        c.notes = "Type each meal as plain text; calories are worked out automatically.";
        cardService.setMeals(c, meals);
        cards.save(c);
    }
}

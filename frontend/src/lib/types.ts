export type MealType =
  | "EARLY_MORNING"
  | "BREAKFAST"
  | "MID_MORNING"
  | "LUNCH"
  | "TEA_TIME"
  | "DINNER"
  | "POST_DINNER";

/** sprite = pixel icon name (see components/pixelArt.ts); color = accent for that meal. */
export const MEALS: { type: MealType; label: string; time: string; sprite: "sun" | "bowl" | "apple" | "cup" | "moon" | "star"; color: string; swap?: Record<string, string> }[] = [
  { type: "EARLY_MORNING", label: "Early morning", time: "06:30", sprite: "sun", color: "#f5c542" },
  { type: "BREAKFAST", label: "Breakfast", time: "08:30", sprite: "bowl", color: "#ffa66b" },
  { type: "MID_MORNING", label: "Mid morning", time: "11:00", sprite: "apple", color: "#ff8fb8" },
  { type: "LUNCH", label: "Lunch", time: "13:30", sprite: "bowl", color: "#5fe0b4", swap: { y: "#6fdc8c", c: "#ff6b81" } },
  { type: "TEA_TIME", label: "Tea time", time: "17:00", sprite: "cup", color: "#ffa66b" },
  { type: "DINNER", label: "Dinner", time: "20:00", sprite: "moon", color: "#b8a1ff" },
  { type: "POST_DINNER", label: "Post dinner", time: "22:00", sprite: "star", color: "#79c4ff" },
];

export const mealLabel = (t: MealType) => MEALS.find((m) => m.type === t)?.label ?? t;

export interface Macros {
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
}

export interface Profile {
  id: number;
  email: string;
  name: string | null;
  birthday: string | null;
  age: number | null;
  heightCm: number | null;
  sex: "FEMALE" | "MALE";
  goalWeightKg: number | null;
  activityLevel: "SEDENTARY" | "LIGHT" | "MODERATE" | "ACTIVE";
  deficitKcal: number;
  gymMet: number;
  waterTargetL: number;
  stepsTarget: number;
  reminderEmail: string | null;
  remindersEnabled: boolean;
  currentWeightKg: number | null;
}

/** Macros are per ONE unit. */
export interface Item extends Macros {
  id: number | null;
  foodId: number | null;
  name: string;
  unit: string;
  quantity: number;
  fromPlan: boolean;
  matched: boolean;
  raw: string | null;
  createdAt: string | null;
}

export interface ItemRequest {
  foodId?: number | null;
  name?: string;
  unit?: string;
  quantity: number;
  kcal?: number;
  protein?: number;
  carbs?: number;
  fat?: number;
  fiber?: number;
}

/** One line of typed text after parsing. Macros per unit. */
export interface ParsedItem extends Macros {
  raw: string;
  query: string;
  matched: boolean;
  approximate: boolean;
  foodId: number | null;
  name: string;
  unit: string | null;
  quantity: number;
  note: string | null;
}

export interface CardMeal {
  mealType: MealType;
  text: string;
  items: Item[];
  totals: Macros;
}

export interface Card {
  id: number;
  title: string;
  notes: string | null;
  meals: CardMeal[];
  totals: Macros;
  unmatched: number;
}

export interface Targets {
  calories: number;
  maintenance: number;
  bmr: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  fiberG: number;
  mode: "LOSE" | "MAINTAIN" | "GAIN";
  profileComplete: boolean;
  // inputs actually used
  weightKg: number;
  weightSource: "LOGGED" | "GOAL" | "DEFAULT";
  weightDate: string | null;
  heightCm: number;
  heightIsDefault: boolean;
  age: number;
  ageIsDefault: boolean;
  sex: "FEMALE" | "MALE";
  activityLevel: string;
  activityFactor: number;
  goalWeightKg: number | null;
  deficitChosen: number;
  deficitActual: number;
  floorApplied: boolean;
  floorKcal: number;
  weeklyChangeKg: number;
  weeksToGoal: number | null;
  proteinBasisKg: number;
}

export interface DayLog {
  weightKg: number | null;
  gymMinutes: number | null;
  waterLiters: number | null;
  steps: number | null;
  planFollowed: boolean | null;
  ateOut: boolean | null;
  noSugar: boolean | null;
  /** calories from the gym log; null when no exercises are logged */
  gymKcal: number | null;
}

export interface Meal {
  mealType: MealType;
  items: Item[];
  totals: Macros;
  plan: CardMeal | null;
}

export interface DaySummary {
  date: string;
  age: number | null;
  heightCm: number | null;
  currentWeightKg: number | null;
  log: DayLog;
  meals: Meal[];
  totals: Macros;
  targets: Targets;
  gymBurnKcal: number;
  netKcal: number;
  remainingKcal: number;
  proteinReached: boolean;
  fiberReached: boolean;
  caloriesWithin: boolean;
  plan: { id: number; title: string } | null;
  gymMet: number;
}

export interface WeightPoint {
  date: string;
  weightKg: number;
}

export interface WeekPoint {
  sunday: string;
  sundayWeight: number | null;
  avgWeight: number | null;
  change: number | null;
  daysLogged: number;
}

export interface HabitRow {
  date: string;
  waterLiters: number | null;
  steps: number | null;
  gymMinutes: number | null;
  waterOk: boolean;
  stepsOk: boolean;
  gymOk: boolean;
  planFollowed: boolean | null;
  ateOut: boolean;
  noSugar: boolean;
}

export interface HabitsResponse {
  waterTargetL: number;
  stepsTarget: number;
  startDate: string;
  rows: HabitRow[];
}

export interface GroceryIngredient {
  key: string;
  name: string;
  purchased: boolean;
}

/** One dish of the plan: only ingredients not listed under an earlier dish, plus the ones it shares. */
export interface GroceryDish {
  mealType: MealType;
  dish: string;
  amount: string | null;
  ingredients: GroceryIngredient[];
  alreadyListed: string[];
}

export interface GItem {
  id: number;
  name: string;
  quantity: string | null;
  purchased: boolean;
}

export interface GList {
  id: number;
  name: string;
  items: GItem[];
}

export interface GroceryView {
  forDate: string;
  planTitle: string | null;
  dishes: GroceryDish[];
  lists: GList[];
  toBuy: { source: string; name: string; quantity: string | null }[];
  mailConfigured: boolean;
  reminderEmail: string;
}

export interface Exercise {
  group: string;
  tip: string;
  name: string;
  kind: "STRENGTH" | "TIME";
  met: number;
}

export interface GymEntry {
  id: number;
  name: string;
  kind: "STRENGTH" | "TIME";
  sets: number | null;
  reps: number | null;
  weightKg: number | null;
  minutes: number;
  intensity: "LIGHT" | "MODERATE" | "HARD";
  met: number;
  kcal: number;
}

export interface GymDay {
  date: string;
  bodyWeightKg: number;
  weightSource: "LOGGED" | "GOAL" | "DEFAULT";
  entries: GymEntry[];
  totalMinutes: number;
  totalKcal: number;
}

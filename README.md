# Bodywise

A personal body tracker with a cute pixel-art, developer-console look: daily weight, meals with calories and macros,
whole-day flash-card meal plans, a gym log, habits, and a grocery list (built from tomorrow's plan) emailed at 7 pm.

- `backend/`: Spring Boot 4 (Java 21), REST API, H2 file database, JWT auth, scheduled email
- `frontend/`: Next.js 16 (App Router, Tailwind CSS 4)

No AI or third-party nutrition API is involved: typed meals are matched against a built-in food list with plain text
rules, and every number is calculated with the formulas below.

## Run it locally

Two terminals:

```bash
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run   → http://localhost:8080
```

```bash
cd frontend
npm install
npm run dev                     # → http://localhost:3000
```

Open http://localhost:3000, create an account, and fill in Profile (birthday, height, goal weight).
New accounts get a sample whole-day flash card you can edit or delete.

The frontend proxies `/api/*` to the backend (`BACKEND_URL`, default `http://localhost:8080`).
Data is stored in `backend/data/` (H2, git-ignored). Delete that folder to start fresh.

## Pages

| Page | What it does |
|---|---|
| **Today** | Height, age (from birthday), weight and the Sunday-to-Sunday change above a month calendar with little habit icons per day. Log weight, gym and "ate out". Calorie gauge (budget + exercise − food), macro bars with ticks for protein and fibre, and "how is this calculated" with your own numbers. Seven meal cards (early morning → post dinner): type what you ate in plain words and the calories are worked out, or "I ate exactly this" to copy the day's card. |
| **Flash cards** | Each card is a whole day: text for every meal slot ("5 soaked almonds, 1 glass coriander seed water"). Unknown foods are flagged; define them once and they're remembered. One card is picked at random as each day's plan. |
| **Gym** | Log exercises as sets × reps (with optional weight) or minutes for machines and classes, at light / moderate / hard intensity. Calories burned are estimated and added to that day's budget. |
| **Habits** | From the day you joined, newest day on top: water (3.45 L), gym, 10k steps, no sugar, and food (on plan / ate out). Click a tick to undo it. |
| **Grocery** | Tomorrow's card broken down meal → dish → ingredients. Each ingredient is listed once (under the first dish that needs it); later dishes just mention it. Plus your own lists and the combined "to be purchased" list the 7 pm email sends. |
| **Profile** | Birthday, height, sex, goal weight, how fast to lose (pace → deficit), activity, gym intensity, water/steps targets, reminder email. |

## How the numbers are worked out

- **BMR**: Mifflin-St Jeor (10·kg + 6.25·cm − 5·age, then −161 women / +5 men). **Maintenance** = BMR × activity factor (activity *outside* the gym).
- **Budget**: maintenance − deficit while you're above your goal, never below 1200 kcal (women) / 1500 kcal (men). You pick a pace: 0.25 / 0.5 / 0.75 / 1 kg a week = 275 / 550 / 825 / 1100 kcal a day (1 kg fat ≈ 7700 kcal).
- **Gym**: kcal = (MET − 1) × body weight kg × hours, using MET values from the Compendium of Physical Activities (× 0.8 light, × 1.2 hard). Sets × reps become time at about 4 s per rep + 90 s rest per set. The result is added to that day's budget.
- **Macros**: protein 1.6 g per kg of goal weight, fat 25% of calories, carbs the rest, fibre 14 g per 1000 kcal (at least 25 g).
- **Weekly change**: weeks run Monday to Sunday; the Sunday weigh-in (or the week's last one) is compared with the previous week.

Food values in the built-in catalog (~290 foods) are typical per-serving estimates for home-style Indian cooking, based on standard reference values (India's IFCT 2017 food tables from NIN, and USDA FoodData Central for raw ingredients), assuming about 1 tsp of oil per serving of sabzi or dal. Expect ±10–20% versus your kitchen. For packaged food, use the label: type the item, click "define", and enter the label values (and ingredients, for the grocery list) once.

## 7 pm grocery email

Set these before starting the backend (Gmail needs an [app password](https://myaccount.google.com/apppasswords)):

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=your-16-char-app-password
```

Without `MAIL_HOST` the email is only written to the backend log. "Send now" on the Grocery page triggers it on demand.

Other settings: `APP_TIMEZONE` (default `Asia/Kolkata`, decides when 7 pm is), `REMINDER_CRON` (default `0 0 19 * * *`),
`JWT_SECRET` (32+ chars, set it for anything beyond local use), `FRONTEND_URL`, `DB_URL` / `DB_USER` / `DB_PASSWORD`.

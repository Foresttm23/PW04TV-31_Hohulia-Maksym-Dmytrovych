# PW04ТВ-31_Гогуля Максим Дмитрович

Практичні роботи (Таски 1-16) з дисципліни "Мобільні системні розробки" (MSE).

## Інформація про студента
- **ПІБ:** Гогуля Максим Дмитрович
- **Група:** ТВ-31
- **Проєкт:** MSE (Гідроелектростанція з контролем рівня води)

## Опис завдань
- **Task 1 (Практична 1):** Прогноз та контроль рівня води ГЕС (`HydroForecastScreen`). Створення UI екранів з використанням Jetpack Compose (Row, Column).
- **Task 2 (Практична 2):** Калькулятор енергетичних показників ГЕС (`HydroConfigScreen`). Базовий стан з `remember` та `mutableStateOf`.
- **Task 3 (Практична 3):** Дашборд моніторингу та аналітики ГЕС (`HydroDashboardScreen`). Складні UI елементи та керування станами кнопок.
- **Task 4 (Практична 4):** Форма авторизації та реєстрації оператора ГЕС (`AuthRegistrationScreen`). Розділення UI на окремі Composable функції, бізнес логіка валідації.
- **Task 5 (Практична 5):** Навігаційний диспетчер шлюзів ГЕС (`HydroNavMenuScreen`). Реалізація багатовіконної навігації з використанням `NavController`, `NavHost` та передачею параметрів через `navArgument`.
- **Task 6 (Практична 6):** Форма контролю водного балансу ГЕС (`HydroWaterControlScreen`). Розробка складних форм введення даних, глибока валідація з візуальними підказками (`supportingText`) та `isError`.
- **Task 7 (Практична 7):** Контролер затворів ГЕС (`HydroSpillwayControlScreen`). Застосування архітектурного патерну **MVVM**, створення `ViewModel`, використання `StateFlow` для реактивного управління станом екрану.
- **Task 8 (Практична 8):** Диспетчерський центр з локальною БД (`HydroOperationsCenterScreen`). Робота з локальним сховищем **Room Database** (SQLite), збереження, читання (`Flow`) та видалення телеметричних показників.

- **Task 9 (Практична 9):** Журнал гідропостів каскаду (`ReservoirJournalScreen`). **Repository + Data Source**: Room, імітований SCADA-сервер і кеш у пам'яті (TTL), режими джерела (локальне / віддалене / агреговане), CRUD, пошук, фільтри, сортування, агрегація та тренд рівня, offline-first синхронізація.
- **Task 10 (Практична 10):** Прогноз притоку каскаду ГЕС (`CascadeForecastScreen`). **Retrofit + Gson**: Open-Meteo Flood API (GloFAS) і Weather API, вкладені JSON, паралельні запити з retry/backoff, офлайн-кеш у Room, розрахунок потужності та паводкового ризику, адаптивна сітка карток.
- **Task 11 (Практична 11):** Захищений доступ до SCADA (`SecureAccessScreen`). Mocked API автентифікації (OkHttp Interceptor), JWT-подібний токен з HMAC-підписом, DataStore, `AuthInterceptor`, термін дії, ролі Адміністратор / Черговий інженер / Спостерігач, обробка 401/403 і підробленого токена.
- **Task 12 (Практична 12):** Діагностика мережевих запитів (`NetworkErrorsScreen`). Перевірка мережі, централізований `ErrorHandler`, Snackbar/діалоги, retry для тимчасових помилок і `Retry-After`, окремі повідомлення для HTTP 400–503, журнал помилок у файл і Logcat.

- **Task 13 (Практична 13):** Потоки датчиків рівня води (`SensorStreamsScreen`). **Flow та Coroutines**: холодні/гарячі потоки (`shareIn`), `combine`, `zip`, `debounce`, `catch`, `retryWhen` з експоненційною затримкою, `callbackFlow`, імітація збою датчика.
- **Task 14 (Практична 14):** Список гідрооб'єктів і мапа (`HydroObjectListScreen`). **LazyColumn** з картками, екран деталей через Navigation, синхронізація вибору зі списком та мапою, стани завантаження / помилки / порожнього списку, адаптивна розкладка.
- **Task 15 (Практична 15):** Пошук і фільтри (`HydroObjectSearchScreen`). Пошук з `debounce`, чотирирівневі фільтри (тип, статус, потужність), сортування, множинний вибір із підсумком.
- **Task 16 (Практична 16):** GPS і мапа (`HydroMapScreen`). **Google Maps SDK** (`maps-compose`), маркери об'єктів, дозволи на геолокацію, `LocationManager` через `callbackFlow`, відстань до станцій (гаверсинус), найближчий об'єкт, сортування за відстанню.

Тестові користувачі Task 11 (mocked): `admin / Admin#2026`, `engineer / Engineer#2026`, `observer / Observer#2026`.

## Технологічний стек
- **Мова:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Навігація:** Jetpack Navigation Compose
- **Архітектура:** MVVM (Model-View-ViewModel) з `StateFlow`
- **Локальна База Даних:** Room Database (SQLite) + `kapt`
- **Асинхронність:** Kotlin Coroutines & Flow
- **Мережа:** Retrofit 2 + Gson, OkHttp (Interceptor, HttpLoggingInterceptor)
- **Сховище токена:** Jetpack DataStore Preferences
- **Мапи та геолокація:** Google Maps SDK + maps-compose, `LocationManager`
- **Тести:** JUnit 4, kotlinx-coroutines-test (`./gradlew testDebugUnitTest`)

## Ключ Google Maps
Для Task 16 потрібен власний ключ Maps SDK for Android. Додайте в `local.properties` (файл не потрапляє в git):

```
MAPS_API_KEY=ваш_ключ
```

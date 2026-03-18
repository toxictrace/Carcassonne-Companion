# 🏰 Carcassonne Companion

Нативное Android-приложение для отслеживания сессий Carcassonne.  
Написано на **Kotlin + Jetpack Compose**, база данных **Room**, сборка через **GitHub Actions**.

---

## 📱 Экраны

| Экран | Описание |
|---|---|
| Dashboard | Глобальная статистика + последние игры |
| Match History | Список всех партий с поиском |
| Match Details | Финальная таблица, разбивка очков |
| New Game | Выбор игроков, цвет мипла, расширения |
| Live Game | Подсчёт очков в реальном времени |
| Players | Список с W/L и win rate |
| Player Profile | Статистика, достижения, история |
| Stats | Глобальные данные + сравнение игроков |
| Settings | Backup/Restore/Clear |

---

## 🚀 Как получить APK (без ПК)

### 1. Создайте репозиторий на GitHub
- Откройте [github.com](https://github.com) на телефоне
- New repository → назовите `carcassonne-companion` → Create

### 2. Загрузите файлы

**Вариант A — через браузер (проще):**
- В репозитории нажмите **Add file → Upload files**
- Загружайте папки по одной (GitHub позволяет drag & drop)

**Вариант B — через Termux (рекомендуется):**
```bash
pkg install git
git config --global user.name "Ваше имя"
git config --global user.email "email@example.com"
git clone https://github.com/ВАШ_НИК/carcassonne-companion
# скопируйте файлы в папку
cd carcassonne-companion
git add .
git commit -m "Initial commit"
git push
```

### 3. GitHub Actions соберёт APK автоматически
- Перейдите в **Actions** → Build APK → дождитесь зелёной галочки (~5-10 мин)
- Нажмите на завершённый workflow → **Artifacts** → скачайте `carcassonne-debug-apk`

### 4. Установите APK
- Разрешите установку из неизвестных источников в настройках Android
- Откройте скачанный ZIP, извлеките `app-debug.apk`, установите

---

## 🏗️ Архитектура

```
app/
├── data/
│   ├── entity/        # Room Entity классы (Player, Game, GamePlayer)
│   ├── dao/           # Data Access Objects
│   ├── repository/    # CarcassonneRepository — единая точка доступа к данным
│   └── CarcassonneDatabase.kt
├── ui/
│   ├── theme/         # Цвета, тема (тёмная, зелёная)
│   ├── components/    # Переиспользуемые компоненты
│   └── screens/       # Все экраны
├── viewmodel/         # MainViewModel — логика + LiveGameState
└── MainActivity.kt    # Навигация (NavHost)
```

**Паттерн:** MVVM  
**База данных:** Room (SQLite)  
**UI:** Jetpack Compose  
**Навигация:** Navigation Compose  
**Зависимости:** через libs.versions.toml (Version Catalog)

---

## 🛠️ Локальная разработка (если появится ПК)

```bash
git clone https://github.com/ВАШ_НИК/carcassonne-companion
cd carcassonne-companion
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Или откройте в **Android Studio** и нажмите Run.

---

## 📦 Зависимости

- Kotlin 2.0
- Jetpack Compose BOM 2024.08
- Room 2.6.1
- Navigation Compose 2.7.7
- Material3
- KSP (Kotlin Symbol Processing)

---

## 🔜 Планы
- [ ] Виджет на рабочий стол

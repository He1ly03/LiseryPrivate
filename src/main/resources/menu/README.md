# LiseryPrivate - Формат меню

Плагин использует синтаксис **LiseryMenu** для настройки меню.

## Основные параметры

```yaml
title: "<black>Название меню"  # Поддержка MiniMessage
size: 27                        # Размер (9, 18, 27, 36, 45, 54)
hide_items: false               # Скрывать инвентарь игрока
```

## Формат предметов

```yaml
items:
  example_item:
    material: DIAMOND           # Материал
    slot: "13"                  # Слот (строка)
    # или slots: "0-8"          # Диапазон слотов
    # или slots: "0,1,2,9,10"   # Список слотов
    display_name: "<green>Название"
    lore:
      - "<gray>Описание"
      - "<yellow>Ещё строка"
    
    # Действия по клику
    left_click_actions:
      - "[open_menu] private"
    right_click_actions:
      - "[close]"
    middle_click_actions:
      - "[chunk_teleport]"
```

## Головы игроков

```yaml
player_head:
  material: PLAYER_HEAD
  slot: "13"
  skull_owner: "%player_name%"  # Имя игрока
  display_name: "<green>Моя голова"
```

## Base64 текстуры

```yaml
custom_head:
  material: PLAYER_HEAD
  slot: "13"
  base64: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvLi4uIn19fQ=="
  display_name: "<gold>Кастомная голова"
```

## Доступные действия

### Навигация
- `[open_menu] <имя>` - Открыть другое меню
- `[close]` - Закрыть меню
- `[next_page]` - Следующая страница
- `[prev_page]` - Предыдущая страница
- `[confirm]` - Запросить подтверждение

### Действия с чанками
- `[chunk_private]` - Заприватить текущий чанк
- `[chunk_unprivate]` - Расприватить чанк
- `[chunk_teleport]` - Телепортироваться к чанку

### Доверенные лица
- `[trust_remove]` - Убрать из доверенных

### Настройки чанка
- `[settings_build_on]` / `[settings_build_off]`
- `[settings_destroy_on]` / `[settings_destroy_off]`
- `[settings_use_on]` / `[settings_use_off]`
- `[settings_switch_on]` / `[settings_switch_off]`
- `[settings_mobs_on]` / `[settings_mobs_off]`
- `[settings_pvp_on]` / `[settings_pvp_off]`
- `[settings_fire_on]` / `[settings_fire_off]`
- `[settings_explosion_on]` / `[settings_explosion_off]`

### Аукцион
- `[auc_buy]` - Купить чанк
- `[auc_remove]` - Убрать с продажи
- `[auc_teleport]` - Телепортироваться к чанку

### Подтверждение
- `[accept]` - Подтвердить действие
- `[deny]` - Отменить действие

## Плейсхолдеры

- `%player%` / `%player_name%` - Имя игрока
- `%chunk%` - Название чанка
- `%owner%` - Владелец чанка
- `%pvp%` - Статус PvP
- `%price%` - Цена (для аукциона)
- `%build_toggle%`, `%destroy_toggle%`, etc. - Статусы настроек

## MiniMessage цвета

```yaml
# Основные цвета
display_name: "<green>Зелёный"
display_name: "<red>Красный"
display_name: "<gold>Золотой"
display_name: "<yellow>Жёлтый"
display_name: "<aqua>Голубой"
display_name: "<white>Белый"
display_name: "<gray>Серый"
display_name: "<black>Чёрный"

# HEX цвета
display_name: "<#ff7a1b>Оранжевый HEX"

# Градиенты
display_name: "<gradient:green:blue>Градиент"

# Форматирование
display_name: "<bold>Жирный"
display_name: "<italic>Курсив"
display_name: "<underlined>Подчёркнутый"
```

## Документация LiseryMenu

Полная документация: [github.com/He1ly03/LiseryMenu](https://github.com/He1ly03/LiseryMenu)

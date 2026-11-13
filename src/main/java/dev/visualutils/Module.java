package dev.visualutils;

/** Lista modulow widocznych w menu. Kazdy mozna wlaczyc i wylaczyc (menu albo wlasny klawisz). */
public enum Module {
    TRACERS(Category.SWIAT, "Tracery", "Linie do graczy"),
    ESP(Category.SWIAT, "ESP", "Ramki i paski HP wokół graczy"),
    NAMETAGS(Category.SWIAT, "Nametagi+", "Nick, HP i dystans (zastępują zwykłe nicki)"),
    SETINFO(Category.SWIAT, "Set gracza", "Materiał oraz Protection i Unbreaking"),
    MOBS(Category.SWIAT, "Pokazuj moby", "ESP i tracery także dla mobów"),
    ITEMESP(Category.SWIAT, "Item ESP", "Wyrzucone przedmioty na ziemi"),
    WAYPOINTS(Category.SWIAT, "Waypointy", "Znaczniki na ekranie + punkt śmierci"),
    TRAPDETECT(Category.SWIAT, "Wykrywacz pułapek", "Skupiska wagoników, TNT i spadających bloków"),

    FULLBRIGHT(Category.WIDOK, "Fullbright", "Pełna jasność w ciemności"),
    ZOOM(Category.WIDOK, "Zoom", "Przybliżenie (FOV w Opcjach)"),
    LOWHP(Category.WIDOK, "Alarm HP", "Czerwona poświata przy niskim zdrowiu"),
    RADAR(Category.WIDOK, "Radar", "Mapa graczy w pobliżu"),
    COMPASS(Category.WIDOK, "Kompas", "Pasek kierunków u góry ekranu"),
    AUTOSPRINT(Category.WIDOK, "Auto-sprint", "Biegniesz do przodu bez klawisza sprintu"),
    FOODWARN(Category.WIDOK, "Alarm głodu", "Ostrzeżenie, gdy jedzenie spada poniżej 6"),
    INVFULL(Category.WIDOK, "Pełny ekwipunek", "Ostrzeżenie, gdy nie ma wolnych miejsc"),
    TOOLWARN(Category.WIDOK, "Alarm narzędzia", "Ostrzeżenie, gdy przedmiot w ręku ma mało wytrzymałości"),

    INFO(Category.HUD, "Info", "Koordynaty, kierunek, prędkość, światło"),
    STATS(Category.HUD, "FPS i ping", "Licznik klatek i opóźnienie"),
    CPS(Category.HUD, "CPS", "Kliknięcia na sekundę (LPM / PPM)"),
    KEYSTROKES(Category.HUD, "Klawisze", "WASD, spacja i myszka na ekranie"),
    ARMOR(Category.HUD, "Pancerz", "Twój pancerz i wytrzymałość"),
    POTIONS(Category.HUD, "Efekty", "Aktywne efekty i czas"),
    CLOCK(Category.HUD, "Zegar", "Aktualna godzina"),

    FRIENDS(Category.PVP, "Friend", "Znajomi na zielono i nie da się ich uderzyć (klawisz G)"),
    TARGETHUD(Category.PVP, "Cel pod celownikiem", "HP, dystans i set patrzonej istoty"),
    NEARBY(Category.PVP, "Gracze w pobliżu", "Lista z dystansem i alert o nowych"),
    ITEMCOUNT(Category.PVP, "Zasoby", "Totemy, jabłka, perły i butelki XP"),
    ELYTRAALERT(Category.PVP, "Alert elytry", "Gracz leci na elytrze w pobliżu: dystans, kierunek, prędkość"),
    ANTYKOSTKA(Category.PVP, "Antykostka", "Zapis układu paska per serwer, jeden klawisz go przywraca"),
    ARMORWARN(Category.PVP, "Alarm pancerza", "Ostrzeżenie, gdy pancerz ma mało wytrzymałości"),
    HITINFO(Category.PVP, "Licznik trafień", "Ile razy trafiłeś / oberwałeś w tej walce");

    /** Zakladki w menu (OPCJE i WYGLAD to zakladki z ustawieniami, bez modulow). */
    public enum Category {
        SWIAT("Świat"),
        WIDOK("Widok"),
        HUD("HUD"),
        PVP("PVP"),
        OPCJE("Opcje"),
        WYGLAD("Wygląd");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public boolean isSettings() {
            return this == OPCJE || this == WYGLAD;
        }
    }

    private final Category category;
    private final String label;
    private final String description;

    Module(Category category, String label, String description) {
        this.category = category;
        this.label = label;
        this.description = description;
    }

    public Category category() {
        return category;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}

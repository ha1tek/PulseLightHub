package com.antigravity.pulselight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenrePresetCatalog {

    private static List<AudioPreset> sGt5Presets = null;
    private static List<AudioPreset> sGtNeo5Presets = null;
    private static Map<String, AudioPreset> sPresetMap = null;

    public static final List<String> ALL_SUBGENRES = Collections.unmodifiableList(Arrays.asList(
            "Testing",
            "House",
            "Techno",
            "Drum and Bass",
            "Dubstep и Bass",
            "Trance",
            "Hard Dance",
            "Synthwave",
            "Ambient",
            "Trap",
            "Phonk",
            "Boom Bap",
            "R&B и Соул",
            "Hard Rock",
            "Punk Rock",
            "Grunge",
            "Progressive Rock",
            "Heavy Metal",
            "Extreme Metal",
            "Metalcore",
            "Dance Pop",
            "K-Pop",
            "Jazz",
            "Blues",
            "Funk и Disco",
            "Reggae и Dub",
            "Reggaeton и Латина",
            "Симфоническая классика",
            "Неоклассика и Саундтреки",
            "Фолк и Кантри",
            "Мировая этника и Афробит"
    ));

    public static synchronized List<AudioPreset> getPresetsForModel(int deviceModel) {
        ensureCatalog();
        return (deviceModel == DeviceModelManager.MODEL_GT_NEO_5) ? sGtNeo5Presets : sGt5Presets;
    }

    public static synchronized AudioPreset getPresetById(String id) {
        if (id == null) return null;
        ensureCatalog();
        return sPresetMap.get(id);
    }

    public static List<String> getAllSubgenres() {
        return ALL_SUBGENRES;
    }

    private static synchronized void ensureCatalog() {
        if (sGt5Presets != null && sGtNeo5Presets != null && sPresetMap != null) {
            return;
        }

        sGt5Presets = new ArrayList<>(250);
        sGtNeo5Presets = new ArrayList<>(250);
        sPresetMap = new HashMap<>(600);

        buildModelCatalog(sGt5Presets, DeviceModelManager.MODEL_GT_5);
        buildModelCatalog(sGtNeo5Presets, DeviceModelManager.MODEL_GT_NEO_5);

        for (AudioPreset p : sGt5Presets) {
            sPresetMap.put(p.id, p);
        }
        for (AudioPreset p : sGtNeo5Presets) {
            sPresetMap.put(p.id, p);
        }
    }

    private static void buildModelCatalog(List<AudioPreset> list, int model) {
        // 0. Testing section (12 presets based on reference tracks)
        buildTestingCatalog(list, model);

        // 1. Electronic - Base 5 presets per subgenre
        addSubgenre(list, model, "House", "house",
                "House: Deep Tech", "House: Electro Groove", "House: Progressive",
                1.40f, 70, 1.35f, 1.40f, 1.25f, 1.00f, true);

        addSubgenre(list, model, "Techno", "techno",
                "Techno: Peak Time", "Techno: Dark Industrial", "Techno: Acid Drive",
                1.48f, 50, 1.45f, 1.50f, 1.20f, 1.15f, true);

        addSubgenre(list, model, "Drum and Bass", "dnb",
                "DnB: Liquid Funk", "DnB: Neurofunk Drive", "DnB: Jungle Breakbeat",
                1.45f, 45, 1.50f, 1.40f, 1.45f, 1.10f, true);

        addSubgenre(list, model, "Dubstep и Bass", "dubstep",
                "Dubstep: Heavy Brostep", "Dubstep: Deep Riddim", "Dubstep: Bass Wobble",
                1.45f, 65, 1.80f, 1.40f, 1.35f, 1.05f, false);

        addSubgenre(list, model, "Trance", "trance",
                "Trance: Uplifting Euphoria", "Trance: Psytrance Pulse", "Trance: Progressive Flow",
                1.42f, 75, 1.30f, 1.45f, 1.25f, 1.25f, true);

        addSubgenre(list, model, "Hard Dance", "hard_dance",
                "Hardstyle: Rawstyle Kick", "Hardstyle: Euphoric Melodic", "Hardcore: Fast Gabber",
                1.50f, 45, 1.40f, 1.60f, 1.25f, 1.10f, true);

        addSubgenre(list, model, "Synthwave", "synthwave",
                "Synthwave: Neon Retrowave", "Synthwave: Darksynth Outrun", "Synthwave: 80s Dream",
                1.40f, 85, 1.35f, 1.30f, 1.30f, 1.20f, false);

        addSubgenre(list, model, "Ambient", "ambient",
                "Ambient: Deep Space Drone", "Ambient: Chillout Lounge", "Ambient: IDM Micro-Pulse",
                1.30f, 140, 1.20f, 1.10f, 1.35f, 1.30f, false);

        // 1.1 Electronic - Additional High-Quality Deep 12-Band Wide Spectrum Presets
        buildDeepElectronicCatalog(list, model);

        // 2. Hip-Hop & Urban (4 subgenres)
        addSubgenre(list, model, "Trap", "trap",
                "Trap: Heavy 808 Sub", "Trap: Dark Drill Sliding", "Trap: Melodic Plugg",
                1.45f, 75, 2.00f, 1.50f, 1.35f, 1.15f, false);

        addSubgenre(list, model, "Phonk", "phonk",
                "Phonk: Drift Cowbell", "Phonk: Wave Ethereal", "Phonk: Brazilian Bass",
                1.48f, 70, 1.85f, 1.45f, 1.40f, 1.25f, true);

        addSubgenre(list, model, "Boom Bap", "boombap",
                "Boom Bap: 90s Golden Era", "Boom Bap: Vinyl Jazz Rap", "Boom Bap: Underground Punch",
                1.38f, 80, 1.30f, 1.40f, 1.45f, 0.95f, false);

        addSubgenre(list, model, "R&B и Соул", "rnb",
                "R&B: Contemporary Smooth", "R&B: Trap Soul Mood", "R&B: Neo-Soul Warmth",
                1.36f, 95, 1.40f, 1.30f, 1.35f, 1.15f, false);

        // 3. Rock & Alternative (4 subgenres)
        addSubgenre(list, model, "Hard Rock", "hard_rock",
                "Rock: Classic Stadium", "Rock: Blues Rock Drive", "Rock: Garage Riff",
                1.42f, 70, 1.25f, 1.40f, 1.40f, 1.10f, true);

        addSubgenre(list, model, "Punk Rock", "punk",
                "Punk: Fast Skate Punk", "Punk: Pop-Punk Energy", "Punk: Post-Punk Wave",
                1.45f, 50, 1.20f, 1.45f, 1.45f, 1.15f, true);

        addSubgenre(list, model, "Grunge", "grunge",
                "Grunge: 90s Seattle Sound", "Grunge: Loud-Quiet Dynamics", "Grunge: Sludge Alternative",
                1.40f, 75, 1.35f, 1.35f, 1.35f, 1.05f, false);

        addSubgenre(list, model, "Progressive Rock", "prog_rock",
                "Prog Rock: Complex Time", "Post-Rock: Cinematic Build", "Shoegaze: Wall of Sound",
                1.38f, 90, 1.30f, 1.30f, 1.35f, 1.25f, false);

        // 4. Metal & Extreme (3 subgenres)
        addSubgenre(list, model, "Heavy Metal", "heavy_metal",
                "Metal: Classic Heavy", "Metal: Power Gallop", "Metal: Speed Thrash",
                1.45f, 55, 1.25f, 1.50f, 1.40f, 1.15f, true);

        addSubgenre(list, model, "Extreme Metal", "extreme_metal",
                "Death Metal: Blastbeat Assault", "Black Metal: Tremolo Fury", "Grindcore: Micro-Burst",
                1.48f, 40, 1.20f, 1.55f, 1.45f, 1.10f, true);

        addSubgenre(list, model, "Metalcore", "metalcore",
                "Metalcore: Heavy Breakdown", "Deathcore: Sub-Drop Slam", "Djent: Poly-Rhythmic Chug",
                1.48f, 60, 1.70f, 1.50f, 1.45f, 1.10f, true);

        // 5. Pop (2 subgenres)
        addSubgenre(list, model, "Dance Pop", "dance_pop",
                "Pop: Modern Radio Hit", "Pop: Europop Energy", "Pop: Electropop Sparkle",
                1.42f, 70, 1.35f, 1.40f, 1.30f, 1.15f, true);

        addSubgenre(list, model, "K-Pop", "kpop",
                "K-Pop: Dynamic Dance Drop", "K-Pop: Hyper Pop Spark", "K-Pop: Vocal Chorus Climax",
                1.48f, 65, 1.40f, 1.45f, 1.35f, 1.25f, true);

        // 6. Jazz, Blues & Funk (3 subgenres)
        addSubgenre(list, model, "Jazz", "jazz",
                "Jazz: Swing Ride Cymbal", "Jazz: Bebop Fast Walk", "Jazz: Smooth Fusion Night",
                1.36f, 85, 1.30f, 1.25f, 1.35f, 1.30f, false);

        addSubgenre(list, model, "Blues", "blues",
                "Blues: Delta Acoustic Slide", "Blues: Chicago Electric Solo", "Blues: Texas Shuffle",
                1.38f, 80, 1.25f, 1.30f, 1.40f, 1.15f, false);

        addSubgenre(list, model, "Funk и Disco", "funk_disco",
                "Funk: Slap Bass Groove", "Disco: Studio 70s Beat", "Nu-Disco: Modern French Touch",
                1.44f, 65, 1.30f, 1.45f, 1.45f, 1.15f, true);

        // 7. Latin & Reggae (2 subgenres)
        addSubgenre(list, model, "Reggae и Dub", "reggae_dub",
                "Reggae: Roots One Drop", "Dub: Deep Echo Sub", "Dancehall: Tropical Riddim",
                1.40f, 95, 1.70f, 1.35f, 1.35f, 1.05f, false);

        addSubgenre(list, model, "Reggaeton и Латина", "reggaeton",
                "Reggaeton: Classic Dembow", "Salsa: Afro-Cuban Brass", "Bachata: Syncopated Romance",
                1.46f, 65, 1.40f, 1.45f, 1.40f, 1.20f, true);

        // 8. Classical (2 subgenres)
        addSubgenre(list, model, "Симфоническая классика", "classic_sym",
                "Классика: Симфоническое Tutti", "Классика: Драматическое Скерцо", "Классика: Барочный Кончерто",
                1.35f, 110, 1.30f, 1.25f, 1.35f, 1.35f, false);

        addSubgenre(list, model, "Неоклассика и Саундтреки", "neoclassic",
                "Неоклассика: Интроспективное Пианино", "Неоклассика: Кинематографический Саундтрек", "Неоклассика: Эпический Трейлер",
                1.38f, 115, 1.45f, 1.30f, 1.40f, 1.30f, false);

        // 9. Folk & Ethnic (2 subgenres)
        addSubgenre(list, model, "Фолк и Кантри", "folk_country",
                "Фолк: Акустический Фингерстайл", "Кантри: Блюграсс Банджо", "Кельтский фолк: Быстрая Джига",
                1.38f, 75, 1.25f, 1.30f, 1.40f, 1.25f, true);

        addSubgenre(list, model, "Мировая этника и Афробит", "ethnic_afro",
                "Афробит: Полиритмический Грув", "Этника: Барабаны Джембе и Токи", "Фламенко: Пальмас и Испанская Гитара",
                1.45f, 60, 1.40f, 1.45f, 1.45f, 1.25f, true);
    }

    private static void buildDeepElectronicCatalog(List<AudioPreset> list, int model) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);

        // ==========================================
        // 1. DUBSTEP & BASS: 10 HIGH-QUALITY DEEP 12-BAND PRESETS
        // ==========================================

        // 1. Dubstep: Deep Sub Low-End 30Hz
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_sub30",
                "Dubstep: Deep Sub Low-End 30Hz",
                1.46f, 65, 0.10f, 30, 240, false, 12.0f, false, 0.10f, true,
                new float[]{2.30f, 2.10f, 1.35f, 1.00f, 0.80f, 0.75f, 0.85f, 1.25f, 0.90f, 0.90f, 0.70f, 0.60f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, false, false, true, true, true, false, true, false, false}
        );

        // 2. Dubstep: Riddim Chop & Bounce
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_riddim",
                "Dubstep: Riddim Chop & Bounce",
                1.48f, 45, 0.12f, 24, 150, false, 14.0f, false, 0.10f, true,
                new float[]{1.80f, 1.60f, 1.40f, 1.85f, 1.90f, 1.70f, 1.50f, 1.40f, 1.10f, 1.00f, 0.80f, 0.70f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, false, false, true, true, true, true, false, false, true, false, false}
        );

        // 3. Dubstep: Tearout Aggression
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_tearout",
                "Dubstep: Tearout Aggression",
                1.50f, 38, 0.11f, 22, 130, false, 15.0f, false, 0.10f, true,
                new float[]{1.60f, 1.50f, 1.55f, 1.70f, 1.95f, 2.05f, 1.90f, 1.60f, 1.20f, 1.10f, 0.80f, 0.70f},
                new int[]{
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, false, false}
        );

        // 4. Dubstep: Melodic Color Bass
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_colorbass",
                "Dubstep: Melodic Color Bass",
                1.42f, 110, 0.06f, 40, 280, true, 13.0f, true, 0.14f, false,
                new float[]{1.30f, 1.35f, 1.25f, 1.50f, 1.70f, 1.85f, 1.80f, 1.60f, 1.45f, 1.35f, 1.25f, 1.15f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, true, true}
        );

        // 5. Dubstep: Deep Dark & Dangerous
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_ddd",
                "Dubstep: Deep Dark & Dangerous",
                1.44f, 85, 0.14f, 40, 260, false, 11.0f, false, 0.08f, true,
                new float[]{2.40f, 2.20f, 1.20f, 0.70f, 0.60f, 0.65f, 0.80f, 1.40f, 0.80f, 1.20f, 0.70f, 0.60f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_LEFT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, false, false, false, false, false, false, true, false, true, false, false}
        );

        // 6. Dubstep: Gunfinger Deathstep
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_deathstep",
                "Dubstep: Gunfinger Deathstep",
                1.52f, 40, 0.12f, 20, 140, false, 15.5f, false, 0.10f, true,
                new float[]{1.90f, 1.75f, 1.65f, 1.50f, 1.80f, 1.90f, 1.75f, 1.60f, 1.15f, 1.05f, 0.80f, 0.70f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // 7. Dubstep: Neurohop & Glitch Bass
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_glitch",
                "Dubstep: Neurohop & Glitch Bass",
                1.46f, 50, 0.09f, 25, 170, true, 16.0f, true, 0.18f, true,
                new float[]{1.60f, 1.50f, 1.40f, 1.65f, 1.85f, 1.75f, 1.70f, 1.55f, 1.35f, 1.25f, 1.05f, 0.95f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, false, false}
        );

        // 8. Dubstep: Trench Minimalist
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_trench",
                "Dubstep: Trench Minimalist",
                1.44f, 70, 0.12f, 35, 220, false, 12.0f, false, 0.08f, true,
                new float[]{2.35f, 2.05f, 1.25f, 0.85f, 0.75f, 0.85f, 1.10f, 1.65f, 0.95f, 1.05f, 0.75f, 0.65f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, false, false, false, false, false, false, true, false, true, false, false}
        );

        // 9. Dubstep: Space LFO Wobble
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_wobble",
                "Dubstep: Space LFO Wobble",
                1.42f, 90, 0.08f, 35, 250, true, 11.5f, true, 0.12f, true,
                new float[]{1.70f, 1.95f, 1.85f, 1.75f, 1.60f, 1.40f, 1.30f, 1.35f, 1.10f, 1.00f, 0.85f, 0.75f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // 10. Dubstep: Heavy Metalstep Slam
        addDeepWidePreset(list, model, "Dubstep и Bass", "dubstep_deep_metalstep",
                "Dubstep: Heavy Metalstep Slam",
                1.48f, 52, 0.10f, 28, 180, false, 14.5f, false, 0.10f, true,
                new float[]{2.20f, 1.90f, 1.65f, 1.60f, 1.80f, 1.85f, 1.75f, 1.65f, 1.20f, 1.15f, 0.85f, 0.75f},
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{true, false, false, true, true, true, true, true, false, true, false, false}
        );


        // ==========================================
        // 2. HOUSE: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // House 1: Deep Atmosphere 12-Band
        addDeepWidePreset(list, model, "House", "house_deep_tech",
                "House: Deep Atmosphere 12-Band",
                1.40f, 80, 0.07f, 32, 240, true, 13.0f, false, 0.10f, true,
                new float[]{1.50f, 1.45f, 1.35f, 1.20f, 1.25f, 1.35f, 1.30f, 1.40f, 1.30f, 1.25f, 1.10f, 1.00f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, false, false, false}
        );

        // House 2: Minimal Dub Immersion
        addDeepWidePreset(list, model, "House", "house_deep_dub",
                "House: Minimal Dub Immersion",
                1.38f, 110, 0.08f, 40, 270, true, 11.0f, false, 0.08f, true,
                new float[]{1.80f, 1.65f, 1.30f, 1.10f, 1.20f, 1.30f, 1.25f, 1.35f, 1.10f, 1.05f, 0.90f, 0.80f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, false, false, false, true, true, true, true, false, false, false, false}
        );

        // House 3: Melodic Organic Journey
        addDeepWidePreset(list, model, "House", "house_deep_organic",
                "House: Melodic Organic Journey",
                1.42f, 90, 0.06f, 35, 260, true, 12.0f, true, 0.12f, false,
                new float[]{1.35f, 1.30f, 1.30f, 1.35f, 1.45f, 1.55f, 1.50f, 1.45f, 1.40f, 1.35f, 1.25f, 1.15f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, true, true}
        );


        // ==========================================
        // 3. TECHNO: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // Techno 1: Deep Hypnotic Sub-Space
        addDeepWidePreset(list, model, "Techno", "techno_deep_hypnotic",
                "Techno: Deep Hypnotic Sub-Space",
                1.50f, 55, 0.11f, 26, 160, false, 14.0f, false, 0.10f, true,
                new float[]{1.85f, 1.70f, 1.50f, 1.15f, 1.05f, 1.10f, 1.20f, 1.35f, 1.30f, 1.35f, 1.15f, 1.05f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, false, true, true, true, true, false, true, false, false}
        );

        // Techno 2: Modular Analog Resonance
        addDeepWidePreset(list, model, "Techno", "techno_deep_modular",
                "Techno: Modular Analog Resonance",
                1.48f, 65, 0.08f, 28, 180, false, 13.5f, true, 0.16f, true,
                new float[]{1.50f, 1.45f, 1.40f, 1.55f, 1.80f, 1.85f, 1.70f, 1.40f, 1.25f, 1.20f, 1.05f, 0.95f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, false, false, false}
        );

        // Techno 3: Dub Techno Deep Chord
        addDeepWidePreset(list, model, "Techno", "techno_deep_dub",
                "Techno: Dub Techno Deep Chord",
                1.40f, 125, 0.07f, 40, 280, true, 10.5f, false, 0.08f, false,
                new float[]{1.75f, 1.60f, 1.35f, 1.25f, 1.45f, 1.55f, 1.40f, 1.30f, 1.15f, 1.10f, 1.00f, 0.90f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, false, false, false}
        );


        // ==========================================
        // 4. DRUM AND BASS: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // DnB 1: Deep Liquid Atmosphere
        addDeepWidePreset(list, model, "Drum and Bass", "dnb_deep_liquid",
                "DnB: Deep Liquid Atmosphere",
                1.42f, 85, 0.06f, 32, 250, true, 13.0f, false, 0.10f, false,
                new float[]{1.70f, 1.60f, 1.35f, 1.25f, 1.40f, 1.55f, 1.50f, 1.45f, 1.30f, 1.20f, 1.10f, 1.00f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // DnB 2: Halftime 170 Sub Bass
        addDeepWidePreset(list, model, "Drum and Bass", "dnb_deep_halftime",
                "DnB: Halftime 170 Sub Bass",
                1.48f, 65, 0.12f, 28, 180, false, 14.0f, false, 0.10f, true,
                new float[]{2.10f, 1.90f, 1.40f, 1.15f, 1.10f, 1.20f, 1.30f, 1.55f, 1.25f, 1.20f, 0.95f, 0.85f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, false, false, true, true, true, false, true, false, false}
        );

        // DnB 3: Deep Neuro Glitch Flow
        addDeepWidePreset(list, model, "Drum and Bass", "dnb_deep_neuro",
                "DnB: Deep Neuro Glitch Flow",
                1.50f, 42, 0.10f, 22, 140, false, 15.0f, true, 0.15f, true,
                new float[]{1.60f, 1.55f, 1.50f, 1.70f, 1.85f, 1.80f, 1.65f, 1.55f, 1.35f, 1.25f, 1.05f, 0.95f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );


        // ==========================================
        // 5. TRANCE: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // Trance 1: Deep Vocal Anthem
        addDeepWidePreset(list, model, "Trance", "trance_deep_vocal",
                "Trance: Deep Vocal Anthem",
                1.42f, 110, 0.05f, 38, 280, true, 12.5f, false, 0.10f, false,
                new float[]{1.35f, 1.30f, 1.35f, 1.30f, 1.50f, 1.75f, 1.80f, 1.65f, 1.50f, 1.40f, 1.30f, 1.20f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, false, false}
        );

        // Trance 2: Goa Acid Horizon
        addDeepWidePreset(list, model, "Trance", "trance_deep_goa",
                "Trance: Goa Acid Horizon",
                1.46f, 50, 0.09f, 24, 150, false, 14.5f, false, 0.10f, true,
                new float[]{1.45f, 1.40f, 1.45f, 1.60f, 1.85f, 1.90f, 1.75f, 1.50f, 1.35f, 1.30f, 1.15f, 1.05f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Trance 3: Cinematic Orchestral Uplift
        addDeepWidePreset(list, model, "Trance", "trance_deep_cinematic",
                "Trance: Cinematic Orchestral Uplift",
                1.40f, 135, 0.05f, 45, 300, true, 10.0f, false, 0.08f, false,
                new float[]{1.50f, 1.45f, 1.35f, 1.35f, 1.55f, 1.70f, 1.75f, 1.65f, 1.55f, 1.45f, 1.35f, 1.25f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, true, true}
        );


        // ==========================================
        // 6. HARD DANCE: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // Hard Dance 1: Deep Reverse Bass
        addDeepWidePreset(list, model, "Hard Dance", "hard_deep_reverse",
                "Hardstyle: Deep Reverse Bass",
                1.48f, 48, 0.11f, 24, 160, false, 14.5f, false, 0.10f, true,
                new float[]{2.00f, 1.80f, 1.60f, 1.45f, 1.25f, 1.20f, 1.30f, 1.50f, 1.25f, 1.20f, 0.95f, 0.85f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Hard Dance 2: Frenchcore 200 Pulse
        addDeepWidePreset(list, model, "Hard Dance", "hard_deep_frenchcore",
                "Hardcore: Frenchcore 200 Pulse",
                1.52f, 36, 0.13f, 18, 120, false, 16.0f, false, 0.08f, true,
                new float[]{1.60f, 1.55f, 1.65f, 1.50f, 1.40f, 1.45f, 1.50f, 1.60f, 1.35f, 1.30f, 1.10f, 1.00f},
                new int[]{
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{true, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Hard Dance 3: Raw Industrial Kick-Drop
        addDeepWidePreset(list, model, "Hard Dance", "hard_deep_raw",
                "Hard Dance: Raw Industrial Kick-Drop",
                1.50f, 50, 0.12f, 25, 160, false, 14.5f, false, 0.10f, true,
                new float[]{1.85f, 1.75f, 1.70f, 1.60f, 1.50f, 1.45f, 1.40f, 1.55f, 1.25f, 1.15f, 0.95f, 0.85f},
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{true, false, false, true, true, true, true, true, false, true, false, false}
        );


        // ==========================================
        // 7. SYNTHWAVE: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // Synthwave 1: Deep Analog Synth Pluck
        addDeepWidePreset(list, model, "Synthwave", "synthwave_deep_analog",
                "Synthwave: Deep Analog Synth Pluck",
                1.42f, 95, 0.07f, 32, 260, true, 12.0f, false, 0.10f, false,
                new float[]{1.45f, 1.40f, 1.35f, 1.40f, 1.60f, 1.75f, 1.70f, 1.55f, 1.40f, 1.35f, 1.20f, 1.10f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Synthwave 2: Cyberpunk Midtempo 100
        addDeepWidePreset(list, model, "Synthwave", "synthwave_deep_midtempo",
                "Synthwave: Cyberpunk Midtempo 100",
                1.48f, 68, 0.10f, 30, 190, false, 13.5f, false, 0.10f, true,
                new float[]{1.90f, 1.75f, 1.55f, 1.60f, 1.75f, 1.70f, 1.60f, 1.50f, 1.25f, 1.20f, 1.00f, 0.90f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Synthwave 3: Chillwave Warm Sunset
        addDeepWidePreset(list, model, "Synthwave", "synthwave_deep_chill",
                "Synthwave: Chillwave Warm Sunset",
                1.38f, 130, 0.05f, 45, 290, true, 9.5f, false, 0.08f, false,
                new float[]{1.50f, 1.45f, 1.30f, 1.25f, 1.45f, 1.55f, 1.50f, 1.40f, 1.30f, 1.25f, 1.15f, 1.05f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, false, false, false}
        );


        // ==========================================
        // 8. AMBIENT: 3 DEEP 12-BAND PRESETS
        // ==========================================

        // Ambient 1: 12-Band Drone Meditation
        addDeepWidePreset(list, model, "Ambient", "ambient_deep_drone",
                "Ambient: 12-Band Drone Meditation",
                1.32f, 220, 0.03f, 60, 350, true, 7.5f, true, 0.10f, false,
                new float[]{1.50f, 1.50f, 1.45f, 1.40f, 1.40f, 1.45f, 1.45f, 1.40f, 1.40f, 1.35f, 1.30f, 1.25f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, true, true}
        );

        // Ambient 2: Sub-Harmonic Binaural
        addDeepWidePreset(list, model, "Ambient", "ambient_deep_binaural",
                "Ambient: Sub-Harmonic Binaural",
                1.34f, 180, 0.04f, 50, 320, true, 8.5f, false, 0.08f, false,
                new float[]{2.00f, 1.85f, 1.35f, 1.15f, 1.20f, 1.25f, 1.30f, 1.30f, 1.35f, 1.40f, 1.30f, 1.25f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Ambient 3: Cosmic Soundscape Echo
        addDeepWidePreset(list, model, "Ambient", "ambient_deep_cosmic",
                "Ambient: Cosmic Soundscape Echo",
                1.36f, 200, 0.04f, 55, 340, true, 8.0f, false, 0.08f, false,
                new float[]{1.40f, 1.35f, 1.30f, 1.35f, 1.50f, 1.60f, 1.65f, 1.55f, 1.50f, 1.45f, 1.40f, 1.35f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, true, true}
        );
    }

    private static void addDeepWidePreset(
            List<AudioPreset> list,
            int model,
            String subgenre,
            String idSuffix,
            String name,
            float sens,
            int decayMs,
            float gateThresh,
            int minHoldMs,
            int maxHoldMs,
            boolean fInterp,
            float fInterpSpeed,
            boolean variation,
            float varDepth,
            boolean limiter,
            float[] wide12Gains,
            int[] gt5Patterns,
            int[] neo5Patterns,
            boolean[] neo5ColorCycle
    ) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);
        String fullId = isNeo5 ? ("neo5_" + idSuffix) : idSuffix;

        AudioPreset p = new AudioPreset(fullId, name, true, model, subgenre);

        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.fftSize = 2048;
        p.useTukeyWindow = true;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.sensitivity = sens;
        p.decayMs = decayMs;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = (gateThresh > 0.001f);
        p.loudnessGateThreshold = gateThresh;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = mapToMinHoldRange(minHoldMs);
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = maxHoldMs;
        p.enableFInterp = fInterp;
        p.fInterpSpeed = fInterpSpeed;
        p.enableRandomVariation = variation;
        p.randomVariationDepth = varDepth;
        p.enableLimiter = limiter;

        // Narrow fallbacks if user switches mode
        p.narrowGains = new float[]{wide12Gains[0], wide12Gains[2], wide12Gains[6], wide12Gains[10]};
        p.narrowThresholds = new float[]{0.14f, 0.14f, 0.14f, 0.14f};
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.narrowPatterns = isNeo5 ?
                new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE} :
                new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL};
        p.narrowColorCycle = isNeo5 ? new boolean[]{false, false, true, true} : new boolean[]{false, false, false, false};

        // Wide spectrum 12 bands
        p.wideGains = wide12Gains;
        p.wideThresholds = new float[]{
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f,
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f
        };
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};

        if (isNeo5) {
            p.widePatterns = neo5Patterns;
            p.wideColorCycle = neo5ColorCycle;
        } else {
            p.widePatterns = gt5Patterns;
            p.wideColorCycle = new boolean[12];
        }

        list.add(p);
    }

    private static void addSubgenre(
            List<AudioPreset> list,
            int model,
            String subgenre,
            String idPrefix,
            String s1Name, String s2Name, String s3Name,
            float sens, int decay,
            float subG, float kickG, float snareG, float trebleG,
            boolean isFastTempo
    ) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);
        String pfx = isNeo5 ? ("neo5_" + idPrefix + "_") : (idPrefix + "_");

        // 1. Style 1 - Core Balanced Style
        AudioPreset p1 = new AudioPreset(pfx + "style1", s1Name, true, model, subgenre);
        configurePreset(p1, model, isFastTempo ? AudioAnalyzer.STUDIO_MODE_FAST : AudioAnalyzer.STUDIO_MODE_DEEP,
                isFastTempo ? AudioAnalyzer.SPECTRUM_MODE_NARROW : AudioAnalyzer.SPECTRUM_MODE_WIDE,
                sens, decay, true, 0.08f, isFastTempo ? 30 : 40, isFastTempo ? 200 : 250,
                false, 12.0f, false, 0.12f, true,
                subG, kickG, snareG, trebleG, 1);
        list.add(p1);

        // 2. Style 2 - Atmospheric / Alternative Hue
        AudioPreset p2 = new AudioPreset(pfx + "style2", s2Name, true, model, subgenre);
        configurePreset(p2, model, AudioAnalyzer.STUDIO_MODE_DEEP, AudioAnalyzer.SPECTRUM_MODE_WIDE,
                sens * 1.04f, (int) (decay * 1.25f), true, 0.06f, 35, 270,
                true, 11.5f, true, 0.14f, true,
                subG * 1.05f, kickG * 0.95f, snareG * 1.10f, trebleG * 1.20f, 2);
        list.add(p2);

        // 3. Style 3 - Drive / High Energy
        AudioPreset p3 = new AudioPreset(pfx + "style3", s3Name, true, model, subgenre);
        configurePreset(p3, model, AudioAnalyzer.STUDIO_MODE_FAST, AudioAnalyzer.SPECTRUM_MODE_NARROW,
                sens * 1.08f, Math.max(35, (int) (decay * 0.82f)), true, 0.09f, 28, 180,
                false, 14.0f, false, 0.10f, true,
                subG * 1.10f, kickG * 1.20f, snareG * 1.20f, trebleG * 1.05f, 3);
        list.add(p3);

        // 4. Beat Tracker - Razor Sharp Attack & Rhythm Focus
        AudioPreset p4 = new AudioPreset(pfx + "beat", subgenre + ": Бит-Трекер", true, model, subgenre);
        int beatDecay = Math.max(35, Math.min(55, decay - 22));
        configurePreset(p4, model, AudioAnalyzer.STUDIO_MODE_FAST, AudioAnalyzer.SPECTRUM_MODE_NARROW,
                sens * 1.14f, beatDecay, true, 0.12f, 24, 150,
                false, 15.0f, false, 0.08f, true,
                subG * 1.30f, kickG * 1.40f, snareG * 1.35f, trebleG * 0.85f, 4);
        list.add(p4);

        // 5. Melody Tracker - Expressive Vocals & Lead Tracking
        AudioPreset p5 = new AudioPreset(pfx + "melody", subgenre + ": Мелоди-Трекер", true, model, subgenre);
        int melodyDecay = Math.max(90, decay + 35);
        configurePreset(p5, model, AudioAnalyzer.STUDIO_MODE_DEEP, AudioAnalyzer.SPECTRUM_MODE_WIDE,
                sens * 1.10f, melodyDecay, true, 0.05f, 45, 290,
                true, 12.0f, true, 0.15f, false,
                subG * 0.70f, kickG * 0.75f, snareG * 1.45f, trebleG * 1.55f, 5);
        list.add(p5);
    }

    private static void configurePreset(
            AudioPreset p, int model, int studioMode, int specMode,
            float sens, int decayMs, boolean gate, float gateThresh,
            int minHoldMs, int maxHoldMs,
            boolean fInterp, float fInterpSpeed,
            boolean variation, float varDepth,
            boolean limiter,
            float subG, float kickG, float snareG, float trebleG,
            int roleType
    ) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);

        p.studioAnalysisMode = studioMode;
        p.spectrumMode = specMode;
        p.fftSize = (studioMode == AudioAnalyzer.STUDIO_MODE_DEEP) ? 2048 : 1024;
        p.useTukeyWindow = (studioMode == AudioAnalyzer.STUDIO_MODE_DEEP);
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.sensitivity = sens;
        p.decayMs = decayMs;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = gate;
        p.loudnessGateThreshold = gateThresh;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = mapToMinHoldRange(minHoldMs);
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = maxHoldMs;
        p.enableFInterp = fInterp;
        p.fInterpSpeed = fInterpSpeed;
        p.enableRandomVariation = variation;
        p.randomVariationDepth = varDepth;
        p.enableLimiter = limiter;

        // Narrow gains: [sub, kick, snare, treble]
        p.narrowGains = new float[]{subG, kickG, snareG, trebleG};
        p.narrowThresholds = new float[]{0.14f, 0.14f, 0.14f, 0.14f};
        p.narrowEnabled = new boolean[]{true, true, true, true};

        // Wide gains (12 bands) interpolated
        p.wideGains = new float[]{
                subG * 1.05f, subG, kickG * 1.05f, kickG,
                snareG * 0.95f, snareG, snareG * 1.05f, trebleG * 0.90f,
                trebleG, trebleG * 1.05f, trebleG, trebleG * 0.95f
        };
        p.wideThresholds = new float[]{
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f,
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f
        };
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};

        if (isNeo5) {
            // Realme GT Neo 5: Awakening Halo single loop
            p.narrowColorCycle = new boolean[4];
            p.wideColorCycle = new boolean[12];

            if (roleType == 4) { // Beat Tracker
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_OFF
                };
                p.narrowColorCycle[1] = true; // Kick switches color
                p.narrowColorCycle[2] = true; // Snare switches color
            } else if (roleType == 5) { // Melody Tracker
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE
                };
                p.narrowColorCycle[2] = true;
                p.narrowColorCycle[3] = true;
                for (int i = 4; i < 12; i++) {
                    p.wideColorCycle[i] = true;
                }
            } else if (roleType == 2) { // Atmospheric
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE
                };
                p.narrowColorCycle[3] = true;
            } else if (roleType == 3) { // Drive
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE
                };
                p.narrowColorCycle[2] = true;
                p.narrowColorCycle[3] = true;
            } else { // Style 1
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_OFF
                };
                p.narrowColorCycle[2] = true;
            }

            p.widePatterns = new int[12];
            for (int i = 0; i < 12; i++) {
                p.widePatterns[i] = p.wideColorCycle[i] ? AudioAnalyzer.PATTERN_COLOR_CYCLE : AudioAnalyzer.PATTERN_ALL;
            }
        } else {
            // Realme GT 5: 4 Independent physical segments (Top, Bottom, Left, Right)
            p.narrowColorCycle = new boolean[]{false, false, false, false};
            p.wideColorCycle = new boolean[12];

            if (roleType == 4) { // Beat Tracker
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM,
                        AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP
                };
            } else if (roleType == 5) { // Melody Tracker
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_ALL
                };
            } else {
                p.narrowPatterns = new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM,
                        AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_ALL
                };
            }

            p.widePatterns = new int[]{
                    AudioAnalyzer.PATTERN_BOTTOM,
                    AudioAnalyzer.PATTERN_TOP_BOTTOM,
                    AudioAnalyzer.PATTERN_BOTTOM,
                    AudioAnalyzer.PATTERN_LEFT_RIGHT,
                    AudioAnalyzer.PATTERN_LEFT,
                    AudioAnalyzer.PATTERN_RIGHT,
                    AudioAnalyzer.PATTERN_LEFT_RIGHT,
                    AudioAnalyzer.PATTERN_TOP_LEFT,
                    AudioAnalyzer.PATTERN_TOP_RIGHT,
                    AudioAnalyzer.PATTERN_TOP,
                    AudioAnalyzer.PATTERN_TOP_BOTTOM,
                    AudioAnalyzer.PATTERN_ALL
            };
        }
    }

    private static void buildTestingCatalog(List<AudioPreset> list, int model) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);

        // =========================================================================
        // TRACK 1: Bad Computer - 32n (Chiptune Complextro / Electro House 128 BPM)
        // =========================================================================

        // Test 1: Вариант 1 (Глубокий 12-полосный — Комплексный баланс)
        addDeepWidePreset(list, model, "Testing", "test1_v1",
                "Test 1: Вариант 1",
                1.46f, 58, 0.08f, 26, 190, true, 14.0f, false, 0.10f, true,
                new float[]{1.85f, 1.70f, 1.45f, 1.30f, 1.60f, 1.75f, 1.65f, 1.45f, 1.35f, 1.40f, 1.25f, 1.15f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Test 1: Вариант 2 (Простой 4-полосный — Бит-Драйв и 16-е доли)
        addNarrowPreset(list, model, "Testing", "test1_v2",
                "Test 1: Вариант 2",
                AudioAnalyzer.STUDIO_MODE_FAST, 1.50f, 42, 0.10f, 22, 140,
                false, 15.0f, false, 0.08f, true,
                new float[]{1.60f, 1.50f, 1.40f, 1.10f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, true, true}
        );

        // Test 1: Вариант 3 (Глубокий 4-полосный — Мелодический синтезатор)
        addNarrowPreset(list, model, "Testing", "test1_v3",
                "Test 1: Вариант 3",
                AudioAnalyzer.STUDIO_MODE_DEEP, 1.42f, 75, 0.06f, 32, 240,
                true, 13.0f, true, 0.12f, false,
                new float[]{1.45f, 1.35f, 1.50f, 1.30f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, true, true}
        );


        // =========================================================================
        // TRACK 2: Crankdat, Sofi - Whiplash (Heavy Bass House / Hybrid Trap / Dubstep)
        // =========================================================================

        // Test 2: Вариант 1 (Глубокий 12-полосный — Дроповый слэм)
        addDeepWidePreset(list, model, "Testing", "test2_v1",
                "Test 2: Вариант 1",
                1.50f, 46, 0.11f, 24, 150, false, 14.5f, false, 0.10f, true,
                new float[]{2.25f, 1.95f, 1.55f, 1.65f, 1.90f, 1.95f, 1.80f, 1.60f, 1.25f, 1.15f, 0.85f, 0.75f},
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{true, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Test 2: Вариант 2 (Глубокий 12-полосный — Саб Фокус 30 Гц)
        addDeepWidePreset(list, model, "Testing", "test2_v2",
                "Test 2: Вариант 2",
                1.46f, 62, 0.10f, 30, 210, false, 12.0f, false, 0.08f, true,
                new float[]{2.40f, 2.15f, 1.40f, 1.20f, 1.35f, 1.50f, 1.60f, 1.40f, 1.10f, 1.05f, 0.80f, 0.70f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_LEFT,
                        AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, false, false, false, true, true, true, false, false, true, false, false}
        );

        // Test 2: Вариант 3 (Простой 4-полосный — Бит-Слэм и Атака)
        addNarrowPreset(list, model, "Testing", "test2_v3",
                "Test 2: Вариант 3",
                AudioAnalyzer.STUDIO_MODE_FAST, 1.54f, 38, 0.12f, 20, 130,
                false, 15.5f, false, 0.08f, true,
                new float[]{1.80f, 1.60f, 1.50f, 1.00f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF
                },
                new boolean[]{false, true, true, false}
        );


        // =========================================================================
        // TRACK 3: Rain Paris - Enemy (Modern Rock / Dark Pop / Alt Vocal Power)
        // =========================================================================

        // Test 3: Вариант 1 (Глубокий 12-полосный — Вокальный фокус)
        addDeepWidePreset(list, model, "Testing", "test3_v1",
                "Test 3: Вариант 1",
                1.42f, 115, 0.05f, 38, 280, true, 12.0f, false, 0.10f, false,
                new float[]{1.35f, 1.30f, 1.25f, 1.40f, 1.65f, 1.85f, 1.80f, 1.60f, 1.45f, 1.40f, 1.30f, 1.20f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, true, true, false, false}
        );

        // Test 3: Вариант 2 (Глубокий 12-полосный — Стена звука и гитары)
        addDeepWidePreset(list, model, "Testing", "test3_v2",
                "Test 3: Вариант 2",
                1.45f, 90, 0.07f, 32, 240, true, 13.0f, true, 0.12f, true,
                new float[]{1.60f, 1.55f, 1.50f, 1.65f, 1.75f, 1.70f, 1.65f, 1.55f, 1.35f, 1.25f, 1.10f, 1.00f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Test 3: Вариант 3 (Глубокий 4-полосный — Рок-Ударные и Акустика)
        addNarrowPreset(list, model, "Testing", "test3_v3",
                "Test 3: Вариант 3",
                AudioAnalyzer.STUDIO_MODE_DEEP, 1.44f, 78, 0.07f, 30, 220,
                true, 12.5f, false, 0.10f, true,
                new float[]{1.40f, 1.55f, 1.50f, 1.20f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, true, true}
        );


        // =========================================================================
        // TRACK 4: Telomic, Sam M - Underwater (Liquid DnB / Soulful 174 BPM)
        // =========================================================================

        // Test 4: Вариант 1 (Глубокий 12-полосный — Скользящий Саб и Воздух)
        addDeepWidePreset(list, model, "Testing", "test4_v1",
                "Test 4: Вариант 1",
                1.44f, 72, 0.06f, 30, 220, true, 13.5f, false, 0.10f, false,
                new float[]{2.10f, 1.90f, 1.35f, 1.20f, 1.40f, 1.55f, 1.50f, 1.45f, 1.40f, 1.35f, 1.25f, 1.15f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT, AudioAnalyzer.PATTERN_LEFT_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Test 4: Вариант 2 (Глубокий 12-полосный — Быстрый Брейкбит 174 BPM)
        addDeepWidePreset(list, model, "Testing", "test4_v2",
                "Test 4: Вариант 2",
                1.48f, 50, 0.09f, 22, 150, false, 15.0f, false, 0.08f, true,
                new float[]{1.80f, 1.70f, 1.50f, 1.35f, 1.45f, 1.55f, 1.50f, 1.65f, 1.50f, 1.40f, 1.20f, 1.10f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                        AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT, AudioAnalyzer.PATTERN_TOP,
                        AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
                },
                new boolean[]{false, false, false, true, true, true, true, true, false, true, false, false}
        );

        // Test 4: Вариант 3 (Глубокий 4-полосный — Атмосферное погружение)
        addNarrowPreset(list, model, "Testing", "test4_v3",
                "Test 4: Вариант 3",
                AudioAnalyzer.STUDIO_MODE_DEEP, 1.38f, 130, 0.04f, 45, 300,
                true, 10.0f, true, 0.12f, false,
                new float[]{1.70f, 1.30f, 1.40f, 1.35f},
                new int[]{
                        AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM,
                        AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL
                },
                new int[]{
                        AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                        AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
                },
                new boolean[]{false, false, true, true}
        );
    }

    private static void addNarrowPreset(
            List<AudioPreset> list,
            int model,
            String subgenre,
            String idSuffix,
            String name,
            int studioMode,
            float sens,
            int decayMs,
            float gateThresh,
            int minHoldMs,
            int maxHoldMs,
            boolean fInterp,
            float fInterpSpeed,
            boolean variation,
            float varDepth,
            boolean limiter,
            float[] narrow4Gains,
            int[] gt5Patterns,
            int[] neo5Patterns,
            boolean[] neo5ColorCycle
    ) {
        boolean isNeo5 = (model == DeviceModelManager.MODEL_GT_NEO_5);
        String fullId = isNeo5 ? ("neo5_" + idSuffix) : idSuffix;

        AudioPreset p = new AudioPreset(fullId, name, true, model, subgenre);

        p.studioAnalysisMode = studioMode;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.fftSize = (studioMode == AudioAnalyzer.STUDIO_MODE_DEEP) ? 2048 : 1024;
        p.useTukeyWindow = (studioMode == AudioAnalyzer.STUDIO_MODE_DEEP);
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.sensitivity = sens;
        p.decayMs = decayMs;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = (gateThresh > 0.001f);
        p.loudnessGateThreshold = gateThresh;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = mapToMinHoldRange(minHoldMs);
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = maxHoldMs;
        p.enableFInterp = fInterp;
        p.fInterpSpeed = fInterpSpeed;
        p.enableRandomVariation = variation;
        p.randomVariationDepth = varDepth;
        p.enableLimiter = limiter;

        p.narrowGains = narrow4Gains;
        p.narrowThresholds = new float[]{0.14f, 0.14f, 0.14f, 0.14f};
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.narrowPatterns = isNeo5 ? neo5Patterns : gt5Patterns;
        p.narrowColorCycle = isNeo5 ? neo5ColorCycle : new boolean[]{false, false, false, false};

        // Wide fallbacks
        p.wideGains = new float[]{
                narrow4Gains[0] * 1.05f, narrow4Gains[0], narrow4Gains[1] * 1.05f, narrow4Gains[1],
                narrow4Gains[2] * 0.95f, narrow4Gains[2], narrow4Gains[2] * 1.05f, narrow4Gains[3] * 0.90f,
                narrow4Gains[3], narrow4Gains[3] * 1.05f, narrow4Gains[3], narrow4Gains[3] * 0.95f
        };
        p.wideThresholds = new float[]{
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f,
                0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f
        };
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.widePatterns = new int[12];
        p.wideColorCycle = new boolean[12];
        for (int i = 0; i < 12; i++) {
            p.widePatterns[i] = AudioAnalyzer.PATTERN_ALL;
        }

        list.add(p);
    }

    private static int mapToMinHoldRange(int originalMs) {
        if (originalMs >= 75 && originalMs <= 95) {
            return originalMs;
        }
        if (originalMs < 75) {
            float norm = Math.max(0f, Math.min(1f, (originalMs - 20f) / 35f));
            return Math.round(75f + norm * 20f);
        }
        return 95;
    }
}

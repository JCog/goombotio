package api.src;

public class SrcEnums {
    
    interface Category {
        String getId();
        Game getGame();
        Variable[] getVariables();
    }
    
    interface Variable {
        String getVarId();
        String getValueId();
    }
    
    public enum Game {
        BUG_FABLES("Bug Fables", "w6jlx74d"),
        SUNSHINE("Super Mario Sunshine", "v1pxjz68"),
        PAPER_MARIO("Paper Mario", "pdvzq96w"),
        PAPER_MARIO_MEMES("Paper Mario", "pdvz9k96"),
        TTYD("Paper Mario: The Thousand-Year Door", "m1zjo360"),
        OOT("The Legend of Zelda: Ocarina of Time", "j1l9qz1g"),
        SMRPG("Super Mario RPG (Switch)", "kdkqqxld");
        
        private final String name;
        private final String id;
        
        Game(String name, String id) {
            this.name = name;
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum PapeVariable implements Variable {
        N64("r8r5y2le", "jq6vjo71"),
        WII("r8r5y2le", "5lm2934q"),
        WII_U("r8r5y2le", "81w7k25q"),
        SWITCH("r8r5y2le", "jqz2x3kq");
        
        private final String varId;
        private final String valueId;
        
        PapeVariable(String varId, String valueId) {
            this.varId = varId;
            this.valueId = valueId;
        }
        
        @Override
        public String getVarId() {
            return varId;
        }
        
        @Override
        public String getValueId() {
            return valueId;
        }
    }
    
    public enum SmrpgVariable implements Variable {
        TURBO("p85pvv7l", "q5vo93ml"),
        NO_TURBO("p85pvv7l", "le2k6x5l");
        
        private final String varId;
        private final String valueId;
    
        SmrpgVariable(String varId, String valueId) {
            this.varId = varId;
            this.valueId = valueId;
        }
        
        @Override
        public String getVarId() {
            return varId;
        }
        
        @Override
        public String getValueId() {
            return valueId;
        }
    }
    
    public enum OotVariable implements Variable {
        GLITCHLESS_ALL_MAIN_QUESTS("7890o58w", "gq79gxpl"),
        GLITCHLESS_ANY("7890o58w", "xqk98nd1");
        
        private final String varId;
        private final String valueId;
        
        OotVariable(String varId, String valueId) {
            this.varId = varId;
            this.valueId = valueId;
        }
        
        @Override
        public String getVarId() {
            return varId;
        }
        
        @Override
        public String getValueId() {
            return valueId;
        }
    }
    
    public enum PapeCategory implements Category {
        PAPE_ANY_PERCENT_NO_ACE("Any% (No ACE)", "z276ozd0", PapeVariable.N64),
        PAPE_ALL_CARDS("All Cards", "zdn419kq", PapeVariable.N64),
        PAPE_GLITCHLESS("Glitchless", "jdrx75d6", PapeVariable.N64),
        PAPE_ANY_PERCENT_NO_PW("Any% (No PW)", "q25q7v2o", PapeVariable.N64),
        PAPE_ALL_BOSSES("All Bosses", "jdz8vgdv", PapeVariable.N64),
        PAPE_HUNDO("100%", "02qolyky", PapeVariable.N64),
        PAPE_REVERSE_ALL_CARDS("Reverse All Cards", "824gw1n2", PapeVariable.N64),
        PAPE_NMSB("Any% No Major Sequence Breaks", "9d8l6wq2", PapeVariable.N64),
        PAPE_ANY_PERCENT("Any%", "rklzlr82", PapeVariable.N64);
        
        private final String name;
        private final String id;
        private final Variable[] vars;
    
        PapeCategory(String name, String id, Variable... vars) {
            this.name = name;
            this.id = id;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.PAPER_MARIO;
        }
    
        @Override
        public Variable[] getVariables() {
            return vars;
        }
    
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum PapeMemesCategory implements Category {
        PAPE_PIGGIES("5 Golden Lil' Oinks", "7kjpw44k"),
        PAPE_ALL_BADGES("All Badges", "02qw5ej2"),
        PAPE_ALL_BLOOPS("All Bloops", "q257ywwd"),
        PAPE_ANY_NO_RNG("Any% No RNG", "wk6o43rk"),
        PAPE_BEAT_CHAPTER_1("Beat Chapter 1", "mkez638k"),
        PAPE_GLITCHLESS_101("Glitchless 101%", "xd11554d"),
        PAPE_MAILMAN("Mailman%", "02qo0yjk"),
        PAPE_SOAP_CAKE("Soap Cake%", "7dgrjl7k"),
        PAPE_STOP_N_SWOP("Stop 'n' Swop", "z27mv9g2");
        
        private final String name;
        private final String id;
        private final Variable[] vars;
        
        PapeMemesCategory(String name, String id, Variable... vars) {
            this.name = name;
            this.id = id;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.PAPER_MARIO_MEMES;
        }
    
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum OotCategory implements Category {
        OOT_ANY_PERCENT("Any%", "q25g198d"),
        OOT_GLITCHLESS_AMQ("Glitchless (All Main Quests)", "zd35jnkn", OotVariable.GLITCHLESS_ALL_MAIN_QUESTS),
        OOT_GLITCHLESS_ANY("Glitchless (Any%)", "zd35jnkn", OotVariable.GLITCHLESS_ANY);
        
        private final String name;
        private final String id;
        private final Variable[] vars;
        
        OotCategory(String name, String uri, Variable... vars) {
            this.name = name;
            this.id = uri;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.OOT;
        }
        
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum SmsCategory implements Category {
        SMS_ANY_PERCENT("Any%", "n2y3r8do"),
        SMS_ALL_EPISODES("All Episodes", "wkpmjjkr"),
        SMS_SHINES_79("79 Shines", "7kjqlxd3"),
        SMS_SHINES_96("96 Shines", "xk9n9y20"),
        SMS_SHINES_120("120 Shines", "z27o9gd0");
        
        private final String name;
        private final String id;
        private final Variable[] vars;
        
        SmsCategory(String name, String uri, Variable... vars) {
            this.name = name;
            this.id = uri;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.OOT;
        }
        
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum BugFablesCategory implements Category {
        BF_ANY_PERCENT("Any%", "n2yog3zd"),
        BF_HUNDO("100%", "w20g3v8k"),
        BF_GLITCHLESS("Any% Glitchless", "9kvo8g32"),
        BF_ALL_BOSSES("All Bosses", "ndx95yjd"),
        BF_ALL_CHAPTERS("All Chapters", "wdmxv5ok"),
        BF_ANY_MYSTERY("Any% MYSTERY?", "824g00m2"),
        BF_ANY_ALL_CODES("Any% All Codes", "n2yogx1d"),
        BF_ANY_DLL("Any% dll", "wk6nylx2");
        
        private final String name;
        private final String id;
        private final Variable[] vars;
        
        BugFablesCategory(String name, String uri, Variable... vars) {
            this.name = name;
            this.id = uri;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.OOT;
        }
        
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum TtydCategory implements Category {
        TTYD_ANY_PERCENT("Any%", "7kjp14k3"),
        TTYD_ALL_CRYSTAL_STARS("All Crystal Stars", "9d8v6l3k"),
        TTYD_GLITCHLESS("Glitchless", "jdz7963k"),
        TTYD_HUNDO("100%", "xk9g86d0"),
        TTYD_ALL_COLLECTIBLES("All Collectibles", "jdz8o5rd"),
        TTYD_MAX_UPGRADES("Max Upgrades", "z27lmw0d");
        
        private final String name;
        private final String id;
        private final Variable[] vars;
        
        TtydCategory(String name, String uri, Variable... vars) {
            this.name = name;
            this.id = uri;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.OOT;
        }
        
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    public enum SmrpgCategory implements Category {
        SMRPG_NORMAL_RTA_TURBO("Normal RTA", "beat-the-game", SmrpgVariable.TURBO);
        
        private final String name;
        private final String id;
        private final Variable[] vars;
    
        SmrpgCategory(String name, String uri, Variable... vars) {
            this.name = name;
            this.id = uri;
            this.vars = vars;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public Game getGame() {
            return Game.OOT;
        }
        
        @Override
        public Variable[] getVariables() {
            return vars;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
}

package api.src.leaderboard;

import jakarta.ws.rs.QueryParam;

import java.util.Map;

public class VariablesInput {
    @QueryParam("var-onvygrnm")
    private String fomtBride;
    
    @QueryParam("var-r8r4k47n")
    private String fomtGlitches;
    
    @QueryParam("var-7890o58w")
    private String ootGlitchlessType;
    
    @QueryParam("var-r8r5y2le")
    private String papeConsole;
    
    @QueryParam("var-p85pvv7l")
    private String smrpgTurbo;
    
    public VariablesInput(Map<String, String> vars) {
        fomtBride = vars.get("onvygrnm");
        fomtGlitches = vars.get("r8r4k47n");
        ootGlitchlessType = vars.get("7890o58w");
        papeConsole = vars.get("r8r5y2le");
        smrpgTurbo = vars.get("p85pvv7l");
    }
    
    public String getFomtBride() {
        return fomtBride;
    }
    
    public void setFomtBride(String fomtBride) {
        this.fomtBride = fomtBride;
    }
    
    public String getFomtGlitches() {
        return fomtGlitches;
    }
    
    public void setFomtGlitches(String fomtGlitches) {
        this.fomtGlitches = fomtGlitches;
    }
    
    public String getOotGlitchlessType() {
        return ootGlitchlessType;
    }
    
    public void setOotGlitchlessType(String ootGlitchlessType) {
        this.ootGlitchlessType = ootGlitchlessType;
    }
    
    public String getPapeConsole() {
        return papeConsole;
    }
    
    public void setPapeConsole(String papeConsole) {
        this.papeConsole = papeConsole;
    }
    
    public String getSmrpgTurbo() {
        return smrpgTurbo;
    }
    
    public void setSmrpgTurbo(String smrpgTurbo) {
        this.smrpgTurbo = smrpgTurbo;
    }
}

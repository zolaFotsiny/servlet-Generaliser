
package modele;

import java.util.HashMap;


public class ModeleView {
    private HashMap<String, Object> liste ;
    private String page ;
    
    public ModeleView() {
        
    }
    
    public HashMap<String, Object> getHash() {
        return this.liste ;
    }
    
    public String getPage() {
        return this.page ;
    }
    
    public void setHash(HashMap<String , Object> liste) {
        this.liste = liste ;
    }
    
    public void setPage(String page) {
        this.page = page ;
    }
}

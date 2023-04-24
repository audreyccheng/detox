package shield.benchmarks.freehealth.models;

public class Drug {

    private Integer id;
    private String name;
    private Integer strength;
    private Integer atcId;
    
    public Drug(Integer id, String name, Integer strength, Integer atcId) {
        this.id = id;
        this.name = name;
        this.strength = strength;
        this.atcId = atcId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }

    public Integer getAtcId() {
        return atcId;
    }

    public void setAtcId(Integer atcId) {
        this.atcId = atcId;
    }
}

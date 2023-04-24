package shield.benchmarks.freehealth.models;

public class Episode {
    private Integer episodeId;
    private Integer patientUid;
    private Integer isvalid;
    private String label;
    private String userdate;
    private String datecreation;
    private Integer creator;
    private Integer prior;

    public Episode(Integer patientUid, Integer isvalid, String label,
                   String userdate, String datecreation, Integer creator, Integer prior) {
        this.patientUid = patientUid;
        this.isvalid = isvalid;
        this.label = label;
        this.userdate = userdate;
        this.datecreation = datecreation;
        this.creator = creator;
        this.prior = prior;
    }

    public Episode(Integer episodeId, Integer patientUid, Integer isvalid, String label,
                   String userdate, String datecreation, Integer creator, Integer prior) {
        this.episodeId = episodeId;
        this.patientUid = patientUid;
        this.isvalid = isvalid;
        this.label = label;
        this.userdate = userdate;
        this.datecreation = datecreation;
        this.creator = creator;
        this.prior = prior;
    }

    public Integer getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Integer episodeId) {
        this.episodeId = episodeId;
    }

    public Integer getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(Integer patientUid) {
        this.patientUid = patientUid;
    }

    public Integer getIsvalid() {
        return isvalid;
    }

    public void setIsvalid(Integer isvalid) {
        this.isvalid = isvalid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUserdate() {
        return userdate;
    }

    public void setUserdate(String userdate) {
        this.userdate = userdate;
    }

    public String getDatecreation() {
        return datecreation;
    }

    public void setDatecreation(String datecreation) {
        this.datecreation = datecreation;
    }

    public Integer getCreator() {
        return creator;
    }

    public void setCreator(Integer creator) {
        this.creator = creator;
    }

    public Integer getPrior() {
        return prior;
    }

    public void setPrior(Integer prior) {
        this.prior = prior;
    }
}

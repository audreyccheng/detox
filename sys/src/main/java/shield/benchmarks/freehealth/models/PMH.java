package shield.benchmarks.freehealth.models;

public class PMH {
    private Integer id;
    private Integer patientUuid;
    private Integer userUuid;
    private Integer categoryId;
    private String label;
    private Integer valid;
    private String comment;
    private String creationdatetime;

    public PMH(Integer id, Integer patientUuid, Integer userUuid, Integer categoryId, String label,
               Integer valid, String comment, String creationdatetime) {
        this.id = id;
        this.patientUuid = patientUuid;
        this.userUuid = userUuid;
        this.categoryId = categoryId;
        this.label = label;
        this.valid = valid;
        this.comment = comment;
        this.creationdatetime = creationdatetime;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(Integer patientUuid) {
        this.patientUuid = patientUuid;
    }

    public Integer getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(Integer userUuid) {
        this.userUuid = userUuid;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getValid() {
        return valid;
    }

    public void setValid(Integer valid) {
        this.valid = valid;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreationdatetime() {
        return creationdatetime;
    }

    public void setCreationdatetime(String creationdatetime) {
        this.creationdatetime = creationdatetime;
    }
}

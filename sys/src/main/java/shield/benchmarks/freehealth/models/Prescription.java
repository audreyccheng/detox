package shield.benchmarks.freehealth.models;

public class Prescription {
    private Integer id;
    private Integer patientUuid;
    private String label;
    private Integer drug;
    private String dosage;
    private String startdate;
    private String enddate;
    private String datecreation;
    private Integer valid;
    private String comment;

    public Prescription(Integer id, Integer patientUuid, String label, Integer drug, String dosage,
                        String startdate, String enddate, String datecreation, Integer valid, String comment) {
        this.id = id;
        this.patientUuid = patientUuid;
        this.label = label;
        this.drug = drug;
        this.dosage = dosage;
        this.startdate = startdate;
        this.enddate = enddate;
        this.datecreation = datecreation;
        this.valid = valid;
        this.comment = comment;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getDrug() {
        return drug;
    }

    public void setDrug(Integer drug) {
        this.drug = drug;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    public String getDatecreation() {
        return datecreation;
    }

    public void setDatecreation(String datecreation) {
        this.datecreation = datecreation;
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
}

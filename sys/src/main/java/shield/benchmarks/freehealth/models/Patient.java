package shield.benchmarks.freehealth.models;

public class Patient {

    private Integer ID;
    private String name;
    private String dateOfBirth;
    private Integer userUuid;
    private String gender;
    private Integer zip;
    private String country;
    private String mails;
    private String tels;
    private String note;
    private Integer isActive;

    public Patient(String name, String dateOfBirth, Integer userUuid, String gender, Integer zip,
                   String country, String mails, String tels, String note, Integer isActive) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.userUuid = userUuid;
        this.gender = gender;
        this.zip = zip;
        this.country = country;
        this.mails = mails;
        this.tels = tels;
        this.note = note;
        this.isActive = isActive;
    }

    public Patient(Integer ID, String name, String dateOfBirth, Integer userUuid, String gender, Integer zip,
                   String country, String mails, String tels, String note, Integer isActive) {
        this.ID = ID;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.userUuid = userUuid;
        this.gender = gender;
        this.zip = zip;
        this.country = country;
        this.mails = mails;
        this.tels = tels;
        this.note = note;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(Integer userUuid) {
        this.userUuid = userUuid;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getZip() {
        return zip;
    }

    public void setZip(Integer zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMails() {
        return mails;
    }

    public void setMails(String mails) {
        this.mails = mails;
    }

    public String getTels() {
        return tels;
    }

    public void setTels(String tels) {
        this.tels = tels;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }
}

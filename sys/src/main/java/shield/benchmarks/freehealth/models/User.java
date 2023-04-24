package shield.benchmarks.freehealth.models;

public class User {
    private Integer userId;
    private Integer validity;
    private String login;
    private String password;
    private String lastlogin;
    private String name;
    private String title;
    private String gender;
    private String mail;
    private String language;

    public User(Integer userId, Integer validity, String login, String password, String lastlogin,
                String name, String title, String gender, String mail, String language) {
        this.userId = userId;
        this.validity = validity;
        this.login = login;
        this.password = password;
        this.lastlogin = lastlogin;
        this.name = name;
        this.title = title;
        this.gender = gender;
        this.mail = mail;
        this.language = language;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getValidity() {
        return validity;
    }

    public void setValidity(Integer validity) {
        this.validity = validity;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastlogin() {
        return lastlogin;
    }

    public void setLastlogin(String lastlogin) {
        this.lastlogin = lastlogin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}

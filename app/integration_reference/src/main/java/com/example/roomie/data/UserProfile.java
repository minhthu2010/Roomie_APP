package com.example.roomie.data;

public class UserProfile {
    private String name;
    private String occupation;
    private String schoolWorkplace;
    private String aboutMe;
    private String livingArea;
    private String phone;
    private String email;

    public UserProfile() {}

    public UserProfile(String name, String occupation, String schoolWorkplace, String aboutMe, String livingArea, String phone, String email) {
        this.name = name;
        this.occupation = occupation;
        this.schoolWorkplace = schoolWorkplace;
        this.aboutMe = aboutMe;
        this.livingArea = livingArea;
        this.phone = phone;
        this.email = email;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getSchoolWorkplace() { return schoolWorkplace; }
    public void setSchoolWorkplace(String schoolWorkplace) { this.schoolWorkplace = schoolWorkplace; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getLivingArea() { return livingArea; }
    public void setLivingArea(String livingArea) { this.livingArea = livingArea; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

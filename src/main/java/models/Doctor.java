package models;

public class Doctor extends User {
    private String speciality;
    private String addressCabine;
    private String licenseNumber;

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }
    public String getAddressCabine() { return addressCabine; }
    public void setAddressCabine(String addressCabine) { this.addressCabine = addressCabine; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
}
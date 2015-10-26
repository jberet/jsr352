/*
 * Copyright (c) 2014-2015 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */
 
package org.jberet.support.io;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.hibernate.validator.constraints.Email;

/**
 * A bean class for fake-person.csv.
 */

//let jackson directly access property fields so it always gets the correct property names without any uppercase or
// lowercase changes.  When jackson accesses properties with getter/setter, it forcifully decapitalize property names.
// Noticed this problem in org.jberet.support.io.ExcelReaderWriterTest.testPersonBeanType1_5()
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)

//define column order for JacksonCsvItemReader and JacksonCsvItemWriter, so this class can be used to create CSV schema
@JsonPropertyOrder({"number", "gender", "title", "givenName", "middleInitial", "surname",
        "streetAddress", "city", "state", "zipCode", "country", "countryFull",
        "emailAddress", "username", "password", "telephoneNumber", "mothersMaiden", "birthday",
        "CCType", "CCNumber", "CVV2", "CCExpires", "nationalID", "UPS", "color",
        "occupation", "company", "vehicle", "domain", "bloodType", "pounds", "kilograms", "feetInches", "centimeters",
        "GUID", "latitude", "longitude"})

public class Person {
    long number;
    String gender;
    String title;
    String givenName;
    char middleInitial;
    String surname;
    String streetAddress;
    String city;
    String state;
    String zipCode;
    String country;
    String countryFull;

    @Email
    String emailAddress;

    String username;
    String password;
    String telephoneNumber;
    String mothersMaiden;

    Date birthday;

    String CCType;
    String CCNumber;
    String CVV2;
    String CCExpires;
    String nationalID;
    String UPS;
    String color;
    String occupation;
    String company;
    String vehicle;
    String domain;
    String bloodType;
    BigDecimal pounds;
    BigDecimal kilograms;
    String feetInches;
    int centimeters;
    String GUID;
    double latitude;
    double longitude;

    public long getNumber() {
        return number;
    }

    public void setNumber(final long number) {
        this.number = number;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    public char getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(final char middleInitial) {
        this.middleInitial = middleInitial;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(final String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getCountryFull() {
        return countryFull;
    }

    public void setCountryFull(final String countryFull) {
        this.countryFull = countryFull;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(final String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getMothersMaiden() {
        return mothersMaiden;
    }

    public void setMothersMaiden(final String mothersMaiden) {
        this.mothersMaiden = mothersMaiden;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(final Date birthday) {
        this.birthday = birthday;
    }

    public String getCCType() {
        return CCType;
    }

    public void setCCType(final String CCType) {
        this.CCType = CCType;
    }

    public String getCCNumber() {
        return CCNumber;
    }

    public void setCCNumber(final String CCNumber) {
        this.CCNumber = CCNumber;
    }

    public String getCVV2() {
        return CVV2;
    }

    public void setCVV2(final String CVV2) {
        this.CVV2 = CVV2;
    }

    public String getCCExpires() {
        return CCExpires;
    }

    public void setCCExpires(final String CCExpires) {
        this.CCExpires = CCExpires;
    }

    public String getNationalID() {
        return nationalID;
    }

    public void setNationalID(final String nationalID) {
        this.nationalID = nationalID;
    }

    public String getUPS() {
        return UPS;
    }

    public void setUPS(final String UPS) {
        this.UPS = UPS;
    }

    public String getColor() {
        return color;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(final String occupation) {
        this.occupation = occupation;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(final String company) {
        this.company = company;
    }

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(final String vehicle) {
        this.vehicle = vehicle;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(final String bloodType) {
        this.bloodType = bloodType;
    }

    public BigDecimal getPounds() {
        return pounds;
    }

    public void setPounds(final BigDecimal pounds) {
        this.pounds = pounds;
    }

    public BigDecimal getKilograms() {
        return kilograms;
    }

    public void setKilograms(final BigDecimal kilograms) {
        this.kilograms = kilograms;
    }

    public String getFeetInches() {
        return feetInches;
    }

    public void setFeetInches(final String feetInches) {
        this.feetInches = feetInches;
    }

    public int getCentimeters() {
        return centimeters;
    }

    public void setCentimeters(final int centimeters) {
        this.centimeters = centimeters;
    }

    public String getGUID() {
        return GUID;
    }

    public void setGUID(final String GUID) {
        this.GUID = GUID;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(final double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(final double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Person{");
        sb.append("id=").append(number);
        sb.append(", gender='").append(gender).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", givenName='").append(givenName).append('\'');
        sb.append(", middleInitial='").append(middleInitial).append('\'');
        sb.append(", surname='").append(surname).append('\'');
        sb.append(", streetAddress='").append(streetAddress).append('\'');
        sb.append(", city='").append(city).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", zipCode='").append(zipCode).append('\'');
        sb.append(", country='").append(country).append('\'');
        sb.append(", countryFull='").append(countryFull).append('\'');
        sb.append(", emailAddress='").append(emailAddress).append('\'');
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", telephoneNumber='").append(telephoneNumber).append('\'');
        sb.append(", mothersMaiden='").append(mothersMaiden).append('\'');
        sb.append(", birthday='").append(getBirthday()).append('\'');
        sb.append(", CCType='").append(CCType).append('\'');
        sb.append(", CCNumber='").append(CCNumber).append('\'');
        sb.append(", CVV2='").append(CVV2).append('\'');
        sb.append(", CCExpires='").append(CCExpires).append('\'');
        sb.append(", nationalID='").append(nationalID).append('\'');
        sb.append(", UPS='").append(UPS).append('\'');
        sb.append(", color='").append(color).append('\'');
        sb.append(", occupation='").append(occupation).append('\'');
        sb.append(", company='").append(company).append('\'');
        sb.append(", vehicle='").append(vehicle).append('\'');
        sb.append(", domain='").append(domain).append('\'');
        sb.append(", bloodType='").append(bloodType).append('\'');
        sb.append(", pounds='").append(pounds).append('\'');
        sb.append(", kilograms='").append(kilograms).append('\'');
        sb.append(", feetInches='").append(feetInches).append('\'');
        sb.append(", centimeters='").append(centimeters).append('\'');
        sb.append(", GUID='").append(GUID).append('\'');
        sb.append(", latitude='").append(latitude).append('\'');
        sb.append(", longitude='").append(longitude).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

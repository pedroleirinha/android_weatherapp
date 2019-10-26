package com.thunder.coolweatherbro;

public class CityObjects {
    CityObject[] data;
}

class CityObject {
    int id;
    String city, name, country, countryCode, region;

    @Override
    public String toString(){
        return city + ", " + countryCode;
    }
}

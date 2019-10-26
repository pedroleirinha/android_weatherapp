package com.thunder.coolweatherbro;

public class WeatherObject {

    String name;
    int visibility;
    coord coord;
    weather[] weather;
    main main;
    sys sys;
    clouds clouds;
    wind wind;

}

class weather {
    int id;
    String main, description, icon;
}

class main {
    double temp, pressure, temp_min, temp_max;
    int humidity;
}

class clouds {
    int all;
}

class wind{
    double speed;
    double deg;
}

class sys {
    int type, id;
    String message, country;
    int sunrise, sunset;
}

class coord{
    double lon, lat;
}



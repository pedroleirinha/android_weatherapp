package com.thunder.coolweatherbro;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WeatherForecast {
    city city;
    list[] list;
}

class list {
    main main;
    weather[] weather;
    clouds clouds;
    wind wind;
    String dt_txt;
}

class city {
    String name, country;
    int id;
    coord coord;
}

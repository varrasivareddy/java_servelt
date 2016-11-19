package nilzor.httpperformance.entities;

import java.util.Collection;
import java.util.List;

public class TestServiceResponse {

    public String cod;
    public double message;
    public City city;
    public Collection<WeatherData> list;

    public static class City {
        public String id;
        public String name;
        public Coordinate coord;
        public String country;
        public int population;
        public Sys sys;
        public int cnt;
    }

    public static class WeatherData {
        public long dt;
        public Temperature temp;
        public float pressure;
        public int humidity;
        public Collection<WeatherDescription> weather;
        public float speed;
        public int deg;
        public int clouds;
        public float rain;
    }

    public static class Temperature {
        public double day;
        public double min;
        public double max;
        public double night;
        public double eve;
    }

    public static class Coordinate {
        public double lon;
        public double lat;
    }

    public static class Sys {
        public int population;
    }

    public static class WeatherDescription {
        public int id;
        public String main;
        public String description;
        public String icon;
    }
}


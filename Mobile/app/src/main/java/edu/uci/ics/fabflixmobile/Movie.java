package edu.uci.ics.fabflixmobile;

import java.util.ArrayList;
import java.util.List;


// Helper class for storing movies
public class Movie {
    private String id;
    private String title;
    private short year;
    private String director;
    private List<String> genres;


    public Movie (String id, String title, short year, String director){
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        genres = new ArrayList<String>();
    }

    public Movie(String title, short year) {
        this("", title, year, "");
    }

    public void addGenre(String genre){
        this.genres.add(genre);
    }


    public String getTitle() {
        return title;
    }

    public short getYear() {
        return year;
    }

    public String getId() {
        return id;
    }

    public String getDirector() {
        return director;
    }

    public List<String> getGenres() {
        return genres;
    }

    // return genres as one String
    public String getGenresStr(){
        String result = "";
        for (String genre: genres){
            result += genre + " ";
        }
        return result;
    }


}
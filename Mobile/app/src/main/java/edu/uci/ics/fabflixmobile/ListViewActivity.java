package edu.uci.ics.fabflixmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListViewActivity extends Activity {
    private String url = Constants.url;

    // set adapter as class attribute so we can update it later
    private MovieListViewAdapter adapter;

    private int resultCount = -1;
    private int currentPage = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        Button prevButton = findViewById(R.id.prev);
        Button nextButton = findViewById(R.id.next);

        // Needs to be declared final
        final ArrayList<Movie> movies = new ArrayList<>();
        this.getMovies(movies, currentPage);

        adapter = new MovieListViewAdapter(movies, this);
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(adapter);

        // Lead user to single-movies using this listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Movie movie = movies.get(position);
                String message = String.format("Clicked on position: %d, title: %s, %d", position, movie.getTitle(), movie.getYear());
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                //initialize the activity(page)/destination
                Intent SingleMoviePage = new Intent(ListViewActivity.this, SingleMovieActivity.class);
                //without starting the activity/page, nothing would happen
                startActivity(SingleMoviePage);
            }
        });

        // to do:
        // button disabling https://www.mysamplecode.com/2011/10/android-enable-disable-button.html
        // use resultCount to determine if disable or enable next
        // same with prev but check if currentPage == 1 to disable
        // use currentPage to also generate the next page # to be called
        // eg prevButton will have prevPage = currentPage-1;
        // then call this.getMovies(movies, prevPage);


        prevButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if (currentPage > 0) {
                    currentPage--;
                    getMovies(movies, currentPage);
                }
            };
        });

        nextButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view){
                currentPage++;
                getMovies(movies, currentPage);
            }}
        );
    }

    // calls API for movies and inserts into ArrayList
    public void getMovies(ArrayList<Movie> movies, int page) {

        movies.clear();

        // Use the same network queue across our application
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;

        String params = String.format("movies?results=20&pageNum=%d", page);

        //request type is POST
        final StringRequest moviesRequest = new StringRequest(Request.Method.GET, url + params, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("ListView.success", response);

                try {
                    JSONObject responseJson = new JSONObject(response);
                    parseMoviesJson(responseJson, movies);


                    // update adapter once this finishes
                    adapter.notifyDataSetChanged();

                } catch (JSONException e){
                    Log.d("ListView.error", e.toString());
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("ListView.error", error.toString());
                    }
                }) {
        };

        // !important: queue.add is where the login request is actually sent
        queue.add(moviesRequest);
    }


    // inserts moviesJson into array list of movies
    public void parseMoviesJson(JSONObject moviesJson, ArrayList<Movie> movies) throws JSONException {
        // moviesJSON = {"movies": JSONArray of movies, "resultCount": int count}
        JSONArray moviesArray = moviesJson.getJSONArray("movies");

        // iterate over movies
        for (int i = 0; i < moviesArray.length(); ++i){
            /* movieJSON = {"movie_title": String, "movie_year": String, "movie_director": String,
             * "movie_genres": JSONArray of JSONObjects, "movie_stars": JSONArray of JSONObjects}
             */
            JSONObject movieJson = moviesArray.getJSONObject(i);

            // Extract JSON data
            String id = movieJson.getString("movie_id");
            String title = movieJson.getString("movie_title");
            Short year = (short) Integer.parseInt(movieJson.getString("movie_year"));
            String director = movieJson.getString("movie_director");

            Movie movie = new Movie(id, title, year, director);
            JSONArray genresArray = movieJson.getJSONArray("movie_genres");
            for (int g = 0; g < genresArray.length(); ++g){
                JSONObject genreJson = genresArray.getJSONObject(g);
                String genreName = genreJson.getString("genre_name");
                movie.addGenre(genreName);
            }

            // Insert into movie object and array
            movies.add(movie);
        }

    }


}
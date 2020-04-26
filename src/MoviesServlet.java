import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


// Declaring a WebServlet called MoviesServlet, which maps to url "/api/movies"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.xml
    @Resource(name = "jdbc/moviedb")
    private DataSource dataSource;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */




    // Build query for MYSQL from request parameters
    private String buildQuery(HttpServletRequest request){

        BuildQuery query = new BuildQuery();
        query.setSelectStr("title, year, director, movies_with_rating.id, rating");

        // add FROM conditions
        query.addFromTables("movies_with_rating");

        // have to join other tables to search for stars and genre
        query.addFromTables("JOIN (stars JOIN stars_in_movies ON id = starId) ON movies_with_rating.id = stars_in_movies.movieId");
        query.addFromTables("JOIN (genres JOIN genres_in_movies ON id = genreId) ON movies_with_rating.id = genres_in_movies.movieId");

        // Add the WHERE conditions from parameters
        query.addParameters(request.getParameterMap());

        query.append("LIMIT 20");

        return query.getQuery();

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try {
            // Get a connection from dataSource
            Connection dbcon = dataSource.getConnection();

            // Build query
            String query = buildQuery(request);
            System.out.println("query = " + query);

            // Perform the query using the helper class (Execute Query
            ExecuteQuery result = new ExecuteQuery(dbcon, query);
            ResultSet rs = result.execute();

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movie_id = rs.getString("id");
                String movie_title = rs.getString("title");
                String movie_year = rs.getString("year");
                String movie_director = rs.getString("director");
                String movie_rating = rs.getString("rating");

                // Movie stars and genres will be stored as arrays
                JsonArray jsonStars = new JsonArray();
                JsonArray jsonGenres = new JsonArray();

                // additional queries for genres and stars
                try {
                    // Get list of first three genres
                    String genreQuery = String.format("SELECT genres.name FROM genres JOIN genres_in_movies ON (genres.id = genreId AND movieId = \"%s\") LIMIT 3", movie_id);
                    ExecuteQuery genreResult = new ExecuteQuery(dbcon, genreQuery);
                    ResultSet genreSet = genreResult.execute();

                    // get list of first three stars

                    String starsQuery = String.format("SELECT * FROM stars JOIN stars_in_movies ON (stars.id = starId AND movieId = \"%s\") LIMIT 3", movie_id);
                    ExecuteQuery starsResult = new ExecuteQuery(dbcon, starsQuery);
                    ResultSet starsSet = starsResult.execute();

                    // assemble genre list (as a JSON object)
                    while (genreSet.next()){
                        JsonObject jsonGenre = new JsonObject();
                        String genre_name = genreSet.getString("name");
                        jsonGenre.addProperty("genre_name", genre_name);

                        // add to the JSON array of stars
                        jsonGenres.add(jsonGenre);
                    }

                    // stars list (as JSON object)
                    while(starsSet.next()){
                        JsonObject jsonStar = new JsonObject();
                        String star_id = starsSet.getString("starId");
                        String star_name = starsSet.getString("name");
                        jsonStar.addProperty("star_id", star_id);
                        jsonStar.addProperty("star_name", star_name);

                        // add to the JSON array of stars
                        jsonStars.add(jsonStar);
                    }

                    // free resources
                    genreResult.close(); starsResult.close();


                } catch (Exception e){
                    System.out.println(e.getMessage());
                }


                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("movie_id", movie_id);
                jsonObject.addProperty("movie_title", movie_title);
                jsonObject.addProperty("movie_year", movie_year);
                jsonObject.addProperty("movie_director", movie_director);
                jsonObject.addProperty("movie_rating", movie_rating);
                jsonObject.add("movie_genres", jsonGenres);
                jsonObject.add("movie_stars", jsonStars);
                jsonArray.add(jsonObject);
            }
            
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            // free resources
            result.close();
            dbcon.close();
        } catch (Exception e) {
        	e.printStackTrace();
        	System.out.println("error");
			// write error message JSON object to output
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("errorMessage", e.getMessage());
			out.write(jsonObject.toString());

			// set reponse status to 500 (Internal Server Error)
			response.setStatus(500);

        }
        out.close();

    }
}

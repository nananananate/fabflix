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
    @Resource(name = "jdbc/testa")
    private DataSource dataSource;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try {
            // Get a connection from dataSource
            Connection dbcon = dataSource.getConnection();

            // Declare our statement
            Statement statement = dbcon.createStatement();

            String query = "SELECT * FROM movies_with_rating limit 20";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movie_id = rs.getString("id");
                String movie_title = rs.getString("title");
                String movie_year = rs.getString("year");
                String movie_director = rs.getString("director");
                String movie_rating = rs.getString("rating");
                String movie_genres = "";
                JsonArray jsonStars = new JsonArray();

                // additional queries for genres and stars
                try {
                    // Get list of first three genres
                    Statement genreStatement = dbcon.createStatement();
                    String genreQuery = String.format("SELECT genres.name FROM genres JOIN genres_in_movies ON (genres.id = genreId AND movieId = \"%s\") LIMIT 3", movie_id);
                    ResultSet genreSet = genreStatement.executeQuery(genreQuery);

                    // get list of first three stars
                    Statement starsStatement = dbcon.createStatement();
                    String starsQuery = String.format("SELECT * FROM stars JOIN stars_in_movies ON (stars.id = starId AND movieId = \"%s\") LIMIT 3", movie_id);
                    ResultSet starsSet = starsStatement.executeQuery(starsQuery);

                    // assemble genre list (just a string)
                    while (genreSet.next()){
                        movie_genres += genreSet.getString("name") + ", ";
                    }

                    // stars list (as JSON object)

                    while(starsSet.next()){
                        JsonObject jsonStar = new JsonObject();
                        String star_id = starsSet.getString("id");
                        String star_name = starsSet.getString("name");
                        jsonStar.addProperty("star_id", star_id);
                        jsonStar.addProperty("star_name", star_name);
                        jsonStars.add(jsonStar);
                    }


                    movie_genres = movie_genres.substring(0, movie_genres.length() - 2);
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
                jsonObject.addProperty("movie_genres", movie_genres);
                jsonObject.add("movie_stars", jsonStars);
                jsonArray.add(jsonObject);
            }
            
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            rs.close();
            statement.close();
            dbcon.close();
        } catch (Exception e) {
        	
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

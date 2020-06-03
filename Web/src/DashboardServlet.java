import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import utilities.MyQuery;
import utilities.MyUtils;


import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard")
public class DashboardServlet extends HttpServlet {
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */

    private boolean notEmpty(String s){
        return (s != null && !s.equals(""));
    }

    // check if star already exists (NOT SUPPOSED TO)
    private boolean starExists(String fullName, Connection dbcon){
        MyQuery query = new MyQuery(dbcon, "SELECT * FROM stars");
        query.append("WHERE stars.name = ?", fullName);
        query.execute();
        return query.exists();
    }

    // Check if movie exists
    private boolean movieExists(String title, String year, String director, Connection dbcon){
        MyQuery query = new MyQuery(dbcon, "SELECT * FROM movies");
        query.addWhereConditions("%s = ?", "title", title);
        query.addWhereConditions("%s = ?", "year", year);
        query.addWhereConditions("%s = ?", "director", director);
        query.execute();
        return query.exists();
    }


    // add new star to database
    private void addStar(HttpServletRequest request, JsonObject responseJson){
        try{
            String fullName = request.getParameter("fullName");
            String birthYear = request.getParameter("birthYear");

            // Check if full name specified
            if (notEmpty(fullName)) {
                Connection dbcon = MyUtils.getWriteConnection();

                // NOT SUPPOSED TO CHECK IF IT ALREADY EXISTS
                if (false) {
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", "star already exists");
                }
                // else, we're good to add the new star
                else {
                    PreparedStatement callStatement = dbcon.prepareStatement("CALL add_star(?, ?)");
                    callStatement.setString(1, fullName);

                    // year is optional
                    int yearInt = 0;
                    if (notEmpty(birthYear)) {
                        yearInt = Integer.parseInt(birthYear);
                        callStatement.setInt(2, yearInt);
                    }
                    // Set year to null if it is not specified
                    else{
                        callStatement.setNull(2, java.sql.Types.INTEGER);
                    }

                    callStatement.execute();
                    callStatement.close();
                    dbcon.close();

                    responseJson.addProperty("status", "success");
                    responseJson.addProperty("message", "successfully added ");
                }
            }
            else{
                responseJson.addProperty("status", "fail");
                responseJson.addProperty("message", "Full name not specified");
            }

        }
        catch (Exception e){
            e.printStackTrace();
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "database error");
        }
    }


    // add new movie
    private void addMovie(HttpServletRequest request, JsonObject responseJson){
        try{
            String title = request.getParameter("title");
            String year = request.getParameter("year");
            String director = request.getParameter("director");
            String starName = request.getParameter("star_name");
            String genreName = request.getParameter("genre_name");

            // All parameters are required
            if (notEmpty(title) && notEmpty(year) && notEmpty(director) && notEmpty(genreName) && notEmpty(starName)){
                Connection dbcon = MyUtils.getWriteConnection();

                // Check if movie already exists
                if (this.movieExists(title, year, director, dbcon)){
                    responseJson.addProperty("status", "fail");
                    responseJson.addProperty("message", "Movie already exists");
                }
                // if movie doesn't exist, we're clear to insert
                else{
                    PreparedStatement callStatement = dbcon.prepareStatement("CALL add_movie(?, ?, ?, ?, ?)");
                    callStatement.setString(1, title);
                    callStatement.setString(2, year);
                    callStatement.setString(3, director);
                    callStatement.setString(4, starName);
                    callStatement.setString(5, genreName);

                    callStatement.execute();
                    callStatement.close();
                    dbcon.close();

                    responseJson.addProperty("status", "success");
                    responseJson.addProperty("message", "New movie successfully added");
                }
            }
            else{
                responseJson.addProperty("status", "fail");
                responseJson.addProperty("message", "Form incomplete -- all fields are required");
            }

        }
        catch (Exception e){
            e.printStackTrace();
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "database error");
        }
    }


    // insert respective data to database from the user-submitted forms
    private void insertToDatabase(HttpServletRequest request, JsonObject responseJson){
        String formType = request.getParameter("form");

        // Check which form was submitted
        if (formType.equals("add_star")){
            this.addStar(request, responseJson);
        }
        else if (formType.equals("add_movie")){
            this.addMovie(request, responseJson);
        }
        else{
            responseJson.addProperty("status", "fail");
            responseJson.addProperty("message", "no form submitted / invalid form");
        }
    }


    // Post request, from the submitted forms
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JsonObject responseJsonObject = new JsonObject();

        insertToDatabase(request, responseJsonObject);

        response.getWriter().write(responseJsonObject.toString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        try {
            System.out.println("GET attempt");
            // Get a connection from dataSource
            Connection connection = MyUtils.getReadConnection();

            // the following codeblock has code adapted from:
            // https://www.progress.com/blogs/jdbc-tutorial-extracting-database-metadata-via-jdbc-driver

            DatabaseMetaData md = connection.getMetaData();

            JsonArray resultSet = new JsonArray();

            ResultSet tables = md.getTables(null, null, null, new String[]{"TABLE"});

            // Get the table names
            while (tables.next()) {
                // create the table entry to hold the information about the table
                JsonObject tableEntry = new JsonObject();
                String tableName = tables.getString("TABLE_NAME");
                // Store the table name
                tableEntry.addProperty("tableName", tableName);
                JsonArray tableColumns = new JsonArray();

                // for each table, get the columns associated with it
                ResultSet columns = md.getColumns(null, null, tableName, null);
                while (columns.next()) {
                    JsonObject columnEntry = new JsonObject();
                    String columnName = columns.getString("COLUMN_NAME");
                    String datatype = columns.getString("TYPE_NAME");
                    String dataSize = columns.getString("COLUMN_SIZE");

                    columnEntry.addProperty("columnName", columnName);
                    columnEntry.addProperty("dataType", datatype);
                    columnEntry.addProperty("dataSize", dataSize);

                    tableColumns.add(columnEntry);
                }
                // store the column data into the tableEntry
                tableEntry.add("columns", tableColumns);

                // store the table entry to the result set
                resultSet.add(tableEntry);
            }

            // End code block

            JsonObject resultJson = new JsonObject();
            resultJson.add("tables", resultSet);

            tables.close();
            connection.close();

            /*
            ResultJson format:
                { tables: array of tableEntries }
            tableEntry format:
                { tableName: string (table name),
                    columns: array of columnEntry
                }
            columnEntry format:
                { columnName: string,
                    dataType: string,
                }
             */

            out.write(resultJson.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            System.out.println("Get finished");

        } catch (Exception e) {
            // write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // set reponse status to 500 (Internal Server Error)
            response.setStatus(500);
        }
    }

}

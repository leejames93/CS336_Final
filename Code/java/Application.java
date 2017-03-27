import static spark.Spark.*;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sql2o.Connection;
import org.sql2o.data.Table;
import spark.ModelAndView;
import spark.servlet.SparkApplication;
import spark.staticfiles.StaticFilesConfiguration;
import spark.template.velocity.VelocityTemplateEngine;

public class Application implements SparkApplication {
    public static void main(String[] args) {
        new Application().init();
    }

    public void init() {
        port(9090);

        String layout = "templates/layout.vtl";


        Connection con = DB.sql2o.open();

        get("/", (request, response) -> {
            Map model = new HashMap();
            String query = "SELECT Year, Unemployment, Party\n" +
                    "FROM election.Economy, election.President\n" +
                    "WHERE Year = Year_Elected";
            List<Map<String,Object>> result = tableToList(con.createQuery(query).executeAndFetchTable());

            model.put("json", listmap_to_json_string(result));

            String q2 = "SELECT p.Year_Elected AS From_Year, c.Year AS To_Year, \n" +
                    "\te.GDP_Growth, c.party AS Candidate, p.party AS President, \n" +
                    "    cg1.majority AS House_Majority, cg2.majority AS Senate_Majority \n" +
                    "    \n" +
                    "FROM President p, Economy e, Candidate c, Congress cg1, Congress cg2\n" +
                    "WHERE e.Year = p.Year_Elected AND p.Year_Elected = c.Year-4 \n" +
                    "\tAND c.result = 'Win'\n" +
                    "    AND cg1.year = p.Year_Elected AND cg2.year = p.Year_Elected\n" +
                    "    AND cg1.chamber = 'House' AND cg2.chamber = 'Senate'\n";
            result = tableToList(con.createQuery(q2).executeAndFetchTable());
            model.put("q2", listmap_to_json_string(result));

            String q3 = "SELECT p.Year_Elected, p.party AS President, c.party AS Candidate, e.GDP_Growth\n" +
                    "FROM President p, Economy e, Candidate c\n" +
                    "WHERE e.Year = p.Year_Elected AND p.Year_Elected = c.Year-4 AND c.result = 'Win'\n";
            result = tableToList(con.createQuery(q3).executeAndFetchTable());
            model.put("q3", result);

            model.put("template", "templates/hello.vtl" );
            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        get("/presidents", (request, response) -> {
            Map model = new HashMap();
            String q1 = "SELECT Name, Party, Year_Elected\n" +
                    "FROM election.President;";
            List<Map<String,Object>> result = tableToList(con.createQuery(q1).executeAndFetchTable());
            model.put("q1", result);

            String q2 = "SELECT p1.Year_Elected AS Year, p1.name AS Current_President, p1.Party AS President_Party,\n" +
                    "    \tc1.Majority AS House_Majority, c2.majority as Senate_Majority,\n" +
                    "\tcd.name AS Elected_President, cd.party AS Elect_Party\n" +
                    "FROM President p1, Congress c1, Congress c2, Candidate cd\n" +
                    "WHERE p1.Year_Elected = c1.Year AND p1.Year_Elected = c2.Year \n" +
                    "\tAND c1.Chamber = 'House' AND c2.Chamber = 'Senate'\n" +
                    "\tAND cd.Year = p1.Year_Elected+4\n" +
                    "    \tAND cd.result = 'Win'\n";
            result = tableToList(con.createQuery(q2).executeAndFetchTable());
            model.put("q2", result);

            model.put("template", "templates/presidents.vtl" );
            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        get("/demoquery", (request, response) -> {
            Map model = new HashMap();
            model.put("template", "templates/demoquery.vtl");
            String option = request.queryParams("option");
            String q = "";
            if(option.equals("Sex")) {
                q = "SELECT DISTINCT c1.Year, c1.name AS Females_Voted_For, \n" +
                        "\tc2.name AS Males_Voted_For\n" +
                        "FROM Demographic d1, Demographic d2, Candidate c1, Candidate c2\n" +
                        "WHERE d1.Voter = 'Female' AND d1.Party = c1.Party AND d1.Year = c1.Year AND\n" +
                        "\td2.Voter = 'Male' AND d2.Party = c2.Party AND d2.Year = c2.Year AND\n" +
                        "  \tc1.Year = c2.Year\n";
            } else if(option.equals("Race")) {
                q = "SELECT DISTINCT c1.Year, c1.name AS Whites_Voted_For, \n" +
                        "\tc2.name AS Blacks_Voted_For,\n" +
                        "    c3.name AS Hispanics_Voted_For\n" +
                        "FROM Demographic d1, Demographic d2, Demographic d3,\n" +
                        "\tCandidate c1, Candidate c2, Candidate c3\n" +
                        "WHERE d1.Voter = 'White' AND d1.Party = c1.party AND c1.Year = d1.Year AND\n" +
                        "   \td2.Voter = 'Black' AND d2.party = c2.party AND c2.Year = d2.Year AND\n" +
                        "\td3.Voter = 'Hispanic' AND d3.party = c3.party AND c3.Year = d3.Year AND\n" +
                        "\tc1.Year = c2.Year AND c2.Year = c3.Year\n";
            } else if(option.equals("Education")) {
                q = "SELECT DISTINCT c1.Year, c1.name AS Grade_School, \n" +
                        "\tc2.name AS High_School,\n" +
                        "    \tc3.name AS College\n" +
                        "FROM Demographic d1, Demographic d2, Demographic d3,\n" +
                        "\tCandidate c1, Candidate c2, Candidate c3\n" +
                        "WHERE d1.Voter = 'GradeSchool' AND d1.Party = c1.party AND c1.Year = d1.Year AND\n" +
                        "d2.Voter = 'HighSchool' AND d2.party = c2.party AND c2.Year = d2.Year AND\n" +
                        "\td3.Voter = 'College' AND d3.party = c3.party AND c3.Year = d3.Year AND\n" +
                        "\tc1.Year = c2.Year AND c2.Year = c3.Year\n";
            }
            List<Map<String, Object>> result = tableToList(con.createQuery(q).executeAndFetchTable());
            model.put("q1",result);
            model.put("option",option);
            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        get("/Random", (req, res) -> {
            Map model = new HashMap();
            List<Map<String, Object>> result;

            String q1 = "SELECT c1.name\n" +
                    "FROM Candidate c1, Candidate c2\n" +
                    "WHERE c1.popular > c2.popular \n" +
                    "\tAND c1.Year = c2.Year \n" +
                    "\tAND c1.result = 'lose'\n";
            result = tableToList(con.createQuery(q1).executeAndFetchTable());
            model.put("q1",result);

            String q2 = "SELECT c1.Year AS Year, c1.name AS Incumbent_Candidate,\n" +
                    "\tc2.name AS New_Candidate, c2.Result AS New_Candidate_Result\n" +
                    "FROM Candidate c1, Candidate c2\n" +
                    "WHERE c1.Year = c2.Year \n" +
                    "AND c1.RE = 'Yes'\n" +
                    "AND c1.name<>c2.name\n";
            result = tableToList(con.createQuery(q2).executeAndFetchTable());
            model.put("q2",result);

            String q3 = "SELECT s1.name, COUNT(*) AS Number_of_Times_Voted_For_Winner\n" +
                    "FROM States s1, Candidate c1\n" +
                    "WHERE c1.result = 'Win' \n" +
                    "\tAND s1.Year = c1.Year \n" +
                    "\tAND s1.Party = c1.Party\n" +
                    "GROUP BY s1.name\n" +
                    "ORDER BY COUNT(*) DESC\n";
            result = tableToList(con.createQuery(q3).executeAndFetchTable());
            model.put("q3",result);

            model.put("template", "templates/random.vtl" );
            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        get("/Demographic", (request, response) -> {
            Map model = new HashMap();
            List<Map<String, Object>> result;
            model.put("template", "templates/demographic.vtl");

            String q1 = "SELECT d.Voter, COUNT(*) AS Number_of_Times_Voted_For_Winner\n" +
                    "FROM Demographic d, Candidate c\n" +
                    "WHERE c.result = 'Win' \n" +
                    "\tAND d.Year = c.Year \n" +
                    "\tAND d.Party = c.Party\n" +
                    "GROUP BY d.Voter\n" +
                    "ORDER BY COUNT(*) DESC\n";
            result = tableToList(con.createQuery(q1).executeAndFetchTable());
            model.put("q1",result);

            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());

        get("/Economy", (request, response) -> {
            Map model = new HashMap();
            List<Map<String, Object>> result;
            model.put("template", "templates/economy.vtl");
            String q1 = "SELECT p1.Year_Elected AS From_Year,\n" +
                    "\tcd.Year AS To_Year,\n" +
                    "\te1.Unemployment_Status AS Unemployment_Went,\n" +
                    "\tp1.name AS Under_President, p1.Party AS President_Party,\n" +
                    "\tcd.name AS Elected, cd.Party AS Elected_Party\n" +
                    "FROM President p1, Candidate cd, Economy e1\n" +
                    "WHERE e1.Year = cd.Year AND p1.Year_Elected = cd.Year-4 AND cd.result = 'Win'\n" +
                    "order by From_Year\n";
            result = tableToList(con.createQuery(q1).executeAndFetchTable());
            model.put("q1",result);

            String q2 = "SELECT p.Year_Elected AS From_Year, c.Year AS To_Year, \n" +
                    "\te.GDP_Growth, c.party AS Candidate, p.party AS President, \n" +
                    "    cg1.majority AS House_Majority, cg2.majority AS Senate_Majority \n" +
                    "    \n" +
                    "FROM President p, Economy e, Candidate c, Congress cg1, Congress cg2\n" +
                    "WHERE e.Year = p.Year_Elected AND p.Year_Elected = c.Year-4 \n" +
                    "\tAND c.result = 'Win'\n" +
                    "    AND cg1.year = p.Year_Elected AND cg2.year = p.Year_Elected\n" +
                    "    AND cg1.chamber = 'House' AND cg2.chamber = 'Senate'\n";
            result = tableToList(con.createQuery(q2).executeAndFetchTable());
            model.put("q2",result);

            return new ModelAndView(model, layout);
        }, new VelocityTemplateEngine());
    }

    private static List<Map<String, Object>> tableToList(Table t) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < t.rows().size(); i++) {
            Map<String, Object> map = new HashMap<>();
            for (int j = 0; j < t.columns().size(); j++) {
                map.put(t.columns().get(j).getName(), t.rows().get(i).getObject(j));
            }
            mapList.add(map);

        }
        return mapList;
    }

    public String listmap_to_json_string(List<Map<String, Object>> list)
    {
        JSONArray json_arr=new JSONArray();
        for (Map<String, Object> map : list) {
            JSONObject json_obj=new JSONObject();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                try {
                    json_obj.put(key,value);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            json_arr.put(json_obj);
        }
        return json_arr.toString();
    }



}
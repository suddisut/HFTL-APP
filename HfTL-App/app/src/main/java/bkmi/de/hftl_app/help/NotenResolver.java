package bkmi.de.hftl_app.help;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import bkmi.de.hftl_app.Database.NotenDB;
import bkmi.de.hftl_app.Database.NotenTabelle;

/**
 * Klasse die eine Verbindung zu HIS/QIS aufbaut und dort die Noten abfragt
 */
public class NotenResolver {

    Context context;
    URL url;
    String s;   //kann wieder gelöscht werden, nur für Testzwecke
    Document doc;
    HttpsURLConnection connection;
    CookieManager cookieManager;

    public NotenResolver(Context context) throws wrongUserdataException{
        this.context = context;

        s="";   //Testzwecke

        //KEKSE!!!
        cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        init();
    }

    //Login im QIS durchführen
    private void init() throws wrongUserdataException{

        String user;
        String password;

        //Zugriff auf die Daten in den Einstellungen
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        //Entschlüsseln
        TextSecure ts;
        try {
            ts = new TextSecure(context);
            password = ts.decrypt(sp.getString("password", ""));
            user = ts.decrypt(sp.getString("username", ""));

            //Hier erfolgt der eigentliche Login über eine HTTP Post methode
            String urlParameters = "asdf=" + user + "&fdsa=" + password;

            url = new URL("https://qisweb.hispro.de/tel/rds?state=user&type=1&;category=auth.login&startpage=portal.vm&breadCrumbSource=portal");
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());

            writer.write(urlParameters);
            writer.flush();
            connection.getContent();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //testen ob Login erfolgreich
        if(!testeZugang()) throw new wrongUserdataException();

    }

    //Zugriff auf Noten
    public boolean getNoten() throws wrongUserdataException{

        Element element;
        Elements elements;

        //testen ob Login erfolgreich
        if(!testeZugang()) throw new wrongUserdataException();

        //Navigation zur richtigen Seite
        String notenspiegelInfo= qisNavigation();

        //eigentliche Abfrage der Noten --- es werden nur die Elemente ausgewertet die "/QIS/images//his_info3.gif" als src haben. Siehe Html-Code von QIS
        url = null;
        try {
            NotenDB notenDB = NotenDB.getInstance(context);

            SQLiteDatabase sqLiteDatabase = notenDB.getWritableDatabase();
            sqLiteDatabase.delete("notendatenbank", null, null);
            SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement(NotenTabelle.SQL_INSERT_Noten);
            url = new URL(notenspiegelInfo);
            connection = (HttpsURLConnection) url.openConnection();
            doc = Jsoup.parse(connection.getInputStream(), "UTF-8", notenspiegelInfo);

            elements = doc.getElementsByAttributeValue("src", "/QIS/images//his_info3.gif");
            if(elements.size()==0){
                sqLiteStatement.bindString(1 , "keine Noten vorhanden");
                sqLiteStatement.bindString(2, "");
                sqLiteStatement.bindString(3 , "");
                sqLiteStatement.bindString(4, "");

                sqLiteStatement.executeInsert();

            }
            for (Element ele : elements) {
                //Zwei Elemente Nach oben, siehe Html-Code
                element = ele.parent().parent().parent();

                /*Speichert die Werte in der "notendatenbank"
                Wert 1 --> Fach
                Wert 2 --> Semester
                Wert 3 --> Note
                Wert 4 --> Link zum Notenspiegel
                 */
                sqLiteStatement.bindString(1 , element.child(1).ownText());
                sqLiteStatement.bindString(2, element.child(2).ownText());
                sqLiteStatement.bindString(3 , element.child(3).ownText());
                sqLiteStatement.bindString(4, element.child(3).child(0).attr("href"));

                sqLiteStatement.executeInsert();  //Schreiben der Werte in die DB
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;

    }

    //Navigiert zu Notenspiegel --> Info
    //gibt die URL als String zurück
    private String qisNavigation() {
        Elements e;
        String seite = "https://qisweb.hispro.de/tel/rds?state=user&type=0&category=menu.browse&breadCrumbSource=portal&startpage=portal.vm";
        try {
             url = new URL(seite);
            connection = (HttpsURLConnection) url.openConnection();
            doc = Jsoup.parse(connection.getInputStream(), "UTF-8", seite);
            e = doc.getElementsByAttribute("href");
            seite = e.get(21).attr("href");//Suche Prüfungsverwaltung...könnte noch überarbeitet werden damit es nicht so statisch ist

            url = new URL(seite);
            connection = (HttpsURLConnection) url.openConnection();
            doc = Jsoup.parse(connection.getInputStream(), "UTF-8", seite);
            e = doc.getElementsByAttribute("href");
            seite= e.get(30).attr("href");//Suche Notenspiegel

            url = new URL(seite);
            connection = (HttpsURLConnection) url.openConnection();
            doc = Jsoup.parse(connection.getInputStream(), "UTF-8", seite);
            e = doc.getElementsByAttribute("href");
            return e.get(31).attr("href");//Suche Leistung anzeigen


            // iterate HttpCookie object
        } catch (IOException e1) {
            e1.printStackTrace();

        }

        return "FEHLER";
    }

    //Die Methode kontrolliert ob der Login auf der QIS-Seite erfolgreich war
    //falls Login erfolgreich wird true zurückgegeben
    private boolean testeZugang(){
        Elements e;
        String seite = "https://qisweb.hispro.de/tel/rds?state=user&type=0&category=menu.browse&breadCrumbSource=portal&startpage=portal.vm";
        try {
            url = new URL(seite);
            connection = (HttpsURLConnection) url.openConnection();
            doc = Jsoup.parse(connection.getInputStream(), "UTF-8", seite);
            e = doc.getElementsByAttribute("href");
            if(e.size()<22) {
                return false;
            }

        }
        catch (Exception e1){
            e1.printStackTrace();
            return false;
        }
        return true;
    }
}


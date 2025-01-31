package bkmi.de.hftl_app.help.Resolver;




import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

import bkmi.de.hftl_app.help.Events.HftlEvent;

/**
 * Klasse um die News von der HftL-Seite zu laden
 */
public class NewsResolver {

    Document doc;
    ArrayList<HftlEvent> termine;

    /*
    * Konstruktor initialisiert die übergebene URL als Document
     */
    public NewsResolver(String url) throws IOException {
        getHtmlAsDoc(url);
        getHftlEvent();
    }

    private boolean getHtmlAsDoc(String url) throws IOException {
        doc = Jsoup.connect(url).timeout(10*1000).get();
        return true;

    }

    // ----- Funktionen für das NewsFragment -----

    /*
    * Erzeugt eine Liste mit den nächsten Terminen von der Hftl-Startseite
     */
    private void getHftlEvent(){
        termine= new ArrayList<>();

        Elements elements = doc.getElementsByClass("news-list-item");
        for(int i=0; i<elements.size(); i++){
            HftlEvent termin = new HftlEvent();

            termin.time= elements.get(i).child(1).text();                                                       //Parsen des Datums
            termin.url="https://www.hft-leipzig.de/"+elements.get(i).child(2).child(0).child(0).attr("href");   //Parsen des Links zur News
            termin.head=elements.get(i).child(2).child(0).child(0).ownText();                                   //Parsen der Überschrift
            termin.text=elements.get(i).child(2).child(1).ownText();                                            //Parsen des Textes

            termine.add(termin);
        }
    }

    public String[] writeNewsListDate(){
        int i = 0;
        String[] s = new String[termine.size()];
        for(HftlEvent termin:termine ){
            s[i++]=termin.time;
        }
        return s;
    }

    public String[] writeNewsListHeadline(){
        int i = 0;
        String[] s = new String[termine.size()];
        for(HftlEvent termin:termine ){
            s[i++]=termin.head;
        }
        return s;
    }

    public String[] writeNewsListContent(){
        int i = 0;
        String[] s = new String[termine.size()];
        for(HftlEvent termin:termine ){
            s[i++]=termin.text;
        }
        return s;
    }

    /*
    * Gibt die URL des Termines an Position "position" zurück, wird für die Detailanzeige benötigt.
     */
    public String getUrlAsString(int position){
        return termine.get(position).url;
    }


   /* Methode sucht nach Relativ-Links und setzt die URL der HFT-Seite davor */

    private String absoluteLinks(Element element, String baseUri)    {
        Elements links = element.select("a[href]");
        for (Element link : links)  {
            if (!link.attr("href").toLowerCase().startsWith("https://"))    {
                link.attr("href", baseUri+link.attr("href"));
            }
        }
        String t = new String(element.outerHtml().replaceAll("<p>&nbsp;</p>", ""));
        return t;
    }
    /* ----- Funktion für die NewsClickedActivty -----
    *   Sucht nach den Details auf der HfTl-Seite und gibt sie als String-Array zurück
     */

    public String[] getDetailsStringArray(){
        String[] s=new String[6];
        Elements elements = doc.getElementsByClass("news-single-item");



        s[0]=elements.get(0).child(1).text()+"\n";   //Überschrift
        s[1]=elements.get(0).child(2).text()+"\n";   //Subhead
        s[2]=elements.get(0).child(0).text();        //Zeit
        s[3]=absoluteLinks(elements.get(0).child(3), "https://www.hft-leipzig.de/");      //Text
        return s;
    }

}

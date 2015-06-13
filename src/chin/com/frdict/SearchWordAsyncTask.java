package chin.com.frdict;

import java.io.IOException;
import java.util.HashMap;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;

public class SearchWordAsyncTask extends AsyncTask<Void, Void, String> {
    WebView webView;
    String word;
    boolean exceptionOccurred = false;

    // sections that we want to move to the back of the page
    static HashMap<String, Boolean> backSectionsMap;
    static
    {
        backSectionsMap = new HashMap<String, Boolean>();
        backSectionsMap.put("Etymology", false);
        backSectionsMap.put("Etymology 1", false);
        backSectionsMap.put("Etymology 2", false);
        backSectionsMap.put("Pronunciation", false);
    }

    public SearchWordAsyncTask(WebView webView, String word) {
        this.webView = webView;
        this.word = word;
    }

    @Override
    protected String doInBackground(Void... params) {
        String html = null;
        try {
            html = Jsoup.connect("http://en.wiktionary.org/w/index.php?title=" + word + "&printable=yes")
                    .ignoreContentType(true).execute().body();
        }
        catch (HttpStatusException e) {
            exceptionOccurred = true;
            if (e.getStatusCode() == 404) {
                html = "Error getting word from Wiktionary. 404-ed.";
            }
            else {
                html = "Response is not 200, something happened.";
            }
            Log.i(Utility.LogTag, html);
        }
        catch (IOException e) {
            html = "IOException-ed. Check your connection.";
            exceptionOccurred = true;
            e.printStackTrace();
        }
        return html;
    }

    @Override
    protected void onPostExecute(String html) {
        try {
            if (exceptionOccurred) {
                webView.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
                return;
            }

            // parse and do some initial cleaning of the DOM
            Document doc = Jsoup.parse(html);
            Element content = doc.select("#mw-content-text").first();
            content.select("script").remove();               // remove <script> tags
            content.select("noscript").remove();             // remove <noscript> tags
            content.select("#toc").remove();                 // remove the table of content
            removeComments(content);                         // remove comments

            // parse for the content of the French section, if it exist
            Elements children = content.children();
            boolean frenchFound = false;
            boolean frenchEndReached = false;
            boolean isCurrentlyBackSection = false;
            Elements frenchCollection = new Elements();
            Elements backSectionCollection = new Elements();
            for (Element elem : children) {
                if (!frenchFound) {
                    if (elem.tagName().equals("h2") && elem.text().equals("French")) {
                        frenchFound = true;
                    }
                    else {
                        elem.remove();
                    }
                }
                else {
                    if (!elem.tagName().equals("h2")  &&                                          // French, English, etc.
                        !(elem.tagName().equals("h3") && elem.text().equals("External links")) && // remove external links section
                        !frenchEndReached) {
                        // get etylmology, pronunciation, etc. sections so that we can move them to the back
                        // of the page later, instead of having them at the beginning of the page
                        if (isBackSectionHeader(elem)) {
                            backSectionsMap.put(elem.text(), true);
                            isCurrentlyBackSection = true;
                            backSectionCollection.add(elem);
                        }
                        else if (isSubheaders(elem)) {
                            // something other than etymology and pronunciation, etc.
                            if (isCurrentlyBackSection) {
                                isCurrentlyBackSection = false;
                                // TODO: clear the map?
                            }
                            frenchCollection.add(elem);
                        }
                        else if (isCurrentlyBackSection) {
                            backSectionCollection.add(elem);
                        }
                        else {
                            isCurrentlyBackSection = false;
                            frenchCollection.add(elem);
                        }
                    }
                    else {
                        frenchEndReached = true;
                        break;
                    }
                }
            }

            // put pronunciation and etymology sections to the back
            frenchCollection.addAll(backSectionCollection);

            //webView.getSettings().setJavaScriptEnabled(true);
            if (frenchFound) {
                webView.loadDataWithBaseURL("http://en.wiktionary.org/w/", frenchCollection.toString(), "text/html", "UTF-8", "");
            }
            else {
                webView.loadDataWithBaseURL("", "Not a French word.", "text/html", "UTF-8", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSubheaders(Element elem) {
        return elem.tagName().equals("h3") || elem.tagName().equals("h4") || elem.tagName().equals("h5");
    }

    private boolean isBackSectionHeader(Element elem) {
        return isSubheaders(elem) && backSectionsMap.containsKey(elem.text());
    }

    private static void removeComments(Node node) {
        for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment"))
                child.remove();
            else {
                removeComments(child);
                i++;
            }
        }
    }
}
